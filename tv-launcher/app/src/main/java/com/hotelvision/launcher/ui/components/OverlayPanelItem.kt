package com.hotelvision.launcher.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val OVERLAY_FOCUS_SETTLE_DELAY_MS = 60L

internal fun overlayEntryFocusDelayMillis(enterSlideMs: Int): Long {
    return enterSlideMs.toLong() + OVERLAY_FOCUS_SETTLE_DELAY_MS
}

internal fun quickSettingsItemTag(itemId: String): String {
    return "quick_settings_item_$itemId"
}

fun Modifier.overlayPanelItemFocusable(
    focusRequester: FocusRequester?,
    cornerRadius: Dp,
    focusedScale: Float = 1.05f,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit
): Modifier {
    val focusModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }

    return this
        .then(focusModifier)
        .gtvFocusScale(
            focusedScale = focusedScale,
            cornerRadius = cornerRadius,
            borderWidth = 4.dp,
            onFocus = onFocusChanged,
            onClick = onClick
        )
}
