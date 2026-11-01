package com.example.service.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.R
import kotlinx.coroutines.*

class SoundSynthesizer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var vibrationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val audioManager: AudioManager? by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= 31) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Get mobile phone's actual alarm volume stream (current, max)
     */
    fun getMobileAlarmVolume(): Pair<Int, Int> {
        val am = audioManager ?: return Pair(5, 7)
        val cur = am.getStreamVolume(AudioManager.STREAM_ALARM)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        return Pair(cur, max)
    }

    /**
     * Set mobile phone's actual alarm volume stream
     */
    fun setMobileAlarmVolume(volumeLevel: Int) {
        try {
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, volumeLevel, 0)
        } catch (_: Exception) {}
    }

    /**
     * Plays a preview of an uploaded alarm tone or custom audio once
     */
    fun playPreview(soundType: String, customUri: String? = null) {
        stopAlarm()
        isPlaying = true
        scope.launch(Dispatchers.IO) {
            try {
                val mp = createMediaPlayerForTone(soundType, customUri) ?: return@launch
                mediaPlayer = mp
                mp.isLooping = false
                mp.setOnCompletionListener {
                    stopAlarm()
                }
                mp.start()
            } catch (_: Exception) {
                stopAlarm()
            }
        }
    }

    /**
     * Starts continuous looping alarm sound with vibration until stopped
     */
    fun startAlarmLoop(soundType: String, customUri: String? = null, withVibration: Boolean = true, isMuted: Boolean = false) {
        stopAlarm()
        isPlaying = true

        if (withVibration) {
            vibrationJob = scope.launch {
                while (isActive && isPlaying) {
                    vibratePattern(longArrayOf(0, 500, 300, 500))
                    delay(1300)
                }
            }
        }

        if (isMuted) {
            // Muted alarm: vibration only, no audio
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val mp = createMediaPlayerForTone(soundType, customUri) ?: return@launch
                mediaPlayer = mp
                mp.isLooping = true
                mp.start()
            } catch (_: Exception) {
                // Ignore playback error
            }
        }
    }

    private fun createMediaPlayerForTone(soundType: String, customUri: String?): MediaPlayer? {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        if (soundType == "custom_tone" && !customUri.isNullOrEmpty()) {
            return try {
                MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(context, Uri.parse(customUri))
                    prepare()
                }
            } catch (e: Exception) {
                // Fallback to alarm_tone_1
                createMediaPlayerFromRaw(R.raw.alarm_tone_1, audioAttributes)
            }
        }

        val rawResId = getRawResForTone(soundType)
        return createMediaPlayerFromRaw(rawResId, audioAttributes)
    }

    private fun createMediaPlayerFromRaw(resId: Int, audioAttributes: AudioAttributes): MediaPlayer? {
        return try {
            MediaPlayer.create(context, resId, audioAttributes, audioManager?.generateAudioSessionId() ?: 0)
        } catch (_: Exception) {
            null
        }
    }

    fun getRawResForTone(soundType: String): Int {
        return when (soundType) {
            "alarm_tone_2" -> R.raw.alarm_tone_2
            "alarm_tone_3" -> R.raw.alarm_tone_3
            "alarm_tone_4" -> R.raw.alarm_tone_4
            "alarm_tone_5" -> R.raw.alarm_tone_5
            "alarm_tone_6" -> R.raw.alarm_tone_6
            "alarm_tone_7" -> R.raw.alarm_tone_7
            "alarm_tone_8" -> R.raw.alarm_tone_8
            "alarm_tone_9" -> R.raw.alarm_tone_9
            "alarm_tone_10" -> R.raw.alarm_tone_10
            "alarm_tone_11" -> R.raw.alarm_tone_11
            else -> R.raw.alarm_tone_1
        }
    }

    fun stopAlarm() {
        isPlaying = false
        vibrationJob?.cancel()
        vibrationJob = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}

        vibrator?.cancel()
    }

    /**
     * Discrete click sound for Tasbih button tap
     */
    fun playTasbihClick(vibrate: Boolean = true) {
        scope.launch {
            try {
                if (vibrate) {
                    vibrateShort(15)
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Celebratory vibration upon completing a Tasbih lap
     */
    fun playTasbihComplete(vibrate: Boolean = true) {
        scope.launch {
            try {
                if (vibrate) {
                    vibratePattern(longArrayOf(0, 60, 40, 80))
                }
            } catch (_: Exception) {}
        }
    }

    private fun vibrateShort(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(ms)
            }
        } catch (_: Exception) {}
    }

    private fun vibratePattern(timings: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, -1)
            }
        } catch (_: Exception) {}
    }
}
