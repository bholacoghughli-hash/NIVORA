package com.nivora.browser.privacy

import android.net.Uri
import android.webkit.WebResourceResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.ByteArrayInputStream

data class TrackerStats(
    val blockedOnCurrentPage: Int = 0,
    val totalBlockedLifetime: Int = 0
)

class TrackerBlockerEngine {

    private val _stats = MutableStateFlow(TrackerStats())
    val stats: StateFlow<TrackerStats> = _stats.asStateFlow()

    // Real list of known ad-networks, cross-site trackers, and telemetry scripts
    private val defaultBlockedDomains = setOf(
        "google-analytics.com",
        "googletagmanager.com",
        "doubleclick.net",
        "googlesyndication.com",
        "adservice.google.com",
        "connect.facebook.net",
        "pixel.facebook.com",
        "analytics.twitter.com",
        "ads-twitter.com",
        "adnxs.com",
        "criteo.com",
        "criteo.net",
        "scorecardresearch.com",
        "outbrain.com",
        "taboola.com",
        "hotjar.com",
        "branch.io",
        "adjust.com",
        "appsflyer.com",
        "amplitude.com",
        "mixpanel.com",
        "segment.io",
        "newrelic.com",
        "chartbeat.com",
        "quantserve.com",
        "advertising.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "casalemedia.com",
        "advertising.amazon.com",
        "a-ads.com"
    )

    private val customBlockedDomains = mutableSetOf<String>()
    private val allowedSites = mutableSetOf<String>()

    var isTrackerBlockingEnabled: Boolean = true
    var isContentBlockingEnabled: Boolean = true

    fun resetPageCounters() {
        _stats.update { it.copy(blockedOnCurrentPage = 0) }
    }

    fun isSiteAllowed(domain: String): Boolean {
        val clean = domain.lowercase().removePrefix("www.")
        return allowedSites.contains(clean)
    }

    fun allowSite(domain: String) {
        val clean = domain.lowercase().removePrefix("www.")
        if (clean.isNotBlank()) {
            allowedSites.add(clean)
        }
    }

    fun removeAllowedSite(domain: String) {
        val clean = domain.lowercase().removePrefix("www.")
        allowedSites.remove(clean)
    }

    fun addCustomBlockDomain(domain: String) {
        val clean = domain.lowercase().removePrefix("www.")
        if (clean.isNotBlank()) {
            customBlockedDomains.add(clean)
        }
    }

    fun removeCustomBlockDomain(domain: String) {
        val clean = domain.lowercase().removePrefix("www.")
        customBlockedDomains.remove(clean)
    }

    fun getAllowedSites(): List<String> = allowedSites.toList().sorted()
    fun getCustomBlockedDomains(): List<String> = customBlockedDomains.toList().sorted()

    /**
     * Intercepts request in WebViewClient. Returns an empty WebResourceResponse if blocked,
     * or null if the request is permitted.
     */
    fun interceptRequest(url: String, currentSiteHost: String?): WebResourceResponse? {
        if (!isTrackerBlockingEnabled && !isContentBlockingEnabled) return null

        val currentHost = currentSiteHost?.lowercase()?.removePrefix("www.") ?: ""
        if (currentHost.isNotBlank() && isSiteAllowed(currentHost)) {
            return null // User explicitly whitelisted this site
        }

        val uri = try { Uri.parse(url) } catch (_: Exception) { return null }
        val reqHost = uri.host?.lowercase()?.removePrefix("www.") ?: return null

        // Check if request matches blocked domains or custom block patterns
        val isBlocked = defaultBlockedDomains.any { blocked ->
            reqHost == blocked || reqHost.endsWith(".$blocked")
        } || customBlockedDomains.any { custom ->
            reqHost == custom || reqHost.endsWith(".$custom")
        }

        if (isBlocked) {
            // Count ONLY requests actually blocked
            _stats.update {
                it.copy(
                    blockedOnCurrentPage = it.blockedOnCurrentPage + 1,
                    totalBlockedLifetime = it.totalBlockedLifetime + 1
                )
            }
            // Return empty response to drop the request
            return WebResourceResponse(
                "text/plain",
                "UTF-8",
                ByteArrayInputStream(ByteArray(0))
            )
        }

        return null
    }
}
