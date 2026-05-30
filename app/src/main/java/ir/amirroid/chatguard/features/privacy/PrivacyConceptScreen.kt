package ir.amirroid.chatguard.features.privacy

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ir.amirroid.chatguard.ui.theme.ChatGuardTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyConceptScreen(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = PrivacyConceptContent.SCREEN_TITLE,
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
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "subtitle") {
                Text(
                    text = PrivacyConceptContent.SCREEN_SUBTITLE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            item(key = "hero") {
                PrivacyHeroSection(
                    title = PrivacyConceptContent.HERO_TITLE,
                    body = PrivacyConceptContent.HERO_BODY,
                    illustrationIcon = Icons.Rounded.Lock,
                )
            }

            item(key = "why") {
                PrivacySectionCard(
                    title = PrivacyConceptContent.WHY_TITLE,
                    body = PrivacyConceptContent.WHY_BODY,
                    leadingIcon = Icons.Rounded.Info,
                )
            }

            item(key = "timeline_header") {
                Text(
                    text = "مسیر یک پیام محافظت‌شده",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            item(key = "timeline") {
                PrivacySectionCard(
                    title = "از گوشی شما تا گوشی مخاطب",
                    body = "هر پیام محافظت‌شده این مراحل را طی می‌کند:",
                    content = {
                        Spacer(Modifier.height(16.dp))
                        PrivacyTimeline(steps = PrivacyConceptContent.TIMELINE_STEPS)
                    },
                )
            }

            item(key = "messenger") {
                PrivacySectionCard(
                    title = PrivacyConceptContent.MESSENGER_RELATION_TITLE,
                    body = PrivacyConceptContent.MESSENGER_RELATION_BODY,
                    leadingIcon = Icons.Rounded.PhoneAndroid,
                    content = {
                        Spacer(Modifier.height(16.dp))
                        PrivacyDualHighlightRow(highlights = PrivacyConceptContent.MESSENGER_HIGHLIGHTS)
                    },
                )
            }

            item(key = "visibility") {
                PrivacySectionCard(
                    title = PrivacyConceptContent.VISIBILITY_TITLE,
                    body = "",
                    leadingIcon = Icons.Rounded.Info,
                    content = {
                        PrivacyVisibilityTable(
                            rows = PrivacyConceptContent.VISIBILITY_ROWS,
                            footnote = PrivacyConceptContent.VISIBILITY_FOOTNOTE,
                        )
                    },
                )
            }

            item(key = "key_exchange") {
                PrivacySectionCard(
                    title = PrivacyConceptContent.KEY_EXCHANGE_TITLE,
                    body = PrivacyConceptContent.KEY_EXCHANGE_BODY,
                    leadingIcon = Icons.Rounded.SwapHoriz,
                )
            }

            item(key = "lost_key") {
                PrivacyCallout(
                    title = PrivacyConceptContent.LOST_KEY_CALLOUT_TITLE,
                    body = PrivacyConceptContent.LOST_KEY_CALLOUT_BODY,
                )
            }

            item(key = "keep_safe") {
                PrivacySectionCard(
                    title = PrivacyConceptContent.KEEP_SAFE_TITLE,
                    body = "",
                    leadingIcon = Icons.Rounded.Key,
                    content = {
                        PrivacyBulletList(items = PrivacyConceptContent.KEEP_SAFE_ITEMS)
                    },
                )
            }

            item(key = "limitations") {
                PrivacySectionCard(
                    title = PrivacyConceptContent.LIMITATIONS_TITLE,
                    body = "",
                    leadingIcon = Icons.Rounded.Info,
                    content = {
                        PrivacyBulletList(items = PrivacyConceptContent.LIMITATION_ITEMS)
                    },
                )
            }

            item(key = "closing") {
                PrivacyClosingBanner(
                    title = PrivacyConceptContent.CLOSING_TITLE,
                    body = PrivacyConceptContent.CLOSING_BODY,
                )
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(showBackground = true)
@Composable
private fun PrivacyConceptScreenPreview() {
    ChatGuardTheme {
        PrivacyConceptScreen(onBack = {})
    }
}
