package com.example

import com.example.data.model.CalculationMethod
import com.example.data.model.Madhab
import com.example.service.prayer.PrayerTimeCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class NamazCalculationTest {

    @Test
    fun testPrayerTimesSequence() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 15, 12, 0, 0)
        }
        val result = PrayerTimeCalculator.calculate(
            calendar = cal,
            latitude = 24.8607,
            longitude = 67.0011,
            methodKey = CalculationMethod.KARACHI.key,
            madhabKey = Madhab.HANAFI.key
        )

        assertTrue("Fajr must be before sunrise", result.fajr < result.sunrise)
        assertTrue("Sunrise must be before Dhuhr", result.sunrise < result.dhuhr)
        assertTrue("Dhuhr must be before Asr", result.dhuhr < result.asr)
        assertTrue("Asr must be before Maghrib", result.asr < result.maghrib)
        assertTrue("Maghrib must be before Isha", result.maghrib < result.isha)
        assertTrue("Ishraq must be after sunrise", result.ishraq > result.sunrise)
    }

    @Test
    fun testHanafiVsShafiiAsr() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 12, 0, 0)
        }
        val hanafiResult = PrayerTimeCalculator.calculate(
            calendar = cal,
            latitude = 24.8607,
            longitude = 67.0011,
            methodKey = CalculationMethod.KARACHI.key,
            madhabKey = Madhab.HANAFI.key
        )
        val shafiiResult = PrayerTimeCalculator.calculate(
            calendar = cal,
            latitude = 24.8607,
            longitude = 67.0011,
            methodKey = CalculationMethod.KARACHI.key,
            madhabKey = Madhab.SHAFII.key
        )

        // Hanafi Asr (shadow factor 2) is later in the day than Shafi'i (shadow factor 1)
        assertTrue("Hanafi Asr must be later than Shafi'i Asr", hanafiResult.asr > shafiiResult.asr)
    }

    @Test
    fun testQiblaBearingAndDistance() {
        // Karachi coordinates: 24.8607 N, 67.0011 E
        val (bearing, distance) = PrayerTimeCalculator.calculateQibla(24.8607, 67.0011)

        // Qibla from Karachi is approximately west-southwest (~260-275 degrees)
        assertTrue("Qibla bearing should be around 260-275 degrees", bearing in 250..285)
        // Distance from Karachi to Makkah is ~2800 km
        assertTrue("Distance to Makkah should be ~2800 km", distance in 2500..3200)
    }

    @Test
    fun testTimeFormatting() {
        val (t1, ap1) = PrayerTimeCalculator.formatTime12("04:35")
        assertEquals("4:35", t1)
        assertEquals("AM", ap1)

        val (t2, ap2) = PrayerTimeCalculator.formatTime12("16:20")
        assertEquals("4:20", t2)
        assertEquals("PM", ap2)

        val (t3, ap3) = PrayerTimeCalculator.formatTime12("12:00")
        assertEquals("12:00", t3)
        assertEquals("PM", ap3)

        val (t4, ap4) = PrayerTimeCalculator.formatTime12("00:15")
        assertEquals("12:15", t4)
        assertEquals("AM", ap4)

        assertEquals("4:20 PM", PrayerTimeCalculator.formatDisplay("16:20", is24h = false))
        assertEquals("16:20", PrayerTimeCalculator.formatDisplay("16:20", is24h = true))
    }

    @Test
    fun testHijriDate() {
        val hijri = PrayerTimeCalculator.getHijriDate()
        assertTrue("Hijri date should contain AH", hijri.contains("AH"))
    }
}
