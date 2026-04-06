package com.hotelvision.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.hotelvision.launcher.R
import com.hotelvision.launcher.ui.GuestMessageItem
import com.hotelvision.launcher.ui.LauncherAction
import com.hotelvision.launcher.ui.theme.TvOverlayBodySize
import com.hotelvision.launcher.ui.theme.TvOverlayHeaderSpacing
import com.hotelvision.launcher.ui.theme.TvOverlayItemPaddingHorizontal
import com.hotelvision.launcher.ui.theme.TvOverlayItemPaddingVertical
import com.hotelvision.launcher.ui.theme.TvOverlayItemSpacing
import com.hotelvision.launcher.ui.theme.TvOverlayMetaSize
import com.hotelvision.launcher.ui.theme.TvOverlaySectionSpacing
import com.hotelvision.launcher.ui.theme.TvOverlaySubtitleSize
import com.hotelvision.launcher.ui.theme.TvOverlayTitleSize

@Composable
fun GuestMessagesOverlay(
    visible: Boolean,
    messages: List<GuestMessageItem>,
    panelWidth: Dp,
    firstItemFocusRequester: FocusRequester,
    onDismiss: () -> Unit,
    onAction: (LauncherAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val enterFadeMs = integerResource(R.integer.gtv_quick_settings_enter_fade_ms)
    val enterSlideMs = integerResource(R.integer.gtv_quick_settings_enter_slide_ms)
    val exitFadeMs = integerResource(R.integer.gtv_quick_settings_exit_fade_ms)
    val exitSlideMs = integerResource(R.integer.gtv_quick_settings_exit_slide_ms)
    val cornerRadius = dimensionResource(R.dimen.gtv_dashboard_corner_radius)
    val horizontalPadding = dimensionResource(R.dimen.gtv_dashboard_content_padding_horizontal)
    val verticalPadding = dimensionResource(R.dimen.gtv_dashboard_content_padding_vertical)
    val scrimColor = colorResource(R.color.gtv_quick_settings_scrim)
    val panelBackground = colorResource(R.color.gtv_quick_settings_panel_background)
    val panelBorder = colorResource(R.color.gtv_quick_settings_panel_border)
    val shape = RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius)

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(overlayEntryFocusDelayMillis(enterSlideMs))
            runCatching { firstItemFocusRequester.requestFocus() }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(enterFadeMs)) + slideInHorizontally(
            animationSpec = tween(enterSlideMs),
            initialOffsetX = { it }
        ),
        exit = fadeOut(animationSpec = tween(exitFadeMs)) + slideOutHorizontally(
            animationSpec = tween(exitSlideMs),
            targetOffsetX = { it }
        ),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("guest_messages_overlay")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(panelWidth)
                    .widthIn(min = dimensionResource(R.dimen.gtv_dashboard_min_width))
                    .fillMaxHeight()
                    .clip(shape)
                    .border(width = 1.dp, color = panelBorder, shape = shape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                panelBackground.copy(alpha = 0.98f),
                                panelBackground
                            )
                        )
                    )
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp && event.key == Key.DirectionLeft) {
                            onDismiss()
                            true
                        } else {
                            false
                        }
                    }
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                    .testTag("guest_messages_panel")
            ) {
                Text(
                    text = "Guest Messages",
                    color = colorResource(R.color.gtv_quick_settings_text_primary),
                    fontSize = TvOverlayTitleSize,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Reservations, concierge notes, and stay updates for this room",
                    color = colorResource(R.color.gtv_quick_settings_text_secondary),
                    fontSize = TvOverlaySubtitleSize,
                    modifier = Modifier.padding(top = TvOverlayHeaderSpacing)
                )
                Spacer(modifier = Modifier.height(TvOverlaySectionSpacing))

                messages.forEachIndexed { index, message ->
                    GuestMessageRow(
                        message = message,
                        focusRequester = if (index == 0) firstItemFocusRequester else null,
                        onClick = {
                            onDismiss()
                            onAction(message.action)
                        }
                    )
                    Spacer(modifier = Modifier.height(TvOverlayItemSpacing))
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GuestMessageRow(
    message: GuestMessageItem,
    focusRequester: FocusRequester?,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val highlightColor = colorResource(R.color.gtv_quick_settings_panel_highlight)
    val highlightContentColor = colorResource(R.color.gtv_quick_settings_panel_highlight_content)
    val itemBackgroundColor = colorResource(R.color.gtv_quick_settings_panel_item_background)
    val primaryTextColor = colorResource(R.color.gtv_quick_settings_text_primary)
    val secondaryTextColor = colorResource(R.color.gtv_quick_settings_text_secondary)
    val rowCornerRadius = dimensionResource(R.dimen.gtv_dashboard_corner_radius)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guest_message_item_${message.id}")
            .overlayPanelItemFocusable(
                focusRequester = focusRequester,
                cornerRadius = rowCornerRadius,
                focusedScale = 1.03f,
                onFocusChanged = { isFocused = it },
                onClick = onClick
            )
            .clip(RoundedCornerShape(rowCornerRadius))
            .background(if (isFocused) highlightColor else itemBackgroundColor)
            .padding(
                horizontal = TvOverlayItemPaddingHorizontal,
                vertical = TvOverlayItemPaddingVertical
            )
    ) {
        Text(
            text = message.title,
            color = if (isFocused) highlightContentColor else primaryTextColor,
            fontSize = TvOverlayBodySize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = message.body,
            color = if (isFocused) highlightContentColor.copy(alpha = 0.78f) else secondaryTextColor,
            fontSize = TvOverlayBodySize,
            modifier = Modifier.padding(top = TvOverlayHeaderSpacing),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${message.badge} | ${message.timestampLabel}",
            color = if (isFocused) highlightContentColor.copy(alpha = 0.7f) else secondaryTextColor,
            fontSize = TvOverlayMetaSize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = TvOverlayItemSpacing - 6.dp)
        )
    }
}
