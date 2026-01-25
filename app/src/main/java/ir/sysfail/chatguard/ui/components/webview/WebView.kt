package ir.sysfail.chatguard.ui.components.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebView(
    state: WebViewState,
    update: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                state.webView = this

                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                setBackgroundColor(Color.TRANSPARENT)

                WebView.setWebContentsDebuggingEnabled(true)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    javaScriptCanOpenWindowsAutomatically = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                    allowContentAccess = true
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        state.isLoading = true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        state.isLoading = false
                        state.canGoBack = canGoBack()
                        state.canGoForward = canGoForward()
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        state.progress =
                            runCatching { newProgress.div(100f).coerceIn(0f, 1f) }.getOrDefault(0f)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        state.title = title ?: ""
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        if (consoleMessage != null) {
                            Log.d(
                                "JSConsole", "[${consoleMessage.messageLevel()}] " +
                                        "${consoleMessage.message()} -- From line " +
                                        "${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}"
                            )
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }

                post {
                    loadUrl(state.url)
                }
            }
        },
        modifier = modifier,
        update = { view ->
            update.invoke(view)
            if (view.url != state.url) {
                view.loadUrl(state.url)
            }
        }
    )
}