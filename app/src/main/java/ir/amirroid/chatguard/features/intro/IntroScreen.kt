package ir.amirroid.chatguard.features.intro

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.amirroid.chatguard.R
import ir.amirroid.chatguard.ui.components.TransparentListItem
import ir.amirroid.chatguard.ui_models.intro.IdentityKeyUsage
import ir.amirroid.chatguard.ui_models.intro.SelectKeyStatus
import org.koin.androidx.compose.koinViewModel

@Composable
fun IntroScreen(onGoToHome: () -> Unit, viewModel: IntroViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
        onResult = viewModel::handlePickedIdentityKeyPairFile
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        Text(
            buildAnnotatedString {
                val text = stringResource(R.string.welcome)
                val splitText = text.split("app_name")
                splitText.getOrNull(0)?.let(::append)
                withStyle(SpanStyle(MaterialTheme.colorScheme.primary)) {
                    append(stringResource(R.string.app_name_persian))
                }
                splitText.getOrNull(1)?.let(::append)
            },
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.app_intro),
            modifier = Modifier
                .padding(top = 16.dp)
                .padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Card(
            modifier = Modifier
                .padding(top = 36.dp)
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            onClick = { viewModel.selectUsage(IdentityKeyUsage.GENERATE_NEW) }
        ) {
            TransparentListItem(
                headlineContent = {
                    Text(stringResource(R.string.create_new_keys))
                },
                leadingContent = {
                    RadioButton(
                        selected = state.currentUsage == IdentityKeyUsage.GENERATE_NEW,
                        onClick = null
                    )
                },
            )
        }
        Card(
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            onClick = {
                viewModel.selectUsage(IdentityKeyUsage.IMPORT_EXISTING)
                filePickerLauncher.launch(arrayOf("*/*"))
            }
        ) {
            val selected = state.currentUsage == IdentityKeyUsage.IMPORT_EXISTING
            val currentSupportingText = when (state.selectedKeyStatus) {
                SelectKeyStatus.UNSELECTED -> stringResource(R.string.select_file) to MaterialTheme.colorScheme.error
                SelectKeyStatus.UNVERIFIED_KEY_PAIR -> stringResource(R.string.unverified_selected_file) to MaterialTheme.colorScheme.error
                SelectKeyStatus.VERIFIED_KEY_PAIR -> stringResource(R.string.verified_file) to MaterialTheme.colorScheme.primary
                else -> null
            }
            TransparentListItem(
                headlineContent = {
                    Text(stringResource(R.string.import_existing_keys))
                },
                leadingContent = {
                    RadioButton(
                        selected = selected,
                        onClick = null
                    )
                },
                supportingContent = currentSupportingText?.let { (text, color) ->
                    {
                        Text(
                            text = text,
                            color = color
                        )
                    }
                }
            )
            AnimatedVisibility(selected && state.selectedKeyStatus == SelectKeyStatus.SELECTING) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.selecting_file),
                        modifier = Modifier.alpha(.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))

        val enabledButton = when (state.currentUsage) {
            IdentityKeyUsage.GENERATE_NEW -> true
            IdentityKeyUsage.IMPORT_EXISTING -> state.selectedKeyStatus == SelectKeyStatus.VERIFIED_KEY_PAIR
        }

        Button(
            onClick = {
                viewModel.saveKeys(onGoToHome)
            },
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(24.dp),
            enabled = enabledButton && state.processing.not()
        ) {
            Text(stringResource(R.string.start))
        }
    }
}