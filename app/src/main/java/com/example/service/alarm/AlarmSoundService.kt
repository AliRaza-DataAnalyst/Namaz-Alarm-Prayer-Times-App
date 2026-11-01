package com.example.service.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AlarmSoundService : Service() {

    companion object {
        const val CHANNEL_ID = "namaz_alarm_channel"
        const val NOTIFICATION_ID = 7001

        const val ACTION_START = "com.example.namaz.ACTION_START"
        const val ACTION_STOP = "com.example.namaz.ACTION_STOP"
        const val ACTION_SNOOZE = "com.example.namaz.ACTION_SNOOZE"

        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_PRAYER = "extra_prayer"
        const val EXTRA_SOUND = "extra_sound"
        const val EXTRA_CUSTOM_URI = "extra_custom_uri"
        const val EXTRA_MUTED = "extra_muted"
        const val EXTRA_VOLUME = "extra_volume"

        private val _currentRingingAlarm = MutableStateFlow<RingingInfo?>(null)
        val currentRingingAlarm = _currentRingingAlarm.asStateFlow()

        data class RingingInfo(
            val label: String,
            val time: String,
            val prayerName: String?,
            val soundType: String,
            val customUri: String? = null,
            val isMuted: Boolean = false
        )

        fun dismissRinging() {
            _currentRingingAlarm.value = null
        }
    }

    private var synthesizer: SoundSynthesizer? = null

    override fun onCreate() {
        super.onCreate()
        synthesizer = SoundSynthesizer(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopAlarmAndSelf()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                snoozeAlarm(intent)
                stopAlarmAndSelf()
                return START_NOT_STICKY
            }
            else -> {
                val label = intent?.getStringExtra(EXTRA_LABEL) ?: "Namaz Alarm"
                val time = intent?.getStringExtra(EXTRA_TIME) ?: ""
                val prayer = intent?.getStringExtra(EXTRA_PRAYER)
                val sound = intent?.getStringExtra(EXTRA_SOUND) ?: "alarm_tone_1"
                val customUri = intent?.getStringExtra(EXTRA_CUSTOM_URI)
                val isMuted = intent?.getBooleanExtra(EXTRA_MUTED, false) ?: false

                _currentRingingAlarm.value = RingingInfo(label, time, prayer, sound, customUri, isMuted)

                val notification = buildNotification(label, time, prayer)
                startForeground(NOTIFICATION_ID, notification)

                synthesizer?.startAlarmLoop(sound, customUri, withVibration = true, isMuted = isMuted)
                return START_STICKY
            }
        }
    }

    private fun snoozeAlarm(intent: Intent?) {
        val label = intent?.getStringExtra(EXTRA_LABEL) ?: "Snoozed Namaz Alarm"
        val time = intent?.getStringExtra(EXTRA_TIME) ?: ""
        val prayer = intent?.getStringExtra(EXTRA_PRAYER)
        AlarmScheduler.scheduleSnooze(this, label, time, prayer, 5)
    }

    private fun stopAlarmAndSelf() {
        _currentRingingAlarm.value = null
        synthesizer?.stopAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(label: String, time: String, prayer: String?): android.app.Notification {
        val title = prayer?.let { "$it Prayer Time" } ?: "Namaz Alarm"
        val text = "$label $time — Tap to open Namaz Alarm"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmSoundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmSoundService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_LABEL, label)
            putExtra(EXTRA_TIME, time)
            putExtra(EXTRA_PRAYER, prayer)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this, 2, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPendingIntent)
            .setFullScreenIntent(openAppPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 5m", snoozePendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Prayer & Namaz Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms and reminders for Namaz"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        synthesizer?.stopAlarm()
        _currentRingingAlarm.value = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
