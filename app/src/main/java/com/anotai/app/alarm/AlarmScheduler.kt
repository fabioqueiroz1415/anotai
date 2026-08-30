package com.anotai.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anotai.app.MainActivity
import com.anotai.app.data.TaskRecord
import com.anotai.app.data.TaskRepository
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmScheduler(
    private val context: Context
) {
    private val alarmManager =
        context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

    private val repository = TaskRepository(context)
    private val cancellationStore = AlertCancellationStore(context)

    fun scheduleNext(
        taskId: Long,
        now: ZonedDateTime = ZonedDateTime.now()
    ) {
        val task = repository.get(taskId) ?: return

        scheduleNotificationKind(task, now)
        schedulePreAlertKind(task, now)
        scheduleAlertKind(task, now)
    }

    fun scheduleNextForKind(
        taskId: Long,
        kind: String,
        now: ZonedDateTime = ZonedDateTime.now()
    ) {
        val task = repository.get(taskId) ?: return

        when (kind) {
            AlarmReceiver.KIND_NOTIFICATION ->
                scheduleNotificationKind(task, now)

            AlarmReceiver.KIND_PRE_ALERT ->
                schedulePreAlertKind(task, now)

            AlarmReceiver.KIND_ALERT ->
                scheduleAlertKind(task, now)
        }
    }

    fun scheduleAfterFiring(
        taskId: Long,
        kind: String,
        eventAtMillis: Long,
        firedLeadMinutes: Int,
        now: ZonedDateTime = ZonedDateTime.now()
    ) {
        val task = repository.get(taskId) ?: return

        val firedEventAt =
            eventAtMillis
                .takeIf { it > 0L }
                ?.let { millis ->
                    ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(millis),
                        ZoneId.systemDefault()
                    )
                }

        when (kind) {
            AlarmReceiver.KIND_NOTIFICATION ->
                scheduleRegularAfterFiring(
                    task = task,
                    kind = kind,
                    leads =
                        ReminderLeadPolicy.notificationLeads(
                            task.originalText
                        ),
                    firedEventAt = firedEventAt,
                    firedLeadMinutes = firedLeadMinutes,
                    now = now
                )

            AlarmReceiver.KIND_ALERT ->
                scheduleAlertAfterFiring(
                    task,
                    firedEventAt,
                    firedLeadMinutes,
                    now
                )

            AlarmReceiver.KIND_PRE_ALERT ->
                schedulePreAlertAfterFiring(
                    task,
                    firedEventAt,
                    firedLeadMinutes,
                    now
                )
        }
    }

    fun cancelSpecificAlert(
        taskId: Long,
        eventAtMillis: Long,
        alertLeadMinutes: Int,
        now: ZonedDateTime = ZonedDateTime.now()
    ) {
        cancellationStore.cancel(
            taskId,
            eventAtMillis,
            alertLeadMinutes
        )

        cancelKind(taskId, AlarmReceiver.KIND_ALERT)

        scheduleNextForKind(
            taskId,
            AlarmReceiver.KIND_ALERT,
            now
        )
    }

    fun cancel(taskId: Long) {
        cancelKind(taskId, AlarmReceiver.KIND_NOTIFICATION)
        cancelKind(taskId, AlarmReceiver.KIND_PRE_ALERT)
        cancelKind(taskId, AlarmReceiver.KIND_ALERT)
        cancellationStore.clearTask(taskId)
    }

    fun replace(taskId: Long) {
        cancel(taskId)
        scheduleNext(taskId)
    }

    fun rescheduleAll() {
        val now = ZonedDateTime.now()

        repository.all().forEach { task ->
            scheduleNext(task.id, now)
        }
    }

    private fun scheduleNotificationKind(
        task: TaskRecord,
        now: ZonedDateTime
    ) {
        val plan =
            MultiLeadAlarmPlanner.plan(
                task,
                ReminderLeadPolicy.notificationLeads(
                    task.originalText
                ),
                now
            )

        scheduleOrCancel(
            task,
            AlarmReceiver.KIND_NOTIFICATION,
            plan
        )
    }

    private fun scheduleAlertKind(
        task: TaskRecord,
        now: ZonedDateTime
    ) {
        val eventAt =
            NextOccurrenceCalculator.nextEvent(task, now)

        if (eventAt == null) {
            cancelKind(task.id, AlarmReceiver.KIND_ALERT)
            return
        }

        val eventMillis = eventAt.toInstant().toEpochMilli()

        val leads =
            ReminderLeadPolicy
                .alertLeads(task.originalText)
                .filterNot { lead ->
                    cancellationStore.isCanceled(
                        task.id,
                        eventMillis,
                        lead
                    )
                }

        var plan =
            MultiLeadAlarmPlanner.planForEvent(
                eventAt,
                leads,
                now
            )

        if (plan == null) {
            val nextEvent =
                NextOccurrenceCalculator.nextEvent(
                    task,
                    maxOf(
                        now,
                        eventAt.plusSeconds(1)
                    )
                )

            if (nextEvent != null) {
                val nextEventMillis =
                    nextEvent.toInstant().toEpochMilli()

                val nextLeads =
                    ReminderLeadPolicy
                        .alertLeads(task.originalText)
                        .filterNot { lead ->
                            cancellationStore.isCanceled(
                                task.id,
                                nextEventMillis,
                                lead
                            )
                        }

                plan =
                    MultiLeadAlarmPlanner.planForEvent(
                        nextEvent,
                        nextLeads,
                        now
                    )
            }
        }

        scheduleOrCancel(
            task,
            AlarmReceiver.KIND_ALERT,
            plan
        )
    }

    private fun schedulePreAlertKind(
        task: TaskRecord,
        now: ZonedDateTime
    ) {
        val eventAt =
            NextOccurrenceCalculator.nextEvent(task, now)

        if (eventAt == null) {
            cancelKind(task.id, AlarmReceiver.KIND_PRE_ALERT)
            return
        }

        val eventMillis = eventAt.toInstant().toEpochMilli()

        val specs =
            ReminderLeadPolicy
                .preAlertSpecs(task.originalText)
                .filterNot { spec ->
                    cancellationStore.isCanceled(
                        task.id,
                        eventMillis,
                        spec.alertLeadMinutes
                    )
                }

        var workingEvent = eventAt
        var workingSpecs = specs

        var plan =
            MultiLeadAlarmPlanner.planForEvent(
                workingEvent,
                workingSpecs.map { it.notificationLeadMinutes },
                now
            )

        if (plan == null) {
            val nextEvent =
                NextOccurrenceCalculator.nextEvent(
                    task,
                    maxOf(
                        now,
                        eventAt.plusSeconds(1)
                    )
                )

            if (nextEvent != null) {
                workingEvent = nextEvent

                val nextEventMillis =
                    nextEvent.toInstant().toEpochMilli()

                workingSpecs =
                    ReminderLeadPolicy
                        .preAlertSpecs(task.originalText)
                        .filterNot { spec ->
                            cancellationStore.isCanceled(
                                task.id,
                                nextEventMillis,
                                spec.alertLeadMinutes
                            )
                        }

                plan =
                    MultiLeadAlarmPlanner.planForEvent(
                        workingEvent,
                        workingSpecs.map {
                            it.notificationLeadMinutes
                        },
                        now
                    )
            }
        }

        if (plan == null) {
            cancelKind(task.id, AlarmReceiver.KIND_PRE_ALERT)
            return
        }

        val targetAlertLead =
            workingSpecs.firstOrNull {
                it.notificationLeadMinutes == plan.leadMinutes
            }?.alertLeadMinutes

        schedulePlan(
            task,
            AlarmReceiver.KIND_PRE_ALERT,
            plan,
            targetAlertLead
        )
    }

    private fun scheduleRegularAfterFiring(
        task: TaskRecord,
        kind: String,
        leads: List<Int>,
        firedEventAt: ZonedDateTime?,
        firedLeadMinutes: Int,
        now: ZonedDateTime
    ) {
        val plan =
            if (firedEventAt != null && firedLeadMinutes >= 0) {
                MultiLeadAlarmPlanner.planAfterFiring(
                    task,
                    leads,
                    firedEventAt,
                    firedLeadMinutes,
                    now
                )
            } else {
                MultiLeadAlarmPlanner.plan(task, leads, now)
            }

        scheduleOrCancel(task, kind, plan)
    }

    private fun scheduleAlertAfterFiring(
        task: TaskRecord,
        firedEventAt: ZonedDateTime?,
        firedLeadMinutes: Int,
        now: ZonedDateTime
    ) {
        if (firedEventAt == null || firedLeadMinutes < 0) {
            scheduleAlertKind(task, now)
            return
        }

        val eventMillis = firedEventAt.toInstant().toEpochMilli()

        val leads =
            ReminderLeadPolicy
                .alertLeads(task.originalText)
                .filterNot { lead ->
                    cancellationStore.isCanceled(
                        task.id,
                        eventMillis,
                        lead
                    )
                }

        val plan =
            MultiLeadAlarmPlanner.planAfterFiring(
                task,
                leads,
                firedEventAt,
                firedLeadMinutes,
                now
            )

        scheduleOrCancel(
            task,
            AlarmReceiver.KIND_ALERT,
            plan
        )
    }

    private fun schedulePreAlertAfterFiring(
        task: TaskRecord,
        firedEventAt: ZonedDateTime?,
        firedLeadMinutes: Int,
        now: ZonedDateTime
    ) {
        if (firedEventAt == null || firedLeadMinutes < 0) {
            schedulePreAlertKind(task, now)
            return
        }

        val eventMillis = firedEventAt.toInstant().toEpochMilli()

        val specs =
            ReminderLeadPolicy
                .preAlertSpecs(task.originalText)
                .filterNot { spec ->
                    cancellationStore.isCanceled(
                        task.id,
                        eventMillis,
                        spec.alertLeadMinutes
                    )
                }

        val plan =
            MultiLeadAlarmPlanner.planAfterFiring(
                task,
                specs.map { it.notificationLeadMinutes },
                firedEventAt,
                firedLeadMinutes,
                now
            )

        if (plan == null) {
            cancelKind(task.id, AlarmReceiver.KIND_PRE_ALERT)
            return
        }

        val targetAlertLead =
            specs.firstOrNull {
                it.notificationLeadMinutes == plan.leadMinutes
            }?.alertLeadMinutes

        schedulePlan(
            task,
            AlarmReceiver.KIND_PRE_ALERT,
            plan,
            targetAlertLead
        )
    }

    private fun scheduleOrCancel(
        task: TaskRecord,
        kind: String,
        plan: PlannedMultiLeadNotice?
    ) {
        if (plan == null) {
            cancelKind(task.id, kind)
            return
        }

        schedulePlan(task, kind, plan, null)
    }

    private fun schedulePlan(
        task: TaskRecord,
        kind: String,
        plan: PlannedMultiLeadNotice,
        targetAlertLead: Int?
    ) {
        val intent =
            Intent(
                context,
                AlarmReceiver::class.java
            ).apply {
                putExtra(AlarmReceiver.EXTRA_TASK_ID, task.id)
                putExtra(AlarmReceiver.EXTRA_KIND, kind)
                putExtra(
                    AlarmReceiver.EXTRA_EVENT_AT_MILLIS,
                    plan.eventAt.toInstant().toEpochMilli()
                )
                putExtra(
                    AlarmReceiver.EXTRA_LEAD_MINUTES,
                    plan.leadMinutes
                )

                targetAlertLead?.let {
                    putExtra(
                        AlarmReceiver.EXTRA_TARGET_ALERT_LEAD_MINUTES,
                        it
                    )
                }
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode(task.id, kind),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val trigger = plan.triggerAtMillis

        if (
            kind == AlarmReceiver.KIND_ALERT &&
            canUseExactAlarm()
        ) {
            val showIntent =
                PendingIntent.getActivity(
                    context,
                    showRequestCode(task.id),
                    Intent(
                        context,
                        MainActivity::class.java
                    ).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )

            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(trigger, showIntent),
                pendingIntent
            )

            return
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger,
                pendingIntent
            )
        }
    }

    private fun cancelKind(taskId: Long, kind: String) {
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode(taskId, kind),
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or
                    PendingIntent.FLAG_IMMUTABLE
            )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun canUseExactAlarm(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

    private fun showRequestCode(taskId: Long): Int =
        6_000_000 + (taskId % 1_000_000L).toInt()

    private fun requestCode(taskId: Long, kind: String): Int {
        val base = (taskId % 1_000_000L).toInt()

        return when (kind) {
            AlarmReceiver.KIND_NOTIFICATION -> 1_000_000 + base
            AlarmReceiver.KIND_ALERT -> 2_000_000 + base
            AlarmReceiver.KIND_PRE_ALERT -> 3_000_000 + base
            else -> 4_000_000 + base
        }
    }
}
