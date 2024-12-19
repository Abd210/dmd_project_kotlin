// NotificationHelper.kt
package com.example.dmd_project_stef.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

object NotificationHelper {

    const val REMINDER_CHANNEL_ID = "reminder_channel"
    const val REMINDER_CHANNEL_NAME = "Task Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for task reminder notifications"
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d("NotificationHelper", "Notification channel created: $REMINDER_CHANNEL_ID")
        }
    }
}
