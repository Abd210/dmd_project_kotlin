//package com.example.dmd_project_stef.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.example.dmd_project_stef.repository.TaskRepository
//
//class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return TaskViewModel(repository) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}
