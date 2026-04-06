package com.hotelvision.launcher.ui.components

import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertEquals
import org.junit.Test

class InstalledAppContextMenuTest {

    @Test
    fun toVisualBounds_expandsBoundsAroundCenterForFocusedScale() {
        val visualBounds = ContextMenuAnchorBounds(
            bounds = IntRect(left = 100, top = 200, right = 260, bottom = 290),
            focusedScale = 1.05f
        ).toVisualBounds()

        assertEquals(IntRect(left = 96, top = 197, right = 264, bottom = 292), visualBounds)
    }

    @Test
    fun toVisualBounds_returnsOriginalBoundsWhenScaleIsNeutral() {
        val originalBounds = IntRect(left = 40, top = 60, right = 140, bottom = 120)

        val visualBounds = ContextMenuAnchorBounds(
            bounds = originalBounds,
            focusedScale = 1f
        ).toVisualBounds()

        assertEquals(originalBounds, visualBounds)
    }
}
