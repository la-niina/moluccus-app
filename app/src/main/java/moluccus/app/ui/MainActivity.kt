package moluccus.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import moluccus.app.BuildConfig
import moluccus.app.R
import moluccus.app.base.AuthActivity
import moluccus.app.base.DetailsActivity
import moluccus.app.databinding.ActivityMainBinding
import moluccus.app.service.MyNetworkCallback
import moluccus.app.util.Extensions.toast
import moluccus.app.util.FirebaseUtils
import okhttp3.*
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var installingDialog: Dialog
    private lateinit var receiver: BroadcastReceiver

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

        checkAndUpdate(this@MainActivity, "", BuildConfig.APPLICATION_ID)
    }

    private fun checkAndUpdate(context: Context, repositoryUrl: String, packageName: String) {
        val latestReleaseUrl = getLatestReleaseUrl(repositoryUrl)
        val currentVersionCode = getCurrentVersionCode(context, packageName)
        val latestVersionCode = getLatestVersionCode(latestReleaseUrl)

        if (latestVersionCode > currentVersionCode) {
            showUpdateDialog(context, latestReleaseUrl)
        }
    }

    private fun getLatestReleaseUrl(repositoryUrl: String): String {
        val httpClient = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repositoryUrl/releases/latest")
            .build()
        val response = httpClient.newCall(request).execute()
        val responseBodyString = response.body()?.string() ?: ""
        val json = JSONObject(responseBodyString)
        return json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")
    }

    private fun getCurrentVersionCode(context: Context, packageName: String): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
        } else {
            return context.packageManager.getPackageInfo(packageName, 0).versionCode
        }
    }

    private fun getLatestVersionCode(latestReleaseUrl: String): Int {
        val httpClient = OkHttpClient()
        val request = Request.Builder()
            .url(latestReleaseUrl)
            .build()
        val response = httpClient.newCall(request).execute()
        val fileSize = response.header("Content-Length")?.toLong()
        val versionCode = fileSize?.toInt() ?: 0
        return versionCode
    }

    private fun showUpdateDialog(context: Context, latestReleaseUrl: String) {
        AlertDialog.Builder(context)
            .setTitle("Update available")
            .setMessage("A new version of Moluccus is available. Do you want to download and install it now?")
            .setPositiveButton("Download") { _, _ ->
                downloadApk(context, latestReleaseUrl)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun downloadApk(context: Context, apkUrl: String) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Moluccus")
            .setDescription("Downloading Moluccus update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "Moluccus.apk")
        downloadManager.enqueue(request)
        val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Moluccus.apk")
        installApk(context, apkFile, packageName)
    }

    private fun installApk(context: Context, apkFile: File, packageName: String) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${packageName}.fileprovider", apkFile)
        } else {
            Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Show installing dialog
        showInstallingDialog()

        context.startActivity(intent)
    }

    private fun showInstallingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.installing_apk_dialog, null)
        installingDialog = Dialog(this).apply {
            setContentView(dialogView)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }

        // Register broadcast receiver
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    installingDialog.dismiss()
                    unregisterReceiver(this)
                }
            }
        }
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}