package com.nivora.browser.ui.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ControlPointDuplicate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.NivoraApplication
import com.nivora.browser.browser.webview.AndroidWebViewEngine
import com.nivora.browser.search.SearchResolver
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.FileOutputStream

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onNavigateHome: () -> Unit,
    onNavigateTabs: () -> Unit,
    onNavigateBookmarks: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateDownloads: () -> Unit,
    onNavigateTools: () -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? NivoraApplication
    val activeEngineState by viewModel.activeEngineState.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val activeTab = viewModel.getActiveTab()
    val isDesktopActive by viewModel.isDesktopActive.collectAsState()
    val isFindVisible by viewModel.isFindInPageVisible.collectAsState()
    val findQuery by viewModel.findInPageQuery.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    // Fullscreen View Handling
    val fullscreenView by (activeTab?.engine?.fullscreenView ?: MutableStateFlow(null)).collectAsState()

    // File Upload Handling
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            filePathCallback?.onReceiveValue(uris.toTypedArray())
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    // Permission and Geolocation Prompts
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingGeoOrigin by remember { mutableStateOf<String?>(null) }
    var pendingGeoCallback by remember { mutableStateOf<GeolocationPermissions.Callback?>(null) }

    // Setup Engine Callbacks
    LaunchedEffect(activeTab) {
        activeTab?.engine?.downloadListener = { url, userAgent, contentDisposition, mimeType, _ ->
            app?.downloadManager?.startDownload(url, userAgent, contentDisposition, mimeType)
        }
        activeTab?.engine?.fileChooserCallback = { callback, _ ->
            filePathCallback = callback
            runCatching { fileChooserLauncher.launch(arrayOf("*/*")) }
            true
        }
        activeTab?.engine?.permissionRequestListener = { request ->
            pendingPermissionRequest = request
        }
        activeTab?.engine?.geolocationListener = { origin, callback ->
            pendingGeoOrigin = origin
            pendingGeoCallback = callback
        }
    }

    // Intercept Android Back button
    BackHandler {
        if (fullscreenView != null) {
            activeTab?.engine?.exitFullscreen()
        } else {
            val handled = viewModel.handleBackPress()
            if (!handled) {
                onNavigateHome()
            }
        }
    }

    if (fullscreenView != null) {
        // Fullscreen Video View Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = {
                    (fullscreenView?.parent as? ViewGroup)?.removeView(fullscreenView)
                    fullscreenView!!
                },
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = { activeTab?.engine?.exitFullscreen() },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Exit Fullscreen",
                    tint = Color.White
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Browser Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home Button
                        IconButton(
                            onClick = onNavigateHome,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("browser_home_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Incognito Badge if active tab is private
                        if (activeTab?.isIncognito == true) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Private Tab",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Universal Address Bar
                        UniversalAddressBar(
                            engineState = activeEngineState,
                            onNavigate = { input -> viewModel.loadUrlOrSearch(input) },
                            modifier = Modifier.weight(1f)
                        )

                        // Tabs Counter Button
                        IconButton(
                            onClick = onNavigateTabs,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("browser_tabs_btn")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(text = "${tabs.size}")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tab,
                                    contentDescription = "Tabs",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Overflow Menu
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("browser_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("New Tab") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.openNewTab()
                                        onNavigateHome()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("New Private Tab") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.openNewTab(isIncognito = true)
                                        onNavigateHome()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.VisibilityOff, contentDescription = null)
                                    }
                                )

                                if (activeTab != null) {
                                    DropdownMenuItem(
                                        text = { Text("Duplicate Tab") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.duplicateTab(activeTab.id)
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.ControlPointDuplicate, contentDescription = null)
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("Bookmark This Page") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.bookmarkCurrentPage()
                                        Toast.makeText(context, "Page bookmarked", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Bookmark, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Add to Reading List") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.addToReadingList()
                                        Toast.makeText(context, "Added to Reading List", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.MenuBook, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Share Page") },
                                    onClick = {
                                        showMenu = false
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TITLE, activeEngineState.title)
                                            putExtra(Intent.EXTRA_SUBJECT, activeEngineState.title)
                                            putExtra(Intent.EXTRA_TEXT, activeEngineState.url)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Share via NIVORA")
                                        context.startActivity(shareIntent)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Copy Link") },
                                    onClick = {
                                        showMenu = false
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("NIVORA Link", activeEngineState.url)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Open in External Browser") },
                                    onClick = {
                                        showMenu = false
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeEngineState.url))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "No other browser installed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Print Page") },
                                    onClick = {
                                        showMenu = false
                                        activeTab?.engine?.printPage("NIVORA - ${activeEngineState.title}")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Print, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Capture Screenshot") },
                                    onClick = {
                                        showMenu = false
                                        val bitmap = activeTab?.engine?.captureScreenshot()
                                        if (bitmap != null) {
                                            try {
                                                val file = File(context.cacheDir, "page_screenshot_${System.currentTimeMillis()}.png")
                                                val out = FileOutputStream(file)
                                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                                out.close()
                                                val authority = "${context.packageName}.fileprovider"
                                                val uri = FileProvider.getUriForFile(context, authority, file)
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "image/png"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share Screenshot"))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Screenshot failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Unable to capture blank page", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Find in Page") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.openFindInPage()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.FindInPage, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(if (isDesktopActive) "Mobile Site" else "Desktop Site")
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.toggleDesktopMode(rememberForSite = true)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Computer, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Browser Tools") },
                                    onClick = {
                                        showMenu = false
                                        onNavigateTools()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Shield, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("History") },
                                    onClick = {
                                        showMenu = false
                                        onNavigateHistory()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.History, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Bookmarks") },
                                    onClick = {
                                        showMenu = false
                                        onNavigateBookmarks()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.BookmarkBorder, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Downloads") },
                                    onClick = {
                                        showMenu = false
                                        onNavigateDownloads()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        showMenu = false
                                        onNavigateSettings()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Settings, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }

                    // Real Find in page search bar with match count
                    AnimatedVisibility(visible = isFindVisible) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = findQuery,
                                onValueChange = { viewModel.setFindQuery(it) },
                                placeholder = { Text("Find in page...", fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("find_in_page_input")
                            )

                            // Match Count Badge
                            if (findQuery.isNotBlank()) {
                                Text(
                                    text = "${activeEngineState.activeMatchOrdinal}/${activeEngineState.totalMatches}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            IconButton(onClick = { viewModel.findNext(false) }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
                            }

                            IconButton(onClick = { viewModel.findNext(true) }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
                            }

                            IconButton(onClick = { viewModel.closeFindInPage() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        }
                    }
                }
            }

            // Main Web View and Error State Container
            Box(modifier = Modifier.weight(1f)) {
                if (activeTab != null && activeTab.engine is AndroidWebViewEngine) {
                    AndroidView(
                        factory = {
                            val webView = (activeTab.engine as AndroidWebViewEngine).webView
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView.layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webView
                        },
                        update = {
                            // Handled reactively by engine
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("browser_webview_canvas")
                    )
                }

                // Real Error State Screen Overlay
                if (activeEngineState.errorCode != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("browser_error_overlay"),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Unable to Load Webpage",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = activeEngineState.errorDescription ?: "Network or server connection failed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            if (!activeEngineState.failingUrl.isNullOrBlank()) {
                                Text(
                                    text = activeEngineState.failingUrl ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = onNavigateHome,
                                    modifier = Modifier.testTag("error_home_btn")
                                ) {
                                    Text("Go Home")
                                }

                                Button(
                                    onClick = { viewModel.retryCurrentPage() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("error_retry_btn")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Try Again")
                                }
                            }
                        }
                    }
                }

                // SSL Security Warning Overlay
                if (activeEngineState.sslError != null && activeEngineState.errorCode == null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("browser_ssl_warning"),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Security Alert",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Security Warning: Untrusted Certificate",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = activeEngineState.sslError ?: "The website's identity could not be verified securely.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            if (activeEngineState.sslCertificateInfo != null) {
                                Text(
                                    text = activeEngineState.sslCertificateInfo ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onNavigateHome,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Back to Safety")
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back Button
                    IconButton(
                        onClick = { viewModel.handleBackPress() },
                        enabled = activeEngineState.canGoBack,
                        modifier = Modifier.testTag("browser_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (activeEngineState.canGoBack) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }

                    // Forward Button
                    IconButton(
                        onClick = { viewModel.goForward() },
                        enabled = activeEngineState.canGoForward,
                        modifier = Modifier.testTag("browser_forward_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (activeEngineState.canGoForward) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }

                    // Stop / Reload Button
                    IconButton(
                        onClick = {
                            if (activeEngineState.isLoading) {
                                viewModel.stop()
                            } else {
                                viewModel.reload()
                            }
                        },
                        modifier = Modifier.testTag("browser_reload_btn")
                    ) {
                        if (activeEngineState.isLoading) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Loading",
                                tint = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload Page",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // New Tab Action
                    IconButton(
                        onClick = {
                            viewModel.openNewTab()
                            onNavigateHome()
                        },
                        modifier = Modifier.testTag("browser_new_tab_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Tab",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Bookmarks shortcut
                    IconButton(
                        onClick = onNavigateBookmarks,
                        modifier = Modifier.testTag("browser_bookmarks_shortcut_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmarks",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // Permission Prompt Dialog (Camera & Microphone)
    pendingPermissionRequest?.let { request ->
        val origin = SearchResolver.extractDomain(request.origin.toString())
        val resources = request.resources.joinToString(", ") { r ->
            if (r.contains("VIDEO")) "Camera" else if (r.contains("AUDIO")) "Microphone" else r
        }

        AlertDialog(
            onDismissRequest = {
                request.deny()
                pendingPermissionRequest = null
            },
            title = { Text("Permission Requested") },
            text = {
                Text("Website \"$origin\" is requesting access to your $resources.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        request.grant(request.resources)
                        pendingPermissionRequest = null
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        request.deny()
                        pendingPermissionRequest = null
                    }
                ) {
                    Text("Deny")
                }
            }
        )
    }

    // Geolocation Prompt Dialog
    if (pendingGeoOrigin != null && pendingGeoCallback != null) {
        val origin = SearchResolver.extractDomain(pendingGeoOrigin!!)

        AlertDialog(
            onDismissRequest = {
                pendingGeoCallback?.invoke(pendingGeoOrigin, false, false)
                pendingGeoOrigin = null
                pendingGeoCallback = null
            },
            title = { Text("Location Request") },
            text = {
                Text("Website \"$origin\" wants to access your device location.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingGeoCallback?.invoke(pendingGeoOrigin, true, true)
                        pendingGeoOrigin = null
                        pendingGeoCallback = null
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingGeoCallback?.invoke(pendingGeoOrigin, false, false)
                        pendingGeoOrigin = null
                        pendingGeoCallback = null
                    }
                ) {
                    Text("Deny")
                }
            }
        )
    }
}
