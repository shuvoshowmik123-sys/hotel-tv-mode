package com.hotelvision.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.hotelvision.launcher.ui.HomeCard
import com.hotelvision.launcher.ui.HomeFeedRowType
import com.hotelvision.launcher.ui.HomeSectionStyle
import com.hotelvision.launcher.ui.LauncherAction
import com.hotelvision.launcher.ui.theme.GtvTextPrimary
import com.hotelvision.launcher.ui.theme.GtvTextSecondary
import com.hotelvision.launcher.ui.theme.TvRowSpacing
import com.hotelvision.launcher.ui.theme.TvScreenHorizontalPadding
import com.hotelvision.launcher.ui.theme.TvSectionSpacing
import com.hotelvision.launcher.ui.theme.TvSurfaceSubtitleSize
import com.hotelvision.launcher.ui.theme.TvSurfaceTitleSize

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContentRow(
    rowId: String,
    title: String,
    subtitle: String,
    cards: List<HomeCard>,
    onAction: (LauncherAction) -> Unit,
    rowType: HomeFeedRowType,
    style: HomeSectionStyle,
    isActive: Boolean = false,
    focusEnabled: Boolean = true,
    firstCardFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    firstCardModifier: Modifier = Modifier,
    onRowFocused: (rowId: String, titleTopInWindowPx: Float) -> Unit = { _, _ -> },
    onCardFocused: (HomeCard) -> Unit,
    modifier: Modifier = Modifier
) {
    if (cards.isEmpty()) return

    val titleAlpha = animateFloatAsState(
        targetValue = if (isActive) 1f else 0.5f,
        animationSpec = tween(durationMillis = 180),
        label = "RowTitleAlpha"
    )
    var titleTopInWindowPx = 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = GtvTextPrimary,
            fontSize = TvSurfaceTitleSize,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(horizontal = TvScreenHorizontalPadding, vertical = 6.dp)
                .onGloballyPositioned { coordinates ->
                    titleTopInWindowPx = coordinates.boundsInWindow().top
                }
                .graphicsLayer { alpha = titleAlpha.value }
        )
        Text(
            text = subtitle,
            color = GtvTextSecondary,
            fontSize = TvSurfaceSubtitleSize,
            modifier = Modifier
                .padding(horizontal = TvScreenHorizontalPadding, vertical = 6.dp)
                .graphicsLayer { alpha = titleAlpha.value }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = TvScreenHorizontalPadding, vertical = TvRowSpacing),
            horizontalArrangement = Arrangement.spacedBy(TvRowSpacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                RailCard(
                    card = card,
                    focusRequester = if (index == 0) firstCardFocusRequester else null,
                    focusModifier = if (index == 0) firstCardModifier else Modifier,
                    focusEnabled = focusEnabled,
                    onAction = onAction,
                    style = style,
                    rowType = rowType,
                    onFocused = { card ->
                        onRowFocused(rowId, titleTopInWindowPx)
                        onCardFocused(card)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(TvSectionSpacing / 4))
    }
}
