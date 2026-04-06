package com.hotelvision.launcher.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.animation.*
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.hotelvision.launcher.R
import com.hotelvision.launcher.performance.LauncherImageKind
import com.hotelvision.launcher.performance.LocalLauncherPerformanceProfile
import com.hotelvision.launcher.performance.buildLauncherImageRequest
import com.hotelvision.launcher.performance.backdropSlideshowIntervalMs
import com.hotelvision.launcher.performance.normalizedBackdropSlideshowImages
import com.hotelvision.launcher.performance.shouldAnimateBackdrop
import com.hotelvision.launcher.performance.shouldAnimateDestinationTransitions
import com.hotelvision.launcher.performance.shouldAnimateEntrance
import com.hotelvision.launcher.performance.shouldAnimateRowAlignment
import com.hotelvision.launcher.performance.shouldAdvanceBackdropSlideshow
import com.hotelvision.launcher.performance.shouldUseFocusDrivenBackdrop
import com.hotelvision.launcher.setup.DefaultLauncherUiState
import com.hotelvision.launcher.ui.components.AllAppsScreen
import com.hotelvision.launcher.ui.components.DefaultLauncherPromptOverlay
import com.hotelvision.launcher.ui.components.GuestMessagesOverlay
import com.hotelvision.launcher.ui.components.ContentRow
import com.hotelvision.launcher.ui.components.QuickSettingsOverlay
import com.hotelvision.launcher.ui.components.ShimmerCard
import com.hotelvision.launcher.ui.components.StaggeredEntrance
import com.hotelvision.launcher.ui.components.TopNavigationBar
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import com.hotelvision.launcher.ui.theme.GtvTextPrimary
import com.hotelvision.launcher.ui.theme.GtvTextSecondary
import com.hotelvision.launcher.ui.theme.TvRowSpacing
import com.hotelvision.launcher.ui.theme.TvScreenHorizontalPadding
import com.hotelvision.launcher.ui.theme.TvScreenVerticalPadding
import com.hotelvision.launcher.ui.theme.TvSectionSpacing
import com.hotelvision.launcher.ui.theme.TvSurfaceBottomPadding
import com.hotelvision.launcher.ui.theme.TvSurfaceSubtitleSize
import com.hotelvision.launcher.ui.theme.TvSurfaceTitleSize
import com.hotelvision.launcher.ui.theme.TvSurfaceTopPadding
import com.hotelvision.launcher.ui.theme.TvWelcomeSubtitleSize
import com.hotelvision.launcher.ui.theme.TvWelcomeSupportingSize
import com.hotelvision.launcher.ui.theme.TvRowTitleSafeTop
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LauncherPanel {
    NONE,
    MESSAGES,
    CONTROLS
}

private enum class ReturnFocusTarget {
    NOTIFICATIONS,
    CONTROLS
}

import com.hotelvision.launcher.ui.components.UnboundScreen
import com.hotelvision.launcher.data.repository.BindingState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LauncherScreen(
    uiState: LauncherUiState,
    launcherSetupState: DefaultLauncherUiState? = null,
    defaultLauncherPromptVisible: Boolean = false,
    onDestinationSelected: (LauncherDestination) -> Unit,
    onAction: (LauncherAction) -> Unit,
    onRequestDefaultLauncher: () -> Unit = {},
    onOpenVendorSettings: () -> Unit = {},
    onDismissDefaultLauncherPrompt: () -> Unit = {},
    onInstalledAppOrderChanged: (List<String>) -> Unit = {},
    onResetGuestPersonalization: () -> Unit = {}
) {
    // ── Binding gate: show lock screen until the device is bound ─────────────
    when (val binding = uiState.bindingState) {
        is BindingState.Unbound -> {
            UnboundScreen(state = binding)
            return
        }
        is BindingState.Loading -> {
            // Show plain black while we determine binding state (avoids flash)
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            return
        }
        is BindingState.Bound -> { /* fall through to normal launcher */ }
    }
    // ─────────────────────────────────────────────────────────────────────────

    val performanceProfile = LocalLauncherPerformanceProfile.current
    val destinationFocusRequesters = remember {
        LauncherDestination.entries.associateWith { FocusRequester() }
    }
    val destinationContentFocusRequesters = remember {
        LauncherDestination.entries.associateWith { FocusRequester() }
    }
    val notificationsFocusRequester = remember { FocusRequester() }
    val gearFocusRequester = remember { FocusRequester() }
    val guestMessagesFocusRequester = remember { FocusRequester() }
    val quickSettingsFocusRequester = remember { FocusRequester() }
    val selectedTopNavRequester = destinationFocusRequesters.getValue(uiState.selectedDestination)
    val selectedContentRequester = destinationContentFocusRequesters.getValue(uiState.selectedDestination)
    val homeListState = rememberLazyListState()
    val roomServiceListState = rememberLazyListState()
    val localGuideListState = rememberLazyListState()
    var activePanel by rememberSaveable { mutableStateOf(LauncherPanel.NONE) }
    var allAppsVisible by rememberSaveable { mutableStateOf(false) }
    var returnFocusTarget by remember { mutableStateOf<ReturnFocusTarget?>(null) }
    var focusedBackdrop by remember { mutableStateOf<AmbientBackdropState?>(null) }
    var previousDestination by rememberSaveable { mutableStateOf(uiState.selectedDestination) }
    var contentTransitionDirection by rememberSaveable { mutableIntStateOf(1) }
    var hasCompletedInitialEntrance by rememberSaveable { mutableStateOf(false) }
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var headerHasFocus by remember { mutableStateOf(false) }
    val overlayOpen = activePanel != LauncherPanel.NONE || defaultLauncherPromptVisible || allAppsVisible
    val navigationFocusEnabled = !overlayOpen
    val animateDestinationEntrance = remember(uiState.selectedDestination, hasCompletedInitialEntrance, performanceProfile) {
        !hasCompletedInitialEntrance && shouldAnimateEntrance(performanceProfile)
    }
    val headerCollapsedByScroll by remember(
        uiState.selectedDestination,
        homeListState,
        roomServiceListState,
        localGuideListState
    ) {
        derivedStateOf {
            when (uiState.selectedDestination) {
                LauncherDestination.HOME ->
                    homeListState.firstVisibleItemIndex > 0 ||
                        homeListState.firstVisibleItemScrollOffset > 0
                LauncherDestination.ROOM_SERVICE ->
                    roomServiceListState.firstVisibleItemIndex > 0 ||
                        roomServiceListState.firstVisibleItemScrollOffset > 0
                LauncherDestination.LOCAL_GUIDE ->
                    localGuideListState.firstVisibleItemIndex > 0 ||
                        localGuideListState.firstVisibleItemScrollOffset > 0
            }
        }
    }

    fun showPanel(panel: LauncherPanel) {
        activePanel = panel
    }

    fun dismissPanel() {
        if (activePanel != LauncherPanel.NONE) {
            returnFocusTarget = when (activePanel) {
                LauncherPanel.MESSAGES -> ReturnFocusTarget.NOTIFICATIONS
                LauncherPanel.CONTROLS -> ReturnFocusTarget.CONTROLS
                LauncherPanel.NONE -> null
            }
            activePanel = LauncherPanel.NONE
        }
    }


    fun dispatchAction(action: LauncherAction) {
        when (action) {
            LauncherAction.OpenNotificationsPanel -> showPanel(LauncherPanel.MESSAGES)
            LauncherAction.OpenControlPanel -> showPanel(LauncherPanel.CONTROLS)
            LauncherAction.ExitTransientUi -> dismissPanel()
            LauncherAction.ResetGuestPersonalization -> onResetGuestPersonalization()
            LauncherAction.OpenAllApps -> allAppsVisible = true
            is LauncherAction.OpenDestination -> onDestinationSelected(action.destination)
            is LauncherAction.EnterAppMoveMode -> Unit
            is LauncherAction.LaunchIntent -> onAction(action)
            is LauncherAction.LaunchPackage -> onAction(action)
            else -> onAction(action)
        }
    }

    LaunchedEffect(activePanel, returnFocusTarget) {
        if (activePanel == LauncherPanel.NONE) {
            returnFocusTarget?.let { focusTarget ->
                delay(50)
                headerHasFocus = true
                runCatching {
                    when (focusTarget) {
                        ReturnFocusTarget.NOTIFICATIONS -> notificationsFocusRequester.requestFocus()
                        ReturnFocusTarget.CONTROLS -> gearFocusRequester.requestFocus()
                    }
                }
                returnFocusTarget = null
            }
        }
    }

    LaunchedEffect(uiState.selectedDestination) {
        contentTransitionDirection = calculateDestinationTransitionDirection(
            previous = previousDestination,
            target = uiState.selectedDestination
        )
        previousDestination = uiState.selectedDestination
        if (!hasCompletedInitialEntrance) {
            hasCompletedInitialEntrance = true
            headerHasFocus = true
            destinationFocusRequesters[uiState.selectedDestination]?.requestFocus()
        }
        focusedBackdrop = null
    }


    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactWidthPercent = integerResource(R.integer.gtv_quick_settings_panel_width_percent_compact)
        val defaultWidthPercent = integerResource(R.integer.gtv_quick_settings_panel_width_percent_default)
        val minimumPanelWidth = dimensionResource(R.dimen.gtv_dashboard_min_width)
        val density = LocalDensity.current
        val panelWidthFraction = if (maxWidth <= 960.dp) {
            compactWidthPercent / 100f
        } else {
            defaultWidthPercent / 100f
        }
        val panelWidth = (maxWidth * panelWidthFraction).coerceAtLeast(minimumPanelWidth)
        val headerVisible = headerHasFocus || !headerCollapsedByScroll || activePanel != LauncherPanel.NONE || defaultLauncherPromptVisible
        val headerSpacer by animateDpAsState(
            targetValue = if (headerVisible) with(density) { headerHeightPx.toDp() } else 0.dp,
            animationSpec = tween(280, easing = EaseOutQuart),
            label = "HeaderSpacer"
        )
        val headerOffset by animateFloatAsState(
            targetValue = if (headerVisible) 0f else -headerHeightPx.toFloat(),
            animationSpec = tween(280, easing = EaseOutQuart),
            label = "HeaderOffset"
        )
        val headerAlpha by animateFloatAsState(
            targetValue = if (headerVisible) 1f else 0f,
            animationSpec = tween(220),
            label = "HeaderAlpha"
        )

        Box(modifier = Modifier.fillMaxSize()) {
            AmbientBackdrop(
                defaultBackdrop = focusedBackdrop ?: uiState.ambientBackdrop,
                performanceProfile = performanceProfile,
                overlayOpen = overlayOpen
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xB3000000), Color.Transparent),
                            endY = 800f
                        )
                    )
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xB3000000), Color.Transparent),
                            endX = 1200f
                        )
                    )
            )
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenWidthPx = with(density) { maxWidth.toPx() }
                val screenHeightPx = with(density) { maxHeight.toPx() }

                // Safety check: Only render gradient if dimensions are valid
                if (screenWidthPx > 0f && screenHeightPx > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // Phase 1.2: Premium Radial Ambient Light (Top-Right Glow)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0x1AFFFFFF), Color.Transparent),
                                    center = Offset(x = screenWidthPx * 0.85f, y = screenHeightPx * 0.05f),
                                    radius = (screenWidthPx * 0.40f).coerceAtLeast(1f)
                                )
                            )
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerSpacer)
            ) {
                if (shouldAnimateDestinationTransitions(performanceProfile)) {
                    AnimatedContent(
                        targetState = uiState.selectedDestination,
                        transitionSpec = {
                            val direction = contentTransitionDirection

                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> (fullWidth * direction * 0.34f).toInt() },
                                animationSpec = tween(340, easing = EaseOutQuart)
                            ) + fadeIn(
                                animationSpec = tween(260)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { fullWidth -> (-fullWidth * direction * 0.34f).toInt() },
                                animationSpec = tween(340, easing = EaseOutQuart)
                            ) + fadeOut(
                                animationSpec = tween(220)
                            ) using SizeTransform(clip = false)
                        },
                        label = "ContentPanelTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { destination ->
                        LauncherDestinationContent(
                            destination = destination,
                            uiState = uiState,
                            homeListState = homeListState,
                            roomServiceListState = roomServiceListState,
                            localGuideListState = localGuideListState,
                            selectedTopNavRequester = selectedTopNavRequester,
                            destinationContentFocusRequesters = destinationContentFocusRequesters,
                            overlayOpen = overlayOpen,
                            animateDestinationEntrance = animateDestinationEntrance,
                            onAction = ::dispatchAction,
                            onHeaderReturn = {
                                headerHasFocus = true
                                selectedTopNavRequester.requestFocus()
                            },
                            onCardFocused = {
                                headerHasFocus = false
                                if (shouldUseFocusDrivenBackdrop(performanceProfile)) {
                                    focusedBackdrop = it.toAmbientBackdrop()
                                }
                            },
                            onNeutralFocus = {
                                headerHasFocus = false
                                focusedBackdrop = null
                            }
                        )
                    }
                } else {
                    LauncherDestinationContent(
                        destination = uiState.selectedDestination,
                        uiState = uiState,
                        homeListState = homeListState,
                        roomServiceListState = roomServiceListState,
                        localGuideListState = localGuideListState,
                        selectedTopNavRequester = selectedTopNavRequester,
                        destinationContentFocusRequesters = destinationContentFocusRequesters,
                        overlayOpen = overlayOpen,
                        animateDestinationEntrance = false,
                        onAction = ::dispatchAction,
                        onHeaderReturn = {
                            headerHasFocus = true
                            selectedTopNavRequester.requestFocus()
                        },
                        onCardFocused = {
                            headerHasFocus = false
                            focusedBackdrop = null
                        },
                        onNeutralFocus = {
                            headerHasFocus = false
                            focusedBackdrop = null
                        }
                    )
                }
            }

            TopNavigationBar(
                branding = uiState.hotelBranding,
                roomInfo = uiState.roomInfo,
                selectedDestination = uiState.selectedDestination,
                destinationFocusRequesters = destinationFocusRequesters,
                notificationsFocusRequester = notificationsFocusRequester,
                gearFocusRequester = gearFocusRequester,
                unreadMessageCount = uiState.guestMessages.size,
                notificationsPanelOpen = activePanel == LauncherPanel.MESSAGES,
                selectedContentFocusRequester = selectedContentRequester,
                settingsPanelOpen = activePanel == LauncherPanel.CONTROLS,
                focusEnabled = navigationFocusEnabled,
                onDestinationSelected = {
                    headerHasFocus = true
                    onDestinationSelected(it)
                },
                onNotificationsClick = { showPanel(LauncherPanel.MESSAGES) },
                onSettingsClick = { showPanel(LauncherPanel.CONTROLS) },
                onHeaderFocusRequested = { headerHasFocus = true },
                onNeutralFocus = { focusedBackdrop = null },
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { headerHeightPx = it.height }
                    .graphicsLayer {
                        translationY = headerOffset
                        alpha = headerAlpha
                    }
            )

            if (activePanel != LauncherPanel.NONE) {
                BackHandler(onBack = ::dismissPanel)
            }

            GuestMessagesOverlay(
                visible = activePanel == LauncherPanel.MESSAGES,
                messages = uiState.guestMessages,
                panelWidth = panelWidth,
                firstItemFocusRequester = guestMessagesFocusRequester,
                onDismiss = ::dismissPanel,
                onAction = ::dispatchAction
            )

            QuickSettingsOverlay(
                visible = activePanel == LauncherPanel.CONTROLS,
                panelWidth = panelWidth,
                launcherSetupState = launcherSetupState,
                firstItemFocusRequester = quickSettingsFocusRequester,
                onRequestDefaultLauncher = onRequestDefaultLauncher,
                onOpenVendorSettings = onOpenVendorSettings,
                onDismiss = ::dismissPanel,
                onAction = ::dispatchAction
            )

            DefaultLauncherPromptOverlay(
                visible = defaultLauncherPromptVisible,
                launcherSetupState = launcherSetupState,
                onRequestDefaultLauncher = onRequestDefaultLauncher,
                onOpenVendorSettings = onOpenVendorSettings,
                onDismiss = onDismissDefaultLauncherPrompt
            )

            if (allAppsVisible) {
                BackHandler(onBack = { allAppsVisible = false })
                AllAppsScreen(
                    allApps = uiState.allInstalledApps,
                    onAction = { action ->
                        allAppsVisible = false
                        dispatchAction(action)
                    },
                    onBack = { allAppsVisible = false }
                )
            }
        }
    }
}

@Composable
private fun LauncherDestinationContent(
    destination: LauncherDestination,
    uiState: LauncherUiState,
    homeListState: androidx.compose.foundation.lazy.LazyListState,
    roomServiceListState: androidx.compose.foundation.lazy.LazyListState,
    localGuideListState: androidx.compose.foundation.lazy.LazyListState,
    selectedTopNavRequester: FocusRequester,
    destinationContentFocusRequesters: Map<LauncherDestination, FocusRequester>,
    overlayOpen: Boolean,
    animateDestinationEntrance: Boolean,
    onAction: (LauncherAction) -> Unit,
    onHeaderReturn: () -> Unit,
    onCardFocused: (HomeCard) -> Unit,
    onNeutralFocus: () -> Unit
) {
    when (destination) {
        LauncherDestination.HOME -> HomeSurface(
            uiState = uiState,
            listState = homeListState,
            onAction = onAction,
            returnFocusRequester = selectedTopNavRequester,
            entryFocusRequester = destinationContentFocusRequesters.getValue(LauncherDestination.HOME),
            focusEnabled = !overlayOpen,
            animateEntrance = animateDestinationEntrance,
            onReturnToHeader = onHeaderReturn,
            onCardFocused = onCardFocused,
            onNeutralFocus = onNeutralFocus
        )

        LauncherDestination.ROOM_SERVICE -> DestinationFeedSurface(
            title = "Room Service",
            subtitle = "Dining, in-room service, and guest assistance available for this stay",
            rows = uiState.feedRows,
            listState = roomServiceListState,
            onAction = onAction,
            returnFocusRequester = selectedTopNavRequester,
            entryFocusRequester = destinationContentFocusRequesters.getValue(LauncherDestination.ROOM_SERVICE),
            focusEnabled = !overlayOpen,
            animateEntrance = animateDestinationEntrance,
            onReturnToHeader = onHeaderReturn,
            onCardFocused = onCardFocused
        )

        LauncherDestination.LOCAL_GUIDE -> DestinationFeedSurface(
            title = "Local Guide",
            subtitle = "Property-curated experiences, offers, and local discovery for hotel guests",
            rows = uiState.feedRows,
            listState = localGuideListState,
            onAction = onAction,
            returnFocusRequester = selectedTopNavRequester,
            entryFocusRequester = destinationContentFocusRequesters.getValue(LauncherDestination.LOCAL_GUIDE),
            focusEnabled = !overlayOpen,
            animateEntrance = animateDestinationEntrance,
            onReturnToHeader = onHeaderReturn,
            onCardFocused = onCardFocused
        )
    }
}

@Composable
private fun AmbientBackdrop(
    defaultBackdrop: AmbientBackdropState,
    performanceProfile: com.hotelvision.launcher.performance.LauncherPerformanceProfile,
    overlayOpen: Boolean
) {
    val slideshowImages = remember(defaultBackdrop.slideshowImages, defaultBackdrop.imageUrl) {
        normalizedBackdropSlideshowImages(
            slideshowImages = defaultBackdrop.slideshowImages,
            fallbackImageUrl = defaultBackdrop.imageUrl
        )
    }
    var slideshowIndex by remember(slideshowImages) { mutableIntStateOf(0) }
    if (slideshowImages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }
    val activeBackdrop = defaultBackdrop.copy(
        imageUrl = slideshowImages[slideshowIndex % slideshowImages.size]
    )
    val context = androidx.compose.ui.platform.LocalContext.current
    val backdropRequest = remember(activeBackdrop.imageUrl, performanceProfile) {
        buildLauncherImageRequest(
            context = context,
            data = activeBackdrop.imageUrl,
            imageKind = LauncherImageKind.BACKDROP,
            profile = performanceProfile
        )
    }

    LaunchedEffect(slideshowImages, performanceProfile, overlayOpen) {
        if (!shouldAdvanceBackdropSlideshow(performanceProfile, overlayOpen, slideshowImages.size)) {
            slideshowIndex = slideshowIndex.coerceAtMost(slideshowImages.lastIndex)
            return@LaunchedEffect
        }
        while (true) {
            delay(backdropSlideshowIntervalMs(performanceProfile))
            slideshowIndex = (slideshowIndex + 1) % slideshowImages.size
        }
    }

    if (shouldAnimateBackdrop(performanceProfile)) {
        Crossfade(
            targetState = backdropRequest,
            label = "AmbientBackdrop"
        ) { model ->
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = model,
                    contentDescription = activeBackdrop.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = backdropRequest,
                contentDescription = activeBackdrop.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun HomeSurface(
    uiState: LauncherUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onAction: (LauncherAction) -> Unit,
    returnFocusRequester: FocusRequester,
    entryFocusRequester: FocusRequester,
    focusEnabled: Boolean,
    animateEntrance: Boolean,
    onReturnToHeader: () -> Unit = {},
    onCardFocused: (HomeCard) -> Unit,
    onNeutralFocus: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val safeTopPx = with(LocalDensity.current) { TvRowTitleSafeTop.toPx() }
    val performanceProfile = LocalLauncherPerformanceProfile.current
    var activeRowId by remember(uiState.feedRows) { mutableStateOf(uiState.feedRows.firstOrNull()?.id) }
    val deepInRows = focusEnabled && (
        listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24
    )

    LaunchedEffect(Unit) {
        onNeutralFocus()
    }

    BackHandler(enabled = deepInRows) {
        coroutineScope.launch {
            listState.animateScrollToItem(0)
            onReturnToHeader()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = TvSurfaceTopPadding, bottom = TvSurfaceBottomPadding)
    ) {
        item {
            StaggeredEntrance(index = 0, enabled = animateEntrance) {
                WelcomeHeader(
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = TvScreenHorizontalPadding, vertical = TvScreenVerticalPadding)
                )
            }
        }

        if (uiState.feedRows.isEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = TvSectionSpacing)) {
                    repeat(2) { rowIndex ->
                        Row(
                            modifier = Modifier.padding(
                                start = TvScreenHorizontalPadding,
                                top = if (rowIndex == 0) 0.dp else TvSectionSpacing + 12.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(TvRowSpacing)
                        ) {
                            repeat(4) {
                                ShimmerCard(
                                    modifier = Modifier.size(332.dp, 186.dp),
                                    cornerRadius = 12
                                )
                            }
                        }
                    }
                }
            }
        } else {
            itemsIndexed(uiState.feedRows, key = { _, row -> row.id }) { index, row ->
                StaggeredEntrance(index = index + 1, enabled = animateEntrance) {
                    ContentRow(
                        rowId = row.id,
                        title = row.title,
                        subtitle = row.subtitle,
                        cards = row.cards,
                        onAction = onAction,
                        rowType = row.rowType,
                        style = row.style,
                        isActive = activeRowId == row.id,
                        focusEnabled = focusEnabled,
                        firstCardFocusRequester = if (index == 0) entryFocusRequester else null,
                        firstCardModifier = if (index == 0) {
                            Modifier
                                .focusProperties { up = returnFocusRequester }
                        } else {
                            Modifier
                        },
                        onRowFocused = { rowId, titleTopInWindowPx ->
                            if (activeRowId != rowId) {
                                activeRowId = rowId
                                coroutineScope.launch {
                                    alignFocusedRowTitle(
                                        listState = listState,
                                        titleTopInWindowPx = titleTopInWindowPx,
                                        safeTopPx = safeTopPx,
                                        animate = shouldAnimateRowAlignment(performanceProfile)
                                    )
                                }
                            }
                        },
                        onCardFocused = onCardFocused
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WelcomeHeader(
    uiState: LauncherUiState,
    modifier: Modifier = Modifier
) {
    val textShadow = Shadow(
        color = Color(0x99000000),
        offset = Offset(2f, 2f),
        blurRadius = 4f
    )

    Column(modifier = modifier) {
        Text(
            text = uiState.welcomeTitle,
            color = GtvTextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(shadow = textShadow)
        )
        Text(
            text = uiState.welcomeSubtitle,
            color = GtvTextSecondary,
            fontSize = TvWelcomeSubtitleSize,
            modifier = Modifier.padding(top = 8.dp),
            style = TextStyle(shadow = textShadow)
        )
        Text(
            text = when (uiState.mealPeriod) {
                MealPeriod.BREAKFAST -> "Breakfast is now being highlighted for this stay."
                MealPeriod.LUNCH -> "Lunch recommendations are now at the top of the home feed."
                MealPeriod.DINNER -> "Dinner recommendations are now leading the home feed tonight."
                MealPeriod.LATE_NIGHT -> "Good evening. Late night dining and amenities are available below."
                MealPeriod.OFF_HOURS -> "Enjoy your stay! Standard apps and services are available below."
            },
            color = GtvTextSecondary,
            fontSize = TvWelcomeSupportingSize,
            modifier = Modifier.padding(top = 10.dp),
            style = TextStyle(shadow = textShadow)
        )
    }
}



@Composable
private fun DestinationFeedSurface(
    title: String,
    subtitle: String,
    rows: List<HomeFeedRow>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onAction: (LauncherAction) -> Unit,
    returnFocusRequester: FocusRequester,
    entryFocusRequester: FocusRequester,
    focusEnabled: Boolean,
    animateEntrance: Boolean,
    onReturnToHeader: () -> Unit = {},
    onCardFocused: (HomeCard) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val safeTopPx = with(LocalDensity.current) { TvRowTitleSafeTop.toPx() }
    val performanceProfile = LocalLauncherPerformanceProfile.current
    var activeRowId by remember(rows) { mutableStateOf(rows.firstOrNull()?.id) }
    val deepInRows = focusEnabled && (
        listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24
    )

    BackHandler(enabled = deepInRows) {
        coroutineScope.launch {
            listState.animateScrollToItem(0)
            onReturnToHeader()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = TvScreenHorizontalPadding, vertical = TvScreenVerticalPadding)
    ) {
        StaggeredEntrance(index = 0, enabled = animateEntrance) {
            SurfaceHeader(title = title, subtitle = subtitle)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = TvSurfaceBottomPadding)
        ) {
            itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
                StaggeredEntrance(index = index + 1, enabled = animateEntrance) {
                    ContentRow(
                        rowId = row.id,
                        title = row.title,
                        subtitle = row.subtitle,
                        cards = row.cards,
                        onAction = onAction,
                        rowType = row.rowType,
                        style = row.style,
                        isActive = activeRowId == row.id,
                        focusEnabled = focusEnabled,
                        firstCardFocusRequester = if (index == 0) entryFocusRequester else null,
                        firstCardModifier = if (index == 0) {
                            Modifier
                                .focusProperties { up = returnFocusRequester }
                        } else {
                            Modifier
                        },
                        onRowFocused = { rowId, titleTopInWindowPx ->
                            if (activeRowId != rowId) {
                                activeRowId = rowId
                                coroutineScope.launch {
                                    alignFocusedRowTitle(
                                        listState = listState,
                                        titleTopInWindowPx = titleTopInWindowPx,
                                        safeTopPx = safeTopPx,
                                        animate = shouldAnimateRowAlignment(performanceProfile)
                                    )
                                }
                            }
                        },
                        onCardFocused = onCardFocused
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SurfaceHeader(
    title: String,
    subtitle: String
) {
    val textShadow = Shadow(
        color = Color(0x99000000),
        offset = Offset(2f, 2f),
        blurRadius = 4f
    )

    Column {
        Text(
            text = title,
            color = GtvTextPrimary,
            fontSize = TvSurfaceTitleSize,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
            style = TextStyle(shadow = textShadow)
        )
        Text(
            text = subtitle,
            color = GtvTextSecondary,
            fontSize = TvSurfaceSubtitleSize,
            modifier = Modifier.padding(top = 8.dp, bottom = TvSectionSpacing),
            style = TextStyle(shadow = textShadow)
        )
    }
}

private fun HomeCard.toAmbientBackdrop(): AmbientBackdropState? {
    val ambientImage = ambientImageUrl?.takeIf { it.isNotBlank() } ?: imageUrl?.takeIf { it.isNotBlank() }
    return ambientImage?.let {
        AmbientBackdropState(
            title = title,
            imageUrl = it,
            slideshowImages = listOf(it)
        )
    }
}

internal fun calculateDestinationTransitionDirection(
    previous: LauncherDestination,
    target: LauncherDestination
): Int {
    return if (target.ordinal >= previous.ordinal) 1 else -1
}

internal fun calculateAdaptiveAppGridColumns(screenWidthDp: Int): Int {
    return when {
        screenWidthDp >= 1600 -> 6
        screenWidthDp >= 1240 -> 5
        screenWidthDp >= 880 -> 4
        else -> 3
    }
}

private suspend fun alignFocusedRowTitle(
    listState: androidx.compose.foundation.lazy.LazyListState,
    titleTopInWindowPx: Float,
    safeTopPx: Float,
    animate: Boolean
) {
    val delta = titleTopInWindowPx - safeTopPx
    val thresholdPx = if (animate) 6f else 32f
    if (abs(delta) > thresholdPx) {
        if (animate) {
            listState.animateScrollBy(delta)
        } else {
            listState.scrollBy(delta)
        }
    }
}

internal fun reorderAppPackages(
    installedApps: List<InstalledAppItem>,
    packageName: String,
    direction: AppMoveDirection,
    columns: Int
): List<String>? {
    val index = installedApps.indexOfFirst { it.packageName == packageName }
    if (index == -1) return null

    val targetIndex = when (direction) {
        AppMoveDirection.LEFT -> if (index % columns == 0) index else index - 1
        AppMoveDirection.RIGHT -> if (index == installedApps.lastIndex || index % columns == columns - 1) index else index + 1
        AppMoveDirection.UP -> if (index - columns < 0) index else index - columns
        AppMoveDirection.DOWN -> (index + columns).takeIf { it <= installedApps.lastIndex } ?: index
    }

    if (targetIndex == index) return null

    val mutableApps = installedApps.toMutableList()
    val movingApp = mutableApps.removeAt(index)
    mutableApps.add(targetIndex, movingApp)
    return mutableApps.map(InstalledAppItem::packageName)
}
