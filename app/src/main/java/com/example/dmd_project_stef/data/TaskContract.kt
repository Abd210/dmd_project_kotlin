package com.example.dmd_project_stef.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.BaseColumns

object TaskContract {
    const val AUTHORITY = "com.example.dmd_project_stef.provider"
    val BASE_CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")
    const val PATH_TASKS = "tasks"

    object TaskEntry : BaseColumns {
        val CONTENT_URI: Uri = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_TASKS)

        const val TABLE_NAME = "tasks"

        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_DEADLINE = "deadline"
        const val COLUMN_IS_COMPLETED = "isCompleted"

        // MIME types
        const val CONTENT_LIST_TYPE: String =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + "." + PATH_TASKS

        const val CONTENT_ITEM_TYPE: String =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + "." + PATH_TASKS
    }
}
