package com.hotelvision.launcher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Replicates the tv_easing_enter.xml and kids_slide_enter_animation.xml 
 * from the Google TV launcher.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    delayMillisPerItem: Int = 80,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val alphaAnim = remember { Animatable(0f) }
    val translationYAnim = remember { Animatable(50f) } // Slide up from 50dp

    LaunchedEffect(Unit) {
        val delay = index * delayMillisPerItem
        
        // Staggered fade in
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 400,
                delayMillis = delay,
                easing = FastOutSlowInEasing
            )
        )
    }

    LaunchedEffect(Unit) {
        val delay = index * delayMillisPerItem
        
        // Staggered slide up
        translationYAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 500,
                delayMillis = delay,
                easing = FastOutSlowInEasing
            )
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = alphaAnim.value
            translationY = translationYAnim.value.dp.toPx()
        }
    ) {
        content()
    }
}
