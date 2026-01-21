package ir.sysfail.chatguard.features.messages

import android.content.ClipData
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.sysfail.chatguard.R
import ir.sysfail.chatguard.ui.components.ExpandIconButton
import ir.sysfail.chatguard.ui.components.TransparentListItem
import ir.sysfail.chatguard.ui_models.message.ChatMessageUiModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesModalBottomSheet(
    state: MessagesScreenState,
    onMessageValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSendPublicKey: () -> Unit,
    onSavePublicKey: (ChatMessageUiModel) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var isSendingMessage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.hasPublicKey) {
                MissingPublicKeyWarning()
            }

            AnimatedContent(
                targetState = isSendingMessage,
                label = "message_mode"
            ) { isComposing ->
                if (isComposing) {
                    MessageComposer(
                        messageText = state.currentNewMessageText,
                        onValueChange = onMessageValueChange,
                        onSend = {
                            scope.launch {
                                sheetState.hide()
                                onSendMessage.invoke()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    MessagesList(
                        state = state,
                        sheetState = sheetState,
                        onStartComposing = { isSendingMessage = true },
                        onSendPublicKey = onSendPublicKey,
                        modifier = Modifier.weight(1f),
                        onSavePublicKey = onSavePublicKey
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageComposer(
    messageText: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = messageText,
        onValueChange = onValueChange,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        label = { Text(stringResource(R.string.message_hint)) },
        leadingIcon = {
            FilledIconButton(onClick = {
                focusManager.clearFocus()
                onSend.invoke()
            }, enabled = messageText.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Send,
                    contentDescription = stringResource(R.string.send_message),
                    modifier = Modifier.rotate(-45f)
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessagesList(
    state: MessagesScreenState,
    sheetState: SheetState,
    onStartComposing: () -> Unit,
    onSendPublicKey: () -> Unit,
    onSavePublicKey: (ChatMessageUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item(key = "header") {
            MessagesHeader(
                hasPublicKey = state.hasPublicKey,
                sheetState = sheetState,
                onStartComposing = onStartComposing,
                onSendPublicKey = onSendPublicKey
            )
        }

        items(
            items = state.messages,
            key = { "message_${it.index}" }
        ) { message ->
            ChatMessageItem(
                message,
                onCopyMessage = {
                    scope.launch {
                        val entry = ClipEntry(ClipData.newPlainText(null, message.message))
                        clipboard.setClipEntry(entry)
                    }
                },
                onSavePublicKey = { onSavePublicKey.invoke(message) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessagesHeader(
    hasPublicKey: Boolean,
    sheetState: SheetState,
    onStartComposing: () -> Unit,
    onSendPublicKey: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        OutlinedButton(
            onClick = onStartComposing,
            enabled = hasPublicKey
        ) {
            Text(stringResource(R.string.send_message))
        }

        MenuOption(
            sheetState = sheetState,
            onSendEncryptionKey = onSendPublicKey
        )
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessageUiModel,
    onCopyMessage: (String) -> Unit,
    onSavePublicKey: () -> Unit
) {
    val colors = when {
        message.isPublicKey -> {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        else -> CardDefaults.cardColors()
    }
    var isExpandedContent by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), colors = colors) {
        if (message.isPublicKey) {
            TransparentListItem(
                headlineContent = {
                    Text(stringResource(R.string.encryption_key_description))
                },
                supportingContent = {
                    Text(message.message, maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
                trailingContent = if (!message.isMessageFromMe) {
                    {
                        Button(onClick = onSavePublicKey) {
                            Text(stringResource(R.string.usage))
                        }
                    }
                } else null
            )
        } else {
            TransparentListItem(
                headlineContent = {
                    AnimatedContent(isExpandedContent) { expanded ->
                        if (expanded) {
                            val text =
                                if (message.isMessageFromMe) R.string.sent_message_at else R.string.received_message_at

                            Text(
                                text = stringResource(text, message.date),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(message.message, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { onCopyMessage.invoke(message.message) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null
                            )
                        }
                        ExpandIconButton(
                            isExpandedContent,
                            onClick = { isExpandedContent = !isExpandedContent }
                        )
                    }
                }
            )
        }
        AnimatedVisibility(isExpandedContent) {
            Text(message.message, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun MissingPublicKeyWarning(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.encryption_key_was_not_found)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(vertical = 4.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuOption(
    sheetState: SheetState,
    onSendEncryptionKey: suspend () -> Unit
) {
    var isMenuOpened by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box {
        IconButton(onClick = { isMenuOpened = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = isMenuOpened,
            onDismissRequest = { isMenuOpened = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.send_encryption_key))
                },
                onClick = {
                    scope.launch {
                        onSendEncryptionKey()
                        sheetState.hide()
                        isMenuOpened = false
                    }
                }
            )
        }
    }
}