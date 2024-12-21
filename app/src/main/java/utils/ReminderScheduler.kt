//package com.example.dmd_project_stef.utils
//
//import android.content.Context
//import androidx.work.*
//import com.example.dmd_project_stef.workers.ReminderWorker
//import java.util.concurrent.TimeUnit
//
//object ReminderScheduler {
//    private const val REMINDER_WORK_NAME = "task_reminder_work"
//
//    fun scheduleTaskReminder(context: Context, intervalHours: Long) {
//        // Define constraints (optional)
//        val constraints = Constraints.Builder()
//            .setRequiresBatteryNotLow(true)
//            .build()
//
//        // Create PeriodicWorkRequest
//        val reminderWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
//            intervalHours,
//            TimeUnit.HOURS
//        )
//            .setConstraints(constraints)
//            .build()
//
//        // Enqueue unique periodic work
//        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//            REMINDER_WORK_NAME,
//            ExistingPeriodicWorkPolicy.REPLACE,
//            reminderWorkRequest
//        )
//    }
//
//    fun cancelTaskReminder(context: Context) {
//        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_NAME)
//    }
//}
