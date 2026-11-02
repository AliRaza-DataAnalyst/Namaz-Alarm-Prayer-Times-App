package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.datasource.IslamicData
import com.example.data.model.AlarmItem
import com.example.data.model.AppSettings
import com.example.service.prayer.PrayerTimeCalculator
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DAYS_SHORT = listOf("S", "M", "T", "W", "T", "F", "S")
private val DAYS_NAMES = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

private val PRAYER_PRESETS = listOf(
    Triple("Tahajjud", "🫀 تہجد کی نماز + صلوۃ الحاجات کی نماز", "04:00"),
    Triple("Fajr", "💗 نمازِ فجر", "04:35"),
    Triple("Ishraq", "☀️ نمازِ اشراق و چاشت", "06:45"),
    Triple("Zuhr", "🫶 نمازِ ظہر", "13:30"),
    Triple("Asr", "❤️ نمازِ عصر", "16:20"),
    Triple("Maghrib", "🫶🤍 نمازِ مغرب", "18:00"),
    Triple("Isha", "🫶 🫀 نمازِ عشاء", "19:15")
)

@Composable
fun AlarmsScreen(
    alarms: List<AlarmItem>,
    settings: AppSettings,
    onToggleAlarm: (String) -> Unit,
    onToggleMute: (String) -> Unit,
    onSaveAlarm: (AlarmItem) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onSyncWithCalculated: () -> Unit,
    onPreviewSound: (String, String?) -> Unit,
    onStopPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    val sortedAlarms = remember(alarms) {
        alarms.sortedBy { it.time }
    }
    val activeCount = remember(alarms) {
        alarms.count { it.isEnabled }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Screen Header Card: well-spaced alarm icon, title, sync time and + buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmberGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Prayer Alarms",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "$activeCount active of ${alarms.size}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable { onSyncWithCalculated() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("sync_times_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync Times",
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Sync Times",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldLight
                                )
                            }
                        }

                        FilledIconButton(
                            onClick = {
                                isCreatingNew = true
                                editingAlarm = AlarmItem(
                                    id = "a_${System.currentTimeMillis()}",
                                    time = "06:00",
                                    label = "🫀 نئی اذان و نماز",
                                    isEnabled = true,
                                    isMuted = false,
                                    days = listOf(0, 1, 2, 3, 4, 5, 6),
                                    soundType = settings.soundType
                                )
                            },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = AmberGold,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("add_alarm_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Alarm", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Alarms List
            if (sortedAlarms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsOff,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Text("No alarms configured", color = TextSecondary, fontSize = 14.sp)
                            Button(
                                onClick = {
                                    isCreatingNew = true
                                    editingAlarm = AlarmItem(
                                        id = "a_${System.currentTimeMillis()}",
                                        time = "06:00",
                                        label = "نئی اذان و نماز",
                                        isEnabled = true,
                                        isMuted = false,
                                        days = listOf(0, 1, 2, 3, 4, 5, 6),
                                        soundType = settings.soundType
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black)
                            ) {
                                Text("Add First Alarm")
                            }
                        }
                    }
                }
            } else {
                items(sortedAlarms, key = { it.id }) { alarm ->
                    AlarmRowItem(
                        alarm = alarm,
                        is24Hour = settings.is24HourFormat,
                        onToggle = { onToggleAlarm(alarm.id) },
                        onToggleMute = { onToggleMute(alarm.id) },
                        onEdit = {
                            isCreatingNew = false
                            editingAlarm = alarm
                        }
                    )
                }
            }
        }

        // Edit/New Alarm Dialog
        editingAlarm?.let { currentAlarm ->
            AlarmEditDialog(
                alarm = currentAlarm,
                isNew = isCreatingNew,
                is24Hour = settings.is24HourFormat,
                onPreviewSound = onPreviewSound,
                onStopPreview = onStopPreview,
                onDismiss = {
                    onStopPreview()
                    editingAlarm = null
                },
                onSave = { saved ->
                    onStopPreview()
                    onSaveAlarm(saved)
                    editingAlarm = null
                },
                onDelete = {
                    onStopPreview()
                    onDeleteAlarm(currentAlarm.id)
                    editingAlarm = null
                }
            )
        }
    }
}

@Composable
private fun AlarmRowItem(
    alarm: AlarmItem,
    is24Hour: Boolean,
    onToggle: () -> Unit,
    onToggleMute: () -> Unit,
    onEdit: () -> Unit
) {
    val alpha = if (alarm.isEnabled) 1.0f else 0.45f
    val (time12, ap) = remember(alarm.time) { PrayerTimeCalculator.formatTime12(alarm.time) }

    val repeatText = remember(alarm.days) {
        when {
            alarm.days.size == 7 -> "Everyday"
            alarm.days.isEmpty() -> "Once"
            else -> alarm.days.mapNotNull { DAYS_NAMES.getOrNull(it) }.joinToString(" ")
        }
    }

    val dateAndScheduleText = remember(alarm.date, alarm.time, alarm.days) {
        if (!alarm.date.isNullOrEmpty()) {
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val formatter = SimpleDateFormat("EEE, d MMM yyyy", Locale.US)
                val parsed = parser.parse(alarm.date)
                if (parsed != null) "📅 ${formatter.format(parsed)} (Once)" else "📅 ${alarm.date}"
            } catch (_: Exception) {
                "📅 ${alarm.date}"
            }
        } else {
            val now = Calendar.getInstance()
            val timeParts = alarm.time.split(":")
            val h = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            val alarmCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (alarm.days.isEmpty()) {
                if (alarmCal.timeInMillis <= now.timeInMillis) {
                    alarmCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                val fmt = SimpleDateFormat("EEE, d MMM yyyy", Locale.US)
                "📅 ${fmt.format(alarmCal.time)} (Once)"
            } else {
                val todayDow = now.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun..6=Sat
                val isToday = alarm.days.contains(todayDow) && alarmCal.timeInMillis > now.timeInMillis
                val dateFmt = SimpleDateFormat("d MMM", Locale.US)
                if (isToday) {
                    "📅 Today, ${dateFmt.format(now.time)} • $repeatText"
                } else {
                    val nextCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, h)
                        set(Calendar.MINUTE, m)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    var daysAhead = 1
                    while (daysAhead <= 7) {
                        nextCal.add(Calendar.DAY_OF_YEAR, 1)
                        val dow = nextCal.get(Calendar.DAY_OF_WEEK) - 1
                        if (alarm.days.contains(dow)) break
                        daysAhead++
                    }
                    if (daysAhead == 1) {
                        "📅 Tomorrow, ${dateFmt.format(nextCal.time)} • $repeatText"
                    } else {
                        val fullFmt = SimpleDateFormat("EEE, d MMM", Locale.US)
                        "📅 ${fullFmt.format(nextCal.time)} • $repeatText"
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onEdit() }
            .testTag("alarm_item_${alarm.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                // Line 1: Time + AM/PM + English Prayer Name badge (same size for all prayers)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (is24Hour) alarm.time else time12,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = alpha)
                    )

                    if (!is24Hour) {
                        Text(
                            text = ap,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = AmberGold.copy(alpha = alpha)
                        )
                    }

                    alarm.prayerName?.let { p ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = p,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight.copy(alpha = alpha),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Line 2: Complete Urdu title with emoji in full width (no truncation)
                Text(
                    text = alarm.label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary.copy(alpha = alpha),
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )

                // Line 3: Date and Schedule for all namaz
                Text(
                    text = dateAndScheduleText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AmberGold.copy(alpha = if (alarm.isEnabled) 0.9f else 0.45f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Speaker icon: clean plain icon (removed bulky circular border for more space)
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("mute_alarm_${alarm.id}")
                ) {
                    Icon(
                        imageVector = if (alarm.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (alarm.isMuted) "Unmute Alarm" else "Mute Alarm",
                        tint = if (alarm.isMuted) RedAlert else AmberGold.copy(alpha = alpha),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Enable/Disable switch
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldGreen,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                        uncheckedTrackColor = Color(0xFF39393D)
                    ),
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditDialog(
    alarm: AlarmItem,
    isNew: Boolean,
    is24Hour: Boolean,
    onPreviewSound: (String, String?) -> Unit,
    onStopPreview: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit,
    onDelete: () -> Unit
) {
    var timeString by remember { mutableStateOf(alarm.time) }
    var prayerName by remember { mutableStateOf(alarm.prayerName) }
    var labelText by remember { mutableStateOf(alarm.label) }
    var selectedDays by remember { mutableStateOf(alarm.days) }
    var soundType by remember { mutableStateOf(alarm.soundType) }
    var customUri by remember { mutableStateOf(alarm.customSoundUri) }
    var isMuted by remember { mutableStateOf(alarm.isMuted) }

    var toneExpanded by remember { mutableStateOf(false) }
    var isPreviewing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            onStopPreview()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customUri = uri.toString()
            soundType = "custom_tone"
        }
    }

    val initialHour = remember {
        alarm.time.split(":").getOrNull(0)?.toIntOrNull() ?: 6
    }
    val initialMinute = remember {
        alarm.time.split(":").getOrNull(1)?.toIntOrNull() ?: 0
    }

    var selectedDate by remember { mutableStateOf(alarm.date) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    var isAm by remember { mutableStateOf(initialHour < 12) }
    var hourInput by remember {
        val h12 = if (initialHour % 12 == 0) 12 else initialHour % 12
        mutableStateOf(if (is24Hour) "%02d".format(initialHour) else "%02d".format(h12))
    }
    var minuteInput by remember {
        mutableStateOf("%02d".format(initialMinute))
    }
    var activeField by remember { mutableStateOf("hour") } // "hour" or "minute"

    fun updateTypedTime(typedH: String, typedM: String, am: Boolean) {
        val rawH = typedH.toIntOrNull() ?: 0
        val rawM = (typedM.toIntOrNull() ?: 0).coerceIn(0, 59)
        val validH24 = if (is24Hour) {
            rawH.coerceIn(0, 23)
        } else {
            val clamped12 = rawH.coerceIn(1, 12)
            if (am) {
                if (clamped12 == 12) 0 else clamped12
            } else {
                if (clamped12 == 12) 12 else clamped12 + 12
            }
        }
        timeString = "%02d:%02d".format(validH24, rawM)
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
                .padding(top = 28.dp, bottom = 16.dp, start = 14.dp, end = 14.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("alarm_edit_dialog"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNew) "New Alarm ⏰" else "Edit Alarm ⏰",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Manually Editable Time Digits (Tap to type with keyboard!)
                        // 1. Manually Editable Digital Time Digits (Direct Keyboard Input)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isHourActive = activeField == "hour"
                            val isMinuteActive = activeField == "minute"

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Hour Box - Directly Editable with Keyboard
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = DarkCardBg,
                                    border = BorderStroke(if (isHourActive) 2.dp else 1.dp, if (isHourActive) AmberGold else DarkSurfaceBorder),
                                    modifier = Modifier
                                        .size(width = 88.dp, height = 78.dp)
                                        .clickable { activeField = "hour" }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        BasicTextField(
                                            value = hourInput,
                                            onValueChange = { newVal ->
                                                activeField = "hour"
                                                val digits = newVal.filter { it.isDigit() }.take(2)
                                                hourInput = digits
                                                if (digits.isNotEmpty()) {
                                                    updateTypedTime(digits, minuteInput, isAm)
                                                }
                                            },
                                            textStyle = TextStyle(
                                                fontSize = 42.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isHourActive) AmberGold else Color.White,
                                                textAlign = TextAlign.Center
                                            ),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Next
                                            ),
                                            singleLine = true,
                                            cursorBrush = SolidColor(AmberGold)
                                        )
                                    }
                                }

                                Text(
                                    text = ":",
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                // Minute Box - Directly Editable with Keyboard
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = DarkCardBg,
                                    border = BorderStroke(if (isMinuteActive) 2.dp else 1.dp, if (isMinuteActive) AmberGold else DarkSurfaceBorder),
                                    modifier = Modifier
                                        .size(width = 88.dp, height = 78.dp)
                                        .clickable { activeField = "minute" }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        BasicTextField(
                                            value = minuteInput,
                                            onValueChange = { newVal ->
                                                activeField = "minute"
                                                val digits = newVal.filter { it.isDigit() }.take(2)
                                                minuteInput = digits
                                                if (digits.isNotEmpty()) {
                                                    updateTypedTime(hourInput, digits, isAm)
                                                }
                                            },
                                            textStyle = TextStyle(
                                                fontSize = 42.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isMinuteActive) AmberGold else Color.White,
                                                textAlign = TextAlign.Center
                                            ),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done
                                            ),
                                            singleLine = true,
                                            cursorBrush = SolidColor(AmberGold)
                                        )
                                    }
                                }

                                // AM/PM Toggle Box
                                if (!is24Hour) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = DarkCardBg,
                                        border = BorderStroke(1.dp, DarkSurfaceBorder),
                                        modifier = Modifier
                                            .width(64.dp)
                                            .height(78.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .background(if (isAm) AmberGold else Color.Transparent)
                                                    .clickable {
                                                        if (!isAm) {
                                                            isAm = true
                                                            updateTypedTime(hourInput, minuteInput, true)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "AM",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAm) Color.Black else TextSecondary
                                                )
                                            }

                                            HorizontalDivider(color = DarkSurfaceBorder, thickness = 1.dp)

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .background(if (!isAm) AmberGold else Color.Transparent)
                                                    .clickable {
                                                        if (isAm) {
                                                            isAm = false
                                                            updateTypedTime(hourInput, minuteInput, false)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "PM",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (!isAm) Color.Black else TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Subtitle helper text
                            Text(
                                text = "⌨️ Tap boxes to type digits directly",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                        }

                        // 2. Alarm Date & Schedule Section (تاریخ)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
                            val tomorrowCal = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) } }
                            val tomorrowStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(tomorrowCal.time) }

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
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = AmberGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Alarm Date (تاریخ)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AmberGold
                                    )
                                }

                                Text(
                                    text = when (selectedDate) {
                                        null -> "Daily (Everyday)"
                                        todayStr -> "Today's Prayer"
                                        tomorrowStr -> "Tomorrow's Prayer"
                                        else -> selectedDate!!
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedDate != null) AmberGold else EmeraldLight
                                )
                            }

                            // Quick Date Selection Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedDate == null,
                                    onClick = { selectedDate = null },
                                    label = { Text("Daily 🔄", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldBgTint,
                                        selectedLabelColor = EmeraldLight,
                                        containerColor = DarkCardBg,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (selectedDate == null) EmeraldGreen else DarkSurfaceBorder,
                                        enabled = true,
                                        selected = selectedDate == null
                                    )
                                )

                                FilterChip(
                                    selected = selectedDate == todayStr,
                                    onClick = { selectedDate = todayStr },
                                    label = { Text("Today 📅", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberBgTint,
                                        selectedLabelColor = AmberGold,
                                        containerColor = DarkCardBg,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (selectedDate == todayStr) AmberGold else DarkSurfaceBorder,
                                        enabled = true,
                                        selected = selectedDate == todayStr
                                    )
                                )

                                FilterChip(
                                    selected = selectedDate == tomorrowStr,
                                    onClick = { selectedDate = tomorrowStr },
                                    label = { Text("Tomorrow 🌅", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberBgTint,
                                        selectedLabelColor = AmberGold,
                                        containerColor = DarkCardBg,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (selectedDate == tomorrowStr) AmberGold else DarkSurfaceBorder,
                                        enabled = true,
                                        selected = selectedDate == tomorrowStr
                                    )
                                )

                                FilterChip(
                                    selected = selectedDate != null && selectedDate != todayStr && selectedDate != tomorrowStr,
                                    onClick = { showDatePickerDialog = true },
                                    label = {
                                        Text(
                                            text = if (selectedDate != null && selectedDate != todayStr && selectedDate != tomorrowStr) {
                                                selectedDate!!
                                            } else {
                                                "Pick Date 🗓️"
                                            },
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberBgTint,
                                        selectedLabelColor = AmberGold,
                                        containerColor = DarkCardBg,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (selectedDate != null && selectedDate != todayStr && selectedDate != tomorrowStr) AmberGold else DarkSurfaceBorder,
                                        enabled = true,
                                        selected = selectedDate != null && selectedDate != todayStr && selectedDate != tomorrowStr
                                    )
                                )
                            }

                            Text(
                                text = "Islamic Hijri Date: ${PrayerTimeCalculator.getHijriDate(Date())}",
                                fontSize = 10.5.sp,
                                color = TextMuted
                            )
                        }

                        // Quick Prayer Presets
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Quick Prayer Presets 🕌",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberGold
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(PRAYER_PRESETS) { (name, urdu, defTime) ->
                                    val isSelected = prayerName == name
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            prayerName = name
                                            labelText = urdu
                                            if (isNew) {
                                                timeString = defTime
                                                val pParts = defTime.split(":")
                                                val pH = pParts.getOrNull(0)?.toIntOrNull() ?: 5
                                                val pM = pParts.getOrNull(1)?.toIntOrNull() ?: 0
                                                isAm = pH < 12
                                                val hDisplay = if (is24Hour) pH else (if (pH % 12 == 0) 12 else pH % 12)
                                                hourInput = "%02d".format(hDisplay)
                                                minuteInput = "%02d".format(pM)
                                            }
                                        },
                                        label = { Text(name, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldBgTint,
                                            selectedLabelColor = EmeraldLight,
                                            containerColor = DarkCardBg,
                                            labelColor = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = if (isSelected) EmeraldGreen else DarkSurfaceBorder,
                                            enabled = true,
                                            selected = isSelected
                                        )
                                    )
                                }
                            }
                        }

                        // Alarm Tone Dropdown
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Alarm Tone 🎵",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberGold
                            )

                            ExposedDropdownMenuBox(
                                expanded = toneExpanded,
                                onExpandedChange = { toneExpanded = !toneExpanded }
                            ) {
                                val currentTone = IslamicData.ALARM_TONES.find { it.id == soundType }
                                    ?: IslamicData.ALARM_TONES.first()

                                OutlinedTextField(
                                    value = "${currentTone.title} (${currentTone.urduTitle})",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toneExpanded) },
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
                                    expanded = toneExpanded,
                                    onDismissRequest = {
                                        toneExpanded = false
                                        onStopPreview()
                                        isPreviewing = false
                                    },
                                    modifier = Modifier.background(DarkCardBg)
                                ) {
                                    IslamicData.ALARM_TONES.forEach { tone ->
                                        val isCurrent = soundType == tone.id
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = tone.title,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isCurrent) AmberGold else Color.White
                                                        )
                                                        Text(
                                                            text = tone.urduTitle,
                                                            fontSize = 11.sp,
                                                            color = if (isCurrent) AmberGold.copy(alpha = 0.85f) else TextSecondary
                                                        )
                                                    }
                                                    if (isCurrent && isPreviewing) {
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
                                                    isPreviewing = false
                                                    audioPickerLauncher.launch("audio/*")
                                                    toneExpanded = false
                                                } else {
                                                    soundType = tone.id
                                                    customUri = null
                                                    // Automatically play preview of clicked tone; immediately switches on next click
                                                    onPreviewSound(tone.id, null)
                                                    isPreviewing = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Tone Preview Play / Stop and Mute Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (isPreviewing) {
                                            onStopPreview()
                                            isPreviewing = false
                                        } else {
                                            onPreviewSound(soundType, customUri)
                                            isPreviewing = true
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPreviewing) RedBgTint else DarkCardBg,
                                        contentColor = if (isPreviewing) RedAlert else AmberGold
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isPreviewing) "Stop" else "Preview Tone", fontSize = 11.sp)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.clickable { isMuted = !isMuted }
                                ) {
                                    Text(
                                        text = if (isMuted) "Muted (Silent) 🔕" else "Sound Active 🔔",
                                        fontSize = 12.sp,
                                        color = if (isMuted) RedAlert else TextSecondary
                                    )
                                    Switch(
                                        checked = !isMuted,
                                        onCheckedChange = { isMuted = !it },
                                        colors = SwitchDefaults.colors(checkedTrackColor = EmeraldGreen)
                                    )
                                }
                            }
                        }

                        // Label Input
                        OutlinedTextField(
                            value = labelText,
                            onValueChange = { labelText = it },
                            label = { Text("Label & Urdu Name (لیبل)", fontSize = 12.sp) },
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("alarm_label_input")
                        )

                        // Days of week repeat
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Repeat Days (تکرار)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Everyday",
                                        fontSize = 11.sp,
                                        color = AmberGold,
                                        modifier = Modifier
                                            .clickable { selectedDays = listOf(0, 1, 2, 3, 4, 5, 6) }
                                            .padding(2.dp)
                                    )
                                    Text("|", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        text = "Once",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        modifier = Modifier
                                            .clickable { selectedDays = emptyList() }
                                            .padding(2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DAYS_SHORT.forEachIndexed { index, dayLetter ->
                                    val isSelected = selectedDays.contains(index)
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) AmberGold else DarkCardBg)
                                            .clickable {
                                                selectedDays = if (isSelected) {
                                                    selectedDays.filter { it != index }
                                                } else {
                                                    (selectedDays + index).sorted()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayLetter,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Actions: Delete, Cancel, Save
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isNew) {
                            FilledTonalIconButton(
                                onClick = onDelete,
                                shape = RoundedCornerShape(12.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = RedBgTint,
                                    contentColor = RedAlert
                                ),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Alarm")
                            }
                        }

                        OutlinedButton(
                            onClick = onDismiss,
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
                                val rawH = hourInput.toIntOrNull() ?: initialHour
                                val rawM = (minuteInput.toIntOrNull() ?: initialMinute).coerceIn(0, 59)
                                val validH24 = if (is24Hour) {
                                    rawH.coerceIn(0, 23)
                                } else {
                                    val clamped12 = if (rawH in 1..12) rawH else (if (initialHour % 12 == 0) 12 else initialHour % 12)
                                    if (isAm) {
                                        if (clamped12 == 12) 0 else clamped12
                                    } else {
                                        if (clamped12 == 12) 12 else clamped12 + 12
                                    }
                                }
                                val finalTimeString = "%02d:%02d".format(validH24, rawM)

                                val updated = alarm.copy(
                                    time = finalTimeString,
                                    prayerName = prayerName,
                                    label = labelText.ifBlank { "Namaz Reminder" },
                                    days = if (selectedDate != null) emptyList() else selectedDays,
                                    date = selectedDate,
                                    soundType = soundType,
                                    customSoundUri = customUri,
                                    isMuted = isMuted,
                                    isEnabled = true
                                )
                                onSave(updated)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberGold,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text("Save Alarm", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = selectedMillis
                            }
                            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK", color = AmberGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = DarkSurface
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White,
                    headlineContentColor = AmberGold,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextSecondary,
                    yearContentColor = Color.White,
                    currentYearContentColor = AmberGold,
                    selectedYearContentColor = Color.Black,
                    selectedYearContainerColor = AmberGold,
                    dayContentColor = Color.White,
                    disabledDayContentColor = TextMuted,
                    selectedDayContentColor = Color.Black,
                    selectedDayContainerColor = AmberGold,
                    todayContentColor = AmberGold,
                    todayDateBorderColor = AmberGold
                )
            )
        }
    }
}
