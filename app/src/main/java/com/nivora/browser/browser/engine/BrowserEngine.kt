package com.nivora.browser.browser.engine

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import kotlinx.coroutines.flow.StateFlow

data class EngineState(
    val url: String = "",
    val title: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSecure: Boolean = false,
    val favicon: Bitmap? = null,
    val sslError: String? = null,
    val sslCertificateInfo: String? = null,
    val errorCode: Int? = null,
    val errorDescription: String? = null,
    val failingUrl: String? = null,
    val activeMatchOrdinal: Int = 0,
    val totalMatches: Int = 0,
    val isDesktopMode: Boolean = false
)

interface BrowserEngine {
    val state: StateFlow<EngineState>
    val fullscreenView: StateFlow<View?>

    var fileChooserCallback: ((ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean)?
    var permissionRequestListener: ((PermissionRequest) -> Unit)?
    var geolocationListener: ((String, GeolocationPermissions.Callback) -> Unit)?
    var downloadListener: ((url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long) -> Unit)?

    fun loadUrl(url: String)
    fun reload()
    fun stopLoading()
    fun goBack(): Boolean
    fun goForward(): Boolean
    fun canGoBack(): Boolean
    fun canGoForward(): Boolean
    fun setDesktopMode(enabled: Boolean)
    fun findInPage(query: String)
    fun clearMatches()
    fun findNext(forward: Boolean)
    fun evaluateJavascript(script: String, callback: ((String) -> Unit)? = null)
    fun exitFullscreen()
    fun captureScreenshot(): Bitmap?
    fun printPage(jobName: String = "NIVORA Page")
    fun clearPrivateSessionData()
    fun destroy()
}
