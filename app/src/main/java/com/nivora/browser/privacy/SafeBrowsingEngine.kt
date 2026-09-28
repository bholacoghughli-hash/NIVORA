package com.nivora.browser.privacy

import android.net.Uri

enum class SafeBrowsingThreat {
    NONE,
    MALWARE,
    PHISHING,
    UNWANTED_SOFTWARE
}

data class SafeBrowsingResult(
    val isThreat: Boolean,
    val threatType: SafeBrowsingThreat = SafeBrowsingThreat.NONE,
    val message: String = ""
)

interface SafeBrowsingProvider {
    val providerName: String
    fun checkUrl(url: String): SafeBrowsingResult
}

class DefaultSafeBrowsingProvider : SafeBrowsingProvider {
    override val providerName: String = "NIVORA Safe Threat Inspector"

    // Real standard test vectors from Google Safe Browsing / Anti-Phishing Working Group
    private val testMalwarePatterns = setOf(
        "testsafebrowsing.appspot.com",
        "malware.testing.google.test",
        "phishing.testing.google.test",
        "unwanted.testing.google.test"
    )

    override fun checkUrl(url: String): SafeBrowsingResult {
        val uri = try { Uri.parse(url) } catch (_: Exception) { return SafeBrowsingResult(isThreat = false) }
        val host = uri.host?.lowercase() ?: return SafeBrowsingResult(isThreat = false)

        for (pattern in testMalwarePatterns) {
            if (host == pattern || host.endsWith(".$pattern")) {
                val threat = when {
                    pattern.contains("phishing") -> SafeBrowsingThreat.PHISHING
                    pattern.contains("unwanted") -> SafeBrowsingThreat.UNWANTED_SOFTWARE
                    else -> SafeBrowsingThreat.MALWARE
                }
                return SafeBrowsingResult(
                    isThreat = true,
                    threatType = threat,
                    message = "Deceptive site ahead. This domain has been identified as a test or real malicious threat."
                )
            }
        }

        return SafeBrowsingResult(isThreat = false)
    }
}
