//TaskViewModel.kt
package com.example.dmd_project_stef.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.dmd_project_stef.data.Task
import com.example.dmd_project_stef.data.TaskDatabase
import com.example.dmd_project_stef.data.TaskRepository
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    val allTasks: LiveData<List<Task>>

    init {
        val taskDao = TaskDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)
        allTasks = repository.allTasks
    }

    fun insert(task: Task) = viewModelScope.launch {
        Log.d("TaskViewModel", "Inserting task: ${task.title}")
        repository.insert(task)
    }

    fun update(task: Task) = viewModelScope.launch {
        Log.d("TaskViewModel", "Updating task: ${task.title}")
        repository.update(task)
    }

    fun delete(task: Task) = viewModelScope.launch {
        Log.d("TaskViewModel", "Deleting task: ${task.title}")
        repository.delete(task)
    }


    fun getTaskById(taskId: Int): LiveData<Task?> {
        val taskLiveData = androidx.lifecycle.MutableLiveData<Task?>()
        viewModelScope.launch {
            taskLiveData.postValue(repository.getTaskById(taskId))
        }
        return taskLiveData
    }
}
