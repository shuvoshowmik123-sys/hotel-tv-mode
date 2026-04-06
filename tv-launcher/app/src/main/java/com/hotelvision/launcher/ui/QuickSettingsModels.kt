package com.hotelvision.launcher.ui

import androidx.compose.ui.graphics.vector.ImageVector

data class QuickSettingsItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val statusText: String? = null,
    val icon: ImageVector,
    val action: LauncherAction,
    val isPrimary: Boolean = false
)

data class QuickSettingsStatusSnapshot(
    val networkStatus: String,
    val bluetoothStatus: String,
    val accessibilityStatus: String,
    val displayStatus: String
)
