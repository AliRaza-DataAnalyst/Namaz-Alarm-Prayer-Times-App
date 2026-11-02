package com.example.service.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.AlarmItem
import com.example.data.model.AppSettings
import java.util.Calendar

object AlarmScheduler {

    fun scheduleAll(context: Context, alarms: List<AlarmItem>, settings: AppSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        for ((index, alarm) in alarms.withIndex()) {
            val baseId = (alarm.id.hashCode() and 0x7FFFFFFF) % 100000

            // First cancel all previous day instances for this alarm
            for (day in 0..6) {
                val reqCode = baseId * 10 + day
                val cancelIntent = Intent(context, AlarmReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    context, reqCode, cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pi)
            }

            if (!alarm.isEnabled) continue

            val timeParts = alarm.time.split(":")
            if (timeParts.size != 2) continue
            val hour = timeParts[0].toIntOrNull() ?: continue
            val minute = timeParts[1].toIntOrNull() ?: continue

            val repeatDays = alarm.days

            if (repeatDays.isEmpty()) {
                // One-shot alarm: schedule next occurrence
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                scheduleExact(context, alarmManager, baseId * 10, cal.timeInMillis, alarm, settings)
            } else {
                // Repeated for selected days (0=Sunday ... 6=Saturday)
                for (dayIndex in repeatDays) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, dayIndex + 1)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.WEEK_OF_YEAR, 1)
                        }
                    }
                    val reqCode = baseId * 10 + dayIndex
                    scheduleExact(context, alarmManager, reqCode, cal.timeInMillis, alarm, settings)
                }
            }
        }
    }

    private fun scheduleExact(
        context: Context,
        alarmManager: AlarmManager,
        reqCode: Int,
        triggerAtMillis: Long,
        alarm: AlarmItem,
        settings: AppSettings
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmSoundService.EXTRA_LABEL, alarm.label)
            putExtra(AlarmSoundService.EXTRA_TIME, alarm.time)
            putExtra(AlarmSoundService.EXTRA_PRAYER, alarm.prayerName)
            putExtra(AlarmSoundService.EXTRA_SOUND, alarm.soundType)
            putExtra(AlarmSoundService.EXTRA_CUSTOM_URI, alarm.customSoundUri ?: settings.customSoundUri)
            putExtra(AlarmSoundService.EXTRA_MUTED, alarm.isMuted)
            putExtra(AlarmSoundService.EXTRA_VOLUME, settings.volume)
        }

        val pi = PendingIntent.getBroadcast(
            context, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun scheduleSnooze(
        context: Context,
        label: String,
        time: String,
        prayer: String?,
        minutes: Int = 5,
        sound: String = "alarm_tone_1",
        customUri: String? = null,
        isMuted: Boolean = false,
        volume: Float = 0.8f
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmSoundService.EXTRA_LABEL, "$label (Snoozed)")
            putExtra(AlarmSoundService.EXTRA_TIME, time)
            putExtra(AlarmSoundService.EXTRA_PRAYER, prayer)
            putExtra(AlarmSoundService.EXTRA_SOUND, sound)
            putExtra(AlarmSoundService.EXTRA_CUSTOM_URI, customUri)
            putExtra(AlarmSoundService.EXTRA_MUTED, isMuted)
            putExtra(AlarmSoundService.EXTRA_VOLUME, volume)
        }

        val pi = PendingIntent.getBroadcast(
            context, 999991, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }
}
