package com.nivora.browser.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 1. JSON Viewer & Formatter
 */
@Composable
fun JsonViewerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var inputJson by remember { mutableStateOf("{\n  \"name\": \"NIVORA\",\n  \"features\": [\"Privacy Shield\", \"Safe Browsing\", \"Notes\"],\n  \"version\": 1.0\n}") }
    var formattedJson by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun format() {
        try {
            val trimmed = inputJson.trim()
            val formatted = if (trimmed.startsWith("[")) {
                JSONArray(trimmed).toString(2)
            } else {
                JSONObject(trimmed).toString(2)
            }
            formattedJson = formatted
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Invalid JSON: ${e.message}"
            formattedJson = ""
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("JSON Viewer & Formatter", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        item {
            OutlinedTextField(
                value = inputJson,
                onValueChange = { inputJson = it },
                label = { Text("Raw JSON Input") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { format() }) { Text("Format / Validate") }
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val toCopy = if (formattedJson.isNotBlank()) formattedJson else inputJson
                    clipboard.setPrimaryClip(ClipData.newPlainText("JSON", toCopy))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }) { Text("Copy") }
            }
        }
        if (errorMessage != null) {
            item {
                Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (formattedJson.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formattedJson,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 2. URL Encoder / Decoder
 */
@Composable
fun UrlCodecDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("https://nivora.browser/search?q=Kotlin & Compose 2026") }
    var outputText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("URL Encoder & Decoder", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        item {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Text or URL") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    try {
                        outputText = URLEncoder.encode(inputText, StandardCharsets.UTF_8.name())
                    } catch (e: Exception) {
                        outputText = "Error encoding: ${e.message}"
                    }
                }) { Text("URL Encode") }

                OutlinedButton(onClick = {
                    try {
                        outputText = URLDecoder.decode(inputText, StandardCharsets.UTF_8.name())
                    } catch (e: Exception) {
                        outputText = "Error decoding: ${e.message}"
                    }
                }) { Text("URL Decode") }
            }
        }
        if (outputText.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = outputText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("URL Codec", outputText))
                            Toast.makeText(context, "Copied result", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Timestamp & Epoch Converter
 */
@Composable
fun TimestampConverterDialog(onDismiss: () -> Unit) {
    var epochInput by remember { mutableStateOf(System.currentTimeMillis().toString()) }
    var localResult by remember { mutableStateOf("") }
    var utcResult by remember { mutableStateOf("") }

    fun convert() {
        try {
            val num = epochInput.trim().toLong()
            val millis = if (num < 100_000_000_000L) num * 1000L else num
            val date = Date(millis)

            val localFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz", Locale.getDefault())
            val utcFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            localResult = localFmt.format(date)
            utcResult = utcFmt.format(date)
        } catch (_: Exception) {
            localResult = "Invalid epoch timestamp"
            utcResult = ""
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Unix Timestamp Converter", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        item {
            OutlinedTextField(
                value = epochInput,
                onValueChange = { epochInput = it },
                label = { Text("Epoch Timestamp (ms or seconds)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { convert() }) { Text("Convert") }
                OutlinedButton(onClick = {
                    epochInput = System.currentTimeMillis().toString()
                    convert()
                }) { Text("Now") }
            }
        }
        if (localResult.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Local Time:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text(localResult, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("UTC Time:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text(utcResult, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/**
 * 4. Unit Converter
 */
@Composable
fun UnitConverterDialog(onDismiss: () -> Unit) {
    var inputValue by remember { mutableStateOf("100") }
    var selectedCategory by remember { mutableStateOf("Storage") } // Storage, Length, Temp

    val resultText = remember(inputValue, selectedCategory) {
        val num = inputValue.toDoubleOrNull() ?: 0.0
        when (selectedCategory) {
            "Storage" -> {
                "• %d Bytes\n• %.2f KB\n• %.2f MB\n• %.4f GB\n• %.6f TB".format(
                    (num * 1024 * 1024).toLong(),
                    num * 1024,
                    num,
                    num / 1024.0,
                    num / (1024.0 * 1024.0)
                )
            }
            "Length" -> {
                "• %.2f meters\n• %.4f kilometers\n• %.2f feet\n• %.2f inches\n• %.4f miles".format(
                    num,
                    num / 1000.0,
                    num * 3.28084,
                    num * 39.3701,
                    num * 0.000621371
                )
            }
            "Temperature" -> {
                val fahr = (num * 9.0 / 5.0) + 32.0
                val kelvin = num + 273.15
                "• %.1f °Celsius\n• %.1f °Fahrenheit\n• %.2f Kelvin".format(num, fahr, kelvin)
            }
            else -> ""
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Offline Unit Converter", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Storage", "Length", "Temperature").forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                label = {
                    Text(
                        when (selectedCategory) {
                            "Storage" -> "Input in Megabytes (MB)"
                            "Length" -> "Input in Meters (m)"
                            else -> "Input in Celsius (°C)"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

/**
 * 5. Color Picker (HEX / RGB / HSL)
 */
@Composable
fun ColorPickerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var red by remember { mutableFloatStateOf(0.12f) }
    var green by remember { mutableFloatStateOf(0.53f) }
    var blue by remember { mutableFloatStateOf(0.9f) }

    val currentColor = Color(red, green, blue)
    val rInt = (red * 255).toInt()
    val gInt = (green * 255).toInt()
    val bInt = (blue * 255).toInt()
    val hexCode = String.format("#%02X%02X%02X", rInt, gInt, bInt)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Color Picker & Hex Generator", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(currentColor)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = hexCode,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        item {
            Text("Red: $rInt", style = MaterialTheme.typography.labelSmall)
            Slider(value = red, onValueChange = { red = it })
        }
        item {
            Text("Green: $gInt", style = MaterialTheme.typography.labelSmall)
            Slider(value = green, onValueChange = { green = it })
        }
        item {
            Text("Blue: $bInt", style = MaterialTheme.typography.labelSmall)
            Slider(value = blue, onValueChange = { blue = it })
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Color Hex", hexCode))
                    Toast.makeText(context, "$hexCode copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy HEX")
                }

                OutlinedButton(onClick = {
                    val rgbStr = "rgb($rInt, $gInt, $bInt)"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Color RGB", rgbStr))
                    Toast.makeText(context, "$rgbStr copied", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Copy RGB")
                }
            }
        }
    }
}

/**
 * 6. Offline Text Editor
 */
@Composable
fun TextEditorDialog(
    onDismiss: () -> Unit,
    onSaveToNotes: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var textContent by remember { mutableStateOf("NIVORA Offline Scratchpad\n\n- Privacy First\n- No cloud tracking\n- Instant local editing\n\nDeveloped by Bhaskar") }
    var title by remember { mutableStateOf("Quick Note") }

    val wordCount = remember(textContent) {
        if (textContent.isBlank()) 0 else textContent.trim().split("\\s+".toRegex()).size
    }
    val charCount = textContent.length
    val lineCount = remember(textContent) {
        if (textContent.isEmpty()) 0 else textContent.lines().size
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Offline Text Editor", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "$wordCount words • $charCount chars • $lineCount lines",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = textContent,
                onValueChange = { textContent = it },
                label = { Text("Content") },
                minLines = 8,
                maxLines = 14,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(title, textContent))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }

                if (onSaveToNotes != null) {
                    OutlinedButton(
                        onClick = {
                            if (textContent.isNotBlank()) {
                                onSaveToNotes(title, textContent)
                                Toast.makeText(context, "Saved to NIVORA Notes", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save to Notes")
                    }
                }
            }
        }
    }
}

/**
 * 7. Offline Markdown Viewer
 */
@Composable
fun MarkdownViewerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var markdownInput by remember {
        mutableStateOf(
            "# NIVORA Browser\n\n" +
            "**Fast. Private. Native.**\n\n" +
            "## Key Features\n" +
            "- Real Tracker Blocking\n" +
            "- Cookie & Site Data Isolation\n" +
            "- Biometric App Lock\n" +
            "- Native Download Manager\n" +
            "- Offline Productivity Suite\n\n" +
            "> Developed by Bhaskar with Jetpack Compose.\n\n" +
            "```kotlin\n" +
            "val engine = NivoraPrivacyShield()\n" +
            "engine.blockTrackers(enabled = true)\n" +
            "```"
        )
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Preview, 1 = Raw Markdown

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Markdown Viewer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Preview") }
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Raw Markdown") }
                    )
                }
            }
        }

        if (selectedTab == 1) {
            item {
                OutlinedTextField(
                    value = markdownInput,
                    onValueChange = { markdownInput = it },
                    minLines = 10,
                    maxLines = 15,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
        } else {
            // Rendered preview
            val lines = markdownInput.lines()
            items(lines.size) { index ->
                val line = lines[index]
                when {
                    line.startsWith("# ") -> {
                        Text(
                            text = line.removePrefix("# "),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    line.startsWith("## ") -> {
                        Text(
                            text = line.removePrefix("## "),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                    line.startsWith("### ") -> {
                        Text(
                            text = line.removePrefix("### "),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }
                    line.startsWith("> ") -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = line.removePrefix("> "),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    line.startsWith("```") -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E1E1E),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = line,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFF9CDCFE),
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                    line.startsWith("- ") || line.startsWith("* ") -> {
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = line.removePrefix("- ").removePrefix("* "),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    line.isBlank() -> {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    else -> {
                        val cleanLine = line.replace("**", "").replace("__", "")
                        Text(
                            text = cleanLine,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Markdown", markdownInput))
                        Toast.makeText(context, "Copied markdown", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Markdown")
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

/**
 * 8. Custom Search Shortcuts Dialog
 */
@Composable
fun SearchShortcutsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var shortcutsMap by remember { mutableStateOf(com.nivora.browser.search.SearchResolver.getShortcuts()) }
    var newKey by remember { mutableStateOf("") }
    var newTemplate by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Custom Search Shortcuts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(
                text = "Type shortcut prefix followed by query in address bar (e.g., 'yt Android tutorial' or 'wiki Kotlin').",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add New Shortcut", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Prefix (e.g., 'so', 'amz')") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTemplate,
                        onValueChange = { newTemplate = it },
                        label = { Text("URL Template with %s (e.g., 'https://site.com/search?q=%s')") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            if (newKey.isBlank() || !newTemplate.contains("%s")) {
                                errorMessage = "Key cannot be empty and URL must contain '%s'"
                            } else {
                                com.nivora.browser.search.SearchResolver.registerShortcut(newKey, newTemplate)
                                shortcutsMap = com.nivora.browser.search.SearchResolver.getShortcuts()
                                newKey = ""
                                newTemplate = ""
                                errorMessage = null
                                Toast.makeText(context, "Shortcut added", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Shortcut")
                    }
                }
            }
        }

        item {
            Text("Active Shortcuts (${shortcutsMap.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }

        items(shortcutsMap.entries.toList().size) { index ->
            val entry = shortcutsMap.entries.toList()[index]
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${entry.key} →",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = entry.value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    if (entry.key !in setOf("yt", "wiki", "gh", "ddg", "reddit")) {
                        OutlinedButton(
                            onClick = {
                                com.nivora.browser.search.SearchResolver.removeShortcut(entry.key)
                                shortcutsMap = com.nivora.browser.search.SearchResolver.getShortcuts()
                                Toast.makeText(context, "Removed ${entry.key}", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Remove", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}
