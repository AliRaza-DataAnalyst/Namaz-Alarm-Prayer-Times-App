package com.example.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.datasource.IslamicData
import com.example.service.alarm.AlarmSoundService
import com.example.service.prayer.PrayerTimeCalculator
import com.example.ui.theme.*

@Composable
fun RingingAlarmDialog(
    info: AlarmSoundService.Companion.RingingInfo,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    // Intercept physical back press to stop the alarm
    BackHandler(onBack = onDismiss)

    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "ringing_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val (time12, ap) = remember(info.time) {
        PrayerTimeCalculator.formatTime12(info.time)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .testTag("ringing_alarm_dialog"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glowing radial aura
        Box(
            modifier = Modifier
                .size(340.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EmeraldGreen.copy(alpha = 0.25f),
                            AmberGold.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, AmberGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PRAYER ALARM ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = AmberGold
                    )
                }
            }

            // Center: Mosque Moon Icon, Huge Clock, Label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(EmeraldBgTint)
                        .border(2.dp, EmeraldGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🕌", fontSize = 42.sp)
                }

                // Huge Time
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (is24Hour) info.time else time12,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    if (!is24Hour) {
                        Text(
                            text = ap,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Normal,
                            color = AmberGold
                        )
                    }
                }

                info.prayerName?.let { p ->
                    Text(
                        text = "$p Prayer",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                }

                Text(
                    text = info.label,
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                val toneInfo = remember(info.soundType) {
                    IslamicData.ALARM_TONES.find { it.id == info.soundType }
                }
                val toneDisplayName = toneInfo?.title ?: info.soundType.replace("_", " ")
                val toneUrdu = toneInfo?.urduTitle ?: ""

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (toneUrdu.isNotBlank()) "Playing: $toneDisplayName ($toneUrdu)" else "Playing: $toneDisplayName",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("dismiss_alarm_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGold,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Stop Alarm",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSnooze(5) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                        )
                    ) {
                        Text("Snooze 5 min", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onSnooze(10) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                        )
                    ) {
                        Text("Snooze 10 min", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
