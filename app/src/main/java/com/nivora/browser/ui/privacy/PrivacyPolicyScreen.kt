package com.nivora.browser.ui.privacy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Privacy Policy & Disclaimers",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("policy_back_btn")) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "NIVORA Privacy Commitment",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Search Freely. Browse Privately. Developed by Bhaskar Gautam.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            item {
                PolicySection(
                    title = "1. Local Data Storage",
                    content = "Your browsing history, bookmarks, reading lists, tab groups, and application preferences are stored exclusively on your device within an encrypted Room SQLite database. NIVORA operates without cloud telemetry, meaning your data never leaves your device unless you initiate an export or share action."
                )
            }

            item {
                PolicySection(
                    title = "2. Search Providers",
                    content = "When you query via the address bar, NIVORA transmits your search terms solely to your chosen search engine (DuckDuckGo, Google, Bing, Brave, Ecosia, Qwant, or Startpage) via TLS encrypted HTTPS requests with Do Not Track (DNT: 1) headers enabled."
                )
            }

            item {
                PolicySection(
                    title = "3. AI Services",
                    content = "When interacting with NIVORA AI assistant, prompts are processed directly via Google's Gemini SDK. NIVORA does not transmit background web browsing history or saved bookmarks to AI services without your explicit invocation."
                )
            }

            item {
                PolicySection(
                    title = "4. Cookies & Cross-Site Trackers",
                    content = "NIVORA isolates third-party cookies by default and drops known telemetry requests via its native request interceptor. You retain full control to wipe cookies, clear web cache, and empty local storage on demand."
                )
            }

            item {
                PolicySection(
                    title = "5. Device Permissions",
                    content = "Camera, microphone, and location hardware access are guarded strictly on demand. NIVORA never grants sensor permissions automatically. Every request displays the requesting origin with explicit allow and deny controls."
                )
            }

            item {
                PolicySection(
                    title = "6. Honest Private Mode Limitations",
                    content = "Private (Incognito) mode isolates cookies and prevents session history from being recorded on your device. However, private browsing does NOT make you invisible to your Internet Service Provider (ISP), network administrators on public Wi-Fi, or the websites you actively log into."
                )
            }

            item {
                PolicySection(
                    title = "7. VPN & Proxy Clarity",
                    content = "NIVORA does not claim false VPN shielding or route traffic through third-party proxy servers without explicit user configuration. Real security relies on verified TLS certificates and request-level tracker blocking."
                )
            }
        }
    }
}

@Composable
private fun PolicySection(title: String, content: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
        }
    }
}
