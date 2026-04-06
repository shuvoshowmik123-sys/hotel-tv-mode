package com.hotelvision.launcher.ui.components

import android.graphics.drawable.ColorDrawable
import org.junit.Assert.assertTrue
import org.junit.Test

class AppTileArtworkTest {

    @Test
    fun resolveInstalledAppTileArtwork_prefersActivityBanner() {
        val artwork = resolveInstalledAppTileArtwork(
            activityBanner = ColorDrawable(0xFF0000),
            applicationBanner = ColorDrawable(0x00FF00),
            applicationLogo = ColorDrawable(0x0000FF),
            applicationIcon = ColorDrawable(0xFFFFFF)
        )

        assertTrue(artwork is AppTileArtwork.Banner)
    }

    @Test
    fun resolveInstalledAppTileArtwork_usesApplicationBannerWhenActivityBannerMissing() {
        val artwork = resolveInstalledAppTileArtwork(
            activityBanner = null,
            applicationBanner = ColorDrawable(0x00FF00),
            applicationLogo = ColorDrawable(0x0000FF),
            applicationIcon = ColorDrawable(0xFFFFFF)
        )

        assertTrue(artwork is AppTileArtwork.Banner)
    }

    @Test
    fun resolveInstalledAppTileArtwork_usesLogoWhenNoBannerExists() {
        val artwork = resolveInstalledAppTileArtwork(
            activityBanner = null,
            applicationBanner = null,
            applicationLogo = ColorDrawable(0x0000FF),
            applicationIcon = ColorDrawable(0xFFFFFF)
        )

        assertTrue(artwork is AppTileArtwork.CompositedLogo)
    }

    @Test
    fun resolveInstalledAppTileArtwork_usesIconWhenLogoMissing() {
        val artwork = resolveInstalledAppTileArtwork(
            activityBanner = null,
            applicationBanner = null,
            applicationLogo = null,
            applicationIcon = ColorDrawable(0xFFFFFF)
        )

        assertTrue(artwork is AppTileArtwork.CompositedIcon)
    }

    @Test
    fun resolveInstalledAppTileArtwork_fallsBackToGenericGlyphWhenNoArtworkExists() {
        val artwork = resolveInstalledAppTileArtwork(
            activityBanner = null,
            applicationBanner = null,
            applicationLogo = null,
            applicationIcon = null
        )

        assertTrue(artwork is AppTileArtwork.GenericFallback)
    }
}
