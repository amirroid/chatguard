package ir.amirroid.chatguard.features.privacy

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.graphics.vector.ImageVector

internal data class PrivacyTimelineStep(
    val stepLabel: String,
    val title: String,
    val description: String,
)

internal data class PrivacyVisibilityRow(
    val label: String,
    val canSee: Boolean,
)

internal data class PrivacyHighlight(
    val title: String,
    val body: String,
    val icon: ImageVector,
)

internal object PrivacyConceptContent {

    const val SCREEN_TITLE = "حریم خصوصی در چت‌گارد"

    const val HERO_TITLE = "لایه‌ای از حفاظت روی پیام‌رسان‌هایی که الان دارید"
    const val HERO_BODY =
        "چت‌گارد جایگزین ایتا، بله یا سروش نیست. روی همان گفت‌وگوهای معمول شما می‌نشیند و کمک می‌کند " +
            "متن واقعی پیام، برای سرور پیام‌رسان و نگاه‌های تصادفی، قابل خواندن نباشد."

    const val WHY_TITLE = "چرا اصلاً به چت‌گارد نیاز است؟"
    const val WHY_BODY =
        "وقتی پیامی را در یک پیام‌رسان می‌فرستید، معمولاً سرویس می‌تواند محتوا را ببیند یا نگه دارد. " +
            "چت‌گارد قبل از ارسال، پیام را روی گوشی شما قفل می‌کند و آن را به شکلی می‌فرستد که " +
            "شبیه متن عادی یا شعر به نظر برسد — نه یک پیام خوانا برای دیگران."

    val TIMELINE_STEPS = listOf(
        PrivacyTimelineStep(
            stepLabel = "۱",
            title = "کلید اختصاصی روی گوشی شما",
            description =
                "یک بار برای خودتان یک «کلید» دیجیتال می‌سازید. این کلید روی گوشی می‌ماند؛ " +
                    "روی سروری در اینترنت ذخیره نمی‌شود.",
        ),
        PrivacyTimelineStep(
            stepLabel = "۲",
            title = "تبادل کلید با مخاطب",
            description =
                "قبل از گفت‌وگوی محرمانه، شما و طرف مقابل یک بار کلید عمومی یکدیگر را دریافت می‌کنید " +
                    "(مثلاً در همان چت، حضوری، یا از راهی که به آن اعتماد دارید).",
        ),
        PrivacyTimelineStep(
            stepLabel = "۳",
            title = "قفل شدن پیام قبل از ارسال",
            description =
                "متن پیام روی دستگاه شما به شکل نامفهوم درمی‌آید. سپس از همان پیام‌رسان همیشگی " +
                    "ارسال می‌شود — اما محتوای واقعی داخل آن قفل است.",
        ),
        PrivacyTimelineStep(
            stepLabel = "۴",
            title = "باز شدن فقط برای شما و مخاطب",
            description =
                "فقط کسی که کلید درست را دارد — شما یا همان مخاطب — می‌تواند پیام را روی گوشی خودش بخواند.",
        ),
    )

    const val MESSENGER_RELATION_TITLE = "چت‌گارد و پیام‌رسان؛ هر کدام چه کار می‌کنند؟"
    const val MESSENGER_RELATION_BODY =
        "پیام‌رسان مسیر ارسال پیام را فراهم می‌کند (مثل جاده). چت‌گارد محتوای داخل نامه را قفل می‌کند. " +
            "شما همچنان از همان اپ قبلی استفاده می‌کنید؛ چت‌گارد کنار آن کار می‌کند."

    val MESSENGER_HIGHLIGHTS = listOf(
        PrivacyHighlight(
            title = "پیام‌رسان",
            body = "پیام را تحویل می‌گیرد و ذخیره می‌کند؛ معمولاً شکل قفل‌شده را می‌بیند، نه متن اصلی.",
            icon = Icons.Rounded.Chat,
        ),
        PrivacyHighlight(
            title = "چت‌گارد",
            body = "قفل و باز کردن پیام فقط روی گوشی شما انجام می‌شود. سرور جداگانه‌ای برای خواندن پیام‌ها ندارد.",
            icon = Icons.Rounded.Lock,
        ),
    )

    const val VISIBILITY_TITLE = "دیگران چه می‌بینند و چه نمی‌بینند؟"
    const val VISIBILITY_FOOTNOTE =
        "اگر کسی کلید شما را نداشته باشد، پیام محافظت‌شده برایش مجموعه‌ای از حروف و واژه‌های بی‌معنی است."

    val VISIBILITY_ROWS = listOf(
        PrivacyVisibilityRow("سرور یا اپراتور پیام‌رسان", canSee = false),
        PrivacyVisibilityRow("افراد داخل همان گفت‌وگو (بدون کلید شما)", canSee = false),
        PrivacyVisibilityRow("شما و مخاطبی که کلیدها را درست رد و بدل کرده‌اید", canSee = true),
        PrivacyVisibilityRow("کسی که گوشی قفل‌شدهٔ شما را دزدیده و رمز گوشی را هم دارد", canSee = true),
    )

    const val KEY_EXCHANGE_TITLE = "چرا باید یک بار کلید را با مخاطب رد و بدل کنیم؟"
    const val KEY_EXCHANGE_BODY =
        "مثل دادن یک قفل اختصاصی: هر دو طرف باید بدانند «قفل این گفت‌وگو» با کدام کلید باز می‌شود. " +
            "این کار یک بار برای هر مخاطب انجام می‌شود؛ بعد از آن، پیام‌های محافظت‌شده همان مسیر را طی می‌کنند. " +
            "اگر کلید اشتباه یا جعلی باشد، ممکن است شخص دیگری خود را جای مخاطب شما جا بزند — " +
            "پس بهتر است کلید را از راهی بگیرید که به آن اطمینان دارید."

    const val LOST_KEY_CALLOUT_TITLE = "اگر کلید خصوصی از دست برود"
    const val LOST_KEY_CALLOUT_BODY =
        "کلید خصوصی مثل کلید خانه است. اگر گوشی را از دست بدهید، فرمت کنید، یا کلید را پاک کنید " +
            "بدون اینکه قبلاً پشتیبان گرفته باشید، پیام‌های قدیمیِ محافظت‌شده دیگر قابل بازیابی نیستند. " +
            "از بخش تنظیمات می‌توانید از کلید خود نسخهٔ پشتیبان بگیرید."

    const val KEEP_SAFE_TITLE = "چه چیزهایی را باید امن نگه دارید؟"
    val KEEP_SAFE_ITEMS = listOf(
        "کلید خصوصی و فایل پشتیبان کلید — هرگز برای دیگران نفرستید.",
        "گوشی خودتان — با رمز قوی و قفل صفحه.",
        "اطمینان از اینکه کلید مخاطب واقعاً از خود اوست، نه شخص ثالث.",
    )

    const val LIMITATIONS_TITLE = "انتظارات واقع‌بینانه"
    val LIMITATION_ITEMS = listOf(
        "چت‌گارد جایگزین پیام‌رسان‌های بزرگ با امنیت سطح بالا نیست؛ یک لایهٔ عملی برای حفظ حریم خصوصی است.",
        "اگر کلید خصوصی لو برود، پیام‌های گذشته با همان مخاطب قابل خواندن می‌شوند.",
        "گفت‌وگوی گروهی پشتیبانی نمی‌شود؛ برای دو نفر طراحی شده است.",
        "امنیت به شیوهٔ رد و بدل کردن اولین کلید هم بستگی دارد.",
    )

    const val CLOSING_TITLE = "خلاصهٔ یک جمله‌ای"
    const val CLOSING_BODY =
        "چت‌گارد به شما کمک می‌کند همان پیام‌رسان‌های ناامن را با خیال راحت‌تری استفاده کنید: " +
            "متن واقعی فقط بین شما و مخاطبتان می‌ماند — به شرط آنکه کلیدها را درست و امن نگه دارید."
}
