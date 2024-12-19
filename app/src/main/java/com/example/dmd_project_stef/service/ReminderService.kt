package com.example.dmd_project_stef.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder // Added import for IBinder
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
        NotificationHelper.createNotificationChannel(this)
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        val tasks = taskDao.getAllTasks().value ?: return

        val currentTime = System.currentTimeMillis()

        tasks.forEach { task ->
            if (!task.isCompleted) {
                if (task.deadline <= currentTime) {
                    // Task is overdue
                    sendNotification(task, overdue = true)
                } else {
                    // Task is due soon (e.g., within the next hour)
                    val oneHourInMillis = TimeUnit.HOURS.toMillis(1)
                    if (task.deadline - currentTime <= oneHourInMillis) {
                        sendNotification(task, overdue = false)
                    }
                }
            }
        }
    }

    private fun sendNotification(task: Task, overdue: Boolean) {
        // Check for POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, do not send notification
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

    override fun onBind(intent: Intent?): IBinder? = null // Ensured IBinder is properly referenced
}
