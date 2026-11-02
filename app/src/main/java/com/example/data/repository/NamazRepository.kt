package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class NamazRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("namaz_app_prefs", Context.MODE_PRIVATE)

    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    private val _donePrayers = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val donePrayers: StateFlow<Map<String, Set<String>>> = _donePrayers.asStateFlow()

    private val _tasbihState = MutableStateFlow(TasbihState())
    val tasbihState: StateFlow<TasbihState> = _tasbihState.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        loadAll()
    }

    private fun loadAll() {
        // Load Settings
        val soundType = prefs.getString("setting_sound", "alarm_tone_1") ?: "alarm_tone_1"
        val customUri = prefs.getString("setting_custom_uri", null)
        val volume = prefs.getFloat("setting_volume", 0.8f)
        val vibration = prefs.getBoolean("setting_vibration", true)
        val is24h = prefs.getBoolean("setting_24h", false)
        val calcMethod = prefs.getString("setting_calc_method", CalculationMethod.KARACHI.key) ?: CalculationMethod.KARACHI.key
        val madhab = prefs.getString("setting_madhab", Madhab.HANAFI.key) ?: Madhab.HANAFI.key
        val cityName = prefs.getString("setting_city", "Karachi") ?: "Karachi"
        val country = prefs.getString("setting_country", "Pakistan") ?: "Pakistan"
        val lat = prefs.getFloat("setting_lat", 24.8607f).toDouble()
        val lng = prefs.getFloat("setting_lng", 67.0011f).toDouble()
        val isAuto = prefs.getBoolean("setting_auto_gps", false)
        val urduLabels = prefs.getBoolean("setting_urdu_labels", true)

        _settings.value = AppSettings(
            soundType = soundType,
            customSoundUri = customUri,
            volume = volume,
            vibration = vibration,
            is24HourFormat = is24h,
            calculationMethod = calcMethod,
            madhab = madhab,
            location = LocationConfig(cityName, country, lat, lng, isAuto),
            urduLabels = urduLabels
        )

        // Load Alarms
        val alarmsRaw = prefs.getString("alarms_json", null)
        if (alarmsRaw != null) {
            try {
                val list = mutableListOf<AlarmItem>()
                val arr = JSONArray(alarmsRaw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val daysList = mutableListOf<Int>()
                    val daysArr = obj.optJSONArray("days")
                    if (daysArr != null) {
                        for (d in 0 until daysArr.length()) daysList.add(daysArr.getInt(d))
                    } else {
                        daysList.addAll(listOf(0, 1, 2, 3, 4, 5, 6))
                    }
                    val pName = obj.optString("prayerName").takeIf { it.isNotEmpty() }
                    val rawLabel = obj.optString("label", "Namaz Alarm")
                    list.add(
                        AlarmItem(
                            id = obj.optString("id", "a_$i"),
                            prayerName = pName,
                            time = obj.optString("time", "06:00"),
                            label = normalizePrayerLabel(pName, rawLabel),
                            isEnabled = obj.optBoolean("isEnabled", true),
                            isMuted = obj.optBoolean("isMuted", false),
                            days = daysList,
                            soundType = obj.optString("soundType", "alarm_tone_1"),
                            customSoundUri = obj.optString("customSoundUri").takeIf { it.isNotEmpty() },
                            date = obj.optString("date").takeIf { it.isNotEmpty() }
                        )
                    )
                }
                _alarms.value = list
            } catch (e: Exception) {
                _alarms.value = defaultAlarms()
            }
        } else {
            _alarms.value = defaultAlarms()
            saveAlarmsInternal(_alarms.value)
        }

        // Load Tasks
        val tasksRaw = prefs.getString("tasks_json", null)
        if (tasksRaw != null) {
            try {
                val list = mutableListOf<TaskItem>()
                val arr = JSONArray(tasksRaw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        TaskItem(
                            id = obj.optString("id", "t_$i"),
                            name = obj.optString("name", ""),
                            reminderTime = obj.optString("reminderTime").takeIf { it.isNotEmpty() },
                            isCompleted = obj.optBoolean("isCompleted", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                _tasks.value = list
            } catch (e: Exception) {
                _tasks.value = defaultTasks()
            }
        } else {
            _tasks.value = defaultTasks()
            saveTasksInternal(_tasks.value)
        }

        // Load Done prayers
        val doneRaw = prefs.getString("done_prayers_json", null)
        if (doneRaw != null) {
            try {
                val map = mutableMapOf<String, Set<String>>()
                val obj = JSONObject(doneRaw)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val prayerArr = obj.getJSONArray(k)
                    val set = mutableSetOf<String>()
                    for (j in 0 until prayerArr.length()) {
                        set.add(prayerArr.getString(j))
                    }
                    map[k] = set
                }
                _donePrayers.value = map
            } catch (e: Exception) {
                _donePrayers.value = emptyMap()
            }
        }

        // Load Tasbih
        val tCount = prefs.getInt("tasbih_count", 0)
        val tGoal = prefs.getInt("tasbih_goal", 33)
        val tDhikr = prefs.getString("tasbih_dhikr_id", "subhanallah") ?: "subhanallah"
        val tTotal = prefs.getInt("tasbih_total", 0)
        _tasbihState.value = TasbihState(tCount, tGoal, tDhikr, tTotal)
    }

    private fun normalizePrayerLabel(prayerName: String?, label: String): String {
        return when (prayerName) {
            "Tahajjud" -> "🫀 تہجد کی نماز + صلوۃ الحاجات کی نماز"
            "Fajr" -> "💗 نمازِ فجر"
            "Ishraq" -> "☀️ نمازِ اشراق و چاشت"
            "Zuhr" -> "🫶 نمازِ ظہر"
            "Asr" -> "❤️ نمازِ عصر"
            "Maghrib" -> "🫶🤍 نمازِ مغرب"
            "Isha" -> "🫶 🫀 نمازِ عشاء"
            else -> label
        }
    }

    private fun defaultAlarms(): List<AlarmItem> = listOf(
        AlarmItem("p1", "Tahajjud", "04:00", "🫀 تہجد کی نماز + صلوۃ الحاجات کی نماز", isEnabled = true),
        AlarmItem("p2", "Fajr", "04:35", "💗 نمازِ فجر", isEnabled = true),
        AlarmItem("p3", "Zuhr", "13:30", "🫶 نمازِ ظہر", isEnabled = true),
        AlarmItem("p4", "Asr", "16:20", "❤️ نمازِ عصر", isEnabled = true),
        AlarmItem("p5", "Maghrib", "18:00", "🫶🤍 نمازِ مغرب", isEnabled = true),
        AlarmItem("p6", "Isha", "19:15", "🫶 🫀 نمازِ عشاء", isEnabled = true)
    )

    private fun defaultTasks(): List<TaskItem> = listOf(
        TaskItem("t1", "📖 تلاوت سورۃ الملک قبل از نوم (Recite Surah Al-Mulk)", "21:30", false),
        TaskItem("t2", "🤲 صبح کے مسنون اذکار (Morning Adhkar)", "06:00", false),
        TaskItem("t3", "✨ جمعۃ المبارک پر 100 بار درود شریف (Read Durood 100x)", "12:00", false)
    )

    fun updateAlarms(newAlarms: List<AlarmItem>) {
        _alarms.value = newAlarms
        saveAlarmsInternal(newAlarms)
    }

    private fun saveAlarmsInternal(list: List<AlarmItem>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("prayerName", item.prayerName ?: "")
            obj.put("time", item.time)
            obj.put("label", item.label)
            obj.put("isEnabled", item.isEnabled)
            obj.put("isMuted", item.isMuted)
            val daysArr = JSONArray()
            item.days.forEach { daysArr.put(it) }
            obj.put("days", daysArr)
            obj.put("soundType", item.soundType)
            obj.put("customSoundUri", item.customSoundUri ?: "")
            obj.put("date", item.date ?: "")
            arr.put(obj)
        }
        prefs.edit().putString("alarms_json", arr.toString()).apply()
    }

    fun updateTasks(newTasks: List<TaskItem>) {
        _tasks.value = newTasks
        saveTasksInternal(newTasks)
    }

    private fun saveTasksInternal(list: List<TaskItem>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("reminderTime", item.reminderTime ?: "")
            obj.put("isCompleted", item.isCompleted)
            obj.put("createdAt", item.createdAt)
            arr.put(obj)
        }
        prefs.edit().putString("tasks_json", arr.toString()).apply()
    }

    fun togglePrayerDone(prayerId: String, dateKey: String = getTodayKey()) {
        val current = _donePrayers.value.toMutableMap()
        val todaySet = current[dateKey]?.toMutableSet() ?: mutableSetOf()
        if (todaySet.contains(prayerId)) {
            todaySet.remove(prayerId)
        } else {
            todaySet.add(prayerId)
        }
        current[dateKey] = todaySet
        _donePrayers.value = current

        // Save
        val obj = JSONObject()
        for ((k, set) in current) {
            val arr = JSONArray()
            set.forEach { arr.put(it) }
            obj.put(k, arr)
        }
        prefs.edit().putString("done_prayers_json", obj.toString()).apply()
    }

    fun updateTasbih(newState: TasbihState) {
        _tasbihState.value = newState
        prefs.edit()
            .putInt("tasbih_count", newState.count)
            .putInt("tasbih_goal", newState.goal)
            .putString("tasbih_dhikr_id", newState.selectedDhikrId)
            .putInt("tasbih_total", newState.totalCount)
            .apply()
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putString("setting_sound", newSettings.soundType)
            .putString("setting_custom_uri", newSettings.customSoundUri)
            .putFloat("setting_volume", newSettings.volume)
            .putBoolean("setting_vibration", newSettings.vibration)
            .putBoolean("setting_24h", newSettings.is24HourFormat)
            .putString("setting_calc_method", newSettings.calculationMethod)
            .putString("setting_madhab", newSettings.madhab)
            .putString("setting_city", newSettings.location.city)
            .putString("setting_country", newSettings.location.country)
            .putFloat("setting_lat", newSettings.location.latitude.toFloat())
            .putFloat("setting_lng", newSettings.location.longitude.toFloat())
            .putBoolean("setting_auto_gps", newSettings.location.isAutoGps)
            .putBoolean("setting_urdu_labels", newSettings.urduLabels)
            .apply()
    }

    fun getTodayKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun calculateStreak(): Int {
        val map = _donePrayers.value
        val required = setOf("p2", "p3", "p4", "p5", "p6") // Fajr, Zuhr, Asr, Maghrib, Isha
        var streak = 0
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (i in 0 until 365) {
            val dateKey = sdf.format(cal.time)
            val done = map[dateKey] ?: emptySet()
            val hasAllRequired = required.all { done.contains(it) }

            if (i == 0) {
                // Today may be in progress
                if (hasAllRequired) streak++
            } else {
                if (hasAllRequired) {
                    streak++
                } else {
                    break
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }
}
