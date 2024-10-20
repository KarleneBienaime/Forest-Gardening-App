package com.example.forestgardeningapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(it.title ?: "New Message", it.body ?: "You have a new message")
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Save the token locally
        saveTokenToPreferences(token)
    }

    private fun showNotification(title: String, content: String) {
        val channelId = "FOREST_GARDENING_CHANNEL"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications) // Make sure this resource exists
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)

        // Create the notification channel (required for Android Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Forest Gardening Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for the Forest Gardening App"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Show the notification
        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "Error showing notification: ${e.message}")
        }
    }

    private fun saveTokenToPreferences(token: String) {
        getSharedPreferences("FCM", Context.MODE_PRIVATE).edit().apply {
            putString("token", token)
            apply()
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"

        // Function to retrieve the token from SharedPreferences
        fun getTokenFromPreferences(context: Context): String? {
            return context.getSharedPreferences("FCM", Context.MODE_PRIVATE)
                .getString("token", null)
        }
    }
}