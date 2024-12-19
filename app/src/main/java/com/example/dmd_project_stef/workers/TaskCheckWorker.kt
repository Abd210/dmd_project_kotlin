// TaskCheckWorker.kt
package com.example.dmd_project_stef.workers

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class TaskCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        Log.d("TaskCheckWorker", "Worker started.")

        // Ensure the notification channel is created
        NotificationHelper.createNotificationChannel(applicationContext)

        return try {
            // Perform the task synchronously
            runBlocking {
                checkTasksAndNotify()
            }
            Log.d("TaskCheckWorker", "Worker completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e("TaskCheckWorker", "Worker failed with exception: ${e.message}")
            Result.failure()
        }
    }

    private suspend fun checkTasksAndNotify() {
        val taskDao = TaskDatabase.getDatabase(applicationContext).taskDao()
        val tasks = taskDao.getAllTasksSync()
        Log.d("TaskCheckWorker", "Fetched tasks for notification: ${tasks.size}")

        val currentTime = System.currentTimeMillis()
        tasks.forEach { task ->
            if (!task.isCompleted) {
                when {
                    task.deadline <= currentTime -> {
                        Log.d("TaskCheckWorker", "Task overdue: ${task.title}")
                        sendNotification(task, overdue = true)
                    }
                    task.deadline - currentTime <= TimeUnit.HOURS.toMillis(1) -> {
                        Log.d("TaskCheckWorker", "Task due soon: ${task.title}")
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
                Log.w("TaskCheckWorker", "POST_NOTIFICATIONS permission not granted. Cannot send notification.")
                return
            }
        }

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("task_id", task.id)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, task.id, intent, pendingIntentFlags
        )

        val title = if (overdue) "Task Overdue" else "Task Due Soon"
        val content = if (overdue) "Task '${task.title}' is overdue!" else "Task '${task.title}' is due within the next hour."

        Log.d("TaskCheckWorker", "Sending notification for task: ${task.title}, overdue: $overdue")

        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.REMINDER_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_reminder) // Ensure this icon exists and is valid
            //.setSmallIcon(android.R.drawable.ic_dialog_info) // Temporary icon for testing
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(task.id, notification)
    }
}
