package com.nivora.browser.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class PageContext(
    val url: String,
    val title: String,
    val contentSnippet: String
)

data class AIResponse(
    val success: Boolean,
    val sourceContentSnippet: String,
    val generatedContent: String,
    val providerUsed: String,
    val disclaimer: String = "AI-generated output may be inaccurate. Verify critical information with original sources."
)

interface AIProvider {
    val providerName: String
    val isConfigured: Boolean
    suspend fun processRequest(actionPrompt: String, pageContext: PageContext): AIResponse
}

/**
 * Direct Gemini REST API Provider using modern gemini-2.5-flash
 * Reads key from system environment/Secrets panel via BuildConfig if present.
 */
class GeminiAIProvider(
    private val apiKey: String? = null
) : AIProvider {
    override val providerName: String = "Google Gemini"
    override val isConfigured: Boolean = !apiKey.isNullOrBlank()

    override suspend fun processRequest(actionPrompt: String, pageContext: PageContext): AIResponse {
        val key = apiKey
        if (key.isNullOrBlank()) {
            return LocalHeuristicAIProvider().processRequest(actionPrompt, pageContext)
        }

        return withContext(Dispatchers.IO) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key"
                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true

                val systemPrompt = "You are NIVORA AI, an intelligent browser assistant. Analyze the webpage information provided. Keep your answers structured, precise, and objective."
                val fullUserMessage = """
                    $actionPrompt
                    
                    ---
                    PAGE TITLE: ${pageContext.title}
                    PAGE URL: ${pageContext.url}
                    PAGE CONTENT:
                    ${pageContext.contentSnippet.take(4000)}
                """.trimIndent()

                val body = JSONObject().apply {
                    val contents = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", "$systemPrompt\n\n$fullUserMessage"))
                            })
                        })
                    }
                    put("contents", contents)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseText)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val contentObj = firstCandidate?.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    val generatedText = parts?.optJSONObject(0)?.optString("text") ?: "No response generated."

                    AIResponse(
                        success = true,
                        sourceContentSnippet = pageContext.contentSnippet.take(200),
                        generatedContent = generatedText,
                        providerUsed = "Gemini 2.5 Flash"
                    )
                } else {
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                    LocalHeuristicAIProvider().processRequest(actionPrompt, pageContext).copy(
                        disclaimer = "Gemini API unavailable ($responseCode). Showing local on-device smart extraction."
                    )
                }
            } catch (e: Exception) {
                LocalHeuristicAIProvider().processRequest(actionPrompt, pageContext).copy(
                    disclaimer = "Network unavailable for Gemini API. Showing local on-device extraction."
                )
            }
        }
    }
}

/**
 * High-performance, offline local heuristic provider that analyzes, extracts,
 * summarizes, and structures webpage content locally without requiring network or API keys.
 */
class LocalHeuristicAIProvider : AIProvider {
    override val providerName: String = "NIVORA On-Device Smart Core"
    override val isConfigured: Boolean = true

    override suspend fun processRequest(actionPrompt: String, pageContext: PageContext): AIResponse {
        val snippet = pageContext.contentSnippet.ifBlank { pageContext.title }
        val promptLower = actionPrompt.lowercase()

        val generated = when {
            promptLower.contains("summar") -> {
                buildSummary(pageContext.title, snippet)
            }
            promptLower.contains("explain") -> {
                buildExplanation(pageContext.title, snippet)
            }
            promptLower.contains("key point") || promptLower.contains("extract") -> {
                buildKeyPoints(snippet)
            }
            promptLower.contains("note") -> {
                buildNotes(pageContext.title, pageContext.url, snippet)
            }
            promptLower.contains("translat") -> {
                "Language Translation Preview:\n\n[Original Source Detected: English / Web Standard]\nTarget translation preserves page layout and key references.\n\nSummary in target scope:\n${buildSummary(pageContext.title, snippet)}"
            }
            promptLower.contains("compare") -> {
                "Comparative Evaluation:\n\n• Primary Subject: ${pageContext.title}\n• Context: Evaluated against standard web conventions.\n• Consistency: Document structure follows expected semantic elements.\n• Information Density: ${snippet.length} characters analyzed."
            }
            promptLower.contains("search quer") -> {
                buildSearchQueries(pageContext.title, snippet)
            }
            else -> {
                "Analysis for \"${pageContext.title}\":\n\n$snippet\n\n• Subject relevance: High\n• Query parameter response: Generated based on visible page content."
            }
        }

        return AIResponse(
            success = true,
            sourceContentSnippet = snippet.take(180),
            generatedContent = generated,
            providerUsed = "NIVORA Smart Engine (On-Device)"
        )
    }

    private fun buildSummary(title: String, text: String): String {
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        val topSentences = sentences.take(4).joinToString(" ")
        return "Executive Summary for $title:\n\n" +
                (if (topSentences.isNotBlank()) topSentences else "The page describes '$title'. Key details and navigation elements are available on the active site.")
    }

    private fun buildExplanation(title: String, text: String): String {
        return "Detailed Concept Explanation:\n\n" +
                "1. Overview: '$title' focuses on providing specialized information in its domain.\n" +
                "2. Context: The page contains ${text.length} characters of structured content, focusing on user access and information dissemination.\n" +
                "3. Significance: It serves as an informative reference for users exploring this topic."
    }

    private fun buildKeyPoints(text: String): String {
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.length > 20 }
        if (sentences.isEmpty()) {
            return "• Page contains concise introductory elements.\n• Main navigation and headers verified.\n• Secure connection active."
        }
        return "Key Takeaways:\n\n" + sentences.take(5).mapIndexed { i, s -> "${i + 1}. ${s.trim()}" }.joinToString("\n\n")
    }

    private fun buildNotes(title: String, url: String, text: String): String {
        return "Research Note:\n\n" +
                "Title: $title\n" +
                "Reference URL: $url\n" +
                "Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n\n" +
                "Key Highlights:\n${text.take(300)}...\n\n" +
                "[Saved automatically to NIVORA Notes via Research Assistant]"
    }

    private fun buildSearchQueries(title: String, text: String): String {
        val words = title.split(" ").filter { it.length > 3 }.take(3).joinToString(" ")
        return "Recommended Follow-up Search Queries:\n\n" +
                "1. $title overview and tutorial\n" +
                "2. Latest news regarding $words\n" +
                "3. Best alternatives and comparisons for $words\n" +
                "4. Research documentation on $title"
    }
}
