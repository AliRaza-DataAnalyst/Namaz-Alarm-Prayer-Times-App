package com.example.data.model

data class AlarmItem(
    val id: String,
    val prayerName: String? = null, // e.g. "Tahajjud", "Fajr", "Ishraq", "Zuhr", "Asr", "Maghrib", "Isha"
    val time: String, // "HH:mm" in 24h format
    val label: String,
    val isEnabled: Boolean = true,
    val isMuted: Boolean = false, // Mute sound when true
    val days: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6), // 0=Sun, 1=Mon, ..., 6=Sat. Empty = once
    val soundType: String = "alarm_tone_1",
    val customSoundUri: String? = null // When soundType == "custom_tone"
)

data class AlarmToneItem(
    val id: String,
    val title: String,
    val urduTitle: String,
    val resName: String? = null,
    val isCustom: Boolean = false
)

data class TaskItem(
    val id: String,
    val name: String,
    val reminderTime: String? = null, // "HH:mm"
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class TasbihState(
    val count: Int = 0,
    val goal: Int = 33,
    val selectedDhikrId: String = "subhanallah",
    val totalCount: Int = 0
)

data class DhikrPreset(
    val id: String,
    val arabic: String,
    val transliteration: String,
    val urdu: String,
    val meaning: String,
    val defaultGoal: Int = 33
)

data class DuaItem(
    val id: String,
    val title: String,
    val urduTitle: String,
    val category: String, // "after_namaz", "morning_evening", "sleep", "forgiveness", "protection"
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val urduTranslation: String,
    val reference: String
)

data class LocationConfig(
    val city: String = "Karachi",
    val country: String = "Pakistan",
    val latitude: Double = 24.8607,
    val longitude: Double = 67.0011,
    val isAutoGps: Boolean = false
)

enum class CalculationMethod(val key: String, val title: String, val desc: String) {
    KARACHI("Karachi", "Univ. of Islamic Sciences, Karachi", "Fajr 18°, Isha 18° (Pakistan, India, Bangladesh)"),
    MWL("MWL", "Muslim World League", "Fajr 18°, Isha 17° (Europe, Far East, Parts of USA)"),
    ISNA("ISNA", "Islamic Society of North America", "Fajr 15°, Isha 15° (USA, Canada)"),
    UMM_AL_QURA("UmmAlQura", "Umm al-Qura University, Makkah", "Fajr 18.5°, Isha 90m after Maghrib (Saudi Arabia)"),
    EGYPT("Egypt", "Egyptian General Authority of Survey", "Fajr 19.5°, Isha 17.5° (Africa, Middle East)"),
    DUBAI("Dubai", "Dubai / UAE Awqaf", "Official UAE government timings"),
    QATAR("Qatar", "Qatar Ministry of Awqaf", "Official Qatar timings"),
    KUWAIT("Kuwait", "Kuwait Ministry of Awqaf", "Official Kuwait timings"),
    SINGAPORE("Singapore", "MUIS Singapore", "Singapore official timings"),
    TEHRAN("Tehran", "Institute of Geophysics, Tehran", "Fajr 17.7°, Isha 14°")
}

enum class Madhab(val key: String, val title: String, val desc: String, val shadowFactor: Double) {
    HANAFI("hanafi", "Hanafi", "Shadow factor 2 (Later Asr time - Pakistan, India, Turkey)", 2.0),
    SHAFII("shafii", "Shafi'i / Standard", "Shadow factor 1 (Maliki, Shafi'i, Hanbali - Arab World)", 1.0)
}

data class AppSettings(
    val soundType: String = "alarm_tone_1",
    val customSoundUri: String? = null,
    val volume: Float = 0.8f,
    val vibration: Boolean = true,
    val is24HourFormat: Boolean = false,
    val calculationMethod: String = CalculationMethod.KARACHI.key,
    val madhab: String = Madhab.HANAFI.key,
    val location: LocationConfig = LocationConfig(),
    val urduLabels: Boolean = true
)

data class PrayerTimesResult(
    val fajr: Long,
    val sunrise: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long,
    val tahajjud: Long,
    val ishraq: Long
)

data class PrayerDisplayItem(
    val key: String,
    val name: String,
    val urduName: String,
    val timeMillis: Long,
    val formattedTime: String,
    val isObligatory: Boolean = true
)
