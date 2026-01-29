package ir.sysfail.chatguard.features.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import ir.sysfail.chatguard.utils.Constants
import org.koin.compose.viewmodel.koinViewModel

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
    onGoToWebFrame: (MessengerPlatform) -> Unit,
    onGoToIntro: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
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
    val context = LocalContext.current

    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { requiredUri ->
            viewModel.saveKeys(requiredUri, onSuccess = {
                Toast.makeText(context, R.string.save_successfully, Toast.LENGTH_SHORT).show()
            })
        }
    }


    var isAccessibilityItemExpanded by rememberSaveable { mutableStateOf(true) }
    var isSettingsItemExpanded by rememberSaveable { mutableStateOf(false) }
    var isExitKeysWarningDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        )
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
        item("settings") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                TransparentListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold)
                    },
                    trailingContent = {
                        ExpandIconButton(
                            isSettingsItemExpanded,
                            onClick = { isSettingsItemExpanded = !isSettingsItemExpanded }
                        )
                    }
                )
                AnimatedVisibility(isSettingsItemExpanded) {
                    Column {
                        TransparentListItem(
                            headlineContent = {
                                Text(stringResource(R.string.export_keys))
                            },
                            modifier = Modifier.clickable {
                                createFileLauncher.launch("identity_keys_backup.${Constants.KEYS_EXTENSION}")
                            }
                        )
                        TransparentListItem(
                            headlineContent = {
                                Text(stringResource(R.string.exit_current_keys))
                            },
                            modifier = Modifier.clickable {
                                isExitKeysWarningDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (isExitKeysWarningDialog) {
        ExitKeysWarningDialog(
            onConfirmExit = {
                viewModel.clearCurrentKeys {
                    onGoToIntro.invoke()
                    isExitKeysWarningDialog = false
                }
            },
            onDismiss = {
                isExitKeysWarningDialog = false
            }
        )

    }
}

@Composable
fun ExitKeysWarningDialog(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.warning))
        },
        text = {
            Text(text = stringResource(R.string.exit_keys_warning))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmExit
            ) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
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