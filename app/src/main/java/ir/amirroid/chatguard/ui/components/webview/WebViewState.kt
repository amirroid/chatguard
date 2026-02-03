package ir.amirroid.chatguard.ui.components.webview

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Immutable
data class WebViewError(
    val message: String?
)

@Stable
class WebViewState(initialUrl: String) {
    var url by mutableStateOf(initialUrl)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var progress by mutableFloatStateOf(0f)
    var title by mutableStateOf("")
    var loadingError: WebViewError? by mutableStateOf(null)

    internal var webView: WebView? = null

    fun goBack() {
        webView?.goBack()
    }

    fun goForward() {
        webView?.goForward()
    }

    fun reload() {
        webView?.reload()
    }

    fun loadUrl(newUrl: String) {
        url = newUrl
        webView?.loadUrl(newUrl)
    }
}


@Composable
fun rememberWebViewState(initialUrl: String): WebViewState {
    return remember { WebViewState(initialUrl) }
}