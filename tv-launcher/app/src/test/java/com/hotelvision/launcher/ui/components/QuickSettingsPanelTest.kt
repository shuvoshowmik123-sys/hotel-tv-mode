package com.hotelvision.launcher.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickSettingsPanelTest {

    @Test
    fun overlayEntryFocusDelayMillis_addsSettleDelayToSlideAnimation() {
        assertEquals(280L, overlayEntryFocusDelayMillis(220))
    }

    @Test
    fun quickSettingsItemTag_matchesComposeTestSelectorFormat() {
        assertEquals("quick_settings_item_network", quickSettingsItemTag("network"))
    }
}
