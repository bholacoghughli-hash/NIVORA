package com.nivora.browser.browser.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.nivora.browser.browser.engine.BrowserEngine
import com.nivora.browser.browser.engine.EngineState
import com.nivora.browser.privacy.SafeBrowsingProvider
import com.nivora.browser.privacy.TrackerBlockerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AndroidWebViewEngine(
    val context: Context,
    val isIncognito: Boolean = false,
    val onPageLoadedCallback: ((url: String, title: String) -> Unit)? = null
) : BrowserEngine {

    val webView: WebView = WebView(context)

    private val _state = MutableStateFlow(EngineState())
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _fullscreenView = MutableStateFlow<View?>(null)
    override val fullscreenView: StateFlow<View?> = _fullscreenView.asStateFlow()
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override var fileChooserCallback: ((ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean)? = null
    override var permissionRequestListener: ((PermissionRequest) -> Unit)? = null
    override var geolocationListener: ((String, GeolocationPermissions.Callback) -> Unit)? = null
    override var downloadListener: ((url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long) -> Unit)? = null

    var trackerBlockerEngine: TrackerBlockerEngine? = null
    var safeBrowsingProvider: SafeBrowsingProvider? = null

    private val defaultUserAgent: String = webView.settings.userAgentString
    private val desktopUserAgent: String =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    init {
        configureWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = !isIncognito
                databaseEnabled = !isIncognito
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }
            }

            val cookieManager = CookieManager.getInstance()
            if (isIncognito) {
                cookieManager.setAcceptCookie(false)
            } else {
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, false)
            }

            // Real find in page listener
            setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                _state.update {
                    it.copy(
                        activeMatchOrdinal = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0,
                        totalMatches = numberOfMatches
                    )
                }
            }

            // Real download listener
            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                downloadListener?.invoke(url, userAgent, contentDisposition, mimetype, contentLength)
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    _state.update {
                        it.copy(
                            progress = newProgress,
                            isLoading = newProgress < 100
                        )
                    }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (!title.isNullOrBlank()) {
                        _state.update { it.copy(title = title) }
                    }
                }

                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    if (icon != null) {
                        _state.update { it.copy(favicon = icon) }
                    }
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    return fileChooserCallback?.invoke(filePathCallback, fileChooserParams) ?: false
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    if (request != null) {
                        if (permissionRequestListener != null) {
                            permissionRequestListener?.invoke(request)
                        } else {
                            request.deny()
                        }
                    }
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?
                ) {
                    if (origin != null && callback != null) {
                        if (geolocationListener != null) {
                            geolocationListener?.invoke(origin, callback)
                        } else {
                            callback.invoke(origin, false, false)
                        }
                    }
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    _fullscreenView.value = view
                    customViewCallback = callback
                }

                override fun onHideCustomView() {
                    _fullscreenView.value = null
                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val reqUrl = request?.url?.toString() ?: return null
                    val currentSiteHost = try { Uri.parse(view?.url ?: "").host } catch (_: Exception) { null }
                    val blocked = trackerBlockerEngine?.interceptRequest(reqUrl, currentSiteHost)
                    if (blocked != null) {
                        return blocked
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    val currentUrl = url ?: ""
                    trackerBlockerEngine?.resetPageCounters()

                    // Check Safe Browsing
                    val threatCheck = if (currentUrl.isNotBlank() && !currentUrl.startsWith("about:")) {
                        safeBrowsingProvider?.checkUrl(currentUrl)
                    } else null

                    _state.update {
                        it.copy(
                            url = currentUrl,
                            isLoading = true,
                            isSecure = currentUrl.startsWith("https://", ignoreCase = true),
                            canGoBack = canGoBack(),
                            canGoForward = canGoForward(),
                            sslError = null,
                            errorCode = if (threatCheck != null && threatCheck.isThreat) 403 else null,
                            errorDescription = if (threatCheck != null && threatCheck.isThreat) "Security Alert: ${threatCheck.message} [Threat: ${threatCheck.threatType.name}]" else null,
                            failingUrl = if (threatCheck != null && threatCheck.isThreat) currentUrl else null
                        )
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    val currentUrl = url ?: ""
                    val pageTitle = view?.title ?: currentUrl
                    _state.update {
                        it.copy(
                            url = currentUrl,
                            title = if (pageTitle.isNotBlank()) pageTitle else it.title,
                            isLoading = false,
                            progress = 100,
                            canGoBack = canGoBack(),
                            canGoForward = canGoForward(),
                            isSecure = currentUrl.startsWith("https://", ignoreCase = true)
                        )
                    }
                    if (currentUrl.isNotBlank() && !currentUrl.startsWith("about:") && !isIncognito) {
                        onPageLoadedCallback?.invoke(currentUrl, pageTitle)
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val reqUrl = request?.url?.toString() ?: return false
                    if (reqUrl.startsWith("http://") || reqUrl.startsWith("https://") || reqUrl.startsWith("about:")) {
                        return false
                    }
                    return true
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        val desc = error?.description?.toString() ?: "Network Connection Error"
                        val code = error?.errorCode ?: -1
                        val failUrl = request.url?.toString() ?: ""
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorCode = code,
                                errorDescription = desc,
                                failingUrl = failUrl
                            )
                        }
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    if (request?.isForMainFrame == true) {
                        val statusCode = errorResponse?.statusCode ?: 0
                        if (statusCode >= 400) {
                            val reason = errorResponse?.reasonPhrase ?: "HTTP Error $statusCode"
                            _state.update {
                                it.copy(
                                    errorCode = statusCode,
                                    errorDescription = "$statusCode: $reason"
                                )
                            }
                        }
                    }
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    val errorDesc = when (error?.primaryError) {
                        SslError.SSL_EXPIRED -> "The security certificate has expired"
                        SslError.SSL_IDMISMATCH -> "Hostname mismatch in SSL certificate"
                        SslError.SSL_UNTRUSTED -> "Untrusted certificate authority"
                        SslError.SSL_NOTYETVALID -> "Certificate is not yet valid"
                        SslError.SSL_DATE_INVALID -> "Date on SSL certificate is invalid"
                        else -> "SSL certificate validation failure"
                    }
                    val certInfo = error?.certificate?.let {
                        "Issued to: ${it.issuedTo.cName}, Issued by: ${it.issuedBy.cName}"
                    }
                    _state.update {
                        it.copy(
                            isSecure = false,
                            sslError = errorDesc,
                            sslCertificateInfo = certInfo
                        )
                    }
                    handler?.cancel()
                }
            }
        }
    }

    override fun loadUrl(url: String) {
        val headers = mapOf("DNT" to "1")
        _state.update {
            it.copy(
                errorCode = null,
                errorDescription = null,
                failingUrl = null
            )
        }
        webView.loadUrl(url, headers)
    }

    override fun reload() {
        _state.update {
            it.copy(
                errorCode = null,
                errorDescription = null,
                failingUrl = null
            )
        }
        webView.reload()
    }

    override fun stopLoading() {
        webView.stopLoading()
        _state.update { it.copy(isLoading = false) }
    }

    override fun goBack(): Boolean {
        return if (webView.canGoBack()) {
            _state.update {
                it.copy(
                    errorCode = null,
                    errorDescription = null,
                    failingUrl = null
                )
            }
            webView.goBack()
            true
        } else {
            false
        }
    }

    override fun goForward(): Boolean {
        return if (webView.canGoForward()) {
            _state.update {
                it.copy(
                    errorCode = null,
                    errorDescription = null,
                    failingUrl = null
                )
            }
            webView.goForward()
            true
        } else {
            false
        }
    }

    override fun canGoBack(): Boolean = webView.canGoBack()

    override fun canGoForward(): Boolean = webView.canGoForward()

    override fun setDesktopMode(enabled: Boolean) {
        webView.settings.userAgentString = if (enabled) desktopUserAgent else defaultUserAgent
        webView.settings.useWideViewPort = enabled
        _state.update { it.copy(isDesktopMode = enabled) }
        webView.reload()
    }

    override fun findInPage(query: String) {
        if (query.isNotBlank()) {
            webView.findAllAsync(query)
        } else {
            clearMatches()
        }
    }

    override fun clearMatches() {
        webView.clearMatches()
        _state.update { it.copy(activeMatchOrdinal = 0, totalMatches = 0) }
    }

    override fun findNext(forward: Boolean) {
        webView.findNext(forward)
    }

    override fun evaluateJavascript(script: String, callback: ((String) -> Unit)?) {
        webView.evaluateJavascript(script) { result ->
            callback?.invoke(result ?: "")
        }
    }

    override fun exitFullscreen() {
        customViewCallback?.onCustomViewHidden()
        _fullscreenView.value = null
        customViewCallback = null
    }

    override fun captureScreenshot(): Bitmap? {
        return try {
            val width = webView.width
            val height = webView.height
            if (width <= 0 || height <= 0) return null
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    override fun printPage(jobName: String) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            printManager?.print(jobName, printAdapter, PrintAttributes.Builder().build())
        } catch (_: Exception) {
            // Handled gracefully
        }
    }

    override fun clearPrivateSessionData() {
        if (isIncognito) {
            webView.clearCache(true)
            webView.clearHistory()
            webView.clearFormData()
        }
    }

    override fun destroy() {
        clearPrivateSessionData()
        webView.stopLoading()
        webView.destroy()
    }
}
