package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.datasource.IslamicData
import com.example.data.model.DuaItem
import com.example.ui.theme.*

private val CATEGORIES = listOf(
    Pair("all", "All Duas"),
    Pair("after_namaz", "After Namaz"),
    Pair("morning_evening", "Morning & Wake"),
    Pair("sleep", "Sleep & Night"),
    Pair("forgiveness", "Forgiveness"),
    Pair("protection", "Protection")
)

@Composable
fun DuasDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    var copiedDuaId by remember { mutableStateOf<String?>(null) }

    val filteredDuas = remember(selectedCategory, searchQuery) {
        IslamicData.MASNOON_DUAS.filter { dua ->
            val matchesCat = selectedCategory == "all" || dua.category == selectedCategory
            val query = searchQuery.trim().lowercase()
            val matchesSearch = query.isEmpty() ||
                    dua.title.lowercase().contains(query) ||
                    dua.urduTitle.contains(query) ||
                    dua.arabic.contains(query) ||
                    dua.translation.lowercase().contains(query) ||
                    dua.urduTranslation.contains(query)
            matchesCat && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
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
                    .testTag("duas_dialog"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                Text("📖", fontSize = 18.sp)
                            }
                            Column {
                                Text(
                                    text = "Masnoon Duas",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Authentic supplications from Sunnah",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceBorder, thickness = 1.dp)

                    // Search & Categories Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCardBg)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Search field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by topic, Arabic, or Urdu...", fontSize = 12.sp, color = TextMuted) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberGold,
                                unfocusedBorderColor = DarkSurfaceBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )

                        // Category chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(CATEGORIES) { (catId, catLabel) ->
                                val isSelected = selectedCategory == catId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = catId },
                                    label = { Text(catLabel, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberGold,
                                        selectedLabelColor = Color.Black,
                                        containerColor = DarkSurface,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) AmberGold else DarkSurfaceBorder,
                                        enabled = true,
                                        selected = isSelected
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // Duas List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (filteredDuas.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No supplications match your search.", color = TextMuted, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(filteredDuas, key = { it.id }) { dua ->
                                DuaCard(
                                    dua = dua,
                                    isCopied = copiedDuaId == dua.id,
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText(
                                            dua.title,
                                            "${dua.arabic}\n\n${dua.urduTranslation}\n\n${dua.translation}"
                                        )
                                        clipboard.setPrimaryClip(clip)
                                        copiedDuaId = dua.id
                                        Toast.makeText(context, "Dua copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
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
private fun DuaCard(
    dua: DuaItem,
    isCopied: Boolean,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
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
            // Title and Copy button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dua.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = dua.urduTitle,
                        fontSize = 12.sp,
                        color = AmberGold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy Dua",
                        tint = if (isCopied) EmeraldLight else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Arabic text box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = dua.arabic,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight,
                    textAlign = TextAlign.Right,
                    lineHeight = 32.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Transliteration
            Text(
                text = "\"${dua.transliteration}\"",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            // Urdu Translation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(10.dp)
            ) {
                Text(
                    text = dua.urduTranslation,
                    fontSize = 12.sp,
                    color = Color(0xFFE2E8F0),
                    textAlign = TextAlign.Right,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // English Meaning
            Text(
                text = "Meaning: ${dua.translation}",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            // Reference
            Text(
                text = dua.reference,
                fontSize = 10.sp,
                color = TextMuted,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}
