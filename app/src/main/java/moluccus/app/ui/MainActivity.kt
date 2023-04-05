package moluccus.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import moluccus.app.R
import moluccus.app.base.AuthActivity
import moluccus.app.base.DetailsActivity
import moluccus.app.databinding.ActivityMainBinding
import moluccus.app.devops.MyGLSurfaceView
import moluccus.app.util.Extensions.toast
import moluccus.app.util.FirebaseUtils
import moluccus.app.util.users

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            askNotificationPermission()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        MyGLSurfaceView(this@MainActivity)
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
                    .setPositiveButton("OK") { _, _ ->
                        // Directly ask for the permission
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    .setNegativeButton("No thanks") { _, _ ->
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
                            val value = snapshot.child("users").child(user.uid).getValue(users::class.java)
                            val checkValue = value?.usr_information

                            if (checkValue == null) {
                                startActivity(Intent(this@MainActivity, DetailsActivity::class.java))
                                finish()
                            } else if (checkValue.usr_name.isNullOrEmpty() || checkValue.usr_handle.isNullOrEmpty()) {
                                startActivity(Intent(this@MainActivity, DetailsActivity::class.java))
                                finish()
                            } else {
                               //
                            }
                        } else {
                            startActivity(Intent(this@MainActivity, AuthActivity::class.java))
                            finish()
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

    override fun onResume() {
        super.onResume()
        val user: FirebaseUser? = FirebaseUtils.firebaseAuth.currentUser
        when {
            user != null -> {
                FirebaseUtils.firebaseDatabase.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val value = snapshot.child("users").child(user.uid).getValue(users::class.java)
                            val checkValue = value?.usr_information

                            if (checkValue == null) {
                                startActivity(Intent(this@MainActivity, DetailsActivity::class.java))
                                finish()
                            } else if (checkValue.usr_name.isNullOrEmpty() || checkValue.usr_handle.isNullOrEmpty()) {
                                startActivity(Intent(this@MainActivity, DetailsActivity::class.java))
                                finish()
                            } else {
                                //
                            }
                        } else {
                            startActivity(Intent(this@MainActivity, AuthActivity::class.java))
                            finish()
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

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}