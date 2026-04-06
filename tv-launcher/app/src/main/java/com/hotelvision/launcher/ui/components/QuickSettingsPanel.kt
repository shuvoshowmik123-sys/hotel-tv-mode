package com.hotelvision.launcher.ui.components

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.SignalWifi4Bar
import androidx.compose.material.icons.outlined.SignalWifiOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.hotelvision.launcher.setup.DefaultLauncherUiState
import com.hotelvision.launcher.ui.LauncherAction
import com.hotelvision.launcher.ui.theme.QsPanelBackground
import com.hotelvision.launcher.ui.theme.QsTileFocused
import com.hotelvision.launcher.ui.theme.QsTileFocusedText
import com.hotelvision.launcher.ui.theme.QsTileResting
import com.hotelvision.launcher.ui.theme.TextPrimary
import com.hotelvision.launcher.ui.theme.TextSecondary
import com.hotelvision.launcher.ui.theme.TextTertiary
import com.hotelvision.launcher.ui.theme.TvCardAlphaFocused
import com.hotelvision.launcher.ui.theme.TvCardAlphaUnfocused
import com.hotelvision.launcher.ui.theme.TvCardScaleFocused
import com.hotelvision.launcher.ui.theme.TvFocusGainMs
import com.hotelvision.launcher.ui.theme.TvFocusLossMs
import com.hotelvision.launcher.ui.theme.TvTypeBody
import com.hotelvision.launcher.ui.theme.TvTypeCaption
import com.hotelvision.launcher.ui.theme.TvTypeTitle

// ── Panel slide animation durations ──────────────────────────────────────────
private const val PANEL_ENTER_SLIDE_MS = 320
private const val PANEL_EXIT_SLIDE_MS  = 260
private const val PANEL_FADE_MS        = 200

// ── Focus settle delay: slide duration + 1 frame (16ms) ──────────────────────
private val PANEL_FOCUS_DELAY_MS = overlayEntryFocusDelayMillis(PANEL_ENTER_SLIDE_MS)

// ── Panel geometry ────────────────────────────────────────────────────────────
private val PANEL_WIDTH          = 320.dp
private val PANEL_CORNER_LEFT    = 24.dp  // only left edges curve

/**
 * Phase 4 — Hotel Quick Settings Panel
 *
 * Contains ONLY:
 *   1. Wi-Fi status display (non-focusable read-only row)
 *   2. Display Brightness slider (focusable)
 *   3. Volume slider (focusable)
 *
 * All consumer tiles (All Settings, Bluetooth, Inputs, Accessibility) are REMOVED.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QuickSettingsOverlay(
    visible: Boolean,
    panelWidth: Dp,
    launcherSetupState: DefaultLauncherUiState?,
    firstItemFocusRequester: FocusRequester,
    onRequestDefaultLauncher: () -> Unit,
    onOpenVendorSettings: () -> Unit,
    onDismiss: () -> Unit,
    onAction: (LauncherAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wifiSsid by rememberWifiSsid(context)
    val wifiConnected by rememberWifiConnected(context)

    // Focus the brightness slider the moment the panel finishes sliding in
    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(PANEL_FOCUS_DELAY_MS)
            runCatching { firstItemFocusRequester.requestFocus() }
        }
    }

    val panelShape = RoundedCornerShape(
        topStart = PANEL_CORNER_LEFT,
        bottomStart = PANEL_CORNER_LEFT,
        topEnd = 0.dp,
        bottomEnd = 0.dp
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(PANEL_FADE_MS)) +
                slideInHorizontally(animationSpec = tween(PANEL_ENTER_SLIDE_MS, easing = FastOutSlowInEasing), initialOffsetX = { it }),
        exit  = fadeOut(animationSpec = tween(PANEL_FADE_MS)) +
                slideOutHorizontally(animationSpec = tween(PANEL_EXIT_SLIDE_MS), targetOffsetX = { it }),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("quick_settings_overlay")
        ) {
            // Scrim — clicking outside dismisses
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            // Side panel — right aligned
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(PANEL_WIDTH)
                    .fillMaxHeight()
                    .clip(panelShape)
                    .background(QsPanelBackground)
                    .onPreviewKeyEvent { event ->
                        // D-pad LEFT from the first focusable control closes the panel
                        if (event.type == KeyEventType.KeyUp && event.key == Key.DirectionLeft) {
                            onDismiss()
                            true
                        } else false
                    }
                    .padding(horizontal = 28.dp, vertical = 40.dp)
                    .testTag("quick_settings_panel"),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // ── Section header ────────────────────────────────────────────
                PanelHeader()

                // ── 1. Wi-Fi Status (read-only, non-focusable) ───────────────
                WifiStatusRow(
                    ssid = wifiSsid,
                    connected = wifiConnected
                )

                // ── Divider ───────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0x1AFFFFFF))
                )

                // ── 2. Brightness Slider ──────────────────────────────────────
                HotelSlider(
                    label = "Brightness",
                    icon = Icons.Outlined.Tune,
                    focusRequester = firstItemFocusRequester,
                    valueRange = 0f..255f,
                    readValue = {
                        runCatching {
                            Settings.System.getInt(
                                context.contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS
                            ).toFloat()
                        }.getOrElse { 128f }
                    },
                    onValueChange = { brightness ->
                        runCatching {
                            Settings.System.putInt(
                                context.contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS,
                                brightness.toInt()
                            )
                        }
                    }
                )

                // ── 3. Volume Slider ──────────────────────────────────────────
                val audioManager = remember {
                    context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                }
                HotelSlider(
                    label = "Volume",
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    focusRequester = null,
                    valueRange = 0f..audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat(),
                    readValue = {
                        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                    },
                    onValueChange = { vol ->
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            vol.toInt(),
                            0
                        )
                    }
                )

                launcherSetupState?.let { setupState ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x1AFFFFFF))
                    )

                    LauncherSetupSection(
                        launcherSetupState = setupState,
                        onRequestDefaultLauncher = onRequestDefaultLauncher,
                        onOpenVendorSettings = onOpenVendorSettings
                    )
                }
            }
        }
    }
}

// ── Panel header ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PanelHeader() {
    Column {
        Text(
            text = "Room Controls",
            color = TextPrimary,
            fontSize = TvTypeTitle,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Brightness and volume for this room",
            color = TextSecondary,
            fontSize = TvTypeCaption
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LauncherSetupSection(
    launcherSetupState: DefaultLauncherUiState,
    onRequestDefaultLauncher: () -> Unit,
    onOpenVendorSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Launcher Setup",
            color = TextPrimary,
            fontSize = TvTypeTitle,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = launcherSetupState.statusText,
            color = TextSecondary,
            fontSize = TvTypeBody
        )
        LauncherSetupButton(
            label = launcherSetupState.recommendedActionLabel,
            subtitle = when {
                launcherSetupState.isDefaultLauncher -> "No further action is needed on this TV."
                else -> "Step through the supported default-launcher flow for this board."
            },
            onClick = onRequestDefaultLauncher,
            testTag = "quick_settings_make_default"
        )
        launcherSetupState.vendorSettingsLabel?.let { label ->
            LauncherSetupButton(
                label = label,
                subtitle = "Open the available TV settings package and look for home/default app options.",
                onClick = onOpenVendorSettings,
                testTag = "quick_settings_vendor_settings"
            )
        }
        Text(
            text = launcherSetupState.adbCommand,
            color = TextTertiary,
            fontSize = TvTypeCaption
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LauncherSetupButton(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) TvCardScaleFocused else 1f,
        animationSpec = tween(
            durationMillis = if (isFocused) TvFocusGainMs else TvFocusLossMs,
            easing = if (isFocused) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "LauncherSetupScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isFocused) TvCardAlphaFocused else TvCardAlphaUnfocused,
        animationSpec = tween(
            durationMillis = if (isFocused) TvFocusGainMs else TvFocusLossMs,
            easing = if (isFocused) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "LauncherSetupAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFocused) QsTileFocused else QsTileResting)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            color = if (isFocused) QsTileFocusedText else TextPrimary,
            fontSize = TvTypeBody,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            color = if (isFocused) QsTileFocusedText.copy(alpha = 0.88f) else TextSecondary,
            fontSize = TvTypeCaption,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// ── Wi-Fi status display (non-focusable) ────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WifiStatusRow(ssid: String, connected: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(QsTileResting)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = if (connected) Icons.Outlined.SignalWifi4Bar else Icons.Outlined.WifiOff,
            contentDescription = "Wi-Fi",
            tint = if (connected) TextPrimary else TextTertiary,
            modifier = Modifier.size(22.dp)
        )
        Column {
            Text(
                text = "Wi-Fi",
                color = TextSecondary,
                fontSize = TvTypeCaption,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (connected) ssid.ifBlank { "Connected" } else "Not connected",
                color = if (connected) TextPrimary else TextTertiary,
                fontSize = TvTypeBody,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Focusable slider control ─────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HotelSlider(
    label: String,
    icon: ImageVector,
    focusRequester: FocusRequester?,
    valueRange: ClosedFloatingPointRange<Float>,
    readValue: () -> Float,
    onValueChange: (Float) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(readValue()) }

    // Kinetic focus contract — 1.08x scale, 0.72 alpha at rest
    val scale by animateFloatAsState(
        targetValue = if (isFocused) TvCardScaleFocused else 1.0f,
        animationSpec = tween(
            durationMillis = if (isFocused) TvFocusGainMs else TvFocusLossMs,
            easing = if (isFocused) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "SliderScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isFocused) TvCardAlphaFocused else TvCardAlphaUnfocused,
        animationSpec = tween(
            durationMillis = if (isFocused) TvFocusGainMs else TvFocusLossMs,
            easing = if (isFocused) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "SliderAlpha"
    )

    val containerColor = if (isFocused) QsTileFocused else QsTileResting
    val labelColor     = if (isFocused) QsTileFocusedText else TextSecondary
    val iconTint       = if (isFocused) QsTileFocusedText else TextPrimary

    val focusModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .then(focusModifier)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = labelColor,
                fontSize = TvTypeCaption,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${((sliderValue - valueRange.start) / (valueRange.endInclusive - valueRange.start) * 100).toInt()}%",
                color = labelColor,
                fontSize = TvTypeCaption,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Slider(
            value = sliderValue,
            onValueChange = { v ->
                sliderValue = v
                onValueChange(v)
            },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = if (isFocused) QsTileFocusedText else TextPrimary,
                activeTrackColor = if (isFocused) QsTileFocusedText.copy(alpha = 0.85f) else TextPrimary.copy(alpha = 0.7f),
                inactiveTrackColor = if (isFocused) QsTileFocusedText.copy(alpha = 0.25f) else TextSecondary.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── State helpers ────────────────────────────────────────────────────────────────
@Composable
private fun rememberWifiSsid(context: Context) = produceState(initialValue = "") {
    val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    val wm = context.applicationContext.getSystemService(android.net.wifi.WifiManager::class.java)
    // Safe SSID read — use ConnectivityManager on API 29+
    value = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On API 29+ WifiManager.connectionInfo is restricted — read via LinkProperties is not available easily,
            // fall back to WifiManager inside app context with READ_PHONE_STATE permission
            @Suppress("DEPRECATION")
            wm?.connectionInfo?.ssid?.removeSurrounding("\"") ?: ""
        } else {
            @Suppress("DEPRECATION")
            wm?.connectionInfo?.ssid?.removeSurrounding("\"") ?: ""
        }
    }.getOrElse { "" }
}

@Composable
private fun rememberWifiConnected(context: Context) = produceState(initialValue = false) {
    val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    value = runCatching {
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }.getOrElse { false }
}
