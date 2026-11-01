package com.example.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.NamazRepository

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repo = NamazRepository(context)
        AlarmScheduler.scheduleAll(context, repo.alarms.value, repo.settings.value)
    }
}
