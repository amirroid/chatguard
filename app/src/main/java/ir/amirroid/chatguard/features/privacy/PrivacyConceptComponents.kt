package ir.amirroid.chatguard.features.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amirroid.chatguard.ui.components.document.DocumentClosingBanner
import ir.amirroid.chatguard.ui.components.document.DocumentContentCard
import ir.amirroid.chatguard.ui.components.document.DocumentHeroSection
import ir.amirroid.chatguard.ui.components.document.DocumentIllustrationPlaceholder
import ir.amirroid.chatguard.ui.components.document.DocumentInfoCallout
import ir.amirroid.chatguard.ui.components.document.DocumentSectionCard
import ir.amirroid.chatguard.ui.components.document.DocumentWarningCallout

@Composable
internal fun PrivacyHeroSection(
    title: String,
    body: String,
    illustrationIcon: ImageVector,
    modifier: Modifier = Modifier,
) = DocumentHeroSection(title, body, illustrationIcon, modifier)

@Composable
internal fun IllustrationPlaceholder(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    boxSize: androidx.compose.ui.unit.Dp = 96.dp,
    showCaption: Boolean = true,
) = DocumentIllustrationPlaceholder(icon, modifier, boxSize)

@Composable
internal fun PrivacySectionCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    content: @Composable (() -> Unit)? = null,
) = DocumentSectionCard(title, body, modifier, leadingIcon, content)

@Composable
internal fun PrivacyCallout(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Info,
) = DocumentWarningCallout(title, body, modifier, icon)

@Composable
internal fun PrivacyClosingBanner(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) = DocumentClosingBanner(title, body, modifier)

@Composable
internal fun PrivacyTimeline(
    steps: List<PrivacyTimelineStep>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        steps.forEachIndexed { index, step ->
            PrivacyTimelineItem(
                step = step,
                showConnector = index < steps.lastIndex,
            )
        }
    }
}

@Composable
private fun PrivacyTimelineItem(
    step: PrivacyTimelineStep,
    showConnector: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = step.stepLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(start = 8.dp, bottom = if (showConnector) 20.dp else 0.dp)
                .weight(1f),
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
internal fun PrivacyDualHighlightRow(
    highlights: List<PrivacyHighlight>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        highlights.forEach { highlight ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    DocumentIllustrationPlaceholder(
                        icon = highlight.icon,
                        boxSize = 56.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = highlight.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = highlight.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PrivacyVisibilityTable(
    rows: List<PrivacyVisibilityRow>,
    footnote: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (row.canSee) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = null,
                    tint = if (row.canSee) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (row.canSee) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Text(
                        text = if (row.canSee) "می‌بیند" else "نمی‌بیند",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = if (row.canSee) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            if (index < rows.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = footnote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
internal fun PrivacyBulletList(
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
