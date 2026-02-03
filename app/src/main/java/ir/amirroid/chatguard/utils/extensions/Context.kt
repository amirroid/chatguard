package ir.amirroid.chatguard.utils.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

val Context.activityOrNull: Activity?
    get() = findActivity()

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}