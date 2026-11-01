package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.datasource.IslamicData
import com.example.data.model.AppSettings
import com.example.data.model.CalculationMethod
import com.example.data.model.Madhab
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentSettings: AppSettings,
    onSaveSettings: (AppSettings) -> Unit,
    onDetectGps: () -> Unit,
    onPreviewSound: (String, String?) -> Unit,
    onStopPreview: () -> Unit,
    getMobileVolume: () -> Pair<Int, Int>,
    setMobileVolume: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var localSettings by remember { mutableStateOf(currentSettings) }
    var selectedCityName by remember { mutableStateOf(currentSettings.location.city) }
    var latText by remember { mutableStateOf(currentSettings.location.latitude.toString()) }
    var lngText by remember { mutableStateOf(currentSettings.location.longitude.toString()) }
    var previewingSound by remember { mutableStateOf<String?>(null) }
    var cityExpanded by remember { mutableStateOf(false) }
    var calcExpanded by remember { mutableStateOf(false) }
    var toneDropdownExpanded by remember { mutableStateOf(false) }

    // Read initial device alarm volume
    val (initialVol, maxVol) = remember { getMobileVolume() }
    var currentDeviceVol by remember { mutableIntStateOf(initialVol) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            localSettings = localSettings.copy(
                soundType = "custom_tone",
                customSoundUri = uri.toString()
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onStopPreview()
        }
    }

    Dialog(
        onDismissRequest = {
            onStopPreview()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(top = 28.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("settings_dialog"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Settings & Tones ⚙️",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Prayer calculations, alarm tones, and mobile volume",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        IconButton(
                            onClick = {
                                onStopPreview()
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceBorder, thickness = 1.dp)

                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // SECTION 1: Alarm Sound Tone (Dropdown Style with 11 Uploaded Tones & Custom Audio)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "ALARM SOUND TONE (اذان و نغمات)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = AmberGold
                                    )
                                }

                                if (previewingSound != null) {
                                    TextButton(
                                        onClick = {
                                            onStopPreview()
                                            previewingSound = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, tint = RedAlert, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Stop Preview", fontSize = 11.sp, color = RedAlert)
                                    }
                                }
                            }

                            // Tone Dropdown Selector
                            ExposedDropdownMenuBox(
                                expanded = toneDropdownExpanded,
                                onExpandedChange = { toneDropdownExpanded = !toneDropdownExpanded }
                            ) {
                                val currentTone = IslamicData.ALARM_TONES.find { it.id == localSettings.soundType }
                                    ?: IslamicData.ALARM_TONES.first()

                                OutlinedTextField(
                                    value = "${currentTone.title} (${currentTone.urduTitle})",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toneDropdownExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberGold,
                                        unfocusedBorderColor = DarkSurfaceBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = DarkCardBg,
                                        unfocusedContainerColor = DarkCardBg
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                )

                                ExposedDropdownMenu(
                                    expanded = toneDropdownExpanded,
                                    onDismissRequest = {
                                        toneDropdownExpanded = false
                                        onStopPreview()
                                        previewingSound = null
                                    },
                                    modifier = Modifier.background(DarkCardBg)
                                ) {
                                    IslamicData.ALARM_TONES.forEach { tone ->
                                        val isCurrent = localSettings.soundType == tone.id
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                                                        Text(
                                                            text = tone.title,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                                            color = if (isCurrent) AmberGold else Color.White
                                                        )
                                                        Text(
                                                            text = tone.urduTitle,
                                                            fontSize = 11.sp,
                                                            color = if (isCurrent) AmberGold.copy(alpha = 0.85f) else TextSecondary
                                                        )
                                                    }
                                                    if (isCurrent && previewingSound == tone.id) {
                                                        Icon(
                                                            imageVector = Icons.Default.VolumeUp,
                                                            contentDescription = "Playing",
                                                            tint = AmberGold,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                if (tone.isCustom) {
                                                    onStopPreview()
                                                    previewingSound = null
                                                    audioPickerLauncher.launch("audio/*")
                                                    toneDropdownExpanded = false
                                                } else {
                                                    localSettings = localSettings.copy(
                                                        soundType = tone.id,
                                                        customSoundUri = null
                                                    )
                                                    // Automatically start preview and switch smoothly on each click
                                                    previewingSound = tone.id
                                                    onPreviewSound(tone.id, null)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Tone Preview Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (localSettings.soundType == "custom_tone") {
                                        "Custom Device Audio Selected 📁"
                                    } else {
                                        "Selected Tone Active 🔔"
                                    },
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )

                                Button(
                                    onClick = {
                                        if (previewingSound != null) {
                                            onStopPreview()
                                            previewingSound = null
                                        } else {
                                            previewingSound = localSettings.soundType
                                            onPreviewSound(localSettings.soundType, localSettings.customSoundUri)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (previewingSound != null) RedBgTint else AmberBgTint,
                                        contentColor = if (previewingSound != null) RedAlert else AmberGold
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (previewingSound != null) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (previewingSound != null) "Stop" else "Preview Tone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider(color = DarkSurfaceBorder, thickness = 0.8.dp)

                        // SECTION 2: Mobile Phone Alarm Volume Stream
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "MOBILE PHONE ALARM VOLUME 🔊",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = AmberGold
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                                            Text("Device Alarm Stream Level", fontSize = 12.sp, color = Color.White)
                                        }
                                        Text(
                                            "$currentDeviceVol / $maxVol",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = AmberGold
                                        )
                                    }

                                    Slider(
                                        value = currentDeviceVol.toFloat(),
                                        onValueChange = {
                                            val newVol = it.toInt().coerceIn(0, maxVol)
                                            currentDeviceVol = newVol
                                            setMobileVolume(newVol)
                                        },
                                        valueRange = 0f..maxVol.toFloat(),
                                        steps = (maxVol - 1).coerceAtLeast(0),
                                        colors = SliderDefaults.colors(
                                            thumbColor = AmberGold,
                                            activeTrackColor = AmberGold,
                                            inactiveTrackColor = DarkSurfaceVariant
                                        )
                                    )

                                    Text(
                                        text = "Changes directly adjust your mobile device's system Alarm Volume stream (AudioManager.STREAM_ALARM).",
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = DarkSurfaceBorder, thickness = 0.8.dp)

                        // SECTION 3: Location & Coordinates
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "LOCATION & COORDINATES",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = AmberGold
                                    )
                                }

                                TextButton(
                                    onClick = onDetectGps,
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.MyLocation, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Auto GPS", fontSize = 11.sp, color = EmeraldLight)
                                }
                            }

                            // City Dropdown Selector
                            ExposedDropdownMenuBox(
                                expanded = cityExpanded,
                                onExpandedChange = { cityExpanded = !cityExpanded }
                            ) {
                                OutlinedTextField(
                                    value = "$selectedCityName (${localSettings.location.country})",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select City", fontSize = 11.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberGold,
                                        unfocusedBorderColor = DarkSurfaceBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = DarkCardBg,
                                        unfocusedContainerColor = DarkCardBg
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                )

                                ExposedDropdownMenu(
                                    expanded = cityExpanded,
                                    onDismissRequest = { cityExpanded = false },
                                    modifier = Modifier.background(DarkCardBg)
                                ) {
                                    IslamicData.POPULAR_CITIES.forEach { city ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("${city.city}, ${city.country}", fontSize = 13.sp, color = Color.White)
                                                    Text(
                                                        "${city.latitude}° N, ${city.longitude}° E",
                                                        fontSize = 10.sp,
                                                        color = TextMuted,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedCityName = city.city
                                                latText = city.latitude.toString()
                                                lngText = city.longitude.toString()
                                                localSettings = localSettings.copy(location = city)
                                                cityExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Latitude and Longitude Manual Text Fields
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = latText,
                                    onValueChange = {
                                        latText = it
                                        it.toDoubleOrNull()?.let { lat ->
                                            localSettings = localSettings.copy(
                                                location = localSettings.location.copy(latitude = lat)
                                            )
                                        }
                                    },
                                    label = { Text("Latitude", fontSize = 10.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberGold,
                                        unfocusedBorderColor = DarkSurfaceBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = DarkCardBg,
                                        unfocusedContainerColor = DarkCardBg
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = lngText,
                                    onValueChange = {
                                        lngText = it
                                        it.toDoubleOrNull()?.let { lng ->
                                            localSettings = localSettings.copy(
                                                location = localSettings.location.copy(longitude = lng)
                                            )
                                        }
                                    },
                                    label = { Text("Longitude", fontSize = 10.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberGold,
                                        unfocusedBorderColor = DarkSurfaceBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = DarkCardBg,
                                        unfocusedContainerColor = DarkCardBg
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider(color = DarkSurfaceBorder, thickness = 0.8.dp)

                        // SECTION 4: Calculation Method
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "ISLAMIC CALCULATION METHOD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = AmberGold
                            )

                            ExposedDropdownMenuBox(
                                expanded = calcExpanded,
                                onExpandedChange = { calcExpanded = !calcExpanded }
                            ) {
                                val currentMethod = CalculationMethod.entries.find { it.key == localSettings.calculationMethod }
                                    ?: CalculationMethod.KARACHI

                                OutlinedTextField(
                                    value = currentMethod.title,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = calcExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberGold,
                                        unfocusedBorderColor = DarkSurfaceBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = DarkCardBg,
                                        unfocusedContainerColor = DarkCardBg
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                )

                                ExposedDropdownMenu(
                                    expanded = calcExpanded,
                                    onDismissRequest = { calcExpanded = false },
                                    modifier = Modifier.background(DarkCardBg)
                                ) {
                                    CalculationMethod.entries.forEach { method ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(method.title, fontSize = 13.sp, color = Color.White)
                                                    Text(method.desc, fontSize = 10.sp, color = TextMuted)
                                                }
                                            },
                                            onClick = {
                                                localSettings = localSettings.copy(calculationMethod = method.key)
                                                calcExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = DarkSurfaceBorder, thickness = 0.8.dp)

                        // SECTION 5: Madhab (Asr Time)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "ASR PRAYER JURISTIC METHOD (MADHAB)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = AmberGold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Madhab.entries.forEach { madhab ->
                                    val isSelected = localSettings.madhab == madhab.key
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                localSettings = localSettings.copy(madhab = madhab.key)
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) EmeraldBgTint else DarkCardBg
                                        ),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(
                                                if (isSelected) EmeraldGreen else DarkSurfaceBorder
                                            )
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = madhab.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) EmeraldLight else Color.White
                                            )
                                            Text(
                                                text = madhab.desc,
                                                fontSize = 10.sp,
                                                color = TextMuted,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = DarkSurfaceBorder, thickness = 0.8.dp)

                        // SECTION 6: Vibration & 24h Toggles
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "SYSTEM PREFERENCES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = AmberGold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Vibrate Toggle
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            localSettings = localSettings.copy(vibration = !localSettings.vibration)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Vibration", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("Haptic alarm", fontSize = 10.sp, color = TextMuted)
                                        }
                                        Switch(
                                            checked = localSettings.vibration,
                                            onCheckedChange = { localSettings = localSettings.copy(vibration = it) },
                                            colors = SwitchDefaults.colors(checkedTrackColor = EmeraldGreen)
                                        )
                                    }
                                }

                                // 24H Format Toggle
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            localSettings = localSettings.copy(is24HourFormat = !localSettings.is24HourFormat)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("24H Format", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(if (localSettings.is24HourFormat) "24h (18:00)" else "12h (6:00 PM)", fontSize = 10.sp, color = TextMuted)
                                        }
                                        Switch(
                                            checked = localSettings.is24HourFormat,
                                            onCheckedChange = { localSettings = localSettings.copy(is24HourFormat = it) },
                                            colors = SwitchDefaults.colors(checkedTrackColor = AmberGold)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceBorder, thickness = 1.dp)

                    // Footer Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onStopPreview()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                            )
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                onStopPreview()
                                onSaveSettings(localSettings)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberGold,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Apply Settings", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
