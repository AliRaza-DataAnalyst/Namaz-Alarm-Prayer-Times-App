package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.service.prayer.PrayerTimeCalculator
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaScreen(
    settings: AppSettings,
    deviceHeading: Float,
    hasSensor: Boolean,
    onManualHeadingChange: (Float) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (qiblaBearing, distanceKm) = remember(settings.location) {
        PrayerTimeCalculator.calculateQibla(
            settings.location.latitude,
            settings.location.longitude
        )
    }

    // Relative angle between current heading and Qibla (0 - 360)
    val relativeAngle = remember(qiblaBearing, deviceHeading) {
        var diff = qiblaBearing - deviceHeading
        while (diff < 0) diff += 360f
        while (diff >= 360) diff -= 360f
        diff
    }

    val isAligned = relativeAngle <= 4f || relativeAngle >= 356f
    val turnAmount = if (relativeAngle > 180f) 360f - relativeAngle else relativeAngle
    val turnDirection = if (relativeAngle > 180f) "left" else "right"

    // Continuous shortest-arc angle accumulators to eliminate 360-degree jump spins
    var continuousHeading by remember { mutableFloatStateOf(deviceHeading) }
    LaunchedEffect(deviceHeading) {
        var diff = (deviceHeading - ((continuousHeading % 360f + 360f) % 360f))
        while (diff > 180f) diff -= 360f
        while (diff < -180f) diff += 360f
        continuousHeading += diff
    }

    var continuousRelative by remember { mutableFloatStateOf(relativeAngle) }
    LaunchedEffect(relativeAngle) {
        var diff = (relativeAngle - ((continuousRelative % 360f + 360f) % 360f))
        while (diff > 180f) diff -= 360f
        while (diff < -180f) diff += 360f
        continuousRelative += diff
    }

    // Smooth animated rotation taking the shortest path
    val animatedHeading by animateFloatAsState(
        targetValue = continuousHeading,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "compass_heading"
    )

    val animatedRelative by animateFloatAsState(
        targetValue = continuousRelative,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "compass_relative"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "Qibla Compass",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Facing Holy Kaaba in Makkah",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Surface(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = settings.location.city,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Alignment Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAligned) EmeraldBgTint else DarkSurface
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (isAligned) EmeraldGreen else DarkSurfaceBorder
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isAligned) EmeraldLight else AmberGold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAligned) {
                        "✨ You are facing the Qibla (Makkah)!"
                    } else {
                        "Turn ${turnAmount.toInt()}° $turnDirection to face Qibla"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAligned) EmeraldLight else TextSecondary
                )
            }
        }

        // Main Compass Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Distance:", fontSize = 11.sp, color = TextMuted)
                        Text(
                            text = "%,d km".format(distanceKm),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Qibla Bearing:", fontSize = 11.sp, color = TextMuted)
                        Text(
                            text = "$qiblaBearing°",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Compass Rose & Dial
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer rotating compass rose (counter-rotated by animatedHeading)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(-animatedHeading)
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f - 12f

                        // Outer ring (glows emerald when aligned)
                        drawCircle(
                            color = if (isAligned) EmeraldGreen else Color(0xFF272F2A),
                            radius = radius,
                            style = Stroke(width = if (isAligned) 4.dp.toPx() else 3.dp.toPx())
                        )

                        // Red North marker triangle on the dial
                        val northArrow = Path().apply {
                            moveTo(center.x, center.y - radius + 2.dp.toPx())
                            lineTo(center.x - 7.dp.toPx(), center.y - radius + 15.dp.toPx())
                            lineTo(center.x + 7.dp.toPx(), center.y - radius + 15.dp.toPx())
                            close()
                        }
                        drawPath(northArrow, RedAlert)

                        // 36 Degree ticks
                        for (i in 0 until 36) {
                            val angleRad = Math.toRadians((i * 10).toDouble())
                            val isMajor = i % 9 == 0
                            val isMedium = i % 3 == 0
                            val tickLen = when {
                                isMajor -> 14.dp.toPx()
                                isMedium -> 9.dp.toPx()
                                else -> 5.dp.toPx()
                            }
                            val color = when {
                                isMajor -> AmberGold
                                isMedium -> TextSecondary
                                else -> TextMuted
                            }

                            val startX = center.x + (radius - tickLen) * sin(angleRad).toFloat()
                            val startY = center.y - (radius - tickLen) * cos(angleRad).toFloat()
                            val endX = center.x + radius * sin(angleRad).toFloat()
                            val endY = center.y - radius * cos(angleRad).toFloat()

                            drawLine(
                                color = color,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx()
                            )
                        }
                    }

                    // Cardinals overlay (fixed or rotating with outer ring)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(-animatedHeading)
                    ) {
                        Text(
                            text = "N",
                            color = RedAlert,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        )
                        Text(
                            text = "S",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        )
                        Text(
                            text = "E",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp)
                        )
                        Text(
                            text = "W",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 16.dp)
                        )
                    }

                    // Kaaba Needle pointing to relative Qibla angle
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(animatedRelative),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val pointerLen = size.minDimension / 2f - 30f

                            // Upwards Golden Kaaba Pointer
                            val upPath = Path().apply {
                                moveTo(center.x, center.y - pointerLen)
                                lineTo(center.x + 12f, center.y)
                                lineTo(center.x, center.y - 8f)
                                close()
                            }
                            drawPath(upPath, AmberGold)

                            val upPathLeft = Path().apply {
                                moveTo(center.x, center.y - pointerLen)
                                lineTo(center.x - 12f, center.y)
                                lineTo(center.x, center.y - 8f)
                                close()
                            }
                            drawPath(upPathLeft, AmberLight)

                            // Downwards counterweight
                            val downPath = Path().apply {
                                moveTo(center.x, center.y + pointerLen * 0.5f)
                                lineTo(center.x + 8f, center.y)
                                lineTo(center.x - 8f, center.y)
                                close()
                            }
                            drawPath(downPath, DarkSurfaceBorder)

                            // Center Pivot Dot
                            drawCircle(color = EmeraldGreen, radius = 7.dp.toPx(), center = center)
                            drawCircle(color = Color.Black, radius = 3.dp.toPx(), center = center)
                        }

                        // Kaaba icon at the tip of the needle
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 18.dp)
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🕋", fontSize = 14.sp)
                        }
                    }

                    // Center heading overlay
                    Column(
                        modifier = Modifier.padding(top = 70.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${deviceHeading.toInt()}°",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            text = "HEADING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Sensor Status / Manual Slider Fallback
                if (!hasSensor) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Compass sensor not available. Adjust heading slider manually:",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Slider(
                            value = deviceHeading,
                            onValueChange = onManualHeadingChange,
                            valueRange = 0f..359f,
                            colors = SliderDefaults.colors(
                                thumbColor = AmberGold,
                                activeTrackColor = AmberGold,
                                inactiveTrackColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Explore,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Live Compass Sensor Active", fontSize = 11.sp, color = EmeraldLight)
                    }
                }
            }
        }

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Accurate Qibla Guidance",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = "The Qibla is the direction of the Kaaba in the Sacred Mosque (Masjid al-Haram) in Makkah, Saudi Arabia. For highest precision, hold your phone flat in your palm and move it in a figure-8 motion to calibrate the magnetometer.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
