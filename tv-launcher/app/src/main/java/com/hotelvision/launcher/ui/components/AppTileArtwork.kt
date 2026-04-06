package com.hotelvision.launcher.ui.components

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil3.compose.AsyncImage
import com.hotelvision.launcher.performance.LocalLauncherPerformanceProfile
import com.hotelvision.launcher.performance.shouldExtractAppDominantColor
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun extractDominantColor(drawable: Drawable): Color? = withContext(Dispatchers.Default) {
    try {
        // Strategy 1: Adaptive icon — extract background drawable color directly
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
            && drawable is android.graphics.drawable.AdaptiveIconDrawable
        ) {
            val bgDrawable = drawable.background
            if (bgDrawable is android.graphics.drawable.ColorDrawable) {
                return@withContext Color(bgDrawable.color)
            }
            // Sample the background layer bitmap
            val bgBitmap = bgDrawable.toBitmap(
                width = 80, height = 80,
                config = android.graphics.Bitmap.Config.ARGB_8888
            )
            val color = sampleCenterColor(bgBitmap)
            bgBitmap.recycle()
            if (color != null) return@withContext color
        }

        // Strategy 2: Legacy icon — sample center region (where background lives)
        val bitmap = drawable.toBitmap(
            width = 80, height = 80,
            config = android.graphics.Bitmap.Config.ARGB_8888
        )
        val color = sampleCenterColor(bitmap)
        bitmap.recycle()
        color
    } catch (e: Exception) {
        null
    }
}

private fun sampleCenterColor(bitmap: android.graphics.Bitmap): Color? {
    val w = bitmap.width
    val h = bitmap.height
    val colorVotes = mutableMapOf<Int, Int>()

    // Sample center 50% area — this is where the icon background lives
    val startX = (w * 0.25f).toInt()
    val endX = (w * 0.75f).toInt()
    val startY = (h * 0.25f).toInt()
    val endY = (h * 0.75f).toInt()

    for (y in startY until endX) {
        for (x in startX until endX) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = android.graphics.Color.alpha(pixel)
            if (alpha > 128) {
                // Round to reduce noise — snap to nearest 16 per channel
                val r = (android.graphics.Color.red(pixel) / 16) * 16
                val g = (android.graphics.Color.green(pixel) / 16) * 16
                val b = (android.graphics.Color.blue(pixel) / 16) * 16
                val quantized = android.graphics.Color.rgb(r, g, b)
                colorVotes[quantized] = (colorVotes[quantized] ?: 0) + 1
            }
        }
    }

    if (colorVotes.isEmpty()) return null

    val best = colorVotes.maxByOrNull { it.value } ?: return null
    val c = best.key
    return Color(c)
}

sealed interface AppTileArtwork {
    data class Banner(val drawable: Drawable) : AppTileArtwork
    data class CompositedLogo(val drawable: Drawable) : AppTileArtwork
    data class CompositedIcon(val drawable: Drawable) : AppTileArtwork
    data object GenericFallback : AppTileArtwork
}

private data class InstalledAppArtworkCacheKey(
    val packageName: String,
    val launchActivityClassName: String?
)

private object InstalledAppArtworkRepository {
    private val artworkCache = ConcurrentHashMap<InstalledAppArtworkCacheKey, AppTileArtwork>()
    private val dominantColorCache = ConcurrentHashMap<InstalledAppArtworkCacheKey, Color?>()

    suspend fun loadArtwork(
        context: Context,
        packageName: String?,
        launchActivityClassName: String?
    ): AppTileArtwork = withContext(Dispatchers.IO) {
        if (packageName.isNullOrBlank()) {
            return@withContext AppTileArtwork.GenericFallback
        }

        val cacheKey = InstalledAppArtworkCacheKey(
            packageName = packageName,
            launchActivityClassName = launchActivityClassName
        )
        artworkCache[cacheKey] ?: resolveInstalledAppTileArtwork(
            context = context,
            packageName = packageName,
            launchActivityClassName = launchActivityClassName
        ).also { artworkCache[cacheKey] = it }
    }

    suspend fun loadDominantColor(
        cacheKey: InstalledAppArtworkCacheKey,
        artwork: AppTileArtwork
    ): Color? {
        val drawable = when (artwork) {
            is AppTileArtwork.CompositedLogo -> artwork.drawable
            is AppTileArtwork.CompositedIcon -> artwork.drawable
            else -> return null
        }

        dominantColorCache[cacheKey]?.let { return it }
        return extractDominantColor(drawable)?.also { color ->
            dominantColorCache[cacheKey] = color
        }
    }

    fun invalidate(packageName: String?) {
        if (packageName.isNullOrBlank()) {
            artworkCache.clear()
            dominantColorCache.clear()
            return
        }

        val keysToRemove = artworkCache.keys.filter { it.packageName == packageName }
        keysToRemove.forEach { key ->
            artworkCache.remove(key)
            dominantColorCache.remove(key)
        }
    }
}

fun invalidateInstalledAppArtworkCache(packageName: String?) {
    InstalledAppArtworkRepository.invalidate(packageName)
}

@Composable
fun InstalledAppTileArtwork(
    packageName: String?,
    launchActivityClassName: String?,
    contentDescription: String,
    stableId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val performanceProfile = LocalLauncherPerformanceProfile.current
    val cacheKey = remember(packageName, launchActivityClassName) {
        InstalledAppArtworkCacheKey(
            packageName = packageName.orEmpty(),
            launchActivityClassName = launchActivityClassName
        )
    }
    val artwork by produceState<AppTileArtwork>(
        initialValue = AppTileArtwork.GenericFallback,
        key1 = context,
        key2 = cacheKey
    ) {
        value = InstalledAppArtworkRepository.loadArtwork(
            context = context,
            packageName = packageName,
            launchActivityClassName = launchActivityClassName
        )
    }
    val dominantColor by produceState<Color?>(
        initialValue = null,
        key1 = cacheKey,
        key2 = artwork,
        key3 = performanceProfile
    ) {
        value = if (shouldExtractAppDominantColor(performanceProfile)) {
            InstalledAppArtworkRepository.loadDominantColor(
                cacheKey = cacheKey,
                artwork = artwork
            )
        } else {
            null
        }
    }

    InstalledAppTileArtwork(
        artwork = artwork,
        contentDescription = contentDescription,
        stableId = stableId,
        dominantColor = dominantColor,
        modifier = modifier
    )
}

@Composable
fun InstalledAppTileArtwork(
    artwork: AppTileArtwork,
    contentDescription: String,
    stableId: String,
    dominantColor: Color? = null,
    modifier: Modifier = Modifier
) {
    when (artwork) {
        is AppTileArtwork.Banner -> {
            AsyncImage(
                model = artwork.drawable,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .fillMaxSize()
                    .testTag(artworkTag(stableId, "banner"))
            )
        }

        is AppTileArtwork.CompositedLogo -> {
            SyntheticInstalledAppTile(
                dominantColor = dominantColor,
                modifier = modifier
                    .fillMaxSize()
                    .testTag(artworkTag(stableId, "logo"))
            ) {
                AsyncImage(
                    model = artwork.drawable,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .fillMaxHeight(0.65f)
                )
            }
        }

        is AppTileArtwork.CompositedIcon -> {
            SyntheticInstalledAppTile(
                dominantColor = dominantColor,
                modifier = modifier
                    .fillMaxSize()
                    .testTag(artworkTag(stableId, "icon"))
            ) {
                BoxWithConstraints {
                    val iconSize = minOf(maxWidth, maxHeight) * 0.72f
                    AsyncImage(
                        model = artwork.drawable,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }

        AppTileArtwork.GenericFallback -> {
            SyntheticInstalledAppTile(
                modifier = modifier
                    .fillMaxSize()
                    .testTag(artworkTag(stableId, "generic"))
            ) {
                GenericInstalledAppGlyph()
            }
        }
    }
}

fun resolveInstalledAppTileArtwork(
    activityBanner: Drawable?,
    applicationBanner: Drawable?,
    applicationLogo: Drawable?,
    applicationIcon: Drawable?
): AppTileArtwork {
    return when {
        applicationIcon != null -> AppTileArtwork.CompositedIcon(applicationIcon)
        applicationLogo != null -> AppTileArtwork.CompositedLogo(applicationLogo)
        applicationBanner != null -> AppTileArtwork.CompositedIcon(applicationBanner)
        activityBanner != null -> AppTileArtwork.CompositedIcon(activityBanner)
        else -> AppTileArtwork.GenericFallback
    }
}

fun resolveInstalledAppTileArtwork(
    context: Context,
    packageName: String?,
    launchActivityClassName: String?
): AppTileArtwork {
    if (packageName.isNullOrBlank()) {
        return AppTileArtwork.GenericFallback
    }

    val packageManager = context.packageManager
    val activityBanner = launchActivityClassName
        ?.takeIf { it.isNotBlank() }
        ?.let { className ->
            runCatching {
                packageManager.getActivityBanner(ComponentName(packageName, className))
            }.getOrNull()
        }

    val applicationBanner = runCatching {
        packageManager.getApplicationBanner(packageName)
    }.getOrNull()

    val applicationLogo = runCatching {
        packageManager.getApplicationLogo(packageName)
    }.getOrNull()

    val applicationIcon = runCatching {
        packageManager.getApplicationIcon(packageName)
    }.getOrNull()

    return resolveInstalledAppTileArtwork(
        activityBanner = activityBanner,
        applicationBanner = applicationBanner,
        applicationLogo = applicationLogo,
        applicationIcon = applicationIcon
    )
}

@Composable
private fun SyntheticInstalledAppTile(
    modifier: Modifier = Modifier,
    dominantColor: Color? = null,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val backgroundModifier = if (dominantColor != null) {
        Modifier.background(dominantColor)
    } else {
        Modifier.background(
            Brush.linearGradient(
                colors = listOf(Color(0xFF22354A), Color(0xFF101923))
            )
        )
    }

    BoxWithConstraints(
        modifier = modifier.then(backgroundModifier)
    ) {
        val scope = this
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            with(scope) {
                content()
            }
        }
    }
}

@Composable
private fun GenericInstalledAppGlyph(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlyphCell()
            GlyphCell()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlyphCell()
            GlyphCell()
        }
    }
}

@Composable
private fun GlyphCell() {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.72f))
    )
}

fun artworkTag(stableId: String, variant: String): String {
    return "installed_app_art_${variant}_$stableId"
}
