package com.example.service.prayer

import com.example.data.model.CalculationMethod
import com.example.data.model.Madhab
import com.example.data.model.PrayerTimesResult
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.*

object PrayerTimeCalculator {

    const val KAABA_LAT = 21.422487
    const val KAABA_LNG = 39.826206

    /**
     * Calculates prayer times for a given day and coordinate.
     */
    fun calculate(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        methodKey: String = CalculationMethod.KARACHI.key,
        madhabKey: String = Madhab.HANAFI.key
    ): PrayerTimesResult {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val tzOffset = calendar.timeZone.getOffset(calendar.timeInMillis) / 3600000.0

        val (fajrAngle, ishaAngle, ishaMinutesAfterMaghrib) = getMethodAngles(methodKey)
        val shadowFactor = if (madhabKey == Madhab.SHAFII.key) 1.0 else 2.0

        // Base Julian Day at 0h UT
        val jd = julianDate(year, month, day) - (longitude / (15.0 * 24.0))

        // Solar transit (Dhuhr)
        val dhuhrHours = computeTransit(jd, longitude, tzOffset)

        // Sunrise & Sunset (angle 0.8333 deg)
        val sunriseDiff = computeSunAngleDiff(jd, latitude, 0.8333, isMorning = true)
        val sunsetDiff = computeSunAngleDiff(jd, latitude, 0.8333, isMorning = false)

        val sunriseHours = dhuhrHours - sunriseDiff
        val sunsetHours = dhuhrHours + sunsetDiff

        // Fajr
        val fajrDiff = computeSunAngleDiff(jd, latitude, fajrAngle, isMorning = true)
        val fajrHours = dhuhrHours - fajrDiff

        // Isha
        val ishaHours = if (ishaMinutesAfterMaghrib != null) {
            sunsetHours + (ishaMinutesAfterMaghrib / 60.0)
        } else {
            val ishaDiff = computeSunAngleDiff(jd, latitude, ishaAngle, isMorning = false)
            dhuhrHours + ishaDiff
        }

        // Asr
        val asrDiff = computeAsrDiff(jd, latitude, shadowFactor)
        val asrHours = dhuhrHours + asrDiff

        // Convert day decimal hours to timestamp millis
        val baseMidnight = Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val fajrMillis = baseMidnight + (fajrHours * 3600000).toLong()
        val sunriseMillis = baseMidnight + (sunriseHours * 3600000).toLong()
        val dhuhrMillis = baseMidnight + (dhuhrHours * 3600000).toLong()
        val asrMillis = baseMidnight + (asrHours * 3600000).toLong()
        val maghribMillis = baseMidnight + (sunsetHours * 3600000).toLong()
        val ishaMillis = baseMidnight + (ishaHours * 3600000).toLong()

        // Ishraq is 15 minutes after sunrise
        val ishraqMillis = sunriseMillis + (15 * 60 * 1000)

        // Tahajjud: start of last third of the night (between Maghrib and next day's Fajr)
        // Next day fajr is approx fajrMillis + 24 hours
        val nextFajrMillis = fajrMillis + (24 * 3600000)
        val nightDuration = nextFajrMillis - maghribMillis
        val tahajjudMillis = maghribMillis + (nightDuration * 2 / 3)

        return PrayerTimesResult(
            fajr = fajrMillis,
            sunrise = sunriseMillis,
            dhuhr = dhuhrMillis,
            asr = asrMillis,
            maghrib = maghribMillis,
            isha = ishaMillis,
            tahajjud = tahajjudMillis,
            ishraq = ishraqMillis
        )
    }

    private data class MethodParams(
        val fajrAngle: Double,
        val ishaAngle: Double,
        val ishaMinutesAfterMaghrib: Double? = null
    )

    private fun getMethodAngles(methodKey: String): MethodParams {
        return when (methodKey) {
            CalculationMethod.MWL.key -> MethodParams(18.0, 17.0)
            CalculationMethod.ISNA.key -> MethodParams(15.0, 15.0)
            CalculationMethod.UMM_AL_QURA.key -> MethodParams(18.5, 0.0, 90.0)
            CalculationMethod.EGYPT.key -> MethodParams(19.5, 17.5)
            CalculationMethod.DUBAI.key -> MethodParams(18.2, 18.2)
            CalculationMethod.QATAR.key -> MethodParams(18.0, 0.0, 90.0)
            CalculationMethod.KUWAIT.key -> MethodParams(18.0, 17.5)
            CalculationMethod.SINGAPORE.key -> MethodParams(20.0, 18.0)
            CalculationMethod.TEHRAN.key -> MethodParams(17.7, 14.0)
            else -> MethodParams(18.0, 18.0) // Karachi default
        }
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun computeTransit(jd: Double, longitude: Double, tzOffset: Double): Double {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(toRadians(g)) + 0.020 * sin(toRadians(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val ra = toDegrees(atan2(cos(toRadians(e)) * sin(toRadians(l)), cos(toRadians(l)))) / 15.0
        val eqT = (q / 15.0) - fixHour(ra)
        return fixHour(12.0 + tzOffset - (longitude / 15.0) - eqT)
    }

    private fun computeSunAngleDiff(jd: Double, latitude: Double, angle: Double, isMorning: Boolean): Double {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(toRadians(g)) + 0.020 * sin(toRadians(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val dec = toDegrees(asin(sin(toRadians(e)) * sin(toRadians(l))))

        val term = (-sin(toRadians(angle)) - sin(toRadians(latitude)) * sin(toRadians(dec))) /
                (cos(toRadians(latitude)) * cos(toRadians(dec)))

        val clamped = term.coerceIn(-1.0, 1.0)
        val hourAngle = toDegrees(acos(clamped)) / 15.0
        return hourAngle
    }

    private fun computeAsrDiff(jd: Double, latitude: Double, shadowFactor: Double): Double {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(toRadians(g)) + 0.020 * sin(toRadians(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val dec = toDegrees(asin(sin(toRadians(e)) * sin(toRadians(l))))

        val asrAlt = toDegrees(atan(1.0 / (shadowFactor + tan(toRadians(abs(latitude - dec))))))
        val term = (sin(toRadians(asrAlt)) - sin(toRadians(latitude)) * sin(toRadians(dec))) /
                (cos(toRadians(latitude)) * cos(toRadians(dec)))

        val clamped = term.coerceIn(-1.0, 1.0)
        return toDegrees(acos(clamped)) / 15.0
    }

    private fun fixAngle(a: Double): Double {
        var res = a % 360.0
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(h: Double): Double {
        var res = h % 24.0
        if (res < 0) res += 24.0
        return res
    }

    private fun toRadians(deg: Double): Double = deg * Math.PI / 180.0
    private fun toDegrees(rad: Double): Double = rad * 180.0 / Math.PI

    /**
     * Calculates Qibla direction (bearing in degrees 0-360) and distance in km.
     */
    fun calculateQibla(latitude: Double, longitude: Double): Pair<Int, Int> {
        val lat1 = toRadians(latitude)
        val lat2 = toRadians(KAABA_LAT)
        val dLng = toRadians(KAABA_LNG - longitude)

        val y = sin(dLng)
        val x = cos(lat1) * tan(lat2) - sin(lat1) * cos(dLng)
        var qibla = toDegrees(atan2(y, x))
        qibla = (qibla + 360.0) % 360.0

        // Distance using Haversine
        val r = 6371.0 // Earth radius in km
        val dLat = lat2 - lat1
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val dist = (r * c).roundToInt()

        return Pair(qibla.roundToInt(), dist)
    }

    /**
     * Computes Islamic Hijri date string.
     */
    fun getHijriDate(date: Date = Date()): String {
        val cal = Calendar.getInstance().apply { time = date }
        val jd = julianDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))

        // Umm Al-Qura / standard astronomical conversion
        val l = jd.toLong() - 1948440 + 10632
        val n = ((l - 1) / 10631).toInt()
        val remL = l - 10631 * n + 354
        val j = (((10985 - remL) / 5316).toInt()) * (((50 * remL) / 17719).toInt()) +
                ((remL / 5670).toInt()) * (((43 * remL) / 15238).toInt())
        val remL2 = remL - (((30 - j) / 15).toInt()) * (((17719 * j) / 50).toInt()) -
                ((j / 16).toInt()) * (((15238 * j) / 43).toInt()) + 29
        val m = ((24 * remL2) / 709).toInt()
        val d = remL2 - ((709 * m) / 24).toInt()
        val y = 30 * n + j - 30

        val islamicMonths = listOf(
            "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
            "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
        )
        val monthName = islamicMonths.getOrElse((m - 1).coerceIn(0, 11)) { "Hijri" }
        return "$d $monthName $y AH"
    }

    fun formatTime24(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return "%02d:%02d".format(h, m)
    }

    fun formatTime12(millis: Long): Pair<String, String> {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        var h = cal.get(Calendar.HOUR)
        if (h == 0) h = 12
        val m = cal.get(Calendar.MINUTE)
        val ap = if (cal.get(Calendar.AM_PM) == Calendar.PM) "PM" else "AM"
        return Pair("%d:%02d".format(h, m), ap)
    }

    fun formatTime12(timeStr: String): Pair<String, String> {
        val parts = timeStr.split(":")
        val h24 = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val ap = if (h24 >= 12) "PM" else "AM"
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        return Pair("%d:%02d".format(h12, m), ap)
    }

    fun formatDisplay(timeStr: String, is24h: Boolean): String {
        if (is24h) return timeStr
        val (time, ap) = formatTime12(timeStr)
        return "$time $ap"
    }
}
