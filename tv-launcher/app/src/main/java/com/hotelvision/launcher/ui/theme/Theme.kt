package com.hotelvision.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import com.hotelvision.launcher.R

// ── Outfit — Google Sans substitute ─────────────────────────────────────────────
// Outfit is a geometric humanist sans with the same rounded, modern DNA as Google
// Sans. It is loaded via Android's downloadable fonts API (no TTF bundled).
val OutfitFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold),
)

@OptIn(ExperimentalTvMaterial3Api::class)
private val DarkColors = darkColorScheme(
    background = HotelBgPrimary,
    surface = SurfaceCard,
    surfaceVariant = SurfaceCardFocused,
    primary = GtvAccentBlue,
    secondary = GtvFocusGlow,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

/**
 * Phase 5.2 — Hotel TV typography scale.
 * All sizes from TvUiTokens. FontFamily = Outfit (Google Sans substitute).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
private val HotelTvTypography = Typography(
    displayLarge  = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Bold,     fontSize = TvTypeDisplay),
    displayMedium = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Bold,     fontSize = TvTypeDisplay),
    displaySmall  = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = TvTypeHeadline),
    headlineLarge = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = TvTypeHeadline),
    headlineMedium= TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Medium,   fontSize = TvTypeHeadline),
    headlineSmall = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Medium,   fontSize = TvTypeTitle),
    titleLarge    = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = TvTypeTitle),
    titleMedium   = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Medium,   fontSize = TvTypeBody),
    titleSmall    = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Medium,   fontSize = TvTypeCaption),
    bodyLarge     = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Normal,   fontSize = TvTypeBody),
    bodyMedium    = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Normal,   fontSize = TvTypeCaption),
    bodySmall     = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Normal,   fontSize = TvTypeCaption),
    labelLarge    = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = TvTypeBody),
    labelMedium   = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Medium,   fontSize = TvTypeCaption),
    labelSmall    = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Normal,   fontSize = TvTypeCaption),
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HotelVisionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = HotelTvTypography,
        content = content
    )
}

