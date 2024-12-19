//ReminderService.kt
package com.example.dmd_project_stef.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.dmd_project_stef.MainActivity
import com.example.dmd_project_stef.R
import com.example.dmd_project_stef.data.Task
import com.example.dmd_project_stef.data.TaskDatabase
import com.example.dmd_project_stef.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ReminderService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d("ReminderService", "Service created.")
        NotificationHelper.createNotificationChannel(this)
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ReminderService", "Service started.")
        serviceScope.launch {
            checkTasksAndNotify()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, NotificationHelper.REMINDER_CHANNEL_ID)
            .setContentTitle("Smart Reminder Service")
            .setContentText("Monitoring your tasks for deadlines.")
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(1, notification)
    }

    private suspend fun checkTasksAndNotify() {
        val taskDao = TaskDatabase.getDatabase(applicationContext).taskDao()
        val tasks = taskDao.getAllTasksSync()
        Log.d("ReminderService", "Fetched tasks: ${tasks.size}")

        val currentTime = System.currentTimeMillis()
        tasks.forEach { task: Task ->
            if (!task.isCompleted) {
                when {
                    task.deadline <= currentTime -> {
                        Log.d("ReminderService", "Task overdue: ${task.title}")
                        sendNotification(task, overdue = true)
                    }
                    task.deadline - currentTime <= TimeUnit.HOURS.toMillis(1) -> {
                        Log.d("ReminderService", "Task due soon: ${task.title}")
                        sendNotification(task, overdue = false)
                    }
                }
            }
        }
    }

    private fun sendNotification(task: Task, overdue: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("ReminderService", "POST_NOTIFICATIONS permission not granted.")
                return
            }
        }

        val notificationManager = NotificationManagerCompat.from(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("task_id", task.id)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, task.id, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (overdue) "Task Overdue" else "Task Due Soon"
        val content = if (overdue) "Task '${task.title}' is overdue!" else "Task '${task.title}' is due within the next hour."

        Log.d("ReminderService", "Sending notification for task: ${task.title}, overdue: $overdue")

        val notification = NotificationCompat.Builder(this, NotificationHelper.REMINDER_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(task.id, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
