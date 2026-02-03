package ir.amirroid.chatguard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import ir.amirroid.chatguard.R

val IranSansFontFamily = FontFamily(
    Font(R.font.iransans),
    Font(R.font.iransans_bold, FontWeight.Bold),
)


// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

fun Typography.copyFont(
    newFonts: FontFamily
): Typography = copy(

    displayLarge = displayLarge.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),
    displayMedium = displayMedium.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),
    displaySmall = displaySmall.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),

    headlineLarge = headlineLarge.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),
    headlineMedium = headlineMedium.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),
    headlineSmall = headlineSmall.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),

    titleLarge = titleLarge.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),
    titleMedium = titleMedium.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),
    titleSmall = titleSmall.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),

    bodyLarge = bodyLarge.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),
    bodyMedium = bodyMedium.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),
    bodySmall = bodySmall.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),

    labelLarge = labelLarge.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),
    labelMedium = labelMedium.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),
    labelSmall = labelSmall.copy(fontFamily = newFonts, textDirection = TextDirection.ContentOrRtl),
)