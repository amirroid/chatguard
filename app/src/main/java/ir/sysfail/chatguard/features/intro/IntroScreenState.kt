package ir.sysfail.chatguard.features.intro

import androidx.compose.runtime.Immutable
import ir.sysfail.chatguard.ui_models.intro.IdentityKeyUsage
import ir.sysfail.chatguard.ui_models.intro.SelectKeyStatus

@Immutable
data class IntroScreenState(
    val currentUsage: IdentityKeyUsage = IdentityKeyUsage.GENERATE_NEW,
    val selectedKeyStatus: SelectKeyStatus = SelectKeyStatus.SELECTING,
    val processing: Boolean = false
)
