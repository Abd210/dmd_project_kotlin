package com.example.dmd_project_stef.data

import androidx.lifecycle.LiveData
import androidx.room.*
import android.database.Cursor

@Dao
interface TaskDao {
    // Existing methods
    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    fun getAllTasks(): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task): Int

    @Delete
    suspend fun delete(task: Task): Int

    // Content Provider methods
    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    fun getAllTasksCursor(): Cursor

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun getTaskByIdCursor(taskId: Int): Cursor

    @Query("DELETE FROM tasks")
    suspend fun deleteAll(): Int

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Int): Int

    // Ensure this method exists
    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    suspend fun getAllTasksSync(): List<Task>
}
