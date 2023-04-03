package moluccus.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import moluccus.app.R
import moluccus.app.base.AuthActivity
import moluccus.app.base.DetailsActivity
import moluccus.app.databinding.ActivityMainBinding
import moluccus.app.route.CheckHandle
import moluccus.app.service.MyNetworkCallback
import moluccus.app.util.Extensions.toast
import moluccus.app.util.FirebaseUtils
import moluccus.app.util.FirebaseUtils.firebaseUser
import okhttp3.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var TAG = "MainActivity"
    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            askNotificationPermission()
        }
    }

    private lateinit var connectivityManager: ConnectivityManager
    private var networkRequest: NetworkRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkRequest = NetworkRequest.Builder().build()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        connectivityManager.registerNetworkCallback(networkRequest!!, MyNetworkCallback())
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Allow notifications")
                    .setMessage("This app requires permission to display notifications. Do you want to grant permission now?")
                    .setPositiveButton("OK") { dialog, which ->
                        // Directly ask for the permission
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    .setNegativeButton("No thanks") { dialog, which ->
                        // Allow the user to continue without notifications
                    }
                    .show()
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser
        when {
            user != null -> {
                FirebaseUtils.firebaseDatabase.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val value =
                                snapshot.child("users").child(user.uid)
                                    .child("usr_information")
                                    .child("usr_handle").value
                            val check_value = value.toString()
                            if (check_value.isEmpty()) {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        DetailsActivity::class.java
                                    )
                                )
                                finish()
                            } else {
                                FirebaseMessaging.getInstance().token.addOnCompleteListener(
                                    OnCompleteListener { task ->
                                    if (!task.isSuccessful) {
                                        Log.w("TAG", "Fetching FCM registration token failed", task.exception)
                                        return@OnCompleteListener
                                    }
                                    // Get new FCM registration token
                                   // val token = task.result
                                    // Log and toast
                                   // println("TAG Fetching FCM registration token $token")
                                })
                            }
                        } else {
                            toast("incomplete account")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w("TAG", "Failed to read value.", error.toException())
                    }
                })
            }
            else -> {
                startActivity(Intent(this@MainActivity, AuthActivity::class.java))
                finish()
            }
        }

    }

    fun checkAndUpdate(context: Context, apkUrl: String, packageName: String, currentVersionCode: Int) {
        val okHttpClient = OkHttpClient.Builder().build()
        val request = Request.Builder().url(apkUrl).build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle download failure
                Log.e(TAG, "Failed to download APK from $apkUrl", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseCode = response.code()
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    // Handle unsuccessful download
                    Log.e(TAG, "Unsuccessful download attempt from $apkUrl with response code $responseCode")
                    return
                }

                // Get the directory for the app's private files
                val fileDirectory = context.filesDir

                // Create the file to store the APK
                val apkFile = File(fileDirectory, "$packageName.apk")

                // Check if the downloaded APK version is greater than or equal to the current version
                val packageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val currentVersionCode = packageInfo.versionCode
                val apkPackageInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
                val apkVersionCode = apkPackageInfo?.versionCode
                if (apkVersionCode!! <= currentVersionCode) {
                    // The downloaded APK version is not greater than the current version, so we don't need to update
                    return
                } else {
                    // Prompt the user to install the app
                    val builder = AlertDialog.Builder(context)
                    builder.setMessage("An update is available. Would you like to download it now?")
                        .setCancelable(false)
                        .setPositiveButton("Yes") { dialog, id ->
                            // Write the APK to the file
                            val inputStream = response.body()?.byteStream()
                            inputStream?.use { input ->
                                FileOutputStream(apkFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            // Install the APK
                            install(context, apkFile, packageName)
                        }
                        .setNegativeButton("No") { dialog, id ->
                            // User cancelled the dialog
                        }
                    val alert = builder.create()
                    alert.show()
                }
            }
        })
    }

    fun install(context: Context, apkFile: File, packageName: String) {
        val packageInstaller = context.packageManager.packageInstaller
        val packageParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(packageParams)
        val session = packageInstaller.openSession(sessionId)
        val out = session.openWrite(packageName, 0, -1)
        val input = FileInputStream(apkFile)
        input.copyTo(out)
        session.fsync(out)
        input.close()
        out.close()
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }




    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}