package ir.amirroid.chatguard.ui.components.messages

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ir.amirroid.chatguard.R

@Composable
fun EncryptionKeyMenuButton(
    onSendEncryptionKey: () -> Unit,
    modifier: Modifier = Modifier,
    previewMenuExpanded: Boolean = false,
) {
    var isMenuOpened by remember(previewMenuExpanded) { mutableStateOf(previewMenuExpanded) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { isMenuOpened = true },
            enabled = !previewMenuExpanded,
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null,
            )
        }

        DropdownMenu(
            expanded = isMenuOpened,
            onDismissRequest = { if (!previewMenuExpanded) isMenuOpened = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.send_encryption_key)) },
                onClick = {
                    if (!previewMenuExpanded) {
                        onSendEncryptionKey()
                        isMenuOpened = false
                    }
                },
            )
        }
    }
}
