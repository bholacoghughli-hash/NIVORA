package com.nivora.browser.ui.privacy

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NivoraApplication
import com.nivora.browser.search.SearchResolver
import com.nivora.browser.ui.browser.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyShieldScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? NivoraApplication
    val trackerEngine = app?.trackerBlockerEngine

    val activeState by viewModel.activeEngineState.collectAsState()
    val stats by (trackerEngine?.stats ?: kotlinx.coroutines.flow.MutableStateFlow(com.nivora.browser.privacy.TrackerStats())).collectAsState()

    val currentHost = remember(activeState.url) {
        SearchResolver.extractDomain(activeState.url)
    }

    var isShieldActiveForSite by remember(currentHost) {
        mutableStateOf(currentHost.isNotBlank() && !(trackerEngine?.isSiteAllowed(currentHost) ?: false))
    }

    var trackerBlockingEnabled by remember {
        mutableStateOf(trackerEngine?.isTrackerBlockingEnabled ?: true)
    }
    var contentBlockingEnabled by remember {
        mutableStateOf(trackerEngine?.isContentBlockingEnabled ?: true)
    }

    var showAddCustomBlockDialog by remember { mutableStateOf(false) }
    var newBlockDomain by remember { mutableStateOf("") }
    var customBlockedList by remember {
        mutableStateOf(trackerEngine?.getCustomBlockedDomains() ?: emptyList())
    }
    var allowedList by remember {
        mutableStateOf(trackerEngine?.getAllowedSites() ?: emptyList())
    }

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "NIVORA Privacy Shield",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("shield_back_btn")) {
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
            // Real Current Site Protection Card
            if (currentHost.isNotBlank() && !currentHost.startsWith("about")) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isShieldActiveForSite) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (isShieldActiveForSite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = currentHost,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (isShieldActiveForSite) "Shield Active • Tracking Blocked" else "Shield Paused for this site",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = isShieldActiveForSite,
                                    onCheckedChange = { active ->
                                        isShieldActiveForSite = active
                                        if (active) {
                                            trackerEngine?.removeAllowedSite(currentHost)
                                        } else {
                                            trackerEngine?.allowSite(currentHost)
                                        }
                                        allowedList = trackerEngine?.getAllowedSites() ?: emptyList()
                                        Toast.makeText(context, if (active) "Shield enabled for $currentHost" else "Shield whitelisted for $currentHost", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Actual requests blocked on this page
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${stats.blockedOnCurrentPage}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Blocked on Page",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${stats.totalBlockedLifetime}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Total Session Blocked",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Global Shield Toggles
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cross-Site Tracker Blocking",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Intercepts tracking scripts, beacons, and telemetry domains",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = trackerBlockingEnabled,
                                onCheckedChange = {
                                    trackerBlockingEnabled = it
                                    trackerEngine?.isTrackerBlockingEnabled = it
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Content & Ad Network Filter",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Drops known ad-exchange requests before network transport",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = contentBlockingEnabled,
                                onCheckedChange = {
                                    contentBlockingEnabled = it
                                    trackerEngine?.isContentBlockingEnabled = it
                                }
                            )
                        }
                    }
                }
            }

            // Custom Block List Header & Add Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Block List (${customBlockedList.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { showAddCustomBlockDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Domain")
                    }
                }
            }

            // Custom Block List Items
            if (customBlockedList.isEmpty()) {
                item {
                    Text(
                        text = "No custom domains blocked. Tap 'Add Domain' to block specific hosts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(customBlockedList.size) { idx ->
                    val domain = customBlockedList[idx]
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = domain, style = MaterialTheme.typography.bodyMedium)
                            IconButton(
                                onClick = {
                                    trackerEngine?.removeCustomBlockDomain(domain)
                                    customBlockedList = trackerEngine?.getCustomBlockedDomains() ?: emptyList()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Allowed Sites (Whitelist)
            item {
                Text(
                    text = "Allow List (${allowedList.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (allowedList.isEmpty()) {
                item {
                    Text(
                        text = "No sites whitelisted. All sites receive full privacy shielding.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(allowedList.size) { idx ->
                    val domain = allowedList[idx]
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = domain, style = MaterialTheme.typography.bodyMedium)
                            IconButton(
                                onClick = {
                                    trackerEngine?.removeAllowedSite(domain)
                                    allowedList = trackerEngine?.getAllowedSites() ?: emptyList()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove from allow list")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCustomBlockDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomBlockDialog = false },
            title = { Text("Block Domain") },
            text = {
                OutlinedTextField(
                    value = newBlockDomain,
                    onValueChange = { newBlockDomain = it },
                    placeholder = { Text("e.g. adserver.net") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBlockDomain.isNotBlank()) {
                            trackerEngine?.addCustomBlockDomain(newBlockDomain.trim())
                            customBlockedList = trackerEngine?.getCustomBlockedDomains() ?: emptyList()
                            newBlockDomain = ""
                            showAddCustomBlockDialog = false
                        }
                    }
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
