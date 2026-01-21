package ir.sysfail.chatguard.core.messanger.utils

import android.view.accessibility.AccessibilityNodeInfo

fun AccessibilityNodeInfo.hasNodeWithId(id: String) =
    findAccessibilityNodeInfosByViewId(id)?.isNotEmpty() ?: false