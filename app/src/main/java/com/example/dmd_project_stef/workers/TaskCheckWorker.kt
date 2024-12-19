package com.example.dmd_project_stef.workers

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.dmd_project_stef.MainActivity
import com.example.dmd_project_stef.R
import com.example.dmd_project_stef.data.Task
import com.example.dmd_project_stef.data.TaskDatabase
import com.example.dmd_project_stef.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TaskCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun doWork(): Result {
        serviceScope.launch {
            checkTasksAndNotify()
        }
        // Indicate whether the work finished successfully with the Result
        return Result.success()
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
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, do not send notification
                return
            }
        }

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("task_id", task.id)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, task.id, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (overdue) "Task Overdue" else "Task Due Soon"
        val content = if (overdue) "Task '${task.title}' is overdue!" else "Task '${task.title}' is due within the next hour."

        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.REMINDER_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(task.id, notification)
    }
}
