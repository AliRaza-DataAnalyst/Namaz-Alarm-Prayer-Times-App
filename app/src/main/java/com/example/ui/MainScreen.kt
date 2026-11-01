package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.alarm.AlarmSoundService
import com.example.ui.dialogs.DuasDialog
import com.example.ui.dialogs.RingingAlarmDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class NavTab(val id: String, val title: String) {
    ALARMS("alarms", "Alarms"),
    NAMAZ("namaz", "Namaz"),
    QIBLA("qibla", "Qibla"),
    TASKS("tasks", "Tasks"),
    TASBIH("tasbih", "Tasbih")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: NamazViewModel
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavTab.ALARMS) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDuasDialog by remember { mutableStateOf(false) }

    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val donePrayers by viewModel.donePrayers.collectAsStateWithLifecycle()
    val tasbihState by viewModel.tasbihState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTimeMillis.collectAsStateWithLifecycle()
    val deviceHeading by viewModel.deviceHeading.collectAsStateWithLifecycle()
    val hasCompass by viewModel.hasCompassSensor.collectAsStateWithLifecycle()
    val ringingInfo by viewModel.ringingAlarm.collectAsStateWithLifecycle()

    val streak = remember(donePrayers) {
        viewModel.repository.calculateStreak()
    }

    // Toast event collector
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.detectGpsLocation()
        }
    }

    // Permission launcher for notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // When notification permission prompt finishes (granted or dismissed), prompt for location permission if needed
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasLocation = hasFine || hasCoarse

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotification = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotification) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (!hasLocation) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        } else {
            if (!hasLocation) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldBgTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🕌", fontSize = 18.sp)
                        }

                        Column {
                            Text(
                                text = "Namaz Alarm",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showSettingsDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${settings.location.city}, ${settings.location.country}",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        // Masnoon Duas Button
                        Surface(
                            shape = CircleShape,
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(
                                onClick = { showDuasDialog = true },
                                modifier = Modifier.testTag("top_duas_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MenuBook,
                                    contentDescription = "Masnoon Duas",
                                    tint = AmberGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Settings Button
                        Surface(
                            shape = CircleShape,
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.testTag("top_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Settings",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        bottomBar = {
            // Floating pill navigation bar matching the iOS/Material hybrid look
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("bottom_nav_bar"),
                    shape = RoundedCornerShape(32.dp),
                    color = DarkSurface.copy(alpha = 0.95f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                    ),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavTab.entries.forEach { tab ->
                            val isSelected = currentTab == tab
                            val icon = when (tab) {
                                NavTab.ALARMS -> if (isSelected) Icons.Filled.Notifications else Icons.Outlined.Notifications
                                NavTab.NAMAZ -> if (isSelected) Icons.Filled.AccessTime else Icons.Outlined.AccessTime
                                NavTab.QIBLA -> if (isSelected) Icons.Filled.Explore else Icons.Outlined.Explore
                                NavTab.TASKS -> if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle
                                NavTab.TASBIH -> if (isSelected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
                            }

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { currentTab = tab }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("nav_tab_${tab.id}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) AmberGold else TextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AmberGold else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.ALARMS -> {
                    AlarmsScreen(
                        alarms = alarms,
                        settings = settings,
                        onToggleAlarm = { viewModel.toggleAlarm(it) },
                        onToggleMute = { viewModel.toggleAlarmMute(it) },
                        onSaveAlarm = { viewModel.saveAlarm(it) },
                        onDeleteAlarm = { viewModel.deleteAlarm(it) },
                        onSyncWithCalculated = { viewModel.syncAlarmsWithAstronomicalTimes() },
                        onPreviewSound = { sound, uri -> viewModel.synthesizer.playPreview(sound, uri) },
                        onStopPreview = { viewModel.synthesizer.stopAlarm() }
                    )
                }
                NavTab.NAMAZ -> {
                    NamazScreen(
                        alarms = alarms,
                        donePrayers = donePrayers,
                        settings = settings,
                        currentTimeMillis = currentTime,
                        streak = streak,
                        onTogglePrayerDone = { viewModel.togglePrayerDone(it) },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
                NavTab.QIBLA -> {
                    QiblaScreen(
                        settings = settings,
                        deviceHeading = deviceHeading,
                        hasSensor = hasCompass,
                        onManualHeadingChange = { viewModel.setManualHeading(it) },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
                NavTab.TASKS -> {
                    TasksScreen(
                        tasks = tasks,
                        settings = settings,
                        onAddTask = { name, time -> viewModel.addTask(name, time) },
                        onToggleTask = { viewModel.toggleTask(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onClearCompleted = { viewModel.clearCompletedTasks() }
                    )
                }
                NavTab.TASBIH -> {
                    TasbihScreen(
                        tasbihState = tasbihState,
                        onTap = { viewModel.tapTasbih() },
                        onSetGoal = { viewModel.setTasbihGoal(it) },
                        onSelectDhikr = { viewModel.selectDhikr(it) },
                        onReset = { viewModel.resetTasbih() }
                    )
                }
            }
        }
    }

    // Masnoon Duas Dialog
    if (showDuasDialog) {
        DuasDialog(
            onDismiss = { showDuasDialog = false }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            onSaveSettings = { viewModel.updateSettings(it) },
            onDetectGps = {
                val hasFine = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasFine || hasCoarse) {
                    viewModel.detectGpsLocation()
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            onPreviewSound = { sound, uri -> viewModel.synthesizer.playPreview(sound, uri) },
            onStopPreview = { viewModel.synthesizer.stopAlarm() },
            getMobileVolume = { viewModel.getMobileAlarmVolume() },
            setMobileVolume = { viewModel.setMobileAlarmVolume(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Full-screen Ringing Alarm Dialog
    ringingInfo?.let { info ->
        RingingAlarmDialog(
            info = info,
            is24Hour = settings.is24HourFormat,
            onDismiss = { viewModel.dismissAlarm() },
            onSnooze = { mins -> viewModel.snoozeAlarm(mins) }
        )
    }
}
