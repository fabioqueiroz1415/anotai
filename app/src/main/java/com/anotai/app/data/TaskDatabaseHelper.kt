package com.anotai.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.anotai.app.model.RecurrenceType
import com.anotai.app.model.TaskDraft
import java.time.LocalDate
import java.time.LocalTime

class TaskDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "anotai.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                original_text TEXT NOT NULL,
                date_iso TEXT,
                time_iso TEXT,
                reminder_minutes INTEGER NOT NULL DEFAULT 60,
                notification_minutes INTEGER NOT NULL DEFAULT 120,
                recurrence_type TEXT NOT NULL,
                weekdays_csv TEXT NOT NULL,
                month_day INTEGER,
                until_iso TEXT,
                completed INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE tasks ADD COLUMN notification_minutes INTEGER NOT NULL DEFAULT 120"
            )
            db.execSQL(
                "UPDATE tasks SET reminder_minutes = 60 WHERE reminder_minutes = 0"
            )
        }

        if (oldVersion < 3) {
            db.execSQL(
                "ALTER TABLE tasks ADD COLUMN completed INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    fun insert(draft: TaskDraft): Long {
        val values = valuesFromDraft(draft).apply {
            put("created_at", System.currentTimeMillis())
        }

        return writableDatabase.insertOrThrow(
            "tasks",
            null,
            values
        )
    }

    fun update(
        id: Long,
        draft: TaskDraft
    ): Boolean =
        writableDatabase.update(
            "tasks",
            valuesFromDraft(draft),
            "id = ?",
            arrayOf(id.toString())
        ) > 0

    fun delete(id: Long): Boolean =
        writableDatabase.delete(
            "tasks",
            "id = ?",
            arrayOf(id.toString())
        ) > 0

    fun setCompleted(id: Long, completed: Boolean): Boolean =
        writableDatabase.update(
            "tasks",
            ContentValues().apply {
                put("completed", if (completed) 1 else 0)
            },
            "id = ?",
            arrayOf(id.toString())
        ) > 0

    fun restore(task: TaskRecord): Boolean {
        val values = ContentValues().apply {
            put("id", task.id)
            put("title", task.title)
            put("original_text", task.originalText)
            put("date_iso", task.date?.toString())
            put("time_iso", task.time?.toString())
            put("reminder_minutes", task.reminderMinutesBefore)
            put("notification_minutes", task.notificationMinutesBefore)
            put("recurrence_type", task.recurrenceType.name)
            put("weekdays_csv", task.weekdaysCsv)
            task.monthDay?.let { put("month_day", it) } ?: putNull("month_day")
            put("until_iso", task.untilDate?.toString())
            put("completed", if (task.completed) 1 else 0)
            put("created_at", System.currentTimeMillis())
        }

        return writableDatabase.insertWithOnConflict(
            "tasks",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        ) != -1L
    }

    fun all(): List<TaskRecord> {
        val items = mutableListOf<TaskRecord>()

        readableDatabase.query(
            "tasks",
            null,
            null,
            null,
            null,
            null,
            "CASE WHEN date_iso IS NULL THEN 1 ELSE 0 END, date_iso ASC, time_iso ASC, id DESC"
        ).use { c ->
            while (c.moveToNext()) {
                items += fromCursor(c)
            }
        }

        return items
    }

    fun get(id: Long): TaskRecord? {
        readableDatabase.query(
            "tasks",
            null,
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
            "1"
        ).use { c ->
            return if (c.moveToFirst()) {
                fromCursor(c)
            } else {
                null
            }
        }
    }

    private fun valuesFromDraft(
        draft: TaskDraft
    ): ContentValues =
        ContentValues().apply {
            put("title", draft.title)
            put("original_text", draft.originalText)
            put("date_iso", draft.date?.toString())
            put("time_iso", draft.time?.toString())
            put(
                "reminder_minutes",
                draft.reminderMinutesBefore
            )
            put(
                "notification_minutes",
                draft.notificationMinutesBefore
            )
            put(
                "recurrence_type",
                draft.recurrenceType.name
            )
            put(
                "weekdays_csv",
                draft.weekdays.joinToString(",") {
                    it.value.toString()
                }
            )

            draft.monthDay?.let {
                put("month_day", it)
            } ?: putNull("month_day")

            put(
                "until_iso",
                draft.untilDate?.toString()
            )
        }

    private fun fromCursor(c: Cursor): TaskRecord {
        fun str(name: String): String? {
            val index = c.getColumnIndexOrThrow(name)
            return if (c.isNull(index)) null else c.getString(index)
        }

        fun int(name: String): Int? {
            val index = c.getColumnIndexOrThrow(name)
            return if (c.isNull(index)) null else c.getInt(index)
        }

        return TaskRecord(
            id =
                c.getLong(
                    c.getColumnIndexOrThrow("id")
                ),
            title =
                c.getString(
                    c.getColumnIndexOrThrow("title")
                ),
            originalText =
                c.getString(
                    c.getColumnIndexOrThrow(
                        "original_text"
                    )
                ),
            date =
                str("date_iso")
                    ?.let(LocalDate::parse),
            time =
                str("time_iso")
                    ?.let(LocalTime::parse),
            reminderMinutesBefore =
                c.getInt(
                    c.getColumnIndexOrThrow(
                        "reminder_minutes"
                    )
                ),
            recurrenceType =
                RecurrenceType.valueOf(
                    c.getString(
                        c.getColumnIndexOrThrow(
                            "recurrence_type"
                        )
                    )
                ),
            weekdaysCsv =
                c.getString(
                    c.getColumnIndexOrThrow(
                        "weekdays_csv"
                    )
                ),
            monthDay =
                int("month_day"),
            untilDate =
                str("until_iso")
                    ?.let(LocalDate::parse),
            notificationMinutesBefore =
                c.getInt(
                    c.getColumnIndexOrThrow(
                        "notification_minutes"
                    )
                ),
            completed =
                c.getInt(
                    c.getColumnIndexOrThrow(
                        "completed"
                    )
                ) != 0
        )
    }
}
