package ir.sysfail.chatguard.features.web_frame

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ir.sysfail.chatguard.core.messanger.models.MessengerPlatform
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.WebContentExtractor
import ir.sysfail.chatguard.ui.components.webview.WebView
import ir.sysfail.chatguard.ui.components.webview.rememberWebViewState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf

@Composable
fun WebFrameScreen(
    platform: MessengerPlatform,
    onBack: () -> Unit,
    viewModel: WebFrameViewModel = koinViewModel()
) {
    val state = rememberWebViewState(initialUrl = platform.url)

    val koin = getKoin()
    val webContentExtractor = remember {
        koin.get<WebContentExtractor> {
            parametersOf(platform)
        }
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(state.title) {
        if (state.title.isBlank()) return@LaunchedEffect
        val isChatScreen = webContentExtractor.isChatPage()
        if (!isChatScreen) return@LaunchedEffect

        webContentExtractor.observeMessages {
            Log.d(
                "sadasdasdsad", "WebFrameScreen:${
                    it.joinToString("\n") { it.text }
                } \n ${
                    webContentExtractor.mapElementsToMessages(
                        it
                    ).joinToString("\n")
                }"
            )
        }

        webContentExtractor.observeSendAction { message ->
            scope.launch {
                Log.d("sadasdasdsad", "WebFrameScreen send: $message")
                webContentExtractor.sendMessage("Transformed: $message")
            }
        }
    }

    NavigationBackHandler(
        rememberNavigationEventState(NavigationEventInfo.None)
    ) {
        if (state.canGoBack) {
            state.goBack()
        } else onBack.invoke()
    }

    WebView(
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding(),
        update = { view ->
            webContentExtractor.attachToWebView(view)

            webContentExtractor.setErrorListener { _, error ->
                Log.e("sadasdasdsad", "WebFrameScreen Error: $error")
            }
        }
    )


    DisposableEffect(Unit) {
        onDispose {
            state.webView?.destroy()
            webContentExtractor.cleanup()
        }
    }
}