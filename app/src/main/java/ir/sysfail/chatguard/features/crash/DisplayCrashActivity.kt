package ir.sysfail.chatguard.features.crash

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.sysfail.chatguard.core.exception.ApplicationCrash
import ir.sysfail.chatguard.ui.theme.ChatGuardTheme
import ir.sysfail.chatguard.utils.Constants
import kotlinx.serialization.json.Json

class DisplayCrashActivity : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val crashJson = intent.getStringExtra(Constants.CRASH_DATA_KEY) ?: return
        val applicationCrash = Json.decodeFromString<ApplicationCrash>(crashJson)

        setContent {
            ChatGuardTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    CrashScreen(applicationCrash, onDismiss = ::finish)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashScreen(
    applicationCrash: ApplicationCrash?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    if (applicationCrash == null) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Application Crashed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                CrashInfoCard(
                    title = "Error Message",
                    icon = Icons.Rounded.Message,
                    iconTint = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = applicationCrash.message.ifEmpty { "No message" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                CrashInfoCard(
                    title = "Exception Type",
                    icon = Icons.Rounded.BugReport,
                    iconTint = MaterialTheme.colorScheme.tertiary
                ) {
                    Text(
                        text = applicationCrash.exception,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                CrashInfoCard(
                    title = "Location",
                    icon = Icons.Rounded.LocationOn,
                    iconTint = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            InfoRow(label = "File", value = applicationCrash.fileName)
                            Spacer(modifier = Modifier.height(4.dp))
                            InfoRow(label = "Line", value = applicationCrash.lineNumber.toString())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                CrashInfoCard(
                    title = "App Information",
                    icon = Icons.Rounded.Info,
                    iconTint = MaterialTheme.colorScheme.secondary
                ) {
                    InfoRow(label = "Version", value = applicationCrash.appVersion)
                }

                Spacer(modifier = Modifier.height(12.dp))

                CrashInfoCard(
                    title = "Device Information",
                    icon = Icons.Rounded.PhoneAndroid,
                    iconTint = MaterialTheme.colorScheme.tertiary
                ) {
                    Column {
                        InfoRow(
                            label = "Model",
                            value = applicationCrash.device.model
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        InfoRow(
                            label = "Android",
                            value = "${applicationCrash.device.androidVersion} (API ${applicationCrash.device.apiVersion})"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                CrashInfoCard(
                    title = "Stack Trace",
                    icon = Icons.Rounded.Code,
                    iconTint = MaterialTheme.colorScheme.error
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = applicationCrash.stacktrace,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val crashReport = buildCrashReport(applicationCrash)
                            clipboardManager.setText(AnnotatedString(crashReport))
                            Toast.makeText(context, "Crash report copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Report")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Close App")
                    }
                }
            }
        }
    }
}

@Composable
private fun CrashInfoCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

private fun buildCrashReport(crash: ApplicationCrash): String {
    return buildString {
        appendLine("═══════════════════════════════════════")
        appendLine("        APPLICATION CRASH REPORT        ")
        appendLine("═══════════════════════════════════════")
        appendLine()

        appendLine("📱 Device Information:")
        appendLine("├─ Model: ${crash.device.model}")
        appendLine("└─ Android: ${crash.device.androidVersion} (API ${crash.device.apiVersion})")
        appendLine()

        appendLine("📦 App Information:")
        appendLine("└─ Version: ${crash.appVersion}")
        appendLine()

        appendLine("❌ Error Details:")
        appendLine("├─ Exception: ${crash.exception}")
        appendLine("├─ Message: ${crash.message}")
        appendLine("├─ Fil ${crash.fileName}")
        appendLine("└─ Line: ${crash.lineNumber}")
        appendLine()

        appendLine("📋 Stack Trace:")
        appendLine("─".repeat(43))
        appendLine(crash.stacktrace)
        appendLine("─".repeat(43))
        appendLine()

        appendLine(
            "Generated on: ${
                java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())
            }"
        )
    }
}