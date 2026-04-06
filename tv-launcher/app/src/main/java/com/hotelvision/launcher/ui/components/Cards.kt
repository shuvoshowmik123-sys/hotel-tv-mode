package com.hotelvision.launcher.ui.components

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.hotelvision.launcher.performance.LauncherImageKind
import com.hotelvision.launcher.performance.LocalLauncherPerformanceProfile
import com.hotelvision.launcher.performance.buildLauncherImageRequest
import com.hotelvision.launcher.performance.shouldAnimateCardMetadata
import com.hotelvision.launcher.performance.shouldBringCardIntoView
import com.hotelvision.launcher.ui.AppMoveDirection
import com.hotelvision.launcher.ui.HomeCard
import com.hotelvision.launcher.ui.HomeFeedRowType
import com.hotelvision.launcher.ui.HomeSectionStyle
import com.hotelvision.launcher.ui.InstalledAppItem
import com.hotelvision.launcher.ui.LauncherAction
import com.hotelvision.launcher.ui.LauncherCardType
import com.hotelvision.launcher.ui.QuickAction
import com.hotelvision.launcher.ui.SourceItem
import com.hotelvision.launcher.ui.theme.GtvCardBackground
import com.hotelvision.launcher.ui.theme.GtvCardBackgroundHover
import com.hotelvision.launcher.ui.theme.GtvBadgeBackground
import com.hotelvision.launcher.ui.theme.GtvAccentBlue
import com.hotelvision.launcher.ui.theme.GtvFocusGlow
import com.hotelvision.launcher.ui.theme.GtvTextPrimary
import com.hotelvision.launcher.ui.theme.GtvTextSecondary
import kotlinx.coroutines.launch

private const val INSTALLED_RAIL_CONTEXT_MENU_SCALE = 1.06f
private const val APP_GRID_CONTEXT_MENU_SCALE = 1.06f

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RailCard(
    card: HomeCard,
    focusModifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    focusEnabled: Boolean = true,
    onAction: (LauncherAction) -> Unit,
    style: HomeSectionStyle,
    rowType: HomeFeedRowType,
    onFocused: (HomeCard) -> Unit,
    modifier: Modifier = Modifier
) {
    val performanceProfile = LocalLauncherPerformanceProfile.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val isInstalledAppRow = rowType == HomeFeedRowType.APPS_INSTALLED
    val width = when {
        isInstalledAppRow -> 64.dp
        rowType == HomeFeedRowType.APP_RECOMMENDATIONS -> 280.dp
        style == HomeSectionStyle.COMPACT -> 220.dp
        else -> 260.dp
    }
    val height = when {
        isInstalledAppRow -> 64.dp
        rowType == HomeFeedRowType.APP_RECOMMENDATIONS -> 158.dp
        style == HomeSectionStyle.COMPACT -> 124.dp
        else -> 146.dp
    }

    var isFocused by remember { mutableStateOf(false) }
    var contextMenuAnchor by remember { mutableStateOf<ContextMenuAnchorBounds?>(null) }
    var contextMenuOpen by remember { mutableStateOf(false) }
    val tileFocusRequester = focusRequester ?: remember { FocusRequester() }
    val view = LocalView.current

    Column(
        modifier = modifier.onFocusChanged { state ->
            isFocused = state.isFocused
            if (state.isFocused) {
                val bounds = contextMenuAnchor?.bounds
                if (bounds != null && shouldBringCardIntoView(
                        itemLeftPx = bounds.left,
                        itemTopPx = bounds.top,
                        itemRightPx = bounds.right,
                        itemBottomPx = bounds.bottom,
                        viewportWidthPx = view.width,
                        viewportHeightPx = view.height
                    )) {
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                }
                onFocused(card)
            }
        }
    ) {
        val cardImageRequest = remember(card.imageUrl, performanceProfile) {
            card.imageUrl?.let {
                buildLauncherImageRequest(
                    context = context,
                    data = it,
                    imageKind = LauncherImageKind.CARD,
                    profile = performanceProfile
                )
            }
        }
        Box(
            modifier = Modifier
                .focusRequester(tileFocusRequester)
                .then(focusModifier)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onGloballyPositioned { coordinates ->
                    contextMenuAnchor = ContextMenuAnchorBounds(
                        bounds = coordinates.boundsInWindow().toIntRect(),
                        focusedScale = INSTALLED_RAIL_CONTEXT_MENU_SCALE
                    )
                }
                .testTag("rail_card_${card.id}")
                .width(width)
                .height(height)
                .gtvFocusScale(
                    focusedScale = if (isInstalledAppRow) 1.08f else 1.08f,
                    cornerRadius = if (isInstalledAppRow) 40.dp else 12.dp,
                    enabled = focusEnabled,
                    onLongClick = if (isInstalledAppRow && card.packageName != null) {
                        {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            contextMenuOpen = true
                        }
                    } else null,
                    onClick = { onAction(card.action) }
                )
                .clip(if (isInstalledAppRow) androidx.compose.foundation.shape.CircleShape else RoundedCornerShape(12.dp))
                .background(GtvCardBackground)
        ) {
            if (rowType == HomeFeedRowType.APPS_INSTALLED && card.packageName != null) {
                InstalledAppTileArtwork(
                    packageName = card.packageName,
                    launchActivityClassName = card.launchActivityClassName,
                    contentDescription = card.title,
                    stableId = card.id,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (cardImageRequest != null) {
                AsyncImage(
                    model = cardImageRequest,
                    contentDescription = card.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1C2A3A), Color(0xFF0D1520))
                            )
                        )
                )
            }
        }

        val packageName = card.packageName
        if (contextMenuOpen && packageName != null && contextMenuAnchor != null) {
            InstalledAppContextMenuPopup(
                anchorBounds = contextMenuAnchor!!,
                packageName = packageName,
                appTitle = card.title,
                tileFocusRequester = tileFocusRequester,
                onDismissRequest = { contextMenuOpen = false },
                onAction = onAction
            )
        }

        // Fixed height slot below the card for the animated metadata
        Box(
            modifier = Modifier
                .width(width)
                .height(56.dp)
                .padding(top = 8.dp)
        ) {
            if (shouldAnimateCardMetadata(performanceProfile)) {
                this@Column.AnimatedVisibility(
                    visible = isFocused,
                    enter = fadeIn(animationSpec = tween(160)) + slideInVertically(animationSpec = tween(160)) { it / 2 },
                    exit = fadeOut(animationSpec = tween(120)) + slideOutVertically(animationSpec = tween(120)) { it / 2 }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = card.title,
                            color = GtvTextPrimary,
                            fontSize = if (rowType == HomeFeedRowType.APP_RECOMMENDATIONS) 16.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else if (isFocused) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = card.title,
                        color = GtvTextPrimary,
                        fontSize = if (rowType == HomeFeedRowType.APP_RECOMMENDATIONS) 16.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QuickActionChip(
    quickAction: QuickAction,
    onAction: (LauncherAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .width(280.dp)
            .gtvFocusScale(
                focusedScale = 1.08f, 
                cornerRadius = 16.dp, 
                onClick = { onAction(quickAction.action) }
            )
            .clip(RoundedCornerShape(16.dp))
            .background(GtvCardBackground)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        CardBadge(
            badge = quickAction.badge,
            highlight = true
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = quickAction.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = quickAction.description,
                color = Color(0xFF9CB0C5),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppGridCard(
    app: InstalledAppItem,
    focusModifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    focusEnabled: Boolean = true,
    moveMode: Boolean = false,
    isMoveTarget: Boolean = false,
    onMoveDirectional: (AppMoveDirection) -> Boolean = { false },
    onFinishMove: () -> Unit = {},
    onAction: (LauncherAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val performanceProfile = LocalLauncherPerformanceProfile.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }
    var contextMenuAnchor by remember { mutableStateOf<ContextMenuAnchorBounds?>(null) }
    var contextMenuOpen by remember { mutableStateOf(false) }
    val tileFocusRequester = focusRequester ?: remember { FocusRequester() }
    val view = LocalView.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.onFocusChanged {
            isFocused = it.isFocused
            if (it.isFocused) {
                val bounds = contextMenuAnchor?.bounds
                if (bounds != null && shouldBringCardIntoView(
                        itemLeftPx = bounds.left,
                        itemTopPx = bounds.top,
                        itemRightPx = bounds.right,
                        itemBottomPx = bounds.bottom,
                        viewportWidthPx = view.width,
                        viewportHeightPx = view.height
                    )) {
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
        }
    ) {
        // The tile itself
        Box(
            modifier = Modifier
                .focusRequester(tileFocusRequester)
                .then(focusModifier)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onGloballyPositioned { coordinates ->
                    contextMenuAnchor = ContextMenuAnchorBounds(
                        bounds = coordinates.boundsInWindow().toIntRect(),
                        focusedScale = APP_GRID_CONTEXT_MENU_SCALE
                    )
                }
                .onPreviewKeyEvent { event ->
                    if (!moveMode || !isMoveTarget || event.type != KeyEventType.KeyUp) {
                        return@onPreviewKeyEvent false
                    }

                    when (event.key) {
                        Key.DirectionLeft -> {
                            onMoveDirectional(AppMoveDirection.LEFT)
                            true
                        }
                        Key.DirectionRight -> {
                            onMoveDirectional(AppMoveDirection.RIGHT)
                            true
                        }
                        Key.DirectionUp -> {
                            onMoveDirectional(AppMoveDirection.UP)
                            true
                        }
                        Key.DirectionDown -> {
                            onMoveDirectional(AppMoveDirection.DOWN)
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Back -> {
                            onFinishMove()
                            true
                        }
                        else -> false
                    }
                }
                .testTag("app_grid_${app.id}")
                .aspectRatio(16f / 9f)
                .gtvFocusScale(
                    focusedScale = APP_GRID_CONTEXT_MENU_SCALE,
                    cornerRadius = 12.dp,
                    enabled = focusEnabled,
                    onLongClick = if (moveMode) {
                        null
                    } else {
                        {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            contextMenuOpen = true
                        }
                    },
                    onClick = {
                        if (moveMode && isMoveTarget) {
                            onFinishMove()
                        } else {
                            onAction(app.action)
                        }
                    }
                )
                .clip(RoundedCornerShape(12.dp))
                .background(GtvCardBackground)
        ) {
            InstalledAppTileArtwork(
                packageName = app.packageName,
                launchActivityClassName = app.launchActivityClassName,
                contentDescription = app.title,
                stableId = app.id,
                modifier = Modifier.fillMaxSize()
            )
            if (moveMode && isMoveTarget) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xD9101318))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("app_move_mode_badge")
                ) {
                    Text(
                        text = "Move",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Reserved label slot — always occupies space to prevent layout shifts
        if (!moveMode && contextMenuOpen && contextMenuAnchor != null) {
            InstalledAppContextMenuPopup(
                anchorBounds = contextMenuAnchor!!,
                packageName = app.packageName,
                appTitle = app.title,
                tileFocusRequester = tileFocusRequester,
                onDismissRequest = { contextMenuOpen = false },
                onAction = onAction
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (shouldAnimateCardMetadata(performanceProfile)) {
                this@Column.AnimatedVisibility(
                    visible = isFocused,
                    enter = fadeIn(animationSpec = tween(160)) +
                            slideInVertically(animationSpec = tween(160)) { it / 2 },
                    exit = fadeOut(animationSpec = tween(120)) +
                            slideOutVertically(animationSpec = tween(120)) { it / 2 }
                ) {
                    Text(
                        text = app.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                    )
                }
            } else if (isFocused) {
                Text(
                    text = app.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SourceGridCard(
    source: SourceItem,
    onAction: (LauncherAction) -> Unit,
    focusEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    var sourceBounds by remember { mutableStateOf<IntRect?>(null) }
    Column(
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onGloballyPositioned { coordinates ->
                sourceBounds = coordinates.boundsInWindow().toIntRect()
            }
            .onFocusChanged { state ->
                if (state.isFocused && sourceBounds != null && shouldBringCardIntoView(
                        itemLeftPx = sourceBounds!!.left,
                        itemTopPx = sourceBounds!!.top,
                        itemRightPx = sourceBounds!!.right,
                        itemBottomPx = sourceBounds!!.bottom,
                        viewportWidthPx = view.width,
                        viewportHeightPx = view.height
                    )) {
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
            .height(140.dp)
            .gtvFocusScale(
                focusedScale = 1.08f, 
                cornerRadius = 12.dp,
                enabled = focusEnabled,
                onClick = { onAction(source.action) }
            )
            .clip(RoundedCornerShape(12.dp))
            .background(GtvCardBackground)
            .padding(18.dp)
    ) {
        CardBadge(
            badge = source.badge,
            highlight = source.isSystemProvided
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = source.title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = source.subtitle,
            color = Color(0xFF9CB0C5),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CardBadge(
    badge: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlight) GtvAccentBlue else GtvBadgeBackground)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = badge,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun androidx.compose.ui.geometry.Rect.toIntRect(): IntRect = IntRect(
    left = left.toInt(),
    top = top.toInt(),
    right = right.toInt(),
    bottom = bottom.toInt()
)
