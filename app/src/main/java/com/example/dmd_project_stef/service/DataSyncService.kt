package com.example.dmd_project_stef.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.dmd_project_stef.data.TaskDao
import com.example.dmd_project_stef.data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DataSyncService : Service() {

    // Binder given to clients
    private val binder = LocalBinder()

    // Coroutine scope for background tasks
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // Database DAO
    private lateinit var taskDao: TaskDao

    inner class LocalBinder : Binder() {
        fun getService(): DataSyncService = this@DataSyncService
    }

    override fun onCreate() {
        super.onCreate()
        val database = TaskDatabase.getDatabase(applicationContext)
        taskDao = database.taskDao()
        Log.d("DataSyncService", "Service created and DAO initialized.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("DataSyncService", "Service bound.")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("DataSyncService", "Service unbound.")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DataSyncService", "Service destroyed.")
    }

    // Example method for syncing data
    fun syncData() {
        serviceScope.launch {
            // Implement your data synchronization logic here
            Log.d("DataSyncService", "Data synchronization started.")
            // For demonstration, let's just log a message
            // Replace this with actual sync operations
            Thread.sleep(2000) // Simulate long-running task
            Log.d("DataSyncService", "Data synchronization completed.")
        }
    }
}
