package com.nivora.browser.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ReaderTheme(val bg: Color, val text: Color, val nameLabel: String) {
    LIGHT(Color(0xFFFCFCFC), Color(0xFF1E293B), "Light"),
    SEPIA(Color(0xFFFBF0D9), Color(0xFF5F4B32), "Sepia"),
    DARK(Color(0xFF1E1E24), Color(0xFFE2E8F0), "Dark"),
    AMOLED(Color(0xFF000000), Color(0xFFD1D5DB), "AMOLED")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderModeScreen(
    title: String,
    url: String,
    content: String,
    onNavigateBack: () -> Unit,
    onShare: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var fontSize by remember { mutableFloatStateOf(18f) }
    var lineSpacingMultiplier by remember { mutableFloatStateOf(1.6f) }
    var maxWidthDp by remember { mutableStateOf(650.dp) }
    var readerTheme by remember { mutableStateOf(ReaderTheme.SEPIA) }
    var showFormatSheet by remember { mutableStateOf(false) }

    // If content is empty or extraction fails, return cleanly
    if (content.isBlank()) {
        onNavigateBack()
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Reader Mode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = readerTheme.text
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("reader_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Reader Mode", tint = readerTheme.text)
                    }
                },
                actions = {
                    IconButton(onClick = { showFormatSheet = true }) {
                        Icon(Icons.Default.FormatSize, contentDescription = "Typography Settings", tint = readerTheme.text)
                    }
                    IconButton(onClick = { onShare("$title\n$url\n\n$content") }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Article", tint = readerTheme.text)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = readerTheme.bg
                )
            )
        },
        containerColor = readerTheme.bg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = maxWidthDp)
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = title.ifBlank { "Article" },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        ),
                        color = readerTheme.text,
                        lineHeight = (fontSize * 1.3f).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = url,
                        style = MaterialTheme.typography.labelMedium,
                        color = readerTheme.text.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Render paragraphs cleanly
                val paragraphs = content.split("\n\n").filter { it.isNotBlank() }
                items(paragraphs.size) { idx ->
                    Text(
                        text = paragraphs[idx].trim(),
                        fontSize = fontSize.sp,
                        color = readerTheme.text,
                        lineHeight = (fontSize * lineSpacingMultiplier).sp,
                        fontFamily = FontFamily.Serif
                    )
                }
            }
        }
    }

    if (showFormatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFormatSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Reader Appearance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Themes
                Text(text = "Theme Palette", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReaderTheme.entries.forEach { theme ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = theme.bg,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (readerTheme == theme) 2.dp else 1.dp,
                                color = if (readerTheme == theme) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { readerTheme = theme }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = theme.nameLabel,
                                    color = theme.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Font Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Text Size", style = MaterialTheme.typography.labelMedium)
                    Text(text = "${fontSize.toInt()} sp", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 14f..28f,
                    steps = 13
                )

                // Line Height Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Line Spacing", style = MaterialTheme.typography.labelMedium)
                    Text(text = "%.1fx".format(lineSpacingMultiplier), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = lineSpacingMultiplier,
                    onValueChange = { lineSpacingMultiplier = it },
                    valueRange = 1.2f..2.2f,
                    steps = 9
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
