package com.hotelvision.launcher.performance

import android.content.Context
import android.app.ActivityManager
import androidx.compose.runtime.staticCompositionLocalOf
import coil3.request.ImageRequest
import coil3.size.Size

enum class LauncherPerformanceProfile {
    STANDARD,
    LOW_RAM
}

enum class LauncherImageKind {
    BACKDROP,
    CARD
}

const val FORCE_LOW_RAM_LAUNCHER_PROFILE = false
private const val LOW_RAM_THRESHOLD_MB = 512L

val LocalLauncherPerformanceProfile = staticCompositionLocalOf {
    LauncherPerformanceProfile.LOW_RAM
}

val STANDARD_TV_IMAGE_SIZE = Size(1280, 720)
val LOW_RAM_TV_IMAGE_SIZE = Size(960, 540)
val STANDARD_TV_CARD_THUMBNAIL_SIZE = Size(320, 180)
val LOW_RAM_TV_CARD_THUMBNAIL_SIZE = Size(240, 135)
const val STANDARD_BACKDROP_SLIDESHOW_INTERVAL_MS = 7_000L
const val LOW_RAM_BACKDROP_SLIDESHOW_INTERVAL_MS = 18_000L

internal fun resolveLauncherPerformanceProfile(
    forceLowRam: Boolean = FORCE_LOW_RAM_LAUNCHER_PROFILE,
    isLowRamDevice: Boolean = false,
    memoryClassMb: Int = Int.MAX_VALUE
): LauncherPerformanceProfile {
    return if (forceLowRam || isLowRamDevice || memoryClassMb <= 192) {
        LauncherPerformanceProfile.LOW_RAM
    } else {
        LauncherPerformanceProfile.STANDARD
    }
}

fun resolveLauncherPerformanceProfile(context: Context): LauncherPerformanceProfile {
    return resolveLauncherPerformanceProfile(
        isLowRamDevice = isLowRamDevice(context),
        memoryClassMb = if (isUnderMemoryThreshold(context)) 0 else Int.MAX_VALUE
    )
}

fun isLowRamDevice(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return activityManager?.isLowRamDevice == true
}

fun isUnderMemoryThreshold(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    return (memoryInfo.totalMem / (1024 * 1024)) < LOW_RAM_THRESHOLD_MB
}

fun shouldUseFocusDrivenBackdrop(profile: LauncherPerformanceProfile): Boolean {
    return profile == LauncherPerformanceProfile.STANDARD
}

fun shouldAnimateDestinationTransitions(profile: LauncherPerformanceProfile): Boolean {
    return profile == LauncherPerformanceProfile.STANDARD
}

fun shouldAnimateBackdrop(profile: LauncherPerformanceProfile): Boolean {
    return profile == LauncherPerformanceProfile.STANDARD
}

fun shouldRotateBackdropSlideshow(profile: LauncherPerformanceProfile): Boolean {
    return true
}

fun shouldAdvanceBackdropSlideshow(
    profile: LauncherPerformanceProfile,
    overlayOpen: Boolean,
    imageCount: Int
): Boolean {
    return shouldRotateBackdropSlideshow(profile) && !overlayOpen && imageCount > 1
}

fun backdropSlideshowIntervalMs(profile: LauncherPerformanceProfile): Long {
    return if (profile == LauncherPerformanceProfile.LOW_RAM) {
        LOW_RAM_BACKDROP_SLIDESHOW_INTERVAL_MS
    } else {
        STANDARD_BACKDROP_SLIDESHOW_INTERVAL_MS
    }
}

fun normalizedBackdropSlideshowImages(
    slideshowImages: List<String>,
    fallbackImageUrl: String
): List<String> {
    val normalized = slideshowImages
        .asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .toList()

    if (normalized.isNotEmpty()) return normalized

    val fallback = fallbackImageUrl.trim()
    return if (fallback.isNotBlank()) listOf(fallback) else emptyList()
}

fun shouldAnimateEntrance(profile: LauncherPerformanceProfile): Boolean {
    return profile == LauncherPerformanceProfile.STANDARD
}

fun shouldAnimateCardMetadata(profile: LauncherPerformanceProfile): Boolean {
    return profile == LauncherPerformanceProfile.STANDARD
}

fun shouldExtractAppDominantColor(profile: LauncherPerformanceProfile): Boolean {
    return true
}

fun shouldAnimateRowAlignment(profile: LauncherPerformanceProfile): Boolean {
    return profile == LauncherPerformanceProfile.STANDARD
}

fun shouldBringCardIntoView(
    itemLeftPx: Int,
    itemTopPx: Int,
    itemRightPx: Int,
    itemBottomPx: Int,
    viewportWidthPx: Int,
    viewportHeightPx: Int,
    edgePaddingPx: Int = 24
): Boolean {
    if (viewportWidthPx <= 0 || viewportHeightPx <= 0) return false
    return itemLeftPx < edgePaddingPx ||
        itemTopPx < edgePaddingPx ||
        itemRightPx > viewportWidthPx - edgePaddingPx ||
        itemBottomPx > viewportHeightPx - edgePaddingPx
}

fun buildLauncherImageRequest(
    context: Context,
    data: Any?,
    imageKind: LauncherImageKind,
    profile: LauncherPerformanceProfile
): ImageRequest {
    val size = when (imageKind) {
        LauncherImageKind.BACKDROP -> if (profile == LauncherPerformanceProfile.LOW_RAM) {
            LOW_RAM_TV_IMAGE_SIZE
        } else {
            STANDARD_TV_IMAGE_SIZE
        }

        LauncherImageKind.CARD -> if (profile == LauncherPerformanceProfile.LOW_RAM) {
            LOW_RAM_TV_CARD_THUMBNAIL_SIZE
        } else {
            STANDARD_TV_CARD_THUMBNAIL_SIZE
        }
    }

    return ImageRequest.Builder(context)
        .data(data)
        .size(size)
        .build()
}
