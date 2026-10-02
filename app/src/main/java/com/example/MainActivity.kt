package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.nivora.browser.browser.tabs.TabManager
import com.nivora.browser.ui.NivoraNavHost
import com.nivora.browser.ui.browser.BrowserViewModel
import com.nivora.browser.ui.browser.BrowserViewModelFactory
import com.nivora.browser.ui.theme.NivoraTheme

class MainActivity : ComponentActivity() {

    private var tabManager: TabManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            val app = application as NivoraApplication
            tabManager = TabManager(this) { url, title ->
                try {
                    app.applicationScope.run {
                        app.browserRepository.recordVisit(url, title)
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val app = application as? NivoraApplication

            if (app == null || tabManager == null) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Starting NIVORA Browser...")
                    }
                }
                return@setContent
            }

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
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                    ) {
                        NivoraNavHost(
                            navController = navController,
                            viewModel = browserViewModel
                        )
                    }
                }
            } catch (e: Exception) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Error initializing engine: ${e.localizedMessage}")
                    }
                }
            }
        }
    }
}
