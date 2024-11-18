package com.usaclean.frenchconnectionuser.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.activities.DashboardActivity
import java.util.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.notification?.let {
            val title = it.title
            val message = it.body
            val type = remoteMessage.data["status"]
            showNotification(title, message, type)
        }
    }

    private fun showNotification(title: String?, message: String?, type: String?) {
        val soundUri =
            Uri.parse("android.resource://" + packageName + "/" + R.raw.driver_accept_notification)


        val notificationBuilder = NotificationCompat.Builder(this, "YOUR_CHANNEL_ID")
            .setSmallIcon(R.drawable.main_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(soundUri)

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}