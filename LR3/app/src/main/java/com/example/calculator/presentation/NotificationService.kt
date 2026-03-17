package com.example.calculator.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.calculator.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
class NotificationService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "NotificationService"
        private const val CHANNEL_ID = "calculator_channel"
        private const val CHANNEL_NAME = "Calculator Notifications"
        private const val NOTIFICATION_ID = 1001
        var fcmToken: String? = null
            private set
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        remoteMessage.data.let { data ->
            if (data.isNotEmpty()) {
                Log.d(TAG, "Data payload: $data")

                when (data["type"]) {
                    "NEW_CALCULATION" -> {
                        showCalculationNotification(
                            title = "New Calculation",
                            message = "${data["expression"]} = ${data["result"]}"
                        )
                    }
                    "THEME_UPDATE" -> {
                        showThemeUpdateNotification(
                            title = "Theme Updated",
                            message = data["message"] ?: "App theme has been updated"
                        )
                    }
                    "HISTORY_CLEARED" -> {
                        showHistoryClearedNotification(
                            title = "History Cleared",
                            message = "Your calculation history has been cleared"
                        )
                    }
                }
            }
        }

        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification Body: ${notification.body}")
            showNotification(
                title = notification.title ?: "Calculator",
                message = notification.body ?: "New message"
            )
        }
    }

    private fun showCalculationNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "calculation")
        }
        showNotification(title, message, intent)
    }

    private fun showThemeUpdateNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "theme_update")
        }
        showNotification(title, message, intent)
    }

    private fun showHistoryClearedNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "history_cleared")
        }
        showNotification(title, message, intent)
    }

    private fun showNotification(title: String, message: String, intent: Intent? = null) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for calculator notifications"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        fcmToken = token

        saveTokenToPreferences(token)
    }

    private fun saveTokenToPreferences(token: String) {
        val prefs = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }
}