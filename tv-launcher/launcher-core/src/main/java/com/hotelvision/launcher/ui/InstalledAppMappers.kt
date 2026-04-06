package com.hotelvision.launcher.ui

internal fun InstalledAppItem.toInstalledAppHomeCard(): HomeCard {
    return HomeCard(
        id = id,
        title = title,
        subtitle = "",
        supportingText = "",
        sourceLabel = "",
        packageName = packageName,
        launchActivityClassName = launchActivityClassName,
        badge = badge,
        accentColor = if (isSystemApp) 0xFF365BDE else 0xFF2B4058,
        cardType = LauncherCardType.APP,
        action = action
    )
}
