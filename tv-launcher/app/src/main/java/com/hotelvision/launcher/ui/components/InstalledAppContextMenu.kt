package com.hotelvision.launcher.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.PopupPositionProvider
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.hotelvision.launcher.R
import com.hotelvision.launcher.ui.LauncherAction

data class ContextMenuAnchorBounds(
    val bounds: IntRect,
    val focusedScale: Float
)

@Composable
fun InstalledAppContextMenuPopup(
    anchorBounds: ContextMenuAnchorBounds,
    packageName: String,
    appTitle: String,
    tileFocusRequester: FocusRequester,
    onDismissRequest: () -> Unit,
    onAction: (LauncherAction) -> Unit
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    val dismissAndRestoreFocus = {
        onDismissRequest()
        tileFocusRequester.requestFocus()
    }

    BackHandler(onBack = dismissAndRestoreFocus)

    LaunchedEffect(Unit) {
        firstItemFocusRequester.requestFocus()
    }

    Popup(
        popupPositionProvider = rememberContextMenuPositionProvider(anchorBounds),
        onDismissRequest = dismissAndRestoreFocus,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        val menuWidth = dimensionResource(R.dimen.gtv_context_menu_width)
        val menuRadius = dimensionResource(R.dimen.gtv_context_menu_radius)
        val menuElevation = dimensionResource(R.dimen.gtv_context_menu_elevation)
        val menuPadding = dimensionResource(R.dimen.gtv_context_menu_padding)
        val menuBackground = colorResource(R.color.gtv_context_menu_background)
        val menuDivider = colorResource(R.color.gtv_context_menu_divider)
        val menuShape = RoundedCornerShape(menuRadius)

        Column(
            modifier = Modifier
                .width(menuWidth)
                .shadow(menuElevation, menuShape, clip = false)
                .border(width = 1.dp, color = menuDivider, shape = menuShape)
                .background(
                    color = menuBackground,
                    shape = menuShape
                )
                .padding(vertical = 12.dp)
                .testTag("installed_app_context_menu")
        ) {
            Text(
                text = appTitle,
                color = colorResource(R.color.gtv_quick_settings_text_primary),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = menuPadding, vertical = 10.dp)
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(menuDivider)
            )

            ContextMenuItem(
                title = "Open",
                modifier = Modifier.focusRequester(firstItemFocusRequester),
                onDismissRequest = dismissAndRestoreFocus,
                tileFocusRequester = tileFocusRequester
            ) {
                onAction(
                    LauncherAction.LaunchPackage(packageName)
                )
            }

            ContextMenuItem(
                title = "Move",
                onDismissRequest = dismissAndRestoreFocus,
                tileFocusRequester = tileFocusRequester
            ) {
                onAction(
                    LauncherAction.EnterAppMoveMode(packageName)
                )
            }

            ContextMenuItem(
                title = "App Info",
                onDismissRequest = dismissAndRestoreFocus,
                tileFocusRequester = tileFocusRequester
            ) {
                onAction(
                    LauncherAction.LaunchIntent(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ContextMenuItem(
    title: String,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    tileFocusRequester: FocusRequester,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val radius = dimensionResource(R.dimen.gtv_context_menu_radius)
    val horizontalPadding = dimensionResource(R.dimen.gtv_context_menu_padding)
    val focusedFill = colorResource(R.color.gtv_context_menu_focus_fill)
    val focusedText = colorResource(R.color.gtv_context_menu_focus_content)
    val defaultText = colorResource(R.color.gtv_quick_settings_text_primary)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .testTag("installed_app_context_menu_${title.lowercase().replace(" ", "_")}")
            .fillMaxWidth()
            .focusProperties { left = tileFocusRequester }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key == Key.DirectionLeft) {
                    onDismissRequest()
                    tileFocusRequester.requestFocus()
                    true
                } else {
                    false
                }
            }
            .gtvFocusScale(
                focusedScale = 1.01f,
                cornerRadius = radius,
                onFocus = { isFocused = it },
                onClick = {
                    onDismissRequest()
                    onClick()
                }
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .background(
                color = if (isFocused) focusedFill else Color.Transparent,
                shape = RoundedCornerShape(radius)
            )
            .padding(horizontal = horizontalPadding, vertical = 14.dp)
    ) {
        Text(
            text = title,
            color = if (isFocused) focusedText else defaultText,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun rememberContextMenuPositionProvider(anchorBounds: ContextMenuAnchorBounds): PopupPositionProvider {
    val density = LocalDensity.current
    val anchorMargin = with(density) { dimensionResource(R.dimen.gtv_context_menu_anchor_margin).roundToPx() }
    val verticalOffset = with(density) { dimensionResource(R.dimen.gtv_context_menu_anchor_vertical_offset).roundToPx() }
    val visualAnchor = remember(anchorBounds) { anchorBounds.toVisualBounds() }

    return remember(anchorBounds, visualAnchor, anchorMargin, verticalOffset) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBoundsInWindow: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val source = if (visualAnchor != IntRect.Zero) visualAnchor else anchorBoundsInWindow
                val preferredX = source.right - popupContentSize.width
                val preferredY = source.centerY() - (popupContentSize.height / 2) + verticalOffset
                val x = preferredX.coerceIn(
                    anchorMargin,
                    windowSize.width - popupContentSize.width - anchorMargin
                )
                val y = preferredY.coerceIn(
                    anchorMargin,
                    windowSize.height - popupContentSize.height - anchorMargin
                )
                return IntOffset(x, y)
            }
        }
    }
}

internal fun ContextMenuAnchorBounds.toVisualBounds(): IntRect {
    if (bounds == IntRect.Zero || focusedScale <= 1f) return bounds

    val centerX = (bounds.left + bounds.right) / 2f
    val centerY = (bounds.top + bounds.bottom) / 2f
    val halfWidth = bounds.width / 2f * focusedScale
    val halfHeight = bounds.height / 2f * focusedScale

    return IntRect(
        left = (centerX - halfWidth).toInt(),
        top = (centerY - halfHeight).toInt(),
        right = (centerX + halfWidth).toInt(),
        bottom = (centerY + halfHeight).toInt()
    )
}

private fun IntRect.centerY(): Int = top + (height / 2)
