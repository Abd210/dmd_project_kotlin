//package com.example.dmd_project_stef.workers
//
//import android.content.Context
//import androidx.work.Worker
//import androidx.work.WorkerParameters
//import com.example.dmd_project_stef.data.TaskDatabase
//import com.example.dmd_project_stef.notifications.NotificationHelper
//import kotlinx.coroutines.runBlocking
//
//class ReminderWorker(appContext: Context, workerParams: WorkerParameters) :
//    Worker(appContext, workerParams) {
//
//    override fun doWork(): Result {
//        return runBlocking {
//            try {
//                // Fetch pending tasks from the database
//                val taskDao = TaskDatabase.getDatabase(applicationContext).taskDao()
//                val pendingTasks = taskDao.getPendingTasks()
//
//                if (pendingTasks.isNotEmpty()) {
//                    // Send notification with pending tasks
//                    NotificationHelper.sendTaskReminderNotification(applicationContext, pendingTasks)
//                }
//
//                Result.success()
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Result.retry()
//            }
//        }
//    }
//}
