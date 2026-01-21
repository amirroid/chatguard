package ir.sysfail.chatguard.features.web_frame

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import ir.sysfail.chatguard.core.messanger.models.MessengerPlatform

@Composable
fun WebFrameScreen(
    platform: MessengerPlatform
) {
    AndroidView(
        factory = {
            WebView(it).apply {
                loadUrl(platform.url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}