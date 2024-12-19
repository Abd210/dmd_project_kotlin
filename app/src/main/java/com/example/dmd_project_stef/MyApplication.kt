package com.example.dmd_project_stef

import android.app.Application
import androidx.work.*
import com.example.dmd_project_stef.workers.TaskCheckWorker
import java.util.concurrent.TimeUnit

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleTaskCheckWorker()
    }

    private fun scheduleTaskCheckWorker() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val taskCheckRequest = PeriodicWorkRequestBuilder<TaskCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TaskCheckWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            taskCheckRequest
        )
    }
}
