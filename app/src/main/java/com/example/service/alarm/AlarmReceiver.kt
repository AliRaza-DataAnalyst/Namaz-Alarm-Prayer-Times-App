package com.example.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmSoundService::class.java).apply {
            action = AlarmSoundService.ACTION_START
            putExtra(AlarmSoundService.EXTRA_LABEL, intent.getStringExtra(AlarmSoundService.EXTRA_LABEL) ?: "Namaz Alarm")
            putExtra(AlarmSoundService.EXTRA_TIME, intent.getStringExtra(AlarmSoundService.EXTRA_TIME) ?: "")
            putExtra(AlarmSoundService.EXTRA_PRAYER, intent.getStringExtra(AlarmSoundService.EXTRA_PRAYER))
            putExtra(AlarmSoundService.EXTRA_SOUND, intent.getStringExtra(AlarmSoundService.EXTRA_SOUND) ?: "alarm_tone_1")
            putExtra(AlarmSoundService.EXTRA_CUSTOM_URI, intent.getStringExtra(AlarmSoundService.EXTRA_CUSTOM_URI))
            putExtra(AlarmSoundService.EXTRA_MUTED, intent.getBooleanExtra(AlarmSoundService.EXTRA_MUTED, false))
            putExtra(AlarmSoundService.EXTRA_VOLUME, intent.getFloatExtra(AlarmSoundService.EXTRA_VOLUME, 0.8f))
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
