package ir.amirroid.chatguard.core.messanger.utils

import android.view.accessibility.AccessibilityNodeInfo

fun AccessibilityNodeInfo.hasNodeWithId(id: String) =
    findAccessibilityNodeInfosByViewId(id)?.isNotEmpty() ?: false

fun AccessibilityNodeInfo.isView() = className == AndroidViewClassNames.VIEW
fun AccessibilityNodeInfo.isRecyclerView() = className == AndroidViewClassNames.RECYCLER_VIEW
fun AccessibilityNodeInfo.isEditText() = className == AndroidViewClassNames.EDIT_TEXT