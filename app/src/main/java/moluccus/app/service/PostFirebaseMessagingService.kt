package moluccus.app.service

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import moluccus.app.R
import moluccus.app.ui.MainActivity

class PostFirebaseMessagingService : FirebaseMessagingService() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        when (remoteMessage.data[remoteMessage.messageId]) {
            "current_uid" -> {
                val current_uid = remoteMessage.data["current_uid"]
                val user_profile = remoteMessage.data["user_profile"]
                val user_handle = remoteMessage.data["user_handle"]
                val user_name = remoteMessage.data["user_name"]
                val message_content = remoteMessage.data["message_content"]

                showNewBlogPostNotification(
                    current_uid,
                    user_profile,
                    message_content,
                    user_handle,
                    user_name
                )
            }
            // Handle other actions here
            else -> {
                // Do nothing
            }
        }
    }

    override fun onNewToken(token: String) {
        // sendRegistrationToServer(token)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showNewBlogPostNotification(
        current_uid: String?,
        user_profile: String?,
        message_content: String?,
        user_handle: String?,
        user_name: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        println("-------------------------------- Working Notification")
        // Download the image and convert it to a Bitmap
        Glide.with(this).asBitmap().load(user_profile).into(object : CustomTarget<Bitmap>() {
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                val builder = NotificationCompat.Builder(
                    this@PostFirebaseMessagingService,
                    current_uid.toString()
                )
                    .setSmallIcon(R.mipmap.ic_launcher) // Use a default icon if downloading the image fails
                    .setLargeIcon(resource)
                    .setContentTitle("$user_name : $user_handle")
                    .setContentText(message_content)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                with(NotificationManagerCompat.from(this@PostFirebaseMessagingService)) {
                    if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                        return
                    } else if (shouldShowRequestPermissionRationale(applicationContext as MainActivity, Manifest.permission.POST_NOTIFICATIONS)) {
                        val builder = AlertDialog.Builder(applicationContext)
                        builder.setTitle("Allow notifications")
                            .setMessage("This app requires permission to display notifications. Do you want to grant permission now?")
                            .setPositiveButton("OK") { dialog, which ->
                                // Directly ask for the permission
                                MainActivity().requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            .setNegativeButton("No thanks") { dialog, which ->
                                // Allow the user to continue without notifications
                            }
                            .show()
                    } else {
                        // Directly ask for the permission
                        MainActivity().requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    notify(current_uid!!.toInt(), builder.build())
                }
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                // Do nothing
            }
        })
    }
}


