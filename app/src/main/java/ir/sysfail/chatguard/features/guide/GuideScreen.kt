package ir.sysfail.chatguard.features.guide

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ir.sysfail.chatguard.R
import ir.sysfail.chatguard.core.floating_button.FloatingButtonController
import ir.sysfail.chatguard.features.messages.ChatMessageItem
import ir.sysfail.chatguard.ui.theme.ChatGuardTheme
import ir.sysfail.chatguard.ui_models.message.ChatMessageUiModel

private const val SAMPLE_MESSAGE = "این یک پیام تستی است!"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column {
        TopAppBar(
            title = {
                Text(stringResource(R.string.guides))
            }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                buildAnnotatedString {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append("چت‌گارد ")
                    pop()
                    append("ابزاری برای ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append("رمزنگاری و رمزگشایی پیام‌ها ")
                    pop()
                    append("در پیام‌رسان‌ها است که با هدف افزایش امنیت و حفظ حریم خصوصی کاربران طراحی شده است.\n\n")
                    append("این برنامه امکان ارسال و دریافت پیام‌های رمزنگاری‌شده را بدون نیاز به تغییر پیام‌رسان اصلی فراهم می‌کند.")
                    appendLine()
                }
            )
            Text("برای استفاده از چت‌گارد، دو روش اصلی در اختیار شما قرار دارد:")
            Text(
                "روش اول: استفاده از نسخه وب در بستر اپلیکیشن",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                buildAnnotatedString {
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
                }
            )
            Text(
                "روش دوم: استفاده از سرویس دسترسی و اپلیکیشن اصلی",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                buildAnnotatedString {
                    append("این روش به شما امکان می‌دهد بدون ورود مجدد به نسخه وب و با استفاده از اپلیکیشن اصلی پیام‌رسان فعالیت کنید.\n\n")

                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append("فعال‌سازی دسترسی‌ها\n")
                    pop()
                    append("پس از اعطای مجوزهای موردنیاز، یک ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append("دکمه دایره‌ای شناور ")
                    pop()
                    append("بر روی صفحه نمایش داده می‌شود.")
                    appendLine()
                }
            )
            Box(
                Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .border(
                            4.dp,
                            Color(FloatingButtonController.GREEN_SUCCESS_COLOR),
                            CircleShape
                        )
                )
            }
            Text(
                buildAnnotatedString {
                    appendLine()
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append("مشاهده و رمزگشایی پیام‌ها\n")
                    pop()
                    append("با انتخاب این دکمه، پیام‌های موجود روی صفحه نمایش داده می‌شوند. در صورتی که کلید رمزنگاری قبلاً ثبت شده باشد، پیام‌ها به‌صورت ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append("رمزگشایی‌شده ")
                    pop()
                    append("نمایش داده خواهند شد.")
                    appendLine()
                }
            )
            ChatMessageItem(
                message = ChatMessageUiModel(0, SAMPLE_MESSAGE, false, "03:30"),
                onCopyMessage = {},
                onSavePublicKey = {}
            )
            Text(
                buildAnnotatedString {
                    appendLine()
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append("استفاده از کلید رمزنگاری\n")
                    pop()
                    append("برای ارسال پیام یا رمزگشایی پیام‌های حاوی کلید، لازم است کلید رمزنگاری مخاطب در اختیار شما باشد. در لیست پیام‌ها، دکمه ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append("«استفاده» ")
                    pop()
                    append("برای ذخیره کلید نمایش داده می‌شود.")
                    appendLine()
                }
            )
            ChatMessageItem(
                message = ChatMessageUiModel(0, SAMPLE_MESSAGE, false, "00:00", isPublicKey = true),
                onCopyMessage = {},
                onSavePublicKey = {
                    Toast.makeText(context, R.string.confirm, Toast.LENGTH_SHORT).show()
                }
            )
            Text(
                buildAnnotatedString {
                    appendLine()
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append("ارسال پیام\n")
                    pop()
                    append("برای ارسال پیام، پس از باز شدن پنجره شناور، گزینه ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append("ارسال پیام ")
                    pop()
                    append("را انتخاب کرده، متن موردنظر را وارد نموده و دکمه ارسال را فشار دهید.")
                }
            )

            Text(
                "نحوه فعال‌سازی سرویس Accessibility",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                buildAnnotatedString {
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
                }
            )
        }
    }
}

@Preview
@Composable
private fun GuideScreenPreview() {
    ChatGuardTheme {
        Scaffold {
            GuideScreen({})
        }
    }
}