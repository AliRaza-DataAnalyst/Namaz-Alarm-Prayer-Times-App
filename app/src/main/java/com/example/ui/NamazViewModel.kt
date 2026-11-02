package com.example.ui

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datasource.IslamicData
import com.example.data.model.*
import com.example.data.repository.NamazRepository
import com.example.service.alarm.AlarmScheduler
import com.example.service.alarm.AlarmSoundService
import com.example.service.alarm.SoundSynthesizer
import com.example.service.prayer.PrayerTimeCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class NamazViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    val repository = NamazRepository(application)
    val synthesizer = SoundSynthesizer(application)

    val alarms = repository.alarms
    val tasks = repository.tasks
    val donePrayers = repository.donePrayers
    val tasbihState = repository.tasbihState
    val settings = repository.settings

    // Current live time (updates every second for live countdown)
    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis = _currentTimeMillis.asStateFlow()

    // Device Compass heading (0 - 360)
    private val _deviceHeading = MutableStateFlow(0f)
    val deviceHeading = _deviceHeading.asStateFlow()

    private val _hasCompassSensor = MutableStateFlow(false)
    val hasCompassSensor = _hasCompassSensor.asStateFlow()

    // Toast message event
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    // Ringing Alarm State (from foreground service or test)
    val ringingAlarm = AlarmSoundService.currentRingingAlarm

    private var sensorManager: SensorManager? = null
    private var rotationVectorSensor: Sensor? = null
    private var geomagneticVectorSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val gravityData = FloatArray(3)
    private val geomagneticData = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private val lowPassFilterAlpha = 0.82f
    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var tickerJob: Job? = null

    init {
        startTicker()
        initCompassSensors()
    }

    private fun startTicker() {
        tickerJob = viewModelScope.launch {
            while (isActive) {
                _currentTimeMillis.value = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    private fun initCompassSensors() {
        sensorManager = getApplication<Application>().getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        geomagneticVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationVectorSensor != null || geomagneticVectorSensor != null || (accelerometer != null && magnetometer != null)) {
            _hasCompassSensor.value = true
            registerSensors()
        } else {
            _hasCompassSensor.value = false
        }
    }

    fun registerSensors() {
        val sm = sensorManager ?: return
        if (rotationVectorSensor != null) {
            sm.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (geomagneticVectorSensor != null) {
            sm.registerListener(this, geomagneticVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun unregisterSensors() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var computedAzimuth: Float? = null

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR || event.sensor.type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
            try {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                computedAzimuth = extractAccurateAzimuth(rotationMatrix)
            } catch (_: Exception) {}
        } else {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // Low-pass filter for gravity to reduce jitter
                for (i in 0 until 3) {
                    gravityData[i] = lowPassFilterAlpha * gravityData[i] + (1 - lowPassFilterAlpha) * event.values[i]
                }
                hasGravity = true
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                // Low-pass filter for geomagnetic field
                for (i in 0 until 3) {
                    geomagneticData[i] = lowPassFilterAlpha * geomagneticData[i] + (1 - lowPassFilterAlpha) * event.values[i]
                }
                hasGeomagnetic = true
            }

            if (hasGravity && hasGeomagnetic) {
                val iMatrix = FloatArray(9)
                if (SensorManager.getRotationMatrix(rotationMatrix, iMatrix, gravityData, geomagneticData)) {
                    computedAzimuth = extractAccurateAzimuth(rotationMatrix)
                }
            }
        }

        computedAzimuth?.let { rawAzimuth ->
            // Apply Geomagnetic Declination to adjust from Magnetic North to True Geographic North
            val loc = settings.value.location
            val declination = try {
                android.hardware.GeomagneticField(
                    loc.latitude.toFloat(),
                    loc.longitude.toFloat(),
                    0f,
                    System.currentTimeMillis()
                ).declination
            } catch (_: Exception) {
                0f
            }

            val trueNorthHeading = (rawAzimuth + declination + 360f) % 360f

            // Smooth the device heading with circular interpolation
            val prevHeading = _deviceHeading.value
            var diff = trueNorthHeading - prevHeading
            while (diff < -180f) diff += 360f
            while (diff > 180f) diff -= 360f

            // Smooth interpolation factor (0.25f)
            val smoothed = (prevHeading + 0.25f * diff + 360f) % 360f
            _deviceHeading.value = smoothed
        }
    }

    /**
     * Extracts azimuth with tilt compensation (handles device flat on palm or tilted towards user).
     */
    private fun extractAccurateAzimuth(rMatrix: FloatArray): Float {
        // First check inclination/pitch to determine if coordinate remapping is necessary
        SensorManager.getOrientation(rMatrix, orientationAngles)
        val pitchRad = orientationAngles[1]
        val pitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat()

        // When device is held tilted upright (> 45 degrees), remap axes so top edge remains heading vector
        val matrixToUse = if (pitchDeg < -45f || pitchDeg > 45f) {
            SensorManager.remapCoordinateSystem(
                rMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remappedMatrix
            )
            remappedMatrix
        } else {
            rMatrix
        }

        val finalOrient = FloatArray(3)
        SensorManager.getOrientation(matrixToUse, finalOrient)
        var azimuth = Math.toDegrees(finalOrient[0].toDouble()).toFloat()
        if (azimuth < 0) azimuth += 360f
        return azimuth
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun setManualHeading(heading: Float) {
        _deviceHeading.value = heading
    }

    // Alarms Operations
    fun toggleAlarm(alarmId: String) {
        val currentList = alarms.value
        val updated = currentList.map {
            if (it.id == alarmId) it.copy(isEnabled = !it.isEnabled) else it
        }
        repository.updateAlarms(updated)
        AlarmScheduler.scheduleAll(getApplication(), updated, settings.value)
    }

    fun saveAlarm(alarm: AlarmItem) {
        val currentList = alarms.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == alarm.id }
        if (index >= 0) {
            currentList[index] = alarm
        } else {
            currentList.add(alarm)
        }
        repository.updateAlarms(currentList)
        AlarmScheduler.scheduleAll(getApplication(), currentList, settings.value)
    }

    fun deleteAlarm(alarmId: String) {
        val updated = alarms.value.filter { it.id != alarmId }
        repository.updateAlarms(updated)
        AlarmScheduler.scheduleAll(getApplication(), updated, settings.value)
    }

    fun syncAlarmsWithAstronomicalTimes() {
        val curSettings = settings.value
        val cal = Calendar.getInstance()
        val result = PrayerTimeCalculator.calculate(
            cal,
            curSettings.location.latitude,
            curSettings.location.longitude,
            curSettings.calculationMethod,
            curSettings.madhab
        )

        val prayerTimesMap = mapOf(
            "Tahajjud" to PrayerTimeCalculator.formatTime24(result.tahajjud),
            "Fajr" to PrayerTimeCalculator.formatTime24(result.fajr),
            "Ishraq" to PrayerTimeCalculator.formatTime24(result.ishraq),
            "Zuhr" to PrayerTimeCalculator.formatTime24(result.dhuhr),
            "Asr" to PrayerTimeCalculator.formatTime24(result.asr),
            "Maghrib" to PrayerTimeCalculator.formatTime24(result.maghrib),
            "Isha" to PrayerTimeCalculator.formatTime24(result.isha)
        )

        val updated = alarms.value.map { alarm ->
            val pName = alarm.prayerName
            if (pName != null && prayerTimesMap.containsKey(pName)) {
                alarm.copy(time = prayerTimesMap[pName] ?: alarm.time)
            } else {
                alarm
            }
        }
        repository.updateAlarms(updated)
        AlarmScheduler.scheduleAll(getApplication(), updated, curSettings)
        viewModelScope.launch {
            _toastEvent.emit("Synced alarms to ${curSettings.location.city} timings!")
        }
    }

    // Namaz Done Tracker Operations (NO SOUND produced on click or completion)
    fun togglePrayerDone(prayerId: String) {
        repository.togglePrayerDone(prayerId)
        val todayDone = repository.donePrayers.value[repository.getTodayKey()] ?: emptySet()
        val isDoneNow = todayDone.contains(prayerId)

        if (isDoneNow) {
            val obligatory = listOf("p2", "p3", "p4", "p5", "p6")
            val allObligatoryDone = obligatory.all { todayDone.contains(it) }
            if (allObligatoryDone) {
                viewModelScope.launch {
                    _toastEvent.emit("Masha'Allah! All 5 obligatory prayers completed today! 🎉")
                }
            }
        }
    }

    fun toggleAlarmMute(alarmId: String) {
        val currentList = alarms.value
        val updated = currentList.map {
            if (it.id == alarmId) it.copy(isMuted = !it.isMuted) else it
        }
        repository.updateAlarms(updated)
        AlarmScheduler.scheduleAll(getApplication(), updated, settings.value)
        val target = updated.find { it.id == alarmId }
        val statusText = if (target?.isMuted == true) "Muted (silent) for ${target.prayerName ?: target.label} 🔕" else "Sound unmuted for ${target?.prayerName ?: target?.label} 🔔"
        viewModelScope.launch {
            _toastEvent.emit(statusText)
        }
    }

    fun getMobileAlarmVolume(): Pair<Int, Int> = synthesizer.getMobileAlarmVolume()

    fun setMobileAlarmVolume(volumeLevel: Int) {
        synthesizer.setMobileAlarmVolume(volumeLevel)
    }

    // Tasks Operations
    fun addTask(name: String, time: String?) {
        if (name.isBlank()) return
        val newTask = TaskItem(
            id = "t_${System.currentTimeMillis()}",
            name = name.trim(),
            reminderTime = time?.takeIf { it.isNotBlank() },
            isCompleted = false
        )
        val updated = listOf(newTask) + tasks.value
        repository.updateTasks(updated)
        synthesizer.playTasbihClick()
    }

    fun toggleTask(taskId: String) {
        val updated = tasks.value.map {
            if (it.id == taskId) {
                it.copy(isCompleted = !it.isCompleted)
            } else it
        }
        repository.updateTasks(updated)
    }

    fun deleteTask(taskId: String) {
        val updated = tasks.value.filter { it.id != taskId }
        repository.updateTasks(updated)
    }

    fun clearCompletedTasks() {
        val updated = tasks.value.filter { !it.isCompleted }
        repository.updateTasks(updated)
    }

    // Tasbih Operations
    fun tapTasbih() {
        val cur = tasbihState.value
        val nextCount = cur.count + 1
        val isGoalLap = nextCount > 0 && nextCount % cur.goal == 0

        if (isGoalLap) {
            synthesizer.playTasbihComplete(settings.value.vibration)
        } else {
            synthesizer.playTasbihClick(settings.value.vibration)
        }

        repository.updateTasbih(
            cur.copy(
                count = nextCount,
                totalCount = cur.totalCount + 1
            )
        )
    }

    fun setTasbihGoal(newGoal: Int) {
        synthesizer.playTasbihClick()
        val cur = tasbihState.value
        repository.updateTasbih(cur.copy(goal = newGoal))
    }

    fun selectDhikr(dhikrId: String) {
        synthesizer.playTasbihClick()
        val cur = tasbihState.value
        val preset = IslamicData.DHIKR_PRESETS.find { it.id == dhikrId }
        repository.updateTasbih(
            cur.copy(
                selectedDhikrId = dhikrId,
                goal = preset?.defaultGoal ?: cur.goal
            )
        )
    }

    fun resetTasbih() {
        synthesizer.playTasbihClick()
        val cur = tasbihState.value
        repository.updateTasbih(cur.copy(count = 0, totalCount = 0))
    }

    // Settings Operations
    fun updateSettings(newSettings: AppSettings) {
        repository.updateSettings(newSettings)
        AlarmScheduler.scheduleAll(getApplication(), alarms.value, newSettings)
        viewModelScope.launch {
            _toastEvent.emit("Settings applied successfully!")
        }
    }

    fun selectPresetCity(city: LocationConfig) {
        val cur = settings.value
        updateSettings(cur.copy(location = city.copy(isAutoGps = false)))
    }

    fun detectGpsLocation() {
        try {
            val lm = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val location: Location? = try {
                lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (_: SecurityException) {
                null
            }

            if (location != null) {
                val lat = Math.round(location.latitude * 10000.0) / 10000.0
                val lng = Math.round(location.longitude * 10000.0) / 10000.0
                val cur = settings.value
                val updatedLoc = LocationConfig(
                    city = "My Location",
                    country = "GPS Detected",
                    latitude = lat,
                    longitude = lng,
                    isAutoGps = true
                )
                updateSettings(cur.copy(location = updatedLoc))
                viewModelScope.launch {
                    _toastEvent.emit("GPS Location detected: $lat, $lng")
                }
            } else {
                viewModelScope.launch {
                    _toastEvent.emit("Could not retrieve GPS location. Check permissions or select city.")
                }
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _toastEvent.emit("Error accessing location: ${e.message}")
            }
        }
    }

    // Test & Ringing Alarms
    fun triggerTestAlarm(customAlarm: AlarmItem? = null) {
        val cur = settings.value
        val label = customAlarm?.label ?: "Test Namaz Reminder (تجرباتی اذان)"
        val time = customAlarm?.time ?: PrayerTimeCalculator.formatTime24(System.currentTimeMillis())
        val prayer = customAlarm?.prayerName ?: "Fajr"
        val sound = customAlarm?.soundType ?: cur.soundType
        val customUri = customAlarm?.customSoundUri ?: cur.customSoundUri
        val isMuted = customAlarm?.isMuted ?: false

        synthesizer.startAlarmLoop(sound, customUri, cur.vibration, isMuted)
        AlarmSoundService.dismissRinging()
    }

    fun dismissAlarm() {
        synthesizer.stopAlarm()
        AlarmSoundService.stopService(getApplication())
    }

    fun snoozeAlarm(minutes: Int = 5) {
        val currentInfo = ringingAlarm.value
        val label = currentInfo?.label ?: "Snoozed Namaz Alarm"
        val time = currentInfo?.time ?: PrayerTimeCalculator.formatTime24(System.currentTimeMillis())
        val prayer = currentInfo?.prayerName
        val sound = currentInfo?.soundType ?: settings.value.soundType
        val customUri = currentInfo?.customUri ?: settings.value.customSoundUri
        val isMuted = currentInfo?.isMuted ?: false

        dismissAlarm()
        AlarmScheduler.scheduleSnooze(
            getApplication(),
            label,
            time,
            prayer,
            minutes,
            sound,
            customUri,
            isMuted,
            settings.value.volume
        )
        viewModelScope.launch {
            _toastEvent.emit("Alarm snoozed for $minutes minutes.")
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        unregisterSensors()
        synthesizer.stopAlarm()
        super.onCleared()
    }
}
