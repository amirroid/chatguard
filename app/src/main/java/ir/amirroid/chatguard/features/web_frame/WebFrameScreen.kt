package ir.amirroid.chatguard.features.web_frame

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import ir.amirroid.chatguard.R
import ir.amirroid.chatguard.core.messanger.models.MessengerPlatform
import ir.amirroid.chatguard.core.web_content_extractor.abstraction.WebContentExtractor
import ir.amirroid.chatguard.core.web_content_extractor.models.InfoMessage
import ir.amirroid.chatguard.core.web_content_extractor.models.InjectedButton
import ir.amirroid.chatguard.ui.components.webview.WebView
import ir.amirroid.chatguard.ui.components.webview.rememberWebViewState
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
                .imePadding()
                .systemBarsPadding(),
            update = { view ->
                webContentExtractor.attachToWebView(view)
                webContentExtractor.setButtonClickListener(viewModel::handleButtonClick)
            },
            onNewPageLoaded = { url ->
                if (viewModel.lastLoadedUrl == url) {
                    return@WebView
                }
                viewModel.lastLoadedUrl = url

                viewModel.onPageLoaded()
                scope.launch {
                    webContentExtractor.clearAllFlags()
                    webContentExtractor.removeMessagesObserver()

                    webContentExtractor.observeBackgroundColor(viewModel::updateBackgroundColor)

                    if (!webContentExtractor.isChatUrl(url)) {
                        return@launch
                    }

                    if (!webContentExtractor.isChatPage()) {
                        return@launch
                    }
                    val userInfo = webContentExtractor.getUserInfo() ?: run {
                        return@launch
                    }
                    viewModel.handleUserInfoKey(userInfo)

                    webContentExtractor.executeInitialScript()
                    webContentExtractor.observeSendPublicKeyButton(viewModel::sendPoeticPublicKey)

                    webContentExtractor.observeMessages { newElements ->
                        val messages = webContentExtractor.mapElementsToMessages(url, newElements)
                        viewModel.onNewMessagesDetected(messages)
                    }

                    webContentExtractor.observeSendAction(viewModel::handleSendMessage)
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
            scope.launch {
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

                    is WebFrameEvent.SendMessage -> {
                        webContentExtractor.sendMessage(event.message)
                    }

                    is WebFrameEvent.UpdateMessageText -> {
                        webContentExtractor.updateMessageText(
                            event.messageId,
                            event.newText
                        )
                    }

                    is WebFrameEvent.RefreshWebView -> {
                        viewModel.lastLoadedUrl = null
                        webViewState.reload()
                    }

                    is WebFrameEvent.ShowToast -> Toast.makeText(
                        context,
                        event.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewState.webView?.destroy()
            webContentExtractor.cleanup()
        }
    }

    BackHandler {
        if (webViewState.canGoBack) {
            webViewState.goBack()
        } else {
            onBack.invoke()
        }
    }

    if (screenState.isSendPlainTextConfirmation) {
        ConfirmSendPlainTextDialog(
            onConfirm = {
                viewModel.closeConfirmSendPlainText(true)
            },
            onDismiss = {
                viewModel.closeConfirmSendPlainText(false)
            }
        )
    }
}

@Composable
fun ConfirmSendPlainTextDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.warning))
        },
        text = {
            Text(
                text = stringResource(R.string.send_plain_text_warning)
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}