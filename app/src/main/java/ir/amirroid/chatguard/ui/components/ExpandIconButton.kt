package ir.amirroid.chatguard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate

@Composable
fun ExpandIconButton(
    expanded: Boolean, onClick: () -> Unit
) {
    val rotate by animateFloatAsState(if (expanded) 180f else 0f)

    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.rotate(rotate)
        )
    }
}