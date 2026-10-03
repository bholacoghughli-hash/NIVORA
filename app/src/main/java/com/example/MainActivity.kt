package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.nivora.browser.browser.tabs.TabManager
import com.nivora.browser.ui.NivoraNavHost
import com.nivora.browser.ui.browser.BrowserViewModel
import com.nivora.browser.ui.browser.BrowserViewModelFactory
import com.nivora.browser.ui.theme.NivoraTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var initError: String? = null
    private var tabManager: TabManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            val app = applicationContext as? NivoraApplication
            if (app != null) {
                tabManager = TabManager(this) { url, title ->
                    try {
                        app.applicationScope.launch {
                            app.browserRepository.recordVisit(url, title)
                        }
                    } catch (_: Exception) {}
                }
            } else {
                initError = "Application context is not NivoraApplication"
            }
        } catch (t: Throwable) {
            initError = "Init Crash: ${t.javaClass.simpleName} - ${t.message}\n" + t.stackTraceToString().take(600)
        }

        setContent {
            var runtimeError by remember { mutableStateOf<String?>(null) }
            val app = applicationContext as? NivoraApplication

            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = Color(0xFF0F172A)
                ) {
                    val finalError = initError ?: runtimeError

                    if (finalError != null) {
                        // Error Screen — App crash hone ke bajay error dikhayega
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "NIVORA Diagnostic Report",
                                color = Color(0xFF38BDF8),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = finalError,
                                    color = Color(0xFFF87171),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    } else if (app != null && tabManager != null) {
                        var composeCrash by remember { mutableStateOf<String?>(null) }

                        if (composeCrash != null) {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(text = "Compose Error:\n$composeCrash", color = Color.White)
                            }
                        } else {
                            try {
                                val browserViewModel: BrowserViewModel = viewModel(
                                    factory = BrowserViewModelFactory(
                                        tabManager = tabManager!!,
                                        browserRepository = app.browserRepository,
                                        settingsRepository = app.settingsRepository
                                    )
                                )
                                val settings by browserViewModel.settings.collectAsState()
                                val navController = rememberNavController()

                                NivoraTheme(
                                    themeMode = settings.themeMode,
                                    accentColor = settings.accentColor
                                ) {
                                    NivoraNavHost(
                                        navController = navController,
                                        viewModel = browserViewModel
                                    )
                                }
                            } catch (t: Throwable) {
                                composeCrash = "${t.message}\n" + t.stackTraceToString().take(500)
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8))
                        }
                    }
                }
            }
        }
    }
}
