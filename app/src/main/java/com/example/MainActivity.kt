package com.example

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.nivora.browser.ui.NivoraDestinations
import com.nivora.browser.ui.NivoraNavHost
import com.nivora.browser.ui.browser.BrowserViewModel
import com.nivora.browser.ui.browser.BrowserViewModelFactory
import com.nivora.browser.ui.theme.NivoraTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NivoraApplication

        setContent {
            val browserViewModel: BrowserViewModel = viewModel(
                factory = BrowserViewModelFactory(
                    tabManager = app.tabManager,
                    browserRepository = app.browserRepository,
                    settingsRepository = app.settingsRepository
                )
            )

            val settings by browserViewModel.settings.collectAsState()
            val navController = rememberNavController()
            val isLocked by app.biometricLockManager.isLocked.collectAsState()

            val unlockLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    app.biometricLockManager.unlockApp()
                } else {
                    Toast.makeText(this, "Authentication required to open NIVORA", Toast.LENGTH_SHORT).show()
                }
            }

            // Handle incoming intents (http/https and nivora shortcuts)
            LaunchedEffect(intent) {
                if (intent?.action == Intent.ACTION_VIEW) {
                    val dataString = intent?.dataString
                    if (!dataString.isNullOrBlank()) {
                        when {
                            dataString.startsWith("nivora://shortcut/new_tab") -> {
                                browserViewModel.openNewTab(isIncognito = false)
                                navController.navigate(NivoraDestinations.HOME)
                            }
                            dataString.startsWith("nivora://shortcut/private_tab") -> {
                                browserViewModel.openNewTab(isIncognito = true)
                                navController.navigate(NivoraDestinations.HOME)
                            }
                            dataString.startsWith("nivora://shortcut/qr_scanner") -> {
                                navController.navigate(NivoraDestinations.QR_SCANNER)
                            }
                            dataString.startsWith("nivora://shortcut/downloads") -> {
                                navController.navigate(NivoraDestinations.DOWNLOADS)
                            }
                            dataString.startsWith("http://") || dataString.startsWith("https://") -> {
                                browserViewModel.loadUrlOrSearch(dataString)
                                navController.navigate(NivoraDestinations.BROWSER)
                            }
                        }
                    }
                }
            }

            var isAppStarting by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(450)
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
                        // Clean Modern Splash Screen
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
                                        .size(88.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(46.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

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

                                Spacer(modifier = Modifier.height(32.dp))

                                Text(
                                    text = "Created by Bhaskar Gautam",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else if (isLocked) {
                        // Biometric Lock Screen Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(24.dp),
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
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "NIVORA is Locked",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Text(
                                    text = "Biometric authentication or device credential required to access your browser sessions.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                                )

                                Button(
                                    onClick = {
                                        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                                        val unlockIntent = keyguardManager?.createConfirmDeviceCredentialIntent(
                                            "Unlock NIVORA",
                                            "Confirm your screen lock to proceed"
                                        )
                                        if (unlockIntent != null) {
                                            unlockLauncher.launch(unlockIntent)
                                        } else {
                                            app.biometricLockManager.unlockApp()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text("Unlock Browser")
                                }
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
