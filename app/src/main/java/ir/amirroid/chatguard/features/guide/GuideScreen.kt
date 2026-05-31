package ir.amirroid.chatguard.features.guide

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ir.amirroid.chatguard.R
import ir.amirroid.chatguard.core.floating_button.FloatingButtonController
import ir.amirroid.chatguard.features.messages.ChatMessageItem
import ir.amirroid.chatguard.ui.components.document.DocumentContentCard
import ir.amirroid.chatguard.ui.components.document.DocumentInfoCallout
import ir.amirroid.chatguard.ui.components.document.DocumentSectionCard
import ir.amirroid.chatguard.ui.components.document.DocumentSectionHeading
import ir.amirroid.chatguard.ui.theme.ChatGuardTheme
import ir.amirroid.chatguard.ui_models.message.ChatMessageUiModel

private const val SAMPLE_MESSAGE = "این یک پیام تستی است!"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.guides),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = 24.dp,
            ) + innerPadding,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "accessibility_note") {
                DocumentInfoCallout(
                    title = stringResource(R.string.guide_accessibility_note_title),
                    body = stringResource(R.string.guide_accessibility_note_body),
                )
            }

            item(key = "intro") {
                DocumentContentCard {
                    GuideIntroText()
                }
            }

            item(key = "methods_overview") {
                DocumentContentCard {
                    GuideMethodsOverviewText()
                }
            }

            item(key = "method_web") {
                DocumentSectionCard(
                    title = "روش اول: استفاده از نسخه وب در بستر اپلیکیشن",
                    leadingIcon = Icons.Rounded.Language,
                    content = {
                        Spacer(Modifier.height(12.dp))
                        GuideWebMethodBodyText()
                    },
                )
            }

            item(key = "method_accessibility") {
                DocumentSectionCard(
                    title = "روش دوم: استفاده از سرویس دسترسی و اپلیکیشن اصلی",
                    leadingIcon = Icons.Rounded.PhoneAndroid,
                    content = {
                        Spacer(Modifier.height(12.dp))
                        GuideAccessibilityMethodIntroText()
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(
                                        width = 4.dp,
                                        color = Color(FloatingButtonController.GREEN_SUCCESS_COLOR),
                                        shape = CircleShape,
                                    ),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        GuideSendEncryptionKeyBottomSheetText()
                        GuideBottomSheetHeaderPreview()
                        Spacer(Modifier.height(16.dp))
                        GuideDecryptMessagesText()
                        Spacer(Modifier.height(8.dp))
                        ChatMessageItem(
                            message = ChatMessageUiModel(
                                index = 0,
                                message = SAMPLE_MESSAGE,
                                isMessageFromMe = false,
                                date = "03:30",
                                isDecryptedMessage = true,
                            ),
                            onCopyMessage = {
                                Toast.makeText(context, R.string.copy, Toast.LENGTH_SHORT).show()
                            },
                            onSavePublicKey = {},
                        )
                        Spacer(Modifier.height(16.dp))
                        GuideUseEncryptionKeyText()
                        Spacer(Modifier.height(8.dp))
                        ChatMessageItem(
                            message = ChatMessageUiModel(
                                index = 0,
                                message = SAMPLE_MESSAGE,
                                isMessageFromMe = false,
                                date = "00:00",
                                isPublicKey = true,
                            ),
                            onCopyMessage = {},
                            onSavePublicKey = {
                                Toast.makeText(context, R.string.usage, Toast.LENGTH_SHORT).show()
                            },
                        )
                        Spacer(Modifier.height(16.dp))
                        GuideSendMessageAccessibilityText()
                    },
                )
            }

            item(key = "accessibility_setup_heading") {
                DocumentSectionHeading(
                    text = "نحوه فعال‌سازی سرویس Accessibility",
                )
            }

            item(key = "accessibility_setup") {
                DocumentSectionCard(
                    title = "",
                    leadingIcon = Icons.Rounded.TouchApp,
                    content = {
                        GuideAccessibilitySetupText()
                    },
                )
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(showBackground = true)
@Composable
private fun GuideScreenPreview() {
    ChatGuardTheme {
        Scaffold {
            GuideScreen(onBack = {})
        }
    }
}
