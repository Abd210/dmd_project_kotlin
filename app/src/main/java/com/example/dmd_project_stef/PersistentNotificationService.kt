//package com.example.dmd_project_stef.service
//
//import android.Manifest
//import android.app.Notification
//import android.app.PendingIntent
//import android.app.Service
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.IBinder
//import android.util.Log
//import androidx.core.app.ActivityCompat
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import com.example.dmd_project_stef.R
//import com.example.dmd_project_stef.data.Task
//import com.example.dmd_project_stef.data.TaskDatabase
//import com.example.dmd_project_stef.notifications.NotificationHelper
//import com.example.dmd_project_stef.ui.MainActivity
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.launch
//import java.util.concurrent.TimeUnit
//
//class PersistentNotificationService : Service() {
//
//    private val serviceJob = Job()
//    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
//
//    override fun onCreate() {
//        super.onCreate()
//        NotificationHelper.createNotificationChannel(this)
//        startForegroundServiceWithNotification()
//        monitorUpcomingReminders()
//        Log.d("PersistentService", "Service created and monitoring started.")
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        // Service is already started in onCreate()
//        return START_STICKY
//    }
//
//    private fun startForegroundServiceWithNotification() {
//        val notificationIntent = Intent(this, MainActivity::class.java)
//        val pendingIntent: PendingIntent = PendingIntent.getActivity(
//            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notification: Notification = NotificationCompat.Builder(this, NotificationHelper.REMINDER_CHANNEL_ID)
//            .setContentTitle("Smart Reminder Active")
//            .setContentText("Fetching upcoming reminders...")
//            .setSmallIcon(R.drawable.ic_reminder) // Ensure this icon exists
//            .setContentIntent(pendingIntent)
//            .setOngoing(true) // Makes the notification persistent
//            .setPriority(NotificationCompat.PRIORITY_LOW) // Lower priority to minimize interruption
//            .build()
//
//        startForeground(2, notification)
//        Log.d("PersistentService", "Foreground service started with persistent notification.")
//    }
//
//    private fun monitorUpcomingReminders() {
//        serviceScope.launch {
//            while (true) {
//                updatePersistentNotification()
//                // Check every minute
//                kotlinx.coroutines.delay(TimeUnit.MINUTES.toMillis(1))
//            }
//        }
//    }
//
//    private suspend fun updatePersistentNotification() {
//        val taskDao = TaskDatabase.getDatabase(applicationContext).taskDao()
//        val tasks: List<Task> = taskDao.getAllTasksSync()
//            .filter { !it.isCompleted && it.deadline > System.currentTimeMillis() }
//
//        if (tasks.isEmpty()) {
//            // Update notification to show no upcoming reminders
//            updateNotification("No upcoming reminders.")
//            Log.d("PersistentService", "No upcoming reminders to display.")
//        } else {
//            // Sort tasks by deadline
//            val sortedTasks = tasks.sortedBy { it.deadline }
//            val nextTask = sortedTasks.first()
//            val currentTime = System.currentTimeMillis()
//            val timeUntil = nextTask.deadline - currentTime
//            val minutesUntil = TimeUnit.MILLISECONDS.toMinutes(timeUntil)
//
//            val contentText = if (minutesUntil > 0) {
//                "Next reminder: '${nextTask.title}' in $minutesUntil minutes."
//            } else {
//                "Next reminder: '${nextTask.title}' is due now."
//            }
//
//            updateNotification(contentText)
//            Log.d("PersistentService", "Updated notification: $contentText")
//        }
//    }
//
//    private fun updateNotification(contentText: String) {
//        // Check for POST_NOTIFICATIONS permission on Android 13+
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//                Log.w("PersistentService", "POST_NOTIFICATIONS permission not granted. Cannot update notification.")
//                return
//            }
//        }
//
//        val notificationManager = NotificationManagerCompat.from(this)
//
//        val notification: Notification = NotificationCompat.Builder(this, NotificationHelper.REMINDER_CHANNEL_ID)
//            .setContentTitle("Smart Reminder Active")
//            .setContentText(contentText)
//            .setSmallIcon(R.drawable.ic_reminder) // Ensure this icon exists
//            .setOngoing(true)
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .build()
//
//        try {
//            notificationManager.notify(2, notification)
//            Log.d("PersistentService", "Notification updated successfully.")
//        } catch (e: SecurityException) {
//            Log.e("PersistentService", "Failed to update notification: ${e.message}")
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        serviceJob.cancel() // Correctly cancel the Job to stop coroutines
//        Log.d("PersistentService", "Service destroyed and monitoring stopped.")
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//}
