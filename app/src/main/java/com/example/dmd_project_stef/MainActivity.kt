package com.example.dmd_project_stef

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dmd_project_stef.data.Task
import com.example.dmd_project_stef.databinding.ActivityMainBinding
import com.example.dmd_project_stef.notifications.NotificationHelper
import com.example.dmd_project_stef.viewmodel.TaskViewModel
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), TaskAdapter.OnItemClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var taskAdapter: TaskAdapter
    private val taskViewModel: TaskViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
            NotificationHelper.createNotificationChannel(this)
        } else {
            // Permission denied
            Snackbar.make(binding.root, "Notifications permission denied.", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Create notification channel if permission is granted
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

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(this)
        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
            setHasFixedSize(true)
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
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
}
