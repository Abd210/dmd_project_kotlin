package com.example.dmd_project_stef.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.example.dmd_project_stef.workers.TaskCheckWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleTaskCheckWorker(context)
        }
    }

    private fun scheduleTaskCheckWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val taskCheckRequest = PeriodicWorkRequestBuilder<TaskCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "TaskCheckWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            taskCheckRequest
        )
    }
}
