// MyApplication.kt
package com.example.dmd_project_stef

import android.app.Application
import android.util.Log
import androidx.work.*
import com.example.dmd_project_stef.notifications.NotificationHelper
import com.example.dmd_project_stef.workers.TaskCheckWorker
import java.util.concurrent.TimeUnit

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("MyApplication", "App created, initializing notification channel and scheduling TaskCheckWorker.")

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Schedule TaskCheckWorker
        scheduleTaskCheckWorker()
    }

    private fun scheduleTaskCheckWorker() {
        // Remove battery constraint for testing
        val constraints = Constraints.Builder()
            //.setRequiresBatteryNotLow(true) // Temporarily remove this constraint
            .build()

        // Periodic work every 15 minutes (minimum interval)
        val taskCheckPeriodicRequest = PeriodicWorkRequestBuilder<TaskCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TaskCheckWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            taskCheckPeriodicRequest
        )

        // One-time immediate work for testing
        val taskCheckImmediateRequest = OneTimeWorkRequestBuilder<TaskCheckWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(taskCheckImmediateRequest)

        Log.d("MyApplication", "TaskCheckWorker scheduled.")
    }
}
