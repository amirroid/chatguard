package ir.sysfail.chatguard.features.web_frame

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ir.sysfail.chatguard.R
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

    Box(Modifier.fillMaxSize()) {
        WebView(
            state = webViewState,
            modifier = Modifier
                .fillMaxSize()
                .background(screenState.backgroundColor)
                .systemBarsPadding(),
            update = { view ->
                webContentExtractor.attachToWebView(view)
                webContentExtractor.setButtonClickListener(viewModel::handleButtonClick)
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
                    webContentExtractor.observeSendPublicKeyButton(viewModel::sendPoeticPublicKey)

                    webContentExtractor.observeMessages { newElements ->
                        val messages = webContentExtractor.mapElementsToMessages(newElements)
                        viewModel.onNewMessagesDetected(messages)
                    }

                    webContentExtractor.observeSendAction(viewModel::handleSendMessage)
                    webContentExtractor.executeInitialScript()
                }
            }
        )

        webViewState.loadingError?.let { error ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.an_error_occurred_in_loading),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    error.message?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alpha(.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(onClick = webViewState::reload) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }

        if (webViewState.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                val progress by animateFloatAsState(
                    webViewState.progress
                )
                CircularProgressIndicator(progress = { progress })
            }
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

                is WebFrameEvent.SendMessage -> webContentExtractor.sendMessage(event.message)
                is WebFrameEvent.UpdateMessageText -> webContentExtractor.updateMessageText(
                    event.messageId,
                    event.newText
                )

                is WebFrameEvent.RefreshWebView -> webViewState.reload()
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