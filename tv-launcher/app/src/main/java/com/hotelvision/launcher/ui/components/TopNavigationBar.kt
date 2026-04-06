package com.hotelvision.launcher.ui.components

import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.hotelvision.launcher.performance.LauncherPerformanceProfile
import com.hotelvision.launcher.performance.LocalLauncherPerformanceProfile
import com.hotelvision.launcher.data.db.entities.RoomInfoEntity
import com.hotelvision.launcher.ui.HotelBranding
import com.hotelvision.launcher.ui.LauncherDestination
import com.hotelvision.launcher.ui.theme.NavTabActiveIndie
import com.hotelvision.launcher.ui.theme.TvNavLabelSize
import com.hotelvision.launcher.ui.theme.TvScreenHorizontalPadding
import com.hotelvision.launcher.ui.theme.TvScreenVerticalPadding
import com.hotelvision.launcher.ui.theme.TvTopNavActionSpacing
import com.hotelvision.launcher.ui.theme.TvTopNavItemSpacing
import com.hotelvision.launcher.ui.theme.TvFocusGainMs
import com.hotelvision.launcher.ui.theme.TvFocusLossMs

private val PillShape = RoundedCornerShape(20.dp)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopNavigationBar(
    branding: HotelBranding,
    roomInfo: RoomInfoEntity?,
    selectedDestination: LauncherDestination,
    destinationFocusRequesters: Map<LauncherDestination, FocusRequester>,
    notificationsFocusRequester: FocusRequester,
    gearFocusRequester: FocusRequester,
    unreadMessageCount: Int,
    notificationsPanelOpen: Boolean,
    selectedContentFocusRequester: FocusRequester,
    settingsPanelOpen: Boolean,
    focusEnabled: Boolean,
    onDestinationSelected: (LauncherDestination) -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHeaderFocusRequested: () -> Unit = {},
    onNeutralFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TvScreenHorizontalPadding, vertical = TvScreenVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lane 1: Logo (Left weighted)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            HotelLogo(
                branding = branding,
                showText = true
            )
        }

        // Lane 2: Destinations (Center weighted)
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.weight(2f),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(TvTopNavItemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LauncherDestination.entries.forEachIndexed { index, destination ->
                    NavigationTab(
                        modifier = Modifier
                            .testTag("top_nav_${destination.name.lowercase()}"),
                        text = destination.title,
                        selected = destination == selectedDestination,
                        focusRequester = destinationFocusRequesters[destination],
                        downFocusRequester = selectedContentFocusRequester,
                        rightFocusRequester = if (index == LauncherDestination.entries.lastIndex) {
                            gearFocusRequester
                        } else {
                            null
                        },
                        focusEnabled = focusEnabled,
                        onClick = { onDestinationSelected(destination) },
                        onFocused = {
                            onNeutralFocus()
                            onHeaderFocusRequested()
                        }
                    )
                }
            }
        }

        // Lane 3: Status & Gear (Right weighted)
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(TvTopNavActionSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBar(roomInfo = roomInfo)
                ActionIconButton(
                    modifier = Modifier.testTag("top_nav_notifications"),
                    icon = Icons.Outlined.Notifications,
                    contentDescription = "Guest messages",
                    selected = notificationsPanelOpen,
                    focusRequester = notificationsFocusRequester,
                    downFocusRequester = selectedContentFocusRequester,
                    focusEnabled = focusEnabled,
                    badgeCount = unreadMessageCount,
                    onClick = onNotificationsClick,
                    onFocused = {
                        onNeutralFocus()
                        onHeaderFocusRequested()
                    }
                )
                ActionIconButton(
                    modifier = Modifier.testTag("top_nav_settings_gear"),
                    icon = Icons.Outlined.Settings,
                    contentDescription = "Controls and settings",
                    selected = settingsPanelOpen,
                    focusRequester = gearFocusRequester,
                    downFocusRequester = selectedContentFocusRequester,
                    focusEnabled = focusEnabled,
                    badgeCount = 0,
                    onClick = onSettingsClick,
                    onFocused = {
                        onNeutralFocus()
                        onHeaderFocusRequested()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationTab(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    focusRequester: FocusRequester?,
    downFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester? = null,
    focusEnabled: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit
) {
    val performanceProfile = LocalLauncherPerformanceProfile.current
    var isFocused by remember { mutableStateOf(false) }

    // Authentic Google TV top_nav_button_selector behavior:
    // Focused tabs scale up to 1.15f (prominent TV distance visibility, but balanced).
    // Selected tabs (clicked active destination) scale to 1.08f to stand out.
    // Inactive tabs sit at 1.0f.
    val scale by animateFloatAsState(
        targetValue = when {
            isFocused -> if (performanceProfile == LauncherPerformanceProfile.LOW_RAM) 1.04f else 1.08f
            selected -> if (performanceProfile == LauncherPerformanceProfile.LOW_RAM) 1.02f else 1.04f
            else -> 1.0f
        },
        animationSpec = tween(
            durationMillis = if (isFocused) TvFocusGainMs else TvFocusLossMs,
            easing = if (isFocused) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "TabScale"
    )

    // Text alpha: full on focus or selected, slightly dimmed otherwise.
    val alpha by animateFloatAsState(
        targetValue = if (isFocused || selected) 1.0f else 0.78f,
        animationSpec = tween(
            durationMillis = if (isFocused) TvFocusGainMs else TvFocusLossMs,
            easing = if (isFocused) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "TabAlpha"
    )

    // Pill background:
    // Focused: 12% translucent glassy grey (as requested, 'like it previously had when selected')
    // Selected: Transparent (no pill for click-active tab)
    // Default: Transparent
    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            isFocused -> Color(0x33E8EAED)
            selected -> Color(0x12E8EAED)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 160),
        label = "TabBackground"
    )

    // Text color: bold white on focus/selected to contrast against translucent pill, grey when inactive.
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            isFocused -> Color(0xFFFFFFFF)
            selected  -> Color(0xFFFFFFFF)
            else      -> Color(0xB2E8EAED)
        },
        animationSpec = tween(durationMillis = 140),
        label = "TabContentColor"
    )

    NavItemContainer(
        modifier = modifier,
        isFocused = isFocused,
        scale = scale,
        alpha = alpha,
        focusRequester = focusRequester,
        downFocusRequester = downFocusRequester,
        rightFocusRequester = rightFocusRequester,
        focusEnabled = focusEnabled,
        onClick = onClick,
        onFocused = onFocused,
        onFocusChanged = { isFocused = it },
        backgroundColor = backgroundColor
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                color = contentColor,
                fontSize = TvNavLabelSize,
                fontWeight = if (isFocused || selected) FontWeight.SemiBold else FontWeight.Medium,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0x66000000),
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        blurRadius = if (performanceProfile == LauncherPerformanceProfile.LOW_RAM) 0f else 4f
                    )
                )
            )
            // ACTIVE underline indicator — unique to selected (not focused) tab
            // Animates in when this tab is the active destination
            val indicatorAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (selected && !isFocused) 1f else 0f,
                animationSpec = tween(durationMillis = 160),
                label = "ActiveIndicatorAlpha"
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .graphicsLayer { this.alpha = indicatorAlpha }
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(1.dp))
                    .background(NavTabActiveIndie)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionIconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    focusEnabled: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    onFocused: () -> Unit
) {
    val performanceProfile = LocalLauncherPerformanceProfile.current
    var isFocused by remember { mutableStateOf(false) }

    // Matches NavigationTab: scale up to 1.08f on focus
    val scale by animateFloatAsState(
        targetValue = when {
            isFocused -> if (performanceProfile == LauncherPerformanceProfile.LOW_RAM) 1.04f else 1.08f
            selected -> if (performanceProfile == LauncherPerformanceProfile.LOW_RAM) 1.02f else 1.04f
            else -> 1.0f
        },
        animationSpec = tween(
            durationMillis = if (isFocused) TvFocusGainMs else TvFocusLossMs,
            easing = if (isFocused) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "IconScale"
    )

    // Background: 12% translucent on focus, transparent otherwise
    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            isFocused -> Color(0x33E8EAED)
            selected -> Color(0x12E8EAED)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 160),
        label = "GearBackground"
    )

    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            isFocused -> Color(0xFFFFFFFF)
            selected  -> Color(0xFFFFFFFF)
            else      -> Color(0xFFE8EAED)  // Full opacity for better visibility
        },
        animationSpec = tween(durationMillis = 140),
        label = "GearContentColor"
    )

    NavItemContainer(
        modifier = modifier,
        isFocused = isFocused,
        scale = scale,
        alpha = 1f, // Icons always stay visible
        focusRequester = focusRequester,
        downFocusRequester = downFocusRequester,
        rightFocusRequester = null,
        focusEnabled = focusEnabled,
        onClick = onClick,
        onFocused = onFocused,
        onFocusChanged = { isFocused = it },
        backgroundColor = backgroundColor
    ) {
        Box {
            Image(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier,
                colorFilter = ColorFilter.tint(contentColor)
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isFocused) Color(0xFF101417) else Color(0xFFE8EAED))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badgeCount.coerceAtMost(9).toString(),
                        color = if (isFocused) Color.White else Color(0xFF11151A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItemContainer(
    modifier: Modifier = Modifier,
    isFocused: Boolean,
    scale: Float,
    alpha: Float,
    focusRequester: FocusRequester?,
    downFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester?,
    focusEnabled: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                // Scale is applied BEFORE clip so the pill expands when focused,
                // replicating the Android View.animate().scaleX(1.15f) behaviour.
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(PillShape)
            .background(backgroundColor)
            .onFocusChanged { state ->
                onFocusChanged(state.isFocused)
                if (state.isFocused) {
                    onFocused()
                }
            }
            .selectable(
                selected = false,
                enabled = focusEnabled,
                onClick = onClick
            )
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .focusProperties {
                down = downFocusRequester
                if (rightFocusRequester != null) {
                    right = rightFocusRequester
                }
                canFocus = focusEnabled
            }
            .focusTarget()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        content()
    }
}
