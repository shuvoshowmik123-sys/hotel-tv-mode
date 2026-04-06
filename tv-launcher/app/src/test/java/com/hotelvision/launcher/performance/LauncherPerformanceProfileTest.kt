package com.hotelvision.launcher.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPerformanceProfileTest {

    @Test
    fun resolveLauncherPerformanceProfile_returnsLowRamWhenForced() {
        val profile = resolveLauncherPerformanceProfile(
            forceLowRam = true,
            isLowRamDevice = false,
            memoryClassMb = 512
        )

        assertEquals(LauncherPerformanceProfile.LOW_RAM, profile)
    }

    @Test
    fun resolveLauncherPerformanceProfile_returnsLowRamWhenDeviceIsMarkedLowRam() {
        val profile = resolveLauncherPerformanceProfile(
            forceLowRam = false,
            isLowRamDevice = true,
            memoryClassMb = 512
        )

        assertEquals(LauncherPerformanceProfile.LOW_RAM, profile)
    }

    @Test
    fun resolveLauncherPerformanceProfile_returnsLowRamWhenMemoryFallsBelowThreshold() {
        val profile = resolveLauncherPerformanceProfile(
            forceLowRam = false,
            isLowRamDevice = false,
            memoryClassMb = 0
        )

        assertEquals(LauncherPerformanceProfile.LOW_RAM, profile)
    }

    @Test
    fun resolveLauncherPerformanceProfile_returnsStandardWhenDeviceIsNotConstrained() {
        val profile = resolveLauncherPerformanceProfile(
            forceLowRam = false,
            isLowRamDevice = false,
            memoryClassMb = 512
        )

        assertEquals(LauncherPerformanceProfile.STANDARD, profile)
    }

    @Test
    fun lowRamProfile_disablesExpensiveBackdropPolicies() {
        assertFalse(shouldUseFocusDrivenBackdrop(LauncherPerformanceProfile.LOW_RAM))
        assertFalse(shouldAnimateBackdrop(LauncherPerformanceProfile.LOW_RAM))
        assertTrue(shouldRotateBackdropSlideshow(LauncherPerformanceProfile.LOW_RAM))
        assertTrue(
            shouldAdvanceBackdropSlideshow(
                profile = LauncherPerformanceProfile.LOW_RAM,
                overlayOpen = false,
                imageCount = 3
            )
        )
        assertFalse(
            shouldAdvanceBackdropSlideshow(
                profile = LauncherPerformanceProfile.LOW_RAM,
                overlayOpen = true,
                imageCount = 3
            )
        )
    }

    @Test
    fun standardProfile_keepsBackdropPoliciesEnabled() {
        assertTrue(shouldUseFocusDrivenBackdrop(LauncherPerformanceProfile.STANDARD))
        assertTrue(shouldAnimateBackdrop(LauncherPerformanceProfile.STANDARD))
        assertTrue(shouldRotateBackdropSlideshow(LauncherPerformanceProfile.STANDARD))
    }

    @Test
    fun normalizedBackdropSlideshowImages_prefersAssetsAndDeduplicatesEntries() {
        val images = normalizedBackdropSlideshowImages(
            slideshowImages = listOf("  ", "https://img-1", "https://img-1", "https://img-2 "),
            fallbackImageUrl = "https://fallback"
        )

        assertEquals(listOf("https://img-1", "https://img-2"), images)
    }

    @Test
    fun normalizedBackdropSlideshowImages_fallsBackToHeroImageWhenAssetsAreEmpty() {
        val images = normalizedBackdropSlideshowImages(
            slideshowImages = emptyList(),
            fallbackImageUrl = " https://hero "
        )

        assertEquals(listOf("https://hero"), images)
    }

    @Test
    fun backdropSlideshowIntervalMs_usesSlowerTimingForLowRam() {
        assertEquals(18_000L, backdropSlideshowIntervalMs(LauncherPerformanceProfile.LOW_RAM))
        assertEquals(7_000L, backdropSlideshowIntervalMs(LauncherPerformanceProfile.STANDARD))
    }

    @Test
    fun shouldAdvanceBackdropSlideshow_requiresMoreThanOneImage() {
        assertFalse(
            shouldAdvanceBackdropSlideshow(
                profile = LauncherPerformanceProfile.LOW_RAM,
                overlayOpen = false,
                imageCount = 1
            )
        )
    }

    @Test
    fun shouldBringCardIntoView_onlyReturnsTrueWhenItemExceedsViewportBounds() {
        assertFalse(
            shouldBringCardIntoView(
                itemLeftPx = 40,
                itemTopPx = 40,
                itemRightPx = 200,
                itemBottomPx = 140,
                viewportWidthPx = 1920,
                viewportHeightPx = 1080
            )
        )

        assertTrue(
            shouldBringCardIntoView(
                itemLeftPx = -12,
                itemTopPx = 40,
                itemRightPx = 200,
                itemBottomPx = 140,
                viewportWidthPx = 1920,
                viewportHeightPx = 1080
            )
        )
    }
}
