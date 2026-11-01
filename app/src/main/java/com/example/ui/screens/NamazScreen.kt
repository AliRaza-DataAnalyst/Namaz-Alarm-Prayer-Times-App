package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmItem
import com.example.data.model.AppSettings
import com.example.service.prayer.PrayerTimeCalculator
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NamazScreen(
    alarms: List<AlarmItem>,
    donePrayers: Map<String, Set<String>>,
    settings: AppSettings,
    currentTimeMillis: Long,
    streak: Int,
    onTogglePrayerDone: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayKey = remember(currentTimeMillis) {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(currentTimeMillis))
    }
    val todayDone = remember(donePrayers, todayKey) {
        donePrayers[todayKey] ?: emptySet()
    }

    val cal = remember(currentTimeMillis, settings.location, settings.calculationMethod, settings.madhab) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    }

    val calculatedTimes = remember(cal, settings.location, settings.calculationMethod, settings.madhab) {
        PrayerTimeCalculator.calculate(
            cal,
            settings.location.latitude,
            settings.location.longitude,
            settings.calculationMethod,
            settings.madhab
        )
    }

    val gregorianDate = remember(currentTimeMillis) {
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(Date(currentTimeMillis))
    }
    val hijriDate = remember(currentTimeMillis) {
        PrayerTimeCalculator.getHijriDate(Date(currentTimeMillis))
    }

    // Obligatory list with original Urdu titles & emojis
    val prayerList = remember(alarms) {
        listOf(
            Triple("p1", "Tahajjud", "🫀 تہجد کی نماز + صلوۃ الحاجات کی نماز"),
            Triple("p2", "Fajr", "💗 نمازِ فجر"),
            Triple("p_ishraq", "Ishraq", "☀️ نمازِ اشراق و چاشت"),
            Triple("p3", "Zuhr", "🫶 نمازِ ظہر"),
            Triple("p4", "Asr", "❤️ نمازِ عصر"),
            Triple("p5", "Maghrib", "🫶🤍 نمازِ مغرب"),
            Triple("p6", "Isha", "🫶 🫀 نمازِ عشاء")
        )
    }

    val obligatoryIds = listOf("p2", "p3", "p4", "p5", "p6")
    val doneCount = remember(todayDone) {
        obligatoryIds.count { todayDone.contains(it) }
    }
    val progress = (doneCount.toFloat() / obligatoryIds.size.toFloat()).coerceIn(0f, 1f)

    // Next Prayer calculation
    val nextPrayerInfo = remember(currentTimeMillis, alarms, calculatedTimes) {
        val candidates = listOf(
            Triple("Tahajjud", "🫀 تہجد کی نماز + صلوۃ الحاجات کی نماز", calculatedTimes.tahajjud),
            Triple("Fajr", "💗 نمازِ فجر", calculatedTimes.fajr),
            Triple("Ishraq", "☀️ نمازِ اشراق و چاشت", calculatedTimes.ishraq),
            Triple("Zuhr", "🫶 نمازِ ظہر", calculatedTimes.dhuhr),
            Triple("Asr", "❤️ نمازِ عصر", calculatedTimes.asr),
            Triple("Maghrib", "🫶🤍 نمازِ مغرب", calculatedTimes.maghrib),
            Triple("Isha", "🫶 🫀 نمازِ عشاء", calculatedTimes.isha)
        )

        var bestName = "Fajr"
        var bestUrdu = "نماز فجر"
        var bestDiff = Long.MAX_VALUE
        var bestTargetTime = 0L

        for ((name, urdu, targetTime) in candidates) {
            var diff = targetTime - currentTimeMillis
            if (diff < 0) {
                // Next day's time
                diff += 24 * 3600 * 1000L
            }
            if (diff < bestDiff) {
                bestDiff = diff
                bestName = name
                bestUrdu = urdu
                bestTargetTime = targetTime
            }
        }

        val totalSec = (bestDiff / 1000).coerceAtLeast(0)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        CountdownInfo(bestName, bestUrdu, h, m, s, bestTargetTime)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Namaz Tracker",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Daily obligatory & voluntary prayers",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (streak > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(AmberBgTint)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = AmberGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$streak Day Streak",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                        }
                    }
                }
            }
        }

        // Hero Card: Next Prayer & Countdown
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("next_prayer_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        listOf(EmeraldDark.copy(alpha = 0.5f), DarkSurfaceBorder)
                    )
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(EmeraldBgTint.copy(alpha = 0.8f), Color.Transparent),
                                radius = 600f
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "NEXT PRAYER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp,
                                    color = EmeraldLight
                                )
                            }

                            Text(
                                text = PrayerTimeCalculator.formatDisplay(
                                    PrayerTimeCalculator.formatTime24(nextPrayerInfo.targetTime),
                                    settings.is24HourFormat
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldLight
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = nextPrayerInfo.name,
                                fontSize = if (nextPrayerInfo.name.length > 7) 28.sp else 34.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = Color.White
                            )
                            Text(
                                text = nextPrayerInfo.urdu,
                                fontSize = 14.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                color = TextSecondary,
                                modifier = Modifier
                                    .padding(bottom = 4.dp, start = 8.dp)
                                    .weight(1f, fill = false)
                            )
                        }

                        // Countdown numbers
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "starts in",
                                fontSize = 13.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "${nextPrayerInfo.hours}h",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AmberGold
                            )
                            Text(":", color = TextMuted, fontSize = 20.sp)
                            Text(
                                text = "%02dm".format(nextPrayerInfo.minutes),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AmberGold
                            )
                            Text(":", color = TextMuted, fontSize = 20.sp)
                            Text(
                                text = "%02ds".format(nextPrayerInfo.seconds),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = AmberLight.copy(alpha = 0.8f)
                            )
                        }

                        HorizontalDivider(color = DarkSurfaceBorder, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(gregorianDate, fontSize = 11.sp, color = TextSecondary)
                            Text(
                                hijriDate,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberGold
                            )
                        }
                    }
                }
            }
        }

        // Today's Progress Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Today's Prayers",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            if (doneCount == obligatoryIds.size) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldBgTint)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Completed ✨",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldLight
                                    )
                                }
                            }
                        }

                        Text(
                            text = "$doneCount of ${obligatoryIds.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldGreen,
                        trackColor = DarkSurfaceVariant
                    )
                }
            }
        }

        // Prayers Checklist Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    prayerList.forEachIndexed { index, (id, name, urdu) ->
                        val isDone = todayDone.contains(id)
                        val alarm = alarms.find { it.prayerName == name }
                        val displayTime = alarm?.time ?: when (name) {
                            "Tahajjud" -> PrayerTimeCalculator.formatTime24(calculatedTimes.tahajjud)
                            "Fajr" -> PrayerTimeCalculator.formatTime24(calculatedTimes.fajr)
                            "Ishraq" -> PrayerTimeCalculator.formatTime24(calculatedTimes.ishraq)
                            "Zuhr" -> PrayerTimeCalculator.formatTime24(calculatedTimes.dhuhr)
                            "Asr" -> PrayerTimeCalculator.formatTime24(calculatedTimes.asr)
                            "Maghrib" -> PrayerTimeCalculator.formatTime24(calculatedTimes.maghrib)
                            "Isha" -> PrayerTimeCalculator.formatTime24(calculatedTimes.isha)
                            else -> "06:00"
                        }

                        if (index > 0) {
                            HorizontalDivider(color = DarkSurfaceBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
                        }

                        PrayerChecklistRow(
                            id = id,
                            name = name,
                            urdu = urdu,
                            time = PrayerTimeCalculator.formatDisplay(displayTime, settings.is24HourFormat),
                            isDone = isDone,
                            onToggle = { onTogglePrayerDone(id) }
                        )
                    }
                }
            }
        }

        // Astronomical Timetable Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Astronomical Timetable",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = settings.madhab.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = "${settings.location.city} (${settings.calculationMethod})",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                color = TextMuted
                            )
                        }

                        TextButton(
                            onClick = onOpenSettings,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Change", color = AmberGold, fontSize = 12.sp)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Timetable Grid
                    val tableItems = listOf(
                        Pair("Tahajjud", calculatedTimes.tahajjud),
                        Pair("Fajr", calculatedTimes.fajr),
                        Pair("Sunrise", calculatedTimes.sunrise),
                        Pair("Ishraq", calculatedTimes.ishraq),
                        Pair("Zuhr", calculatedTimes.dhuhr),
                        Pair("Asr", calculatedTimes.asr),
                        Pair("Maghrib", calculatedTimes.maghrib),
                        Pair("Isha", calculatedTimes.isha)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tableItems.take(4).forEach { (label, millis) ->
                                TimetableCell(
                                    label = label,
                                    time = PrayerTimeCalculator.formatDisplay(
                                        PrayerTimeCalculator.formatTime24(millis),
                                        settings.is24HourFormat
                                    )
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tableItems.drop(4).forEach { (label, millis) ->
                                TimetableCell(
                                    label = label,
                                    time = PrayerTimeCalculator.formatDisplay(
                                        PrayerTimeCalculator.formatTime24(millis),
                                        settings.is24HourFormat
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerChecklistRow(
    id: String,
    name: String,
    urdu: String,
    time: String,
    isDone: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .background(if (isDone) EmeraldBgTint.copy(alpha = 0.35f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("prayer_row_$id"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Circular Check Button
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isDone) EmeraldGreen else Color.Transparent)
                .clickable { onToggle() }
                .then(
                    if (!isDone) Modifier.background(Color.Transparent) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = TextMuted,
                            radius = size.minDimension / 2f - 2f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // English name and time: uniform 14.sp font size across all prayers, guaranteed 1 line
        Column(
            modifier = Modifier.widthIn(min = 72.dp, max = 82.dp)
        ) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDone) TextMuted else Color.White,
                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = time,
                fontSize = 11.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false
            )
        }

        // Urdu title with original emojis: all text on ONE same line
        Text(
            text = urdu,
            fontSize = if (name == "Tahajjud") 11.sp else 12.5.sp,
            maxLines = 1,
            softWrap = false,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = if (isDone) TextMuted else Color(0xFFE2E8F0),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimetableCell(label: String, time: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCardBg)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 11.sp, color = TextMuted)
            Text(
                time,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        }
    }
}

private data class CountdownInfo(
    val name: String,
    val urdu: String,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val targetTime: Long
)
