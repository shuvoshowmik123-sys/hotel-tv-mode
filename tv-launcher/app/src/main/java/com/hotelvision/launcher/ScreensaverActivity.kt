package com.hotelvision.launcher

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.hotelvision.launcher.performance.LauncherImageKind
import com.hotelvision.launcher.performance.LocalLauncherPerformanceProfile
import com.hotelvision.launcher.performance.buildLauncherImageRequest
import com.hotelvision.launcher.ui.theme.HotelVisionTheme
import com.hotelvision.launcher.ui.theme.OutfitFamily
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val KEN_BURNS_DURATION_MS = 12_000
private const val IMAGE_HOLD_MS          = 8_000L
private const val CROSSFADE_MS           = 1_500L

class ScreensaverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureImmersiveWindow()

        setContent {
            HotelVisionTheme {
                val images = intent.getStringArrayListExtra(EXTRA_IMAGES) ?: arrayListOf()
                Log.d("Screensaver", "Received ${images.size} images: $images")
                ScreensaverScreen(images = images)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        finish()
        overridePendingTransition(0, android.R.anim.fade_out)
        return true
    }

    private fun configureImmersiveWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_IMAGES = "extra_screensaver_images"
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ScreensaverScreen(images: List<String>) {
    val performanceProfile = LocalLauncherPerformanceProfile.current
    val safeImages = if (images.isNotEmpty()) images else listOf(
        "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=1800&q=80",
        "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1800&q=80",
        "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?auto=format&fit=crop&w=1800&q=80"
    )

    var currentIndex by remember { mutableIntStateOf(0) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var crossfadeAlpha by remember { mutableFloatStateOf(1f) }
    var screenWidthPx by remember { mutableFloatStateOf(0f) }
    var kenBurnsKey by remember { mutableIntStateOf(0) }
    var imageLoadSuccess by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(safeImages) {
        while (true) {
            delay(IMAGE_HOLD_MS - CROSSFADE_MS)
            crossfadeAlpha = 0f
            delay(CROSSFADE_MS)
            previousIndex = currentIndex
            currentIndex = (currentIndex + 1) % safeImages.size
            kenBurnsKey++
            crossfadeAlpha = 1f
        }
    }

    val kenBurns = rememberInfiniteTransition(label = "KenBurns_$kenBurnsKey")
    val panX by kenBurns.animateFloat(
        initialValue = -0.04f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(KEN_BURNS_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PanX"
    )
    val scale by kenBurns.animateFloat(
        initialValue = 1.08f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(KEN_BURNS_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "KenBurnsScale"
    )

    val currentImageRequest = remember(currentIndex, safeImages, performanceProfile) {
        buildLauncherImageRequest(
            context = context,
            data = safeImages[currentIndex],
            imageKind = LauncherImageKind.BACKDROP,
            profile = performanceProfile
        )
    }

    val previousImageRequest = remember(previousIndex, safeImages, performanceProfile) {
        buildLauncherImageRequest(
            context = context,
            data = safeImages[previousIndex],
            imageKind = LauncherImageKind.BACKDROP,
            profile = performanceProfile
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { screenWidthPx = it.width.toFloat() }
    ) {
        AnimatedGradientBackground()

        if (imageLoadSuccess) {
            if (crossfadeAlpha < 1f) {
                AsyncImage(
                    model = previousImageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = panX * screenWidthPx
                            alpha = 1f - crossfadeAlpha
                        }
                )
            }

            AsyncImage(
                model = currentImageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = panX * screenWidthPx
                        alpha = crossfadeAlpha
                    }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color(0xCC000000),
                            Color(0xF2000000)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        ClockOverlay(modifier = Modifier.align(Alignment.BottomStart).padding(48.dp, 40.dp))
    }
}

@Composable
private fun AnimatedGradientBackground() {
    val transition = rememberInfiniteTransition(label = "GradientTransition")
    val colorPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ColorPhase"
    )

    val colors = listOf(
        Color(0xFF0F0C29),
        Color(0xFF302B63),
        Color(0xFF24243E),
        Color(0xFF1A1A2E),
        Color(0xFF16213E),
        Color(0xFF0F3460)
    )

    val startIndex = (colorPhase * (colors.size - 1)).toInt().coerceIn(0, colors.size - 2)
    val endIndex = (startIndex + 1).coerceAtMost(colors.size - 1)
    val fraction = (colorPhase * (colors.size - 1)) - startIndex

    val c1 = colors[startIndex]
    val c2 = colors[endIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(c1, c2),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ClockOverlay(modifier: Modifier = Modifier) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    var time by remember { mutableStateOf(timeFormat.format(Date())) }
    var date by remember { mutableStateOf(dateFormat.format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            time = timeFormat.format(Date())
            date = dateFormat.format(Date())
        }
    }

    Column(modifier = modifier) {
        Text(
            text = time,
            fontFamily = OutfitFamily,
            fontWeight = FontWeight.Light,
            fontSize = 88.sp,
            color = Color.White,
            lineHeight = 88.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = date,
            fontFamily = OutfitFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            color = Color(0xCCFFFFFF)
        )
    }
}
