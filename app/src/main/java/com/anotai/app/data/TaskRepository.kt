package com.anotai.app.data

import android.content.Context
import com.anotai.app.alarm.ReminderEditPolicy
import com.anotai.app.model.TaskDraft

class TaskRepository(context: Context) {
    private val db =
        TaskDatabaseHelper(
            context.applicationContext
        )

    fun save(
        draft: TaskDraft
    ): Long =
        db.insert(
            normalized(
                draft
            )
        )

    fun update(
        id: Long,
        draft: TaskDraft
    ): Boolean =
        db.update(
            id,
            normalized(
                draft
            )
        )

    fun delete(
        id: Long
    ): Boolean =
        db.delete(id)

    fun all(): List<TaskRecord> =
        db.all()

    fun get(
        id: Long
    ): TaskRecord? =
        db.get(id)

    private fun normalized(
        draft: TaskDraft
    ): TaskDraft =
        draft.copy(
            notificationMinutesBefore =
                ReminderEditPolicy
                    .notificationPrimary(
                        draft.originalText
                    ),
            reminderMinutesBefore =
                ReminderEditPolicy
                    .alertPrimary(
                        draft.originalText
                    )
        )
}
