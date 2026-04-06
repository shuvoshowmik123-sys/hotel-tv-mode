package com.hotelvision.launcher.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Spacing tokens ─────────────────────────────────────────────────────────────
val TvScreenHorizontalPadding   = 32.dp
val TvScreenVerticalPadding     = 12.dp
val TvTopNavItemSpacing         = 10.dp
val TvTopNavActionSpacing       = 12.dp
val TvSectionSpacing            = 16.dp
val TvRowSpacing                = 14.dp
val TvSurfaceTopPadding         = 16.dp
val TvSurfaceBottomPadding      = 60.dp
val TvRowTitleSafeTop           = 48.dp
val TvOverlayHeaderSpacing      = 8.dp
val TvOverlaySectionSpacing     = 20.dp
val TvOverlayItemSpacing        = 12.dp
val TvOverlayItemPaddingHorizontal = 16.dp
val TvOverlayItemPaddingVertical   = 12.dp

// ── Typography tokens ──────────────────────────────────────────────────────────
val TvNavLabelSize              = 14.sp
val TvWelcomeSubtitleSize       = 15.sp
val TvWelcomeSupportingSize     = 13.sp
val TvSurfaceTitleSize          = 18.sp
val TvSurfaceSubtitleSize       = 14.sp
val TvOverlayTitleSize          = 24.sp
val TvOverlaySubtitleSize       = 15.sp
val TvOverlayBodySize           = 14.sp
val TvOverlayMetaSize           = 13.sp
val TvQuickSettingsLabelSize    = 12.sp

// Phase 5.2 typography scale (matches master directive)
val TvTypeDisplay               = 26.sp   // hero section title
val TvTypeHeadline              = 20.sp   // section headers, menu category
val TvTypeTitle                 = 15.sp   // card dish names, tab labels
val TvTypeBody                  = 12.sp   // descriptions, subtitles
val TvTypeCaption               = 10.sp   // metadata, time labels, badges
val TvTypeClock                 = 22.sp   // top bar clock

// ── Phase 1: Kinetic focus contract tokens ─────────────────────────────────────
const val TvCardAlphaUnfocused  = 0.72f   // resting alpha — receding
const val TvCardAlphaFocused    = 1.0f    // focused alpha — full brightness
const val TvCardScaleFocused    = 1.06f   // scale on focus gain
const val TvCardScaleResting    = 1.0f    // resting scale

val TvCardElevationFocused      = 6.dp    // shadow when focused
val TvCardElevationResting      = 1.dp    // shadow at rest

// Animation durations (asymmetric — snappy to focus, faster to release)
const val TvFocusGainMs         = 180     // focus gain duration
const val TvFocusLossMs         = 120     // focus loss duration
const val TvContentSlideMs      = 260     // AnimatedContent slide
const val TvContentFadeMs       = 160     // AnimatedContent fade
