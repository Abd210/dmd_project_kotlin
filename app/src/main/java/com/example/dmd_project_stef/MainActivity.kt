package com.example.dmd_project_stef

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dmd_project_stef.data.Task
import com.example.dmd_project_stef.databinding.ActivityMainBinding
import com.example.dmd_project_stef.notifications.NotificationHelper
import com.example.dmd_project_stef.service.DataSyncService
import com.example.dmd_project_stef.viewmodel.TaskViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), TaskAdapter.OnItemClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var taskAdapter: TaskAdapter
    private val taskViewModel: TaskViewModel by viewModels()

    // Permission launcher for POST_NOTIFICATIONS
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
            NotificationHelper.createNotificationChannel(this)
        } else {
            Log.d("MainActivity", "Notification permission denied")
            Snackbar.make(binding.root, "Notifications permission denied.", Snackbar.LENGTH_SHORT).show()
        }
    }

    // Bound Service variables
    private var dataSyncService: DataSyncService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as DataSyncService.LocalBinder
            dataSyncService = binder.getService()
            isBound = true
            Log.d("MainActivity", "DataSyncService connected.")
            // Example: Start data synchronization
            dataSyncService?.syncData()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            dataSyncService = null
            Log.d("MainActivity", "DataSyncService disconnected.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)

        // Initialize RecyclerView
        setupRecyclerView()

        // Observe tasks from ViewModel
        taskViewModel.allTasks.observe(this) { tasks ->
            taskAdapter.submitList(tasks)
        }

        // Set up FAB to navigate to AddTaskActivity
        binding.fabAddTask.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivity(intent)
        }

        // Set up Test Notification button
        binding.btnTestNotification.setOnClickListener {
            triggerTestNotification()
        }

        // Handle notification permissions
        handleNotificationPermissions()

        // Bind to DataSyncService
        Intent(this, DataSyncService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStart() {
        super.onStart()
        // If not already bound, bind to the service
        if (!isBound) {
            Intent(this, DataSyncService::class.java).also { intent ->
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Unbind from the service to prevent leaks
        if (isBound) {
            unbindService(connection)
            isBound = false
            Log.d("MainActivity", "DataSyncService unbound.")
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(this, this) // Pass context and listener
        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
            setHasFixedSize(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu) // Ensure menu_main.xml exists
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Navigate to SettingsActivity
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onItemClick(task: Task) {
        // Navigate to AddTaskActivity for editing the selected task
        val intent = Intent(this, AddTaskActivity::class.java).apply {
            putExtra(AddTaskActivity.EXTRA_TASK_ID, task.id)
        }
        startActivity(intent)
    }

    override fun onItemLongClick(task: Task) {
        // Delete the task and show a Snackbar confirmation
        taskViewModel.delete(task)
        Snackbar.make(binding.root, "Task '${task.title}' deleted", Snackbar.LENGTH_SHORT).show()
    }

    override fun onShareClick(task: Task) {
        shareTask(task)
    }

    private fun shareTask(task: Task) {
        val shareText = StringBuilder().apply {
            append("Task Details:\n")
            append("Title: ${task.title}\n")
            append("Description: ${task.description ?: getString(R.string.no_description)}\n")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val deadlineDate = Date(task.deadline)
            append("Deadline: ${sdf.format(deadlineDate)}\n")
            append("Completed: ${if (task.isCompleted) "Yes" else "No"}")
        }.toString()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        // Optionally, set a chooser title using the string resource
        val chooser = Intent.createChooser(shareIntent, getString(R.string.share_task))

        // Verify that there's at least one app to handle the intent
        if (shareIntent.resolveActivity(packageManager) != null) {
            startActivity(chooser)
        } else {
            Snackbar.make(binding.root, "No app available to share the task.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun triggerTestNotification() {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.dmd_project_stef.workers.TaskCheckWorker>()
            .build()
        androidx.work.WorkManager.getInstance(this).enqueue(workRequest)
        Snackbar.make(binding.root, "Test notification triggered.", Snackbar.LENGTH_SHORT).show()
    }

    private fun handleNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    NotificationHelper.createNotificationChannel(this)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Provide rationale and request permission
                    Snackbar.make(binding.root, "Notifications are important for task reminders.", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Allow") {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }.show()
                }
                else -> {
                    // Directly request for permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Permission automatically granted on older Android versions
            NotificationHelper.createNotificationChannel(this)
        }
    }
}
