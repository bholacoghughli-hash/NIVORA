package com.nivora.browser.search

import android.util.Patterns
import com.nivora.browser.data.local.SearchProvider
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SearchResolver {

    private val COMMON_TLDS = setOf(
        "com", "org", "net", "edu", "gov", "mil", "io", "ai", "co", "in", "app", "dev",
        "me", "info", "biz", "online", "site", "tech", "xyz", "cloud", "store", "news",
        "live", "world", "space", "agency", "digital", "top", "pro", "mobi", "tv", "cc"
    )

    fun isUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.contains(" ") || trimmed.contains("\n")) return false

        // Check if starts with common web schemes
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true)
        ) {
            return true
        }

        // Check against Android Web URL pattern
        if (Patterns.WEB_URL.matcher(trimmed).matches()) {
            return true
        }

        // Check domain pattern with common TLD
        val parts = trimmed.split("/")
        val hostPart = parts.firstOrNull() ?: return false
        val hostTokens = hostPart.split(".")
        if (hostTokens.size >= 2) {
            val tld = hostTokens.last().lowercase()
            if (COMMON_TLDS.contains(tld)) {
                return true
            }
        }

        // Localhost pattern
        if (hostPart.startsWith("localhost", ignoreCase = true) || hostPart.startsWith("127.0.0.1")) {
            return true
        }

        return false
    }

    private val defaultShortcuts = mutableMapOf(
        "yt" to "https://www.youtube.com/results?search_query=%s",
        "wiki" to "https://en.wikipedia.org/wiki/Special:Search?search=%s",
        "gh" to "https://github.com/search?q=%s",
        "ddg" to "https://duckduckgo.com/?q=%s",
        "reddit" to "https://www.reddit.com/search/?q=%s"
    )

    private val userCustomShortcuts = mutableMapOf<String, String>()

    fun getShortcuts(): Map<String, String> {
        val combined = HashMap(defaultShortcuts)
        combined.putAll(userCustomShortcuts)
        return combined
    }

    fun registerShortcut(keyword: String, searchTemplate: String) {
        val cleanKey = keyword.trim().lowercase()
        if (cleanKey.isNotBlank() && searchTemplate.contains("%s")) {
            userCustomShortcuts[cleanKey] = searchTemplate.trim()
        }
    }

    fun removeShortcut(keyword: String) {
        userCustomShortcuts.remove(keyword.trim().lowercase())
    }

    fun resolveInput(input: String, searchProvider: SearchProvider): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "about:blank"

        // Check if starts with a shortcut prefix (e.g. "yt Android tutorial")
        val spaceIndex = trimmed.indexOf(' ')
        if (spaceIndex > 0) {
            val prefix = trimmed.substring(0, spaceIndex).lowercase()
            val queryPart = trimmed.substring(spaceIndex + 1).trim()
            val shortcutTemplate = getShortcuts()[prefix]
            if (shortcutTemplate != null && queryPart.isNotBlank()) {
                val encodedQuery = URLEncoder.encode(queryPart, StandardCharsets.UTF_8.name())
                return String.format(shortcutTemplate, encodedQuery)
            }
        }

        if (isUrl(trimmed)) {
            return if (!trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true) &&
                !trimmed.startsWith("file://", ignoreCase = true) &&
                !trimmed.startsWith("about:", ignoreCase = true)
            ) {
                "https://$trimmed"
            } else {
                trimmed
            }
        }

        // Otherwise format query using search provider
        val encodedQuery = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name())
        return String.format(searchProvider.searchUrl, encodedQuery)
    }

    fun extractDomain(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: url
        } catch (_: Exception) {
            url
        }
    }
}
