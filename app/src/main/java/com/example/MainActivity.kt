package com.example

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var urlInput by remember { mutableStateOf("https://duckduckgo.com") }
            var currentUrl by remember { mutableStateOf("https://duckduckgo.com") }
            var pageTitle by remember { mutableStateOf("NIVORA") }
            var isLoading by remember { mutableStateOf(false) }
            var progress by remember { mutableIntStateOf(0) }
            var canGoBack by remember { mutableStateOf(false) }
            var canGoForward by remember { mutableStateOf(false) }
            val focusManager = LocalFocusManager.current

            BackHandler(enabled = canGoBack) {
                webView?.goBack()
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = Color(0xFF0A0F1D)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF111827),
                            shadowElevation = 4.dp
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val home = "https://duckduckgo.com"
                                            urlInput = home
                                            webView?.loadUrl(home)
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = "Home",
                                            tint = Color(0xFF38BDF8)
                                        )
                                    }

                                    // Address Bar
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(Color(0xFF1E293B))
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            BasicTextField(
                                                value = urlInput,
                                                onValueChange = { urlInput = it },
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                ),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                                keyboardActions = KeyboardActions(
                                                    onGo = {
                                                        focusManager.clearFocus()
                                                        val target = urlInput.trim()
                                                        val finalUrl = if (target.startsWith("http://") || target.startsWith("https://")) {
                                                            target
                                                        } else if (target.contains(".") && !target.contains(" ")) {
                                                            "https://$target"
                                                        } else {
                                                            val q = URLEncoder.encode(target, StandardCharsets.UTF_8.name())
                                                            "https://duckduckgo.com/?q=$q"
                                                        }
                                                        urlInput = finalUrl
                                                        webView?.loadUrl(finalUrl)
                                                    }
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { webView?.reload() },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reload",
                                            tint = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                if (isLoading && progress < 100) {
                                    LinearProgressIndicator(
                                        progress = { progress / 100f },
                                        modifier = Modifier.fillMaxWidth().height(2.dp),
                                        color = Color(0xFF00C9FF),
                                        trackColor = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }

                        // Web View
                        Box(modifier = Modifier.weight(1f)) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            databaseEnabled = true
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                            setSupportZoom(true)
                                            builtInZoomControls = true
                                            displayZoomControls = false
                                            cacheMode = WebSettings.LOAD_DEFAULT
                                        }
                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                progress = newProgress
                                                isLoading = newProgress < 100
                                            }

                                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                                if (!title.isNullOrBlank()) {
                                                    pageTitle = title
                                                }
                                            }
                                        }
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                currentUrl = url ?: ""
                                                urlInput = currentUrl
                                                isLoading = true
                                                canGoBack = canGoBack()
                                                canGoForward = canGoForward()
                                            }

                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                isLoading = false
                                                canGoBack = canGoBack()
                                                canGoForward = canGoForward()
                                            }
                                        }
                                        loadUrl(currentUrl)
                                        webView = this
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Bottom Navigation Controls
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF111827),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { webView?.goBack() },
                                    enabled = canGoBack
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = if (canGoBack) Color.White else Color(0xFF475569)
                                    )
                                }

                                Text(
                                    text = "NIVORA BROWSER",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = { webView?.goForward() },
                                    enabled = canGoForward
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Forward",
                                        tint = if (canGoForward) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
