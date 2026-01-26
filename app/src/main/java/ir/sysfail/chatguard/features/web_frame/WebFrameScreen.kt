package ir.sysfail.chatguard.features.web_frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ir.sysfail.chatguard.core.messanger.models.MessengerPlatform
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.WebContentExtractor
import ir.sysfail.chatguard.core.web_content_extractor.models.InfoMessage
import ir.sysfail.chatguard.core.web_content_extractor.models.InjectedButton
import ir.sysfail.chatguard.ui.components.webview.WebView
import ir.sysfail.chatguard.ui.components.webview.rememberWebViewState
import kotlinx.coroutines.cancel
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
    val webViewState = rememberWebViewState(initialUrl = platform.url)
    val screenState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val koin = getKoin()
    val webContentExtractor = remember {
        koin.get<WebContentExtractor> {
            parametersOf(platform)
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WebView(
            state = webViewState,
            modifier = Modifier
                .fillMaxSize()
                .background(screenState.backgroundColor)
                .systemBarsPadding(),
            update = { view ->
                webContentExtractor.attachToWebView(view)
            },
            onNewPageLoaded = {
                webContentExtractor.observeBackgroundColor(viewModel::updateBackgroundColor)

                scope.launch {
                    viewModel.onPageLoaded()

                    if (!webContentExtractor.isChatPage()) {
                        cancel()
                        return@launch
                    }
                    val userInfo = webContentExtractor.getUserInfo() ?: run {
                        cancel()
                        return@launch
                    }
                    viewModel.handleUserInfoKey(userInfo)

                    webContentExtractor.observeMessages { newElements ->
                        val messages = webContentExtractor.mapElementsToMessages(newElements)
                        viewModel.onNewMessagesDetected(messages)
                    }

                    webContentExtractor.observeSendAction { message ->
                    }
                }
            }
        )

        if (webViewState.isLoading) {
            CircularProgressIndicator()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WebFrameEvent.ShowInfoMessage -> {
                    webContentExtractor.injectInfoMessage(
                        InfoMessage(
                            text = event.message,
                            type = event.type
                        )
                    )
                }

                is WebFrameEvent.ShowInfoMessageResource -> {
                    webContentExtractor.injectInfoMessage(
                        InfoMessage(
                            text = context.getString(event.message),
                            type = event.type
                        )
                    )
                }

                is WebFrameEvent.ClearInfoMessage -> webContentExtractor.removeInjectedInfoMessage()

                is WebFrameEvent.InjectButton -> {
                    webContentExtractor.injectButton(
                        event.messageId, button = InjectedButton(
                            id = event.buttonId,
                            text = context.getString(event.text),
                            buttonType = event.buttonType
                        )
                    )
                }

                else -> Unit
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewState.webView?.destroy()
            webContentExtractor.cleanup()
        }
    }


    NavigationBackHandler(
        rememberNavigationEventState(NavigationEventInfo.None)
    ) {
        if (webViewState.canGoBack) {
            webViewState.goBack()
        } else onBack.invoke()
    }
}