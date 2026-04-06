package com.hotelvision.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.hotelvision.launcher.ui.InstalledAppItem
import com.hotelvision.launcher.ui.LauncherAction
import com.hotelvision.launcher.ui.theme.GtvCardBackground
import com.hotelvision.launcher.ui.theme.GtvCardBackgroundHover
import com.hotelvision.launcher.ui.theme.GtvFocusGlow
import com.hotelvision.launcher.ui.theme.GtvTextPrimary
import com.hotelvision.launcher.ui.theme.GtvTextSecondary
import com.hotelvision.launcher.ui.theme.TvScreenHorizontalPadding
import com.hotelvision.launcher.ui.theme.TvScreenVerticalPadding

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AllAppsScreen(
    allApps: List<InstalledAppItem>,
    onAction: (LauncherAction) -> Unit,
    onBack: () -> Unit
) {
    val gridState = rememberLazyGridState()
    val firstFocusRequester = remember { FocusRequester() }

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        firstFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TvScreenHorizontalPadding, vertical = TvScreenVerticalPadding)
        ) {
            Column {
                Text(
                    text = "All Apps",
                    color = GtvTextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All installed applications on this TV",
                    color = GtvTextSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(
                start = TvScreenHorizontalPadding,
                end = TvScreenHorizontalPadding,
                bottom = TvScreenVerticalPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(allApps, key = { it.id }) { app ->
                AllAppTile(
                    app = app,
                    focusRequester = if (app.id == allApps.firstOrNull()?.id) firstFocusRequester else null,
                    onAction = onAction
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AllAppTile(
    app: InstalledAppItem,
    focusRequester: FocusRequester? = null,
    onAction: (LauncherAction) -> Unit
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    val tileFocusRequester = focusRequester ?: remember { FocusRequester() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Box(
            modifier = Modifier
                .focusRequester(tileFocusRequester)
                .size(100.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GtvCardBackground)
                .gtvFocusScale(
                    focusedScale = 1.08f,
                    cornerRadius = 20.dp,
                    onClick = { onAction(app.action) }
                )
        ) {
            val icon = remember(app.packageName) {
                try {
                    val pm = context.packageManager
                    pm.getApplicationIcon(app.packageName)
                } catch (_: Exception) {
                    null
                }
            }

            if (icon != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(icon)
                        .crossfade(true)
                        .build(),
                    contentDescription = app.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1C2A3A), Color(0xFF0D1520))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.title.take(2).uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = app.title,
            color = if (isFocused) GtvTextPrimary else GtvTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
        )
    }
}
