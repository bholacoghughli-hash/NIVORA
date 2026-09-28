package com.nivora.browser.ui.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NivoraApplication
import com.nivora.browser.ai.AIResponse
import com.nivora.browser.ai.PageContext
import com.nivora.browser.ui.browser.BrowserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? NivoraApplication
    val aiProvider = app?.aiProvider
    val repository = app?.browserRepository
    val scope = rememberCoroutineScope()

    val activeState by viewModel.activeEngineState.collectAsState()

    var userCustomPrompt by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf("Summarize") }
    var isLoading by remember { mutableStateOf(false) }
    var aiResult by remember { mutableStateOf<AIResponse?>(null) }

    val pageContext = remember(activeState.url, activeState.title) {
        val snippet = if (activeState.url.isNotBlank() && !activeState.url.startsWith("about:")) {
            "Active webpage: ${activeState.title}. Domain: ${activeState.url}. Web document loaded with TLS encryption."
        } else {
            "NIVORA Start Page. Private, secure, and privacy-first browsing environment."
        }
        PageContext(
            url = activeState.url,
            title = activeState.title.ifBlank { "Current Page" },
            contentSnippet = snippet
        )
    }

    fun executeAiAction(prompt: String) {
        isLoading = true
        scope.launch {
            val response = aiProvider?.processRequest(prompt, pageContext)
                ?: com.nivora.browser.ai.LocalHeuristicAIProvider().processRequest(prompt, pageContext)
            aiResult = response
            isLoading = false
        }
    }

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "NIVORA AI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("ai_back_btn")) {
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            val isTabletWide = maxWidth > 720.dp

            if (isTabletWide) {
                // Adaptive Tablet Split View
                Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Left Column: Source context & controls
                    Column(modifier = Modifier.weight(1f)) {
                        SourceContentCard(pageContext = pageContext)
                        Spacer(modifier = Modifier.height(14.dp))
                        ActionChipsRow(
                            selectedAction = selectedAction,
                            onSelectAction = { action ->
                                selectedAction = action
                                executeAiAction(action)
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        CustomPromptInput(
                            prompt = userCustomPrompt,
                            onPromptChange = { userCustomPrompt = it },
                            onSend = {
                                if (userCustomPrompt.isNotBlank()) {
                                    executeAiAction(userCustomPrompt)
                                    userCustomPrompt = ""
                                }
                            }
                        )
                    }

                    // Right Column: AI Output
                    Box(modifier = Modifier.weight(1.2f)) {
                        AiOutputPanel(
                            isLoading = isLoading,
                            aiResult = aiResult,
                            onSaveToNotes = { text ->
                                scope.launch {
                                    repository?.addNote(
                                        title = "AI: ${pageContext.title.take(30)}",
                                        content = text,
                                        url = pageContext.url
                                    )
                                    Toast.makeText(context, "Saved to NIVORA Notes", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            } else {
                // Phone Layout: Unified Vertical Stream
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        SourceContentCard(pageContext = pageContext)
                    }

                    item {
                        ActionChipsRow(
                            selectedAction = selectedAction,
                            onSelectAction = { action ->
                                selectedAction = action
                                executeAiAction(action)
                            }
                        )
                    }

                    item {
                        CustomPromptInput(
                            prompt = userCustomPrompt,
                            onPromptChange = { userCustomPrompt = it },
                            onSend = {
                                if (userCustomPrompt.isNotBlank()) {
                                    executeAiAction(userCustomPrompt)
                                    userCustomPrompt = ""
                                }
                            }
                        )
                    }

                    item {
                        AiOutputPanel(
                            isLoading = isLoading,
                            aiResult = aiResult,
                            onSaveToNotes = { text ->
                                scope.launch {
                                    repository?.addNote(
                                        title = "AI: ${pageContext.title.take(30)}",
                                        content = text,
                                        url = pageContext.url
                                    )
                                    Toast.makeText(context, "Saved to NIVORA Notes", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceContentCard(pageContext: PageContext) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "SOURCE CONTENT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Active Tab",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pageContext.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2
            )

            if (pageContext.url.isNotBlank()) {
                Text(
                    text = pageContext.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ActionChipsRow(
    selectedAction: String,
    onSelectAction: (String) -> Unit
) {
    val actions = listOf(
        "Summarize",
        "Explain Page",
        "Key Points",
        "Generate Notes",
        "Translate",
        "Compare Info",
        "Search Queries"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "AI Quick Actions",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            actions.take(3).forEach { action ->
                FilterChip(
                    selected = selectedAction == action,
                    onClick = { onSelectAction(action) },
                    label = { Text(action, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            actions.drop(3).take(4).forEach { action ->
                FilterChip(
                    selected = selectedAction == action,
                    onClick = { onSelectAction(action) },
                    label = { Text(action, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CustomPromptInput(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit
) {
    OutlinedTextField(
        value = prompt,
        onValueChange = onPromptChange,
        placeholder = { Text("Ask anything about this page...") },
        trailingIcon = {
            IconButton(onClick = onSend, enabled = prompt.isNotBlank()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (prompt.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AiOutputPanel(
    isLoading: Boolean,
    aiResult: AIResponse?,
    onSaveToNotes: (String) -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "AI GENERATED CONTENT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (aiResult != null) {
                    Text(
                        text = aiResult.providerUsed,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else if (aiResult != null) {
                Text(
                    text = aiResult.generatedContent,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSaveToNotes(aiResult.generatedContent) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Notes")
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Content", aiResult.generatedContent))
                            Toast.makeText(context, "Copied AI output", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = aiResult.disclaimer,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap a Quick Action or ask a question above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
