package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.datasource.IslamicData
import com.example.data.model.TasbihState
import com.example.ui.theme.*

@Composable
fun TasbihScreen(
    tasbihState: TasbihState,
    onTap: () -> Unit,
    onSetGoal: (Int) -> Unit,
    onSelectDhikr: (String) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }

    val selectedDhikr = remember(tasbihState.selectedDhikrId) {
        IslamicData.DHIKR_PRESETS.find { it.id == tasbihState.selectedDhikrId }
            ?: IslamicData.DHIKR_PRESETS.first()
    }

    val rounds = tasbihState.count / tasbihState.goal
    val currentLap = tasbihState.count % tasbihState.goal
    val displayLap = if (tasbihState.count > 0 && currentLap == 0) tasbihState.goal else currentLap
    val lapProgress = (displayLap.toFloat() / tasbihState.goal.toFloat()).coerceIn(0f, 1f)

    // Interaction source for button press scale animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tasbih_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
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
                        text = "Digital Tasbih",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text("📿", fontSize = 20.sp)
                }
                Text(
                    text = "Dhikr counter & remembrance",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            FilledTonalIconButton(
                onClick = { soundEnabled = !soundEnabled },
                shape = RoundedCornerShape(12.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (soundEnabled) AmberBgTint else DarkSurface,
                    contentColor = if (soundEnabled) AmberGold else TextMuted
                )
            ) {
                Icon(
                    imageVector = if (soundEnabled) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                    contentDescription = "Toggle Audio"
                )
            }
        }

        // Dhikr Presets Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(IslamicData.DHIKR_PRESETS) { preset ->
                val isSelected = preset.id == selectedDhikr.id
                Card(
                    modifier = Modifier
                        .clickable { onSelectDhikr(preset.id) }
                        .testTag("dhikr_preset_${preset.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) EmeraldBgTint else DarkSurface
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isSelected) EmeraldGreen else DarkSurfaceBorder
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = preset.arabic,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) EmeraldLight else Color.White
                        )
                        Text(
                            text = preset.transliteration,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Selected Dhikr Details Card
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = selectedDhikr.arabic,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberGold
                )
                Text(
                    text = "${selectedDhikr.transliteration} • ${selectedDhikr.urdu}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = "\"${selectedDhikr.meaning}\"",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Big Tactile Counter Button with Circular Progress Ring
        Box(
            modifier = Modifier
                .size(260.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background track & Animated progress arc
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 10.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f

                // Track
                drawCircle(
                    color = Color(0xFF272F2A),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )

                // Progress Arc
                drawArc(
                    color = EmeraldGreen,
                    startAngle = -90f,
                    sweepAngle = 360f * lapProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Center Tactile Tap Button
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF2F7D46), Color(0xFF143D22)),
                            radius = 280f
                        )
                    )
                    .border(5.dp, EmeraldGreen, CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = EmeraldLight)
                    ) {
                        onTap()
                    }
                    .testTag("tasbih_tap_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${tasbihState.count}",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        text = "TAP TO COUNT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight.copy(alpha = 0.9f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Goal Selection Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(33, 99, 100).forEach { goalValue ->
                val isSelected = tasbihState.goal == goalValue
                Button(
                    onClick = { onSetGoal(goalValue) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) AmberGold else DarkSurface,
                        contentColor = if (isSelected) Color.Black else TextSecondary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isSelected) AmberGold else DarkSurfaceBorder
                        )
                    )
                ) {
                    Text("Goal $goalValue", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        // Stats Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Lap Progress", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$displayLap / ${tasbihState.goal}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                VerticalDivider(modifier = Modifier.height(30.dp), color = DarkSurfaceBorder)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rounds Done", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$rounds",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = EmeraldLight
                    )
                }

                VerticalDivider(modifier = Modifier.height(30.dp), color = DarkSurfaceBorder)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Count", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${tasbihState.totalCount}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = AmberGold
                    )
                }
            }
        }

        // Reset Counter with Confirmation
        if (showResetConfirm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(RedAlert.copy(alpha = 0.5f))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reset Tasbih counter to 0?", fontSize = 13.sp, color = Color.White)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { showResetConfirm = false },
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onReset()
                                showResetConfirm = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedAlert,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            OutlinedButton(
                onClick = {
                    if (tasbihState.count > 0 || tasbihState.totalCount > 0) {
                        showResetConfirm = true
                    }
                },
                enabled = tasbihState.count > 0 || tasbihState.totalCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_tasbih_button"),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary,
                    disabledContentColor = TextMuted
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (tasbihState.count == 0 && tasbihState.totalCount == 0) "Counter is at 0" else "Reset Counter to 0",
                    fontSize = 13.sp
                )
            }
        }
    }
}
