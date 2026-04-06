package com.hotelvision.launcher.ui

import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.hotelvision.launcher.ui.components.AppTileArtwork
import com.hotelvision.launcher.ui.components.InstalledAppTileArtwork
import com.hotelvision.launcher.ui.components.artworkTag
import com.hotelvision.launcher.ui.theme.HotelVisionTheme
import org.junit.Rule
import org.junit.Test

class InstalledAppTileArtworkTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun installedAppTileArtwork_rendersBannerBranch() {
        composeRule.setContent {
            HotelVisionTheme {
                Box(modifier = Modifier.size(280.dp, 164.dp)) {
                    InstalledAppTileArtwork(
                        artwork = AppTileArtwork.Banner(ColorDrawable(0xFF22354A.toInt())),
                        contentDescription = "Banner app",
                        stableId = "banner_app",
                        modifier = Modifier
                    )
                }
            }
        }

        composeRule.onNodeWithTag(artworkTag("banner_app", "banner")).assertIsDisplayed()
    }

    @Test
    fun installedAppTileArtwork_rendersCompositedLogoBranch() {
        composeRule.setContent {
            HotelVisionTheme {
                Box(modifier = Modifier.size(280.dp, 164.dp)) {
                    InstalledAppTileArtwork(
                        artwork = AppTileArtwork.CompositedLogo(ColorDrawable(0xFF365BDE.toInt())),
                        contentDescription = "Logo app",
                        stableId = "logo_app",
                        modifier = Modifier
                    )
                }
            }
        }

        composeRule.onNodeWithTag(artworkTag("logo_app", "logo")).assertIsDisplayed()
    }

    @Test
    fun installedAppTileArtwork_rendersCompositedIconBranch() {
        composeRule.setContent {
            HotelVisionTheme {
                Box(modifier = Modifier.size(280.dp, 164.dp)) {
                    InstalledAppTileArtwork(
                        artwork = AppTileArtwork.CompositedIcon(ColorDrawable(0xFF2B4058.toInt())),
                        contentDescription = "Icon app",
                        stableId = "icon_app",
                        modifier = Modifier
                    )
                }
            }
        }

        composeRule.onNodeWithTag(artworkTag("icon_app", "icon")).assertIsDisplayed()
    }

    @Test
    fun installedAppTileArtwork_rendersGenericFallbackBranch() {
        composeRule.setContent {
            HotelVisionTheme {
                Box(modifier = Modifier.size(280.dp, 164.dp)) {
                    InstalledAppTileArtwork(
                        artwork = AppTileArtwork.GenericFallback,
                        contentDescription = "Generic app",
                        stableId = "generic_app",
                        modifier = Modifier
                    )
                }
            }
        }

        composeRule.onNodeWithTag(artworkTag("generic_app", "generic")).assertIsDisplayed()
    }

    @Test
    fun launcherRowsUseInstalledAppArtworkRenderer() {
        composeRule.setContent {
            HotelVisionTheme {
                LauncherScreen(
                    uiState = LauncherUiState(
                        selectedDestination = LauncherDestination.HOME,
                        feedRows = listOf(
                            HomeFeedRow(
                                id = "apps_row",
                                title = "Apps on This TV",
                                subtitle = "",
                                rowType = HomeFeedRowType.APPS_INSTALLED,
                                style = HomeSectionStyle.COMPACT,
                                cards = listOf(
                                    HomeCard(
                                        id = "home_app",
                                        title = "Missing App",
                                        subtitle = "",
                                        supportingText = "",
                                        packageName = "com.example.missing",
                                        badge = "MS",
                                        accentColor = 0xFF365BDE,
                                        cardType = LauncherCardType.APP,
                                        action = LauncherAction.None
                                    )
                                )
                            )
                        ),
                    ),
                    onDestinationSelected = {},
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithTag(artworkTag("home_app", "generic")).assertIsDisplayed()

        composeRule.setContent {
            HotelVisionTheme {
                LauncherScreen(
                    uiState = LauncherUiState(
                        selectedDestination = LauncherDestination.ROOM_SERVICE,
                        feedRows = listOf(
                            HomeFeedRow(
                                id = "apps_row_two",
                                title = "Apps on This TV",
                                subtitle = "",
                                rowType = HomeFeedRowType.APPS_INSTALLED,
                                style = HomeSectionStyle.COMPACT,
                                cards = listOf(
                                    HomeCard(
                                        id = "grid_app",
                                        title = "Missing App",
                                        subtitle = "",
                                        supportingText = "",
                                        packageName = "com.example.missing",
                                        badge = "MS",
                                        accentColor = 0xFF365BDE,
                                        cardType = LauncherCardType.APP,
                                        action = LauncherAction.None
                                    )
                                )
                            )
                        )
                    ),
                    onDestinationSelected = {},
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithTag(artworkTag("grid_app", "generic")).assertIsDisplayed()
    }
}
