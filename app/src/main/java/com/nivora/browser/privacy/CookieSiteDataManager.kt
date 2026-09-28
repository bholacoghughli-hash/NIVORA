package com.nivora.browser.privacy

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import com.nivora.browser.data.repository.BrowserRepository
import com.nivora.browser.ui.browser.HistoryTimeRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CookieSiteDataManager(
    private val context: Context,
    private val browserRepository: BrowserRepository
) {
    fun clearAllCookies(onComplete: (() -> Unit)? = null) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies {
            cookieManager.flush()
            onComplete?.invoke()
        }
    }

    fun clearWebStorage() {
        WebStorage.getInstance().deleteAllData()
    }

    fun clearCache(webView: WebView?) {
        webView?.clearCache(true)
    }

    fun setAcceptThirdPartyCookies(webView: WebView, accept: Boolean) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptThirdPartyCookies(webView, accept)
    }

    fun clearBrowsingData(
        scope: CoroutineScope,
        timeRange: HistoryTimeRange,
        clearHistory: Boolean,
        clearCookies: Boolean,
        clearCache: Boolean,
        clearSiteData: Boolean,
        clearDownloads: Boolean,
        onComplete: () -> Unit
    ) {
        scope.launch {
            if (clearHistory) {
                if (timeRange == HistoryTimeRange.ALL_TIME) {
                    browserRepository.clearHistory()
                } else {
                    val cutoff = System.currentTimeMillis() - timeRange.durationMillis
                    browserRepository.deleteHistorySince(cutoff)
                }
            }

            if (clearCookies) {
                withContext(Dispatchers.Main) {
                    clearAllCookies()
                }
            }

            if (clearSiteData) {
                withContext(Dispatchers.Main) {
                    clearWebStorage()
                }
            }

            if (clearDownloads) {
                browserRepository.clearDownloads()
            }

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
