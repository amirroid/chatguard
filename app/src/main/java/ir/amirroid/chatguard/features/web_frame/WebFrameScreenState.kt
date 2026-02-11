package ir.amirroid.chatguard.features.web_frame

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class WebFrameScreenState(
    val backgroundColor: Color = Color.Transparent,
    var isSendPlainTextConfirmation: Boolean = false
)
