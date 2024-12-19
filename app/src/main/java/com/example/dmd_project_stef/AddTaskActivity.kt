//AddTaskActivity.kt
package com.example.dmd_project_stef

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.dmd_project_stef.data.Task
import com.example.dmd_project_stef.databinding.ActivityAddTaskBinding
import com.example.dmd_project_stef.viewmodel.TaskViewModel
import java.util.*

class AddTaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK_ID = "com.example.dmd_project_stef.TASK_ID"
    }

    private lateinit var binding: ActivityAddTaskBinding
    private val taskViewModel: TaskViewModel by viewModels()
    private var taskId: Int? = null
    private var selectedDeadline: Long = System.currentTimeMillis()
    private var isDeadlineDateSet: Boolean = false
    private var isDeadlineTimeSet: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Check if editing an existing task
        taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId != -1) {
            loadTask(taskId!!)
        }

        // Set up DatePickerDialog
        binding.btnSelectDate.setOnClickListener {
            showDatePickerDialog()
        }

        // Set up TimePickerDialog
        binding.btnSelectTime.setOnClickListener {
            if (isDeadlineDateSet) {
                showTimePickerDialog()
            } else {
                Toast.makeText(this, "Please select the deadline date first.", Toast.LENGTH_SHORT).show()
            }
        }

        // Save Task button
        binding.btnSaveTask.setOnClickListener {
            saveTask()
        }
    }

    private fun loadTask(taskId: Int) {
        taskViewModel.getTaskById(taskId).observe(this, { task ->
            task?.let {
                binding.etTaskTitle.setText(it.title)
                binding.etTaskDescription.setText(it.description)
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = it.deadline
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                selectedDeadline = it.deadline
                isDeadlineDateSet = true
                isDeadlineTimeSet = true

                binding.tvSelectedDate.text = "Selected Date: $year-${month + 1}-$day"
                binding.tvSelectedTime.text = "Selected Time: ${String.format("%02d:%02d", hour, minute)}"
            }
        })
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // Update the calendar with the selected date
            calendar.set(selectedYear, selectedMonth, selectedDay)
            isDeadlineDateSet = true

            // Update the selectedDeadline with the new date while preserving the time
            val currentTime = selectedDeadline
            val currentCalendar = Calendar.getInstance()
            currentCalendar.timeInMillis = currentTime
            val hour = currentCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = currentCalendar.get(Calendar.MINUTE)
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            selectedDeadline = calendar.timeInMillis

            // Update UI to show selected date
            binding.tvSelectedDate.text = "Selected Date: $selectedYear-${selectedMonth + 1}-$selectedDay"

            // If time was previously set, update the selectedDeadline accordingly
            if (isDeadlineTimeSet) {
                binding.tvSelectedTime.text = binding.tvSelectedTime.text.toString() // Keep existing time display
            }
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        // Set the calendar to the current selectedDeadline
        calendar.timeInMillis = selectedDeadline
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            // Update the calendar with the selected time
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            isDeadlineTimeSet = true
            selectedDeadline = calendar.timeInMillis

            // Update UI to show selected time
            binding.tvSelectedTime.text = "Selected Time: ${String.format("%02d:%02d", selectedHour, selectedMinute)}"
        }, hour, minute, true) // true for 24-hour format

        timePickerDialog.show()
    }

    private fun saveTask() {
        val title = binding.etTaskTitle.text.toString().trim()
        val description = binding.etTaskDescription.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isDeadlineDateSet || !isDeadlineTimeSet) {
            Toast.makeText(this, "Please select both deadline date and time", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDeadline < System.currentTimeMillis()) {
            Toast.makeText(this, "Deadline cannot be in the past.", Toast.LENGTH_SHORT).show()
            return
        }

        val task = if (taskId != null && taskId != -1) {
            Task(
                id = taskId!!,
                title = title,
                description = description,
                deadline = selectedDeadline,
                isCompleted = false // Consider preserving this if editing
            )
        } else {
            Task(
                title = title,
                description = description,
                deadline = selectedDeadline
            )
        }

        if (taskId != null && taskId != -1) {
            taskViewModel.update(task)
            Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show()
        } else {
            taskViewModel.insert(task)
            Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // Optional: Handle configuration changes to preserve state
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("selectedDeadline", selectedDeadline)
        outState.putBoolean("isDeadlineDateSet", isDeadlineDateSet)
        outState.putBoolean("isDeadlineTimeSet", isDeadlineTimeSet)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        selectedDeadline = savedInstanceState.getLong("selectedDeadline", System.currentTimeMillis())
        isDeadlineDateSet = savedInstanceState.getBoolean("isDeadlineDateSet", false)
        isDeadlineTimeSet = savedInstanceState.getBoolean("isDeadlineTimeSet", false)

        if (isDeadlineDateSet) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDeadline
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            binding.tvSelectedDate.text = "Selected Date: $year-${month + 1}-$day"
        }

        if (isDeadlineTimeSet) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDeadline
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            binding.tvSelectedTime.text = "Selected Time: ${String.format("%02d:%02d", hour, minute)}"
        }
    }
}
