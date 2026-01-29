package ir.sysfail.chatguard.features.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ir.sysfail.chatguard.R
import ir.sysfail.chatguard.core.messanger.models.MessengerPlatform
import ir.sysfail.chatguard.core.permission.implementation.AccessibilityPermissionItem
import ir.sysfail.chatguard.core.permission.implementation.NotificationsPermissionItem
import ir.sysfail.chatguard.core.permission.implementation.OverlayPermissionItem
import ir.sysfail.chatguard.core.permission.implementation.PermissionState
import ir.sysfail.chatguard.core.permission.implementation.rememberPermissionState
import ir.sysfail.chatguard.ui.components.ExpandIconButton
import ir.sysfail.chatguard.ui.components.TransparentListItem
import ir.sysfail.chatguard.ui.theme.ChatGuardTheme

@Immutable
data class MessengerItem(
    @field:StringRes val name: Int,
    @field:StringRes val description: Int,
    val platform: MessengerPlatform
)

val messengerItems = listOf(
    MessengerItem(
        name = R.string.eitaa,
        description = R.string.click_to_enter,
        platform = MessengerPlatform.EITAA
    ),
    MessengerItem(
        name = R.string.soroush,
        description = R.string.click_to_enter,
        platform = MessengerPlatform.SOROUSH
    ),
    MessengerItem(
        name = R.string.bale,
        description = R.string.click_to_enter,
        platform = MessengerPlatform.BALE

    ),
)

val accessibilityPermissionItem = AccessibilityPermissionItem()
val overlayPermissionItem = OverlayPermissionItem()
val notificationsPermissionItem = NotificationsPermissionItem()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoToWebFrame: (MessengerPlatform) -> Unit
) {
    val permissionsState = rememberPermissionState(
        permissions = remember {
            listOf(
                accessibilityPermissionItem,
                overlayPermissionItem,
                notificationsPermissionItem
            )
        }
    )
    var isAccessibilityItemExpanded by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = WindowInsets.navigationBars.asPaddingValues()
    ) {
        item("header") {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.app_name_persian))
                }
            )
        }
        items(messengerItems, key = { it.name }) { messengerItem ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = {
                    onGoToWebFrame.invoke(messengerItem.platform)
                }
            ) {
                TransparentListItem(
                    headlineContent = {
                        Text(stringResource(messengerItem.name), fontWeight = FontWeight.Bold)
                    },
                    supportingContent = {
                        Text(
                            stringResource(messengerItem.description),
                            modifier = Modifier.alpha(.7f)
                        )
                    }
                )
            }
        }
        item("accessibility_service") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                TransparentListItem(
                    headlineContent = {
                        Text(stringResource(R.string.use_application), fontWeight = FontWeight.Bold)
                    },
                    supportingContent = {
                        Text(
                            stringResource(R.string.use_application_description),
                            modifier = Modifier.alpha(.7f)
                        )
                    },
                    trailingContent = {
                        ExpandIconButton(
                            isAccessibilityItemExpanded,
                            onClick = { isAccessibilityItemExpanded = !isAccessibilityItemExpanded }
                        )
                    }
                )
                AnimatedVisibility(isAccessibilityItemExpanded) {
                    AccessibilityOptionMenu(permissionsState = permissionsState)
                }
            }
        }
    }
}

@Composable
fun AccessibilityOptionMenu(permissionsState: PermissionState) {
    Column(
        modifier = Modifier
    ) {
        val isNotificationsGranted =
            permissionsState.permissionsGranted[notificationsPermissionItem.name] == true
        val isOverlayGranted =
            permissionsState.permissionsGranted[overlayPermissionItem.name] == true
        val isAccessibilityGranted =
            permissionsState.permissionsGranted[accessibilityPermissionItem.name] == true

        DisplayPermissionItem(
            name = stringResource(R.string.notification_permission),
            description = stringResource(R.string.notification_permission_description),
            isPermissionGranted = isNotificationsGranted,
            onRequestPermission = { permissionsState.requestAccess(notificationsPermissionItem.name) }
        )
        DisplayPermissionItem(
            name = stringResource(R.string.overlay_permission),
            description = stringResource(R.string.overlay_permission_description),
            isPermissionGranted = isOverlayGranted,
            onRequestPermission = { permissionsState.requestAccess(overlayPermissionItem.name) }
        )
        DisplayPermissionItem(
            name = stringResource(R.string.accessibility_permission),
            description = stringResource(R.string.accessibility_permission_description),
            isPermissionGranted = isAccessibilityGranted,
            requestPermissionEnabled = isOverlayGranted && isNotificationsGranted,
            onRequestPermission = { permissionsState.requestAccess(accessibilityPermissionItem.name) }
        )
    }
}

@Composable
fun DisplayPermissionItem(
    name: String,
    description: String,
    isPermissionGranted: Boolean,
    requestPermissionEnabled: Boolean = true,
    onRequestPermission: () -> Unit
) {
    TransparentListItem(
        headlineContent = {
            Text(name)
        },
        overlineContent = {
            AnimatedContent(isPermissionGranted) { granted ->
                if (granted) {
                    Text(
                        stringResource(R.string.permission_accepted),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        stringResource(R.string.required_permission),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        supportingContent = {
            Text(
                text = description,
                modifier = Modifier.alpha(.7f)
            )
        },
        trailingContent = {
            Button(
                onClick = onRequestPermission,
                enabled = !isPermissionGranted && requestPermissionEnabled
            ) {
                Text(stringResource(R.string.access_permission))
            }
        }
    )
}


@Preview
@Composable
fun HomeScreenPreview() {
    ChatGuardTheme {
        Scaffold {
            HomeScreen(
                onGoToWebFrame = {}
            )
        }
    }
}