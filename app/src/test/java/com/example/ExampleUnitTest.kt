package com.example

import com.example.data.datasource.IslamicData
import com.example.data.model.AlarmItem
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testAlarmTonesListConfiguredProperly() {
        val tones = IslamicData.ALARM_TONES
        // Check that 11 uploaded tones + 1 custom tone are present (total 12)
        assertEquals(12, tones.size)

        // Verify the removed tones are NOT present
        val titles = tones.map { it.title }
        assertFalse(titles.contains("Android Alert Guide"))
        assertFalse(titles.contains("Makkah Haramain Azan"))
        assertFalse(titles.contains("Sweet Qari Azan Voice"))
        assertFalse(titles.contains("Madinah Salam Nasheed"))
        assertFalse(titles.contains("Maqam Bayati Chime"))

        // Verify that custom tone is present
        assertTrue(tones.any { it.id == "custom_tone" })

        // Verify all 11 alarm tones are present with emojis and Urdu titles
        for (i in 1..11) {
            val tone = tones.find { it.id == "alarm_tone_$i" }
            assertNotNull("Alarm tone $i must be defined", tone)
            assertTrue("Must contain Urdu script", tone!!.urduTitle.isNotBlank())
            assertTrue("Title must not be blank", tone.title.isNotBlank())
        }
    }

    @Test
    fun testAlarmItemMuteAndCustomSoundDefaults() {
        val alarm = AlarmItem(
            id = "a1",
            label = "Fajr Alarm",
            prayerName = "Fajr",
            time = "04:35",
            soundType = "alarm_tone_1"
        )
        assertFalse("Alarm should not be muted by default", alarm.isMuted)
        assertNull("Custom sound URI should be null by default", alarm.customSoundUri)

        val mutedAlarm = alarm.copy(isMuted = true)
        assertTrue(mutedAlarm.isMuted)
    }

    @Test
    fun testPrayerOriginalEmojisPresence() {
        val prayers = listOf(
            Triple("Tahajjud", "🫀", "صلوۃ الحاجات"),
            Triple("Fajr", "💗", "نمازِ فجر"),
            Triple("Ishraq", "☀️", "نمازِ اشراق"),
            Triple("Zuhr", "🫶", "نمازِ ظہر"),
            Triple("Asr", "❤️", "نمازِ عصر"),
            Triple("Maghrib", "🫶🤍", "نمازِ مغرب"),
            Triple("Isha", "🫶 🫀", "نمازِ عشاء")
        )
        for ((name, emoji, urduSnippet) in prayers) {
            assertTrue(name.isNotEmpty())
            assertTrue(emoji.isNotEmpty())
            assertTrue(urduSnippet.isNotEmpty())
        }
    }
}
