package com.hotelvision.launcher.ui.theme

import androidx.compose.ui.graphics.Color

// ── Existing Google TV extracted colors ────────────────────────────────────────
val GtvBackground       = Color(0xFF0F1115)
val GtvCardBackground   = Color(0xFF1E232C)
val GtvCardBackgroundHover = Color(0xFF2D333B)
val GtvFocusGlow        = Color(0xFFE8F0FE)
val GtvTextPrimary      = Color(0xFFE8EAED)
val GtvTextSecondary    = Color(0xFF9AA0A6)
val GtvAccentBlue       = Color(0xFF8AB4F8)
val GtvBadgeBackground  = Color(0x26E8EAED) // white 15% opacity

// ── Phase 5.1: Hotel TV Kinetic Design System Palette ─────────────────────────

// Background
val HotelBgPrimary      = Color(0xFF0F0F1A)  // root screen background

// Surface tokens (white-tinted glass)
val SurfaceCard         = Color(0x1AFFFFFF)  // unfocused card — white 10%
val SurfaceCardFocused  = Color(0x33FFFFFF)  // focused card  — white 20%

// Text hierarchy
val TextPrimary         = Color(0xFFFFFFFF)  // headlines, focused labels
val TextSecondary       = Color(0xB3FFFFFF)  // descriptions, metadata (70% white)
val TextTertiary        = Color(0x66FFFFFF)  // disabled / inactive (40% white)

// Navigation
val NavTabActivePill    = Color(0x33FFFFFF)  // focused tab pill
val NavTabActiveIndie   = Color(0xFFFFFFFF)  // 2dp underline indicator for active tab

// Scrim layers (for background image readability)
val ScreenScrimTop      = Color(0x99000000)  // 60% black — top of screen
val ScreenScrimBottom   = Color(0xCC000000)  // 80% black — bottom of screen

// Ambient light (global top-right radial)
val AmbientGlow         = Color(0x1AFFFFFF)  // white 10% — cinematic depth

// Quick Settings
val QsPanelBackground   = Color(0xE6141414)  // 90% dark panel
val QsTileResting       = Color(0x1AFFFFFF)  // white 10% tile resting
val QsTileFocused       = Color(0xFFFFFFFF)  // solid white tile focused
val QsTileFocusedText   = Color(0xFF141414)  // dark text on focused tile

// Availability badges
val BadgeAvailable      = Color(0xFF4CAF50)  // green — available now
val BadgeUnavailable    = Color(0xFF607D8B)  // grey — unavailable

