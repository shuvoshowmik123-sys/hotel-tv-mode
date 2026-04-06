package com.hotelvision.launcher.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.hotelvision.launcher.ui.theme.GtvCardBackground

fun Modifier.shimmerLoadingAnimation(
    widthOfShadowBrush: Float = 500f,
    angle: Float = 270f,
    durationMillis: Int = 1200
): Modifier = composed {
    val shimmerColors = listOf(
        GtvCardBackground.copy(alpha = 0.4f),
        GtvCardBackground.copy(alpha = 0.8f),
        GtvCardBackground.copy(alpha = 0.4f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnimation by transition.animateFloat(
        initialValue = -widthOfShadowBrush,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnimation, y = 0.0f),
        end = Offset(x = translateAnimation + widthOfShadowBrush, y = angle)
    )

    this.background(brush)
}

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .shimmerLoadingAnimation()
    )
}
