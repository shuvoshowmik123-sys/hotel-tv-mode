package com.hotelvision.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.hotelvision.launcher.setup.DefaultLauncherFlow
import com.hotelvision.launcher.setup.DefaultLauncherUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DefaultLauncherPromptOverlay(
    visible: Boolean,
    launcherSetupState: DefaultLauncherUiState?,
    onRequestDefaultLauncher: () -> Unit,
    onOpenVendorSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible || launcherSetupState == null) return

    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            delay(120)
            runCatching { primaryFocusRequester.requestFocus() }
        }
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .testTag("default_launcher_prompt")
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(720.dp)
                .background(Color(0xEE10161D), RoundedCornerShape(28.dp))
                .padding(horizontal = 36.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = if (launcherSetupState.isDefaultLauncher) {
                    "Launcher Ready"
                } else {
                    "Set Hotel Vision as Default Launcher"
                },
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = launcherSetupState.statusText,
                color = Color(0xFFD6DEE6),
                fontSize = 18.sp,
                lineHeight = 24.sp
            )
            Text(
                text = when (launcherSetupState.recommendedFlow) {
                    DefaultLauncherFlow.ROLE_MANAGER -> "Recommended next step: ask Android to grant the Home role directly."
                    DefaultLauncherFlow.HOME_CHOOSER -> "Recommended next step: open the system launcher chooser."
                    DefaultLauncherFlow.VENDOR_SETTINGS -> "Recommended next step: open the TV settings package exposed by this board."
                    DefaultLauncherFlow.ADB_PROVISIONING -> "If the firmware blocks app-side flows, provision the launcher over ADB with the command below."
                },
                color = Color(0xFFAAB6C3),
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF151D26), RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ADB provisioning command",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = launcherSetupState.adbCommand,
                    color = Color(0xFFD6DEE6),
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                PromptActionButton(
                    text = launcherSetupState.recommendedActionLabel,
                    focusRequester = primaryFocusRequester,
                    onClick = onRequestDefaultLauncher,
                    modifier = Modifier.weight(1f)
                )
                val vendorSettingsLabel = launcherSetupState.vendorSettingsLabel
                if (vendorSettingsLabel != null) {
                    PromptActionButton(
                        text = vendorSettingsLabel,
                        onClick = onOpenVendorSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
                PromptActionButton(
                    text = "Later",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PromptActionButton(
    text: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .gtvFocusScale(
                focusedScale = 1.04f,
                cornerRadius = 18.dp,
                onClick = onClick
            )
            .background(
                color = if (isFocused) Color.White else Color(0xFF273342),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = text,
            color = if (isFocused) Color(0xFF10161D) else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
