package com.hotelvision.launcher.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hotelvision.launcher.performance.LauncherPerformanceProfile
import com.hotelvision.launcher.performance.LocalLauncherPerformanceProfile
import com.hotelvision.launcher.ui.theme.GtvFocusGlow
import com.hotelvision.launcher.ui.theme.TvCardAlphaFocused
import com.hotelvision.launcher.ui.theme.TvCardAlphaUnfocused
import com.hotelvision.launcher.ui.theme.TvCardScaleFocused
import com.hotelvision.launcher.ui.theme.TvFocusGainMs
import com.hotelvision.launcher.ui.theme.TvFocusLossMs

/**
 * Phase 1.1 — Hotel TV Universal Kinetic Focus Contract
 *
 * Unfocused: scale=1.0f, alpha=0.72f (resting/receding)
 * Focused:   scale=1.08f, alpha=1.0f (full brightness pop)
 *
 * Easing is asymmetric by design (Google TV Amati spec):
 *   - Focus GAIN: FastOutSlowInEasing, 200ms (snappy pop)
 *   - Focus LOSS: LinearOutSlowInEasing, 150ms (slightly faster release)
 *
 * ALL transforms go through graphicsLayer{} — never Modifier.scale().
 * graphicsLayer triggers GPU compositing; layout transforms cause CPU re-layout.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.gtvFocusScale(
    focusedScale: Float = TvCardScaleFocused,
    cornerRadius: Dp = 12.dp,
    borderWidth: Dp = 3.dp,
    focusedBorderColor: Color = GtvFocusGlow,
    enabled: Boolean = true,
    onFocus: ((Boolean) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val performanceProfile = LocalLauncherPerformanceProfile.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val effectiveFocusedScale = if (performanceProfile == LauncherPerformanceProfile.LOW_RAM) {
        minOf(focusedScale, 1.03f)
    } else {
        focusedScale
    }
    val effectiveUnfocusedAlpha = if (performanceProfile == LauncherPerformanceProfile.LOW_RAM) {
        0.84f
    } else {
        TvCardAlphaUnfocused
    }
    val effectiveBorderWidth = if (performanceProfile == LauncherPerformanceProfile.LOW_RAM) {
        0.dp
    } else {
        borderWidth
    }

    // ── Scale: 1.08f on focus, 1.0f at rest ─────────────────────────────────
    val scale by animateFloatAsState(
        targetValue = if (isFocused) effectiveFocusedScale else 1.0f,
        animationSpec = tween(
            durationMillis = if (isFocused) TvFocusGainMs else TvFocusLossMs,
            easing = if (isFocused) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "FocusScale"
    )

    // ── Alpha: 1.0f on focus, 0.72f at rest (receding effect) ───────────────
    val alpha by animateFloatAsState(
        targetValue = if (isFocused) TvCardAlphaFocused else effectiveUnfocusedAlpha,
        animationSpec = tween(
            durationMillis = if (isFocused) TvFocusGainMs else TvFocusLossMs,
            easing = if (isFocused) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "FocusAlpha"
    )

    // ── Focus ring border (for accessibility visibility) ─────────────────────
    val strokeWidth by animateFloatAsState(
        targetValue = if (isFocused) effectiveBorderWidth.value else 0f,
        animationSpec = tween(durationMillis = TvFocusLossMs),
        label = "StrokeWidth"
    )

    val focusCallback = remember(onFocus) { onFocus }

    this
        .then(
            if (onClick != null || onLongClick != null) {
                Modifier.combinedClickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,   // MANDATORY: kill default highlight
                    onClick = { onClick?.invoke() },
                    onLongClick = onLongClick
                )
            } else {
                Modifier.focusable(
                    enabled = enabled,
                    interactionSource = interactionSource
                )
            }
        )
        .focusProperties { canFocus = enabled }
        // MANDATORY: GPU compositing layer — never use Modifier.scale()
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .drawWithContent {
            drawContent()
            if (strokeWidth > 0f) {
                drawRoundRect(
                    color = focusedBorderColor.copy(alpha = if (isFocused) 0.7f else 0f),
                    style = Stroke(width = strokeWidth.dp.toPx()),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
        }
        .then(if (focusCallback != null) {
            Modifier.onFocusChanged { focusCallback(it.isFocused) }
        } else Modifier)
}
