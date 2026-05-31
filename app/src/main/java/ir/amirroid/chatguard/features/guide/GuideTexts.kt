package ir.amirroid.chatguard.features.guide

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/**
 * Original guide copy — wording must remain unchanged.
 */
@Composable
internal fun GuideIntroText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("چت‌گارد ")
            pop()
            append("ابزاری برای ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("رمزنگاری و رمزگشایی پیام‌ها ")
            pop()
            append("در پیام‌رسان‌ها است که با هدف افزایش امنیت و حفظ حریم خصوصی کاربران طراحی شده است.\n\n")
            append("این برنامه امکان ارسال و دریافت پیام‌های رمزنگاری‌شده را بدون نیاز به تغییر پیام‌رسان اصلی فراهم می‌کند.")
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun GuideMethodsOverviewText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = "برای استفاده از چت‌گارد، دو روش اصلی در اختیار شما قرار دارد:",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
internal fun GuideWebMethodBodyText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            append("در این روش می‌توانید یکی از پیام‌رسان‌های پشتیبانی‌شده را انتخاب کرده و گفت‌وگو را آغاز کنید.\n\n")

            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("رمزنگاری پیام‌ها\n")
            pop()
            append("برای ارسال پیام به‌صورت رمزنگاری‌شده، لازم است ابتدا ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("کلید رمزنگاری مخاطب ")
            pop()
            append("را در اختیار داشته باشید.\n\n")

            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("دریافت کلید رمزنگاری مخاطب\n")
            pop()
            append("در صورتی که مخاطب کلید رمزنگاری خود را برای شما ارسال کرده باشد، در کنار پیام مربوطه دکمه‌ای با عنوان ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("«استفاده» ")
            pop()
            append("نمایش داده می‌شود که با انتخاب آن، کلید ذخیره خواهد شد.\n\n")

            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("ارسال کلید رمزنگاری خود\n")
            pop()
            append("برای ارسال کلید رمزنگاری خود به شخص مقابل، می‌توانید از دکمه بالای صفحه با ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("آیکون کلید ")
            pop()
            append("استفاده کنید.\n\n")

            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("ارسال پیام\n")
            pop()
            append("اگر کلید رمزنگاری موجود باشد، پیام به‌صورت خودکار به‌شکل رمزنگاری‌شده ارسال می‌شود.")
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun GuideSendEncryptionKeyBottomSheetText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("ارسال کلید رمزنگاری از پنجرهٔ پیام‌ها\n")
            pop()
            append("پس از باز شدن پنجرهٔ شناور (دکمهٔ سبز رنگ)، در بالای لیست پیام‌ها دکمهٔ ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("«ارسال پیام» ")
            pop()
            append("و منوی ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("⋮ ")
            pop()
            append("(سه‌نقطه) در سمت مقابل قرار دارد. با لمس منو و انتخاب ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("«ارسال کلید رمزنگاری» ")
            pop()
            append("کلید عمومی شما برای مخاطب ارسال می‌شود و پنجره بسته خواهد شد.")
            appendLine()
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun GuideAccessibilityMethodIntroText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            append("این روش به شما امکان می‌دهد بدون ورود مجدد به نسخه وب و با استفاده از اپلیکیشن اصلی پیام‌رسان فعالیت کنید.\n\n")

            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("فعال‌سازی دسترسی‌ها\n")
            pop()

            append("پس از اعطای مجوزهای موردنیاز، یک ")

            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("دکمه دایره‌ای شناور ")
            pop()

            append("در داخل اپلیکیشن پیام‌رسان، پس از ورود نمایش داده می‌شود.")
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun GuideDecryptMessagesText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("مشاهده و رمزگشایی پیام‌ها\n")
            pop()
            append("با انتخاب این دکمه، پیام‌های موجود روی صفحه نمایش داده می‌شوند. در صورتی که کلید رمزنگاری قبلاً ثبت شده باشد، پیام‌ها به‌صورت ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("رمزگشایی‌شده ")
            pop()
            append("نمایش داده خواهند شد.")
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun GuideUseEncryptionKeyText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("استفاده از کلید رمزنگاری\n")
            pop()
            append("برای ارسال پیام یا رمزگشایی پیام‌های حاوی کلید، لازم است کلید رمزنگاری مخاطب در اختیار شما باشد. در لیست پیام‌ها، دکمه ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("«استفاده» ")
            pop()
            append("برای ذخیره کلید نمایش داده می‌شود.")
            appendLine()
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun GuideSendMessageAccessibilityText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("ارسال پیام\n")
            pop()
            append("برای ارسال پیام، پس از باز شدن پنجره شناور، گزینه ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("ارسال پیام ")
            pop()
            append("را انتخاب کرده، متن موردنظر را وارد نموده و دکمه ارسال را فشار دهید. ")

            append("اگر کلید مخاطب را نداشته باشید، این دکمه غیرفعال خواهد بود.")
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun GuideAccessibilitySetupText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            append("برای فعال‌سازی کامل قابلیت‌های چت‌گارد، لازم است سرویس دسترسی (Accessibility Service) را فعال کنید.\n\n")
            append("ابتدا بر روی گزینه ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("اعطای مجوز ")
            pop()
            append("کلیک کنید. در صفحه تنظیمات بازشده، وارد بخش ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("Downloaded apps ")
            pop()
            append("(یا «برنامه‌های دانلودشده») شوید.\n\n")
            append("در لیست برنامه‌ها، گزینه ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("ChatGuard ")
            pop()
            append("را پیدا کرده و انتخاب کنید. سپس گزینه ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("Use service ")
            pop()
            append("یا ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("Enable ")
            pop()
            append("را فعال کنید.\n\n")
            append("در مرحله بعد، پیام هشدار سیستم را مطالعه کرده و برای نهایی شدن، گزینه ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("Allow ")
            pop()
            append("یا ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("OK ")
            pop()
            append("را انتخاب نمایید.\n\n")
            append("پس از فعال‌سازی موفق، می‌توانید به برنامه بازگشته و از تمامی قابلیت‌های چت‌گارد بدون محدودیت استفاده کنید.")
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}
