package com.hotelvision.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherScreenReorderTest {

    @Test
    fun reorderAppPackages_movesItemRightWithinRow() {
        val apps = listOf(
            testApp("one"),
            testApp("two"),
            testApp("three")
        )

        val reordered = reorderAppPackages(apps, "one", AppMoveDirection.RIGHT, columns = 5)

        assertEquals(listOf("two", "one", "three"), reordered)
    }

    @Test
    fun reorderAppPackages_movesItemDownByGridColumnCount() {
        val apps = listOf(
            testApp("one"),
            testApp("two"),
            testApp("three"),
            testApp("four"),
            testApp("five"),
            testApp("six")
        )

        val reordered = reorderAppPackages(apps, "one", AppMoveDirection.DOWN, columns = 5)

        assertEquals(listOf("two", "three", "four", "five", "six", "one"), reordered)
    }

    @Test
    fun reorderAppPackages_returnsNullWhenPackageIsMissing() {
        val reordered = reorderAppPackages(listOf(testApp("one")), "missing", AppMoveDirection.LEFT, columns = 5)

        assertNull(reordered)
    }

    @Test
    fun calculateAdaptiveAppGridColumns_prefersLargerTilesOnStandardWidths() {
        assertEquals(4, calculateAdaptiveAppGridColumns(960))
        assertEquals(5, calculateAdaptiveAppGridColumns(1280))
        assertEquals(6, calculateAdaptiveAppGridColumns(1680))
    }

    @Test
    fun calculateDestinationTransitionDirection_tracksHorizontalNavDirection() {
        assertEquals(1, calculateDestinationTransitionDirection(LauncherDestination.HOME, LauncherDestination.ROOM_SERVICE))
        assertEquals(-1, calculateDestinationTransitionDirection(LauncherDestination.LOCAL_GUIDE, LauncherDestination.ROOM_SERVICE))
    }

    private fun testApp(packageName: String): InstalledAppItem {
        return InstalledAppItem(
            id = packageName,
            packageName = packageName,
            title = packageName,
            subtitle = "",
            badge = packageName.take(2).uppercase(),
            isSystemApp = false,
            action = LauncherAction.None
        )
    }
}
