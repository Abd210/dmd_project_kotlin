package com.example.dmd_project_stef.data

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.room.Room
import com.example.dmd_project_stef.data.TaskContract.TaskEntry
import com.example.dmd_project_stef.data.TaskDatabase
import kotlinx.coroutines.runBlocking

class TaskContentProvider : ContentProvider() {

    companion object {
        private const val TASKS = 100
        private const val TASK_ID = 101

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(TaskContract.AUTHORITY, TaskContract.PATH_TASKS, TASKS)
            addURI(TaskContract.AUTHORITY, "${TaskContract.PATH_TASKS}/#", TASK_ID)
        }

        // Singleton instance of TaskDatabase
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getDatabase(context: android.content.Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    override fun onCreate(): Boolean {
        // Initialize the Room database
        getDatabase(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val taskDao = getDatabase(context!!).taskDao()
        val cursor: Cursor?

        when (uriMatcher.match(uri)) {
            TASKS -> {
                cursor = taskDao.getAllTasksCursor()
            }
            TASK_ID -> {
                val id = ContentUris.parseId(uri).toInt()
                cursor = taskDao.getTaskByIdCursor(id)
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        // Set notification URI on the Cursor
        cursor?.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            TASKS -> TaskEntry.CONTENT_LIST_TYPE
            TASK_ID -> TaskEntry.CONTENT_ITEM_TYPE
            else -> throw IllegalStateException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != TASKS) {
            throw IllegalArgumentException("Insertion is not supported for URI: $uri")
        }

        values?.let {
            val taskDao = getDatabase(context!!).taskDao()
            val task = convertContentValuesToTask(it)

            // Execute the suspend function within runBlocking
            val id = runBlocking { taskDao.insert(task) }

            // Notify listeners of the change
            context?.contentResolver?.notifyChange(uri, null)

            // Return the new URI with the appended ID
            return ContentUris.withAppendedId(uri, id)
        } ?: throw IllegalArgumentException("ContentValues cannot be null")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val taskDao = getDatabase(context!!).taskDao()
        var rowsDeleted: Int

        when (uriMatcher.match(uri)) {
            TASKS -> {
                // Execute the suspend function within runBlocking
                rowsDeleted = runBlocking { taskDao.deleteAll() }
            }
            TASK_ID -> {
                val id = ContentUris.parseId(uri).toInt()
                // Execute the suspend function within runBlocking
                rowsDeleted = runBlocking { taskDao.deleteById(id) }
            }
            else -> throw IllegalArgumentException("Deletion not supported for URI: $uri")
        }

        if (rowsDeleted != 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }

        return rowsDeleted
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        val taskDao = getDatabase(context!!).taskDao()
        var rowsUpdated: Int

        when (uriMatcher.match(uri)) {
            TASKS -> {
                if (values == null) throw IllegalArgumentException("ContentValues cannot be null")
                val task = convertContentValuesToTask(values)
                // Execute the suspend function within runBlocking
                rowsUpdated = runBlocking { taskDao.update(task) }
            }
            TASK_ID -> {
                val id = ContentUris.parseId(uri).toInt()
                if (values == null) throw IllegalArgumentException("ContentValues cannot be null")
                val task = convertContentValuesToTask(values).copy(id = id)
                // Execute the suspend function within runBlocking
                rowsUpdated = runBlocking { taskDao.update(task) }
            }
            else -> throw IllegalArgumentException("Update not supported for URI: $uri")
        }

        if (rowsUpdated != 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }

        return rowsUpdated
    }

    // Helper method to convert ContentValues to Task
    private fun convertContentValuesToTask(values: ContentValues): Task {
        val id = values.getAsInteger(TaskEntry.COLUMN_ID) ?: 0
        val title = values.getAsString(TaskEntry.COLUMN_TITLE)
            ?: throw IllegalArgumentException("Task requires a title")
        val description = values.getAsString(TaskEntry.COLUMN_DESCRIPTION)
        val deadline = values.getAsLong(TaskEntry.COLUMN_DEADLINE)
            ?: throw IllegalArgumentException("Task requires a deadline")
        val isCompleted = values.getAsBoolean(TaskEntry.COLUMN_IS_COMPLETED) ?: false

        return Task(
            id = id,
            title = title,
            description = description,
            deadline = deadline,
            isCompleted = isCompleted
        )
    }
}
