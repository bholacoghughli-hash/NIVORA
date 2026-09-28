package com.nivora.browser.ui.tools

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import com.nivora.browser.translation.TranslationEngine
import com.nivora.browser.ui.browser.BrowserViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit,
    onNavigateQrScanner: () -> Unit = {},
    onNavigateQrGenerator: () -> Unit = {},
    onNavigatePdfViewer: () -> Unit = {},
    onNavigatePermissions: () -> Unit = {},
    onNavigateReaderMode: () -> Unit = {},
    onNavigateNotes: () -> Unit = {},
    onNavigateWebApps: () -> Unit = {},
    onNavigateBackup: () -> Unit = {},
    onNavigatePasswordGenerator: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val app = context.applicationContext as? com.example.NivoraApplication
    val repository = app?.browserRepository
    val activeState by viewModel.activeEngineState.collectAsState()
    val isDesktop by viewModel.isDesktopActive.collectAsState()
    val activeTab = viewModel.getActiveTab()

    // Sheet states for offline utilities
    var activeToolSheet by remember { mutableStateOf<String?>(null) } // json, url, time, unit, color, translate, editor, markdown, shortcuts

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Browser Tools & Utilities",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("tools_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "NIVORA Power Suite",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Native offline utilities, web page tools, translation, and productivity suite.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Developed by Bhaskar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Webpage Tools
            item {
                Text(text = "Active Page Tools", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.MenuBook,
                    title = "Reader Mode",
                    subtitle = "Declutter webpage for distraction-free reading",
                    onClick = onNavigateReaderMode
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.Translate,
                    title = "Translate Webpage",
                    subtitle = "Translate active site to Spanish, French, Hindi, German, etc.",
                    onClick = { activeToolSheet = "translate" }
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.Print,
                    title = "Print / Save as PDF",
                    subtitle = "Native Android print manager & PDF exporter",
                    onClick = {
                        val webView = (activeTab?.engine as? com.nivora.browser.browser.webview.AndroidWebViewEngine)?.webView
                        if (webView != null) {
                            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager
                            val printAdapter = webView.createPrintDocumentAdapter("NIVORA_Page_${System.currentTimeMillis()}")
                            printManager?.print("NIVORA Web Document", printAdapter, android.print.PrintAttributes.Builder().build())
                        } else {
                            Toast.makeText(context, "No active page to print", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.CameraAlt,
                    title = "Capture Fullscreen Screenshot",
                    subtitle = "Capture visible webpage canvas and share",
                    onClick = {
                        val webView = (activeTab?.engine as? com.nivora.browser.browser.webview.AndroidWebViewEngine)?.webView
                        if (webView != null && webView.width > 0 && webView.height > 0) {
                            try {
                                val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bitmap)
                                webView.draw(canvas)
                                val file = File(context.cacheDir, "nivora_screenshot_${System.currentTimeMillis()}.png")
                                FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Webpage Screenshot"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not capture screenshot", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "No webpage loaded to capture", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.Computer,
                    title = "Desktop Site Mode",
                    subtitle = if (isDesktop) "Desktop User-Agent Active" else "Mobile User-Agent Active",
                    trailing = {
                        Switch(
                            checked = isDesktop,
                            onCheckedChange = { viewModel.toggleDesktopMode(rememberForSite = true) },
                            modifier = Modifier.testTag("tools_desktop_switch")
                        )
                    }
                )
            }

            // Offline Utilities
            item {
                Text(text = "Offline Developer & General Utilities", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.DataObject,
                    title = "JSON Viewer & Formatter",
                    subtitle = "Parse, validate, and pretty-print JSON documents",
                    onClick = { activeToolSheet = "json" }
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.Link,
                    title = "URL Encoder / Decoder",
                    subtitle = "Encode and decode special URL query characters",
                    onClick = { activeToolSheet = "url" }
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.Schedule,
                    title = "Timestamp & Epoch Converter",
                    subtitle = "Convert Unix epoch milliseconds to human date/time",
                    onClick = { activeToolSheet = "time" }
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.Calculate,
                    title = "Unit Converter",
                    subtitle = "Convert digital storage, length, and temperature",
                    onClick = { activeToolSheet = "unit" }
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.ColorLens,
                    title = "Color Picker & HEX Tool",
                    subtitle = "Visual RGB/HEX color palette generator",
                    onClick = { activeToolSheet = "color" }
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.Key,
                    title = "Password Generator",
                    subtitle = "Generate strong cryptographically secure passwords",
                    onClick = onNavigatePasswordGenerator
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.EditNote,
                    title = "Offline Text Editor",
                    subtitle = "Distraction-free scratchpad with word & line counter",
                    onClick = { activeToolSheet = "editor" }
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.Code,
                    title = "Markdown Viewer & Preview",
                    subtitle = "Write, edit, and preview Markdown formatted text",
                    onClick = { activeToolSheet = "markdown" }
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.Link,
                    title = "Custom Search Shortcuts",
                    subtitle = "Configure 'yt', 'wiki', 'gh' and custom keyword search aliases",
                    onClick = { activeToolSheet = "shortcuts" }
                )
            }

            // App Extensions & Native
            item {
                Text(text = "Native Android Modules", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.EditNote,
                    title = "NIVORA Notes",
                    subtitle = "Research notes, AI summaries, and pinned articles",
                    onClick = onNavigateNotes
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.Apps,
                    title = "Web Apps & PWAs",
                    subtitle = "Manage standalone web applications",
                    onClick = onNavigateWebApps
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.Backup,
                    title = "Backup & Data Transfer",
                    subtitle = "Export / import JSON and HTML bookmarks",
                    onClick = onNavigateBackup
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.QrCodeScanner,
                    title = "QR Code Scanner",
                    subtitle = "Safe camera scan with link security verification",
                    onClick = onNavigateQrScanner
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.QrCode,
                    title = "QR Code Generator",
                    subtitle = "Generate QR for URLs, plain text, Wi-Fi, and vCards",
                    onClick = onNavigateQrGenerator
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.PictureAsPdf,
                    title = "Native PDF Viewer",
                    subtitle = "Document renderer with zoom, pagination & search",
                    onClick = onNavigatePdfViewer
                )
            }

            item {
                ToolActionCard(
                    icon = Icons.Default.Security,
                    title = "Permissions Center",
                    subtitle = "Manage Camera, Mic, Geolocation, Notifications",
                    onClick = onNavigatePermissions
                )
            }
        }
    }

    // Modal Bottom Sheets for Offline Tools
    if (activeToolSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeToolSheet = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            when (activeToolSheet) {
                "json" -> JsonViewerDialog { activeToolSheet = null }
                "url" -> UrlCodecDialog { activeToolSheet = null }
                "time" -> TimestampConverterDialog { activeToolSheet = null }
                "unit" -> UnitConverterDialog { activeToolSheet = null }
                "color" -> ColorPickerDialog { activeToolSheet = null }
                "editor" -> TextEditorDialog(
                    onDismiss = { activeToolSheet = null },
                    onSaveToNotes = { noteTitle, noteContent ->
                        scope.launch {
                            repository?.addNote(
                                title = noteTitle,
                                content = noteContent,
                                url = null
                            )
                        }
                    }
                )
                "markdown" -> MarkdownViewerDialog { activeToolSheet = null }
                "shortcuts" -> SearchShortcutsDialog { activeToolSheet = null }
                "translate" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Translate Webpage", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "Choose target language for '${activeState.title.take(30)}':",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TranslationEngine.supportedLanguages.forEach { lang ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val webView = (activeTab?.engine as? com.nivora.browser.browser.webview.AndroidWebViewEngine)?.webView
                                        if (webView != null) {
                                            TranslationEngine.translateWebPage(webView, lang.code)
                                            Toast.makeText(context, "Translating to ${lang.displayName}...", Toast.LENGTH_SHORT).show()
                                        }
                                        activeToolSheet = null
                                    }
                            ) {
                                Text(text = lang.displayName, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (trailing != null) {
                trailing()
            }
        }
    }
}
