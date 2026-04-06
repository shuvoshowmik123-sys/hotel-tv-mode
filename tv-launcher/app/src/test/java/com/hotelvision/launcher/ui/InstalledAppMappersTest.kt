package com.hotelvision.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class InstalledAppMappersTest {

    @Test
    fun toInstalledAppHomeCard_preservesLaunchActivityClassName() {
        val app = InstalledAppItem(
            id = "com.example.avex",
            packageName = "com.example.avex",
            launchActivityClassName = "com.example.avex.MainActivity",
            title = "Avex TV",
            subtitle = "Installed TV app",
            badge = "ATV",
            isSystemApp = true,
            action = LauncherAction.None
        )

        val card = app.toInstalledAppHomeCard()

        assertEquals("com.example.avex", card.packageName)
        assertEquals("com.example.avex.MainActivity", card.launchActivityClassName)
        assertEquals(LauncherCardType.APP, card.cardType)
        assertEquals(0xFF365BDE, card.accentColor)
    }
}
