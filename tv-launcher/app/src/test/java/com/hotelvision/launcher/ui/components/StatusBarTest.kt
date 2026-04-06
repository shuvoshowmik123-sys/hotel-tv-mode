package com.hotelvision.launcher.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusBarTest {

    @Test
    fun statusBar_refreshesOncePerMinute() {
        assertEquals(60_000L, STATUS_BAR_REFRESH_MS)
    }
}
