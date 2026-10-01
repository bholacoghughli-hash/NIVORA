package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var tabManager: TabManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as? NivoraApplication

        try {
            if (app != null) {
                tabManager = TabManager(this) { url, title ->
                    app.applicationScope.launch {
                        app.browserRepository.recordVisit(url, title)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            if (app == null || tabManager == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@setContent
            }

            val browserViewModel: BrowserViewModel = viewModel(
                factory = BrowserViewModelFactory(
                    tabManager = tabManager!!,
                    browserRepository = app.browserRepository,
                    settingsRepository = app.settingsRepository
                )
            )

            val settings by browserViewModel.settings.collectAsState()
            val navController = rememberNavController()

            var isAppStarting by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(350)
                isAppStarting = false
            }

            NivoraTheme(
                themeMode = settings.themeMode,
                accentColor = settings.accentColor
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    if (isAppStarting) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "NIVORA",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Search Freely. Browse Privately.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        NivoraNavHost(
                            navController = navController,
                            viewModel = browserViewModel
                        )
                    }
                }
            }
        }
    }
}
