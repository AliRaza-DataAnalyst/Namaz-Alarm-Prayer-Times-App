package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.TaskItem
import com.example.service.prayer.PrayerTimeCalculator
import com.example.ui.theme.*

private val ISLAMIC_SUGGESTIONS = listOf(
    Pair("📖 Recite Surah Al-Kahf (Friday)", "11:00"),
    Pair("🤲 Morning Adhkar & Ayat al-Kursi", "06:00"),
    Pair("🌅 Evening Adhkar before Maghrib", "17:30"),
    Pair("🌙 Recite Surah Al-Mulk before sleep", "21:30"),
    Pair("✨ Read Durood Sharif 100 times", "14:00"),
    Pair("💧 Make Wudu before going to sleep", "22:00")
)

@Composable
fun TasksScreen(
    tasks: List<TaskItem>,
    settings: AppSettings,
    onAddTask: (String, String?) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onClearCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var taskText by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf("") }

    val openCount = remember(tasks) { tasks.count { !it.isCompleted } }
    val doneCount = remember(tasks) { tasks.count { it.isCompleted } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Tasks",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$openCount to do • $doneCount completed",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (doneCount > 0) {
                    TextButton(
                        onClick = onClearCompleted,
                        colors = ButtonDefaults.textButtonColors(contentColor = RedAlert),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear done", fontSize = 12.sp)
                    }
                }
            }
        }

        // Add Task Card
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
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = taskText,
                            onValueChange = { taskText = it },
                            placeholder = { Text("New task or Islamic reminder...", fontSize = 13.sp, color = TextMuted) },
                            singleLine = true,
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
                                .weight(1f)
                                .testTag("task_input_field")
                        )

                        FilledIconButton(
                            onClick = {
                                if (taskText.isNotBlank()) {
                                    onAddTask(taskText, reminderTime.ifBlank { null })
                                    taskText = ""
                                    reminderTime = ""
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = AmberGold,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("add_task_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task")
                        }
                    }

                    // Optional reminder time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Optional reminder time (HH:mm):", fontSize = 11.sp, color = TextMuted)
                        OutlinedTextField(
                            value = reminderTime,
                            onValueChange = { reminderTime = it },
                            placeholder = { Text("e.g. 21:30", fontSize = 11.sp, color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberGold,
                                unfocusedBorderColor = DarkSurfaceBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = DarkCardBg,
                                unfocusedContainerColor = DarkCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(110.dp)
                        )
                    }
                }
            }
        }

        // Quick Islamic Suggestions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "✨ QUICK ISLAMIC REMINDERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = AmberGold
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ISLAMIC_SUGGESTIONS) { (name, time) ->
                        SuggestionChip(
                            onClick = { onAddTask(name, time) },
                            label = { Text(name, fontSize = 11.sp, color = Color.White) },
                            shape = RoundedCornerShape(12.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = DarkSurface
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                borderColor = DarkSurfaceBorder,
                                enabled = true
                            )
                        )
                    }
                }
            }
        }

        // Task List Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                )
            ) {
                if (tasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nothing to do yet. Add your first task or reminder above.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        tasks.forEachIndexed { index, task ->
                            if (index > 0) {
                                HorizontalDivider(color = DarkSurfaceBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
                            }
                            TaskRowItem(
                                task = task,
                                is24Hour = settings.is24HourFormat,
                                onToggle = { onToggleTask(task.id) },
                                onDelete = { onDeleteTask(task.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRowItem(
    task: TaskItem,
    is24Hour: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("task_item_${task.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox button
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (task.isCompleted) EmeraldGreen else Color.Transparent)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (task.isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = TextMuted,
                        radius = size.minDimension / 2f - 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.name,
                fontSize = 14.sp,
                color = if (task.isCompleted) TextMuted else Color.White,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
            )

            task.reminderTime?.let { time ->
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Reminder ${PrayerTimeCalculator.formatDisplay(time, is24Hour)}",
                        fontSize = 11.sp,
                        color = AmberGold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
