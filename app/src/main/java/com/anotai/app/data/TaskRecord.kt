package com.anotai.app.data

import com.anotai.app.model.RecurrenceType
import java.time.LocalDate
import java.time.LocalTime

data class TaskRecord(
    val id: Long,
    val title: String,
    val originalText: String,
    val date: LocalDate?,
    val time: LocalTime?,
    val reminderMinutesBefore: Int,
    val recurrenceType: RecurrenceType,
    val weekdaysCsv: String,
    val monthDay: Int?,
    val untilDate: LocalDate?,
    val notificationMinutesBefore: Int = 120,
    val completed: Boolean = false
)
