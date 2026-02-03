package ir.amirroid.chatguard.features.intro

import androidx.compose.runtime.Immutable
import ir.amirroid.chatguard.ui_models.intro.IdentityKeyUsage
import ir.amirroid.chatguard.ui_models.intro.SelectKeyStatus

@Immutable
data class IntroScreenState(
    val currentUsage: IdentityKeyUsage = IdentityKeyUsage.GENERATE_NEW,
    val selectedKeyStatus: SelectKeyStatus = SelectKeyStatus.SELECTING,
    val processing: Boolean = false
)
