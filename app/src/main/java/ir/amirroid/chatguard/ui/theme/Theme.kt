package ir.amirroid.chatguard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF00315F),
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFD1E5FF),

    secondary = Color(0xFF64B5F6),
    onSecondary = Color(0xFF001A33),

    tertiary = Color(0xFF42A5F5),
    onTertiary = Color(0xFF00101F),

    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFE2E2E2),

    surface = Color(0xFF141414),
    onSurface = Color(0xFFE2E2E2),

    error = Color(0xFFCF6679),
    onError = Color(0xFF300000),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF90CAF9),
    onPrimaryContainer = Color(0xFF001D38),

    secondary = Color(0xFF64B5F6),
    onSecondary = Color(0xFF001A33),

    tertiary = Color(0xFF1976D2),
    onTertiary = Color(0xFFFFFFFF),

    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0D0D0D),

    surface = Color(0xFFF6F6F6),
    onSurface = Color(0xFF0D0D0D),

    error = Color(0xFFD32F2F),
    onError = Color(0xFFFFFFFF),
)

@Composable
fun ChatGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = remember { Typography.copyFont(IranSansFontFamily) },
            content = content
        )
    }
}