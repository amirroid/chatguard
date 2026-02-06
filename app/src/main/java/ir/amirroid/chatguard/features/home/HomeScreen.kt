package ir.amirroid.chatguard.features.home

import android.os.Build
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amirroid.chatguard.BuildConfig
import ir.amirroid.chatguard.R
import ir.amirroid.chatguard.core.messanger.models.MessengerPlatform
import ir.amirroid.chatguard.core.permission.implementation.AccessibilityPermissionItem
import ir.amirroid.chatguard.core.permission.implementation.NotificationsPermissionItem
import ir.amirroid.chatguard.core.permission.implementation.OverlayPermissionItem
import ir.amirroid.chatguard.core.permission.implementation.PermissionState
import ir.amirroid.chatguard.core.permission.implementation.rememberPermissionState
import ir.amirroid.chatguard.ui.components.ExpandIconButton
import ir.amirroid.chatguard.ui.components.TransparentListItem
import ir.amirroid.chatguard.utils.Constants
import org.koin.compose.viewmodel.koinViewModel

@Immutable
data class MessengerItem(
    @field:StringRes val name: Int,
    @field:StringRes val description: Int,
    val platform: MessengerPlatform
)


private val messengerItems = listOf(
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

private val accessibilityPermissionItem = AccessibilityPermissionItem()
private val overlayPermissionItem = OverlayPermissionItem()
private val notificationsPermissionItem = NotificationsPermissionItem()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoToWebFrame: (MessengerPlatform) -> Unit,
    onGoToIntro: () -> Unit,
    onGoToGuides: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var isAccessibilityItemExpanded by rememberSaveable { mutableStateOf(true) }
    var isSettingsItemExpanded by rememberSaveable { mutableStateOf(false) }
    var isExitKeysWarningDialog by rememberSaveable { mutableStateOf(false) }

    val permissionsState = rememberPermissionState(
        permissions = remember {
            buildList {
                add(accessibilityPermissionItem)
                add(overlayPermissionItem)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(notificationsPermissionItem)
                }
            }
        }
    )

    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { requiredUri ->
            viewModel.saveKeys(
                uri = requiredUri,
                onSuccess = {
                    Toast.makeText(
                        context,
                        R.string.save_successfully,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()
        )
    ) {
        item(key = "header") {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.app_name_persian))
                }
            )
        }

        items(
            items = messengerItems,
            key = { it.name }
        ) { messengerItem ->
            MessengerCard(
                messengerItem = messengerItem,
                onClick = { onGoToWebFrame(messengerItem.platform) }
            )
        }

        item(key = "accessibility_service") {
            AccessibilityServiceCard(
                isExpanded = isAccessibilityItemExpanded,
                onExpandToggle = { isAccessibilityItemExpanded = !isAccessibilityItemExpanded },
                permissionsState = permissionsState
            )
        }

        item(key = "settings") {
            SettingsCard(
                isExpanded = isSettingsItemExpanded,
                onExpandToggle = { isSettingsItemExpanded = !isSettingsItemExpanded },
                onExportKeys = {
                    createFileLauncher.launch("identity_keys_backup.${Constants.KEYS_EXTENSION}")
                },
                onExitKeys = { isExitKeysWarningDialog = true },
                onGoToGuides = onGoToGuides,
                onOpenNewIssue = { uriHandler.openUri("${Constants.REPOSITORY_URL}/issues") }
            )
        }
        item("version") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = stringResource(R.string.version))
                Text(
                    text = BuildConfig.VERSION_NAME,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }

    if (isExitKeysWarningDialog) {
        ExitKeysWarningDialog(
            onConfirmExit = {
                viewModel.clearCurrentKeys {
                    onGoToIntro()
                    isExitKeysWarningDialog = false
                }
            },
            onDismiss = { isExitKeysWarningDialog = false }
        )
    }
}


@Composable
private fun MessengerCard(
    messengerItem: MessengerItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        TransparentListItem(
            headlineContent = {
                Text(
                    text = stringResource(messengerItem.name),
                    fontWeight = FontWeight.Bold
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(messengerItem.description),
                    modifier = Modifier.alpha(0.7f)
                )
            }
        )
    }
}

@Composable
private fun AccessibilityServiceCard(
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    permissionsState: PermissionState
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        TransparentListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.use_application),
                    fontWeight = FontWeight.Bold
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.use_application_description),
                    modifier = Modifier.alpha(0.7f)
                )
            },
            trailingContent = {
                ExpandIconButton(
                    expanded = isExpanded,
                    onClick = onExpandToggle
                )
            }
        )

        AnimatedVisibility(visible = isExpanded) {
            AccessibilityOptionMenu(permissionsState = permissionsState)
        }
    }
}

@Composable
private fun SettingsCard(
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onExportKeys: () -> Unit,
    onExitKeys: () -> Unit,
    onGoToGuides: () -> Unit,
    onOpenNewIssue: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        TransparentListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.settings),
                    fontWeight = FontWeight.Bold
                )
            },
            trailingContent = {
                ExpandIconButton(
                    expanded = isExpanded,
                    onClick = onExpandToggle
                )
            }
        )

        AnimatedVisibility(visible = isExpanded) {
            Column {
                TransparentListItem(
                    headlineContent = {
                        Text(stringResource(R.string.export_keys))
                    },
                    modifier = Modifier.clickable(onClick = onExportKeys)
                )

                TransparentListItem(
                    headlineContent = {
                        Text(stringResource(R.string.exit_current_keys))
                    },
                    modifier = Modifier.clickable(onClick = onExitKeys)
                )

                TransparentListItem(
                    headlineContent = {
                        Text(stringResource(R.string.new_issue))
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(16.dp)
                        )
                    },
                    modifier = Modifier.clickable(onClick = onOpenNewIssue)
                )
                TransparentListItem(
                    headlineContent = {
                        Text(stringResource(R.string.guides))
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(16.dp)
                        )
                    },
                    modifier = Modifier.clickable(onClick = onGoToGuides)
                )
            }
        }
    }
}

@Composable
private fun AccessibilityOptionMenu(
    permissionsState: PermissionState
) {
    val isNotificationsGranted = permissionsState
        .permissionsGranted[notificationsPermissionItem.name] ?: true
    val isOverlayGranted = permissionsState
        .permissionsGranted[overlayPermissionItem.name] == true
    val isAccessibilityGranted = permissionsState
        .permissionsGranted[accessibilityPermissionItem.name] == true

    Column {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            DisplayPermissionItem(
                name = stringResource(R.string.notification_permission),
                description = stringResource(R.string.notification_permission_description),
                isPermissionGranted = isNotificationsGranted,
                onRequestPermission = {
                    permissionsState.requestAccess(notificationsPermissionItem.name)
                }
            )
        }

        DisplayPermissionItem(
            name = stringResource(R.string.overlay_permission),
            description = stringResource(R.string.overlay_permission_description),
            isPermissionGranted = isOverlayGranted,
            onRequestPermission = {
                permissionsState.requestAccess(overlayPermissionItem.name)
            }
        )

        DisplayPermissionItem(
            name = stringResource(R.string.accessibility_permission),
            description = stringResource(R.string.accessibility_permission_description),
            isPermissionGranted = isAccessibilityGranted,
            requestPermissionEnabled = isOverlayGranted && isNotificationsGranted,
            onRequestPermission = {
                permissionsState.requestAccess(accessibilityPermissionItem.name)
            }
        )
    }
}

@Composable
private fun DisplayPermissionItem(
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
            AnimatedContent(
                targetState = isPermissionGranted,
                label = "permission_status"
            ) { granted ->
                if (granted) {
                    Text(
                        text = stringResource(R.string.permission_accepted),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.required_permission),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        supportingContent = {
            Text(
                text = description,
                modifier = Modifier.alpha(0.7f)
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

@Composable
private fun ExitKeysWarningDialog(
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
            TextButton(onClick = onConfirmExit) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}