package com.hotelvision.launcher.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import com.hotelvision.launcher.ui.theme.HotelVisionTheme
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LauncherFocusNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun dpadUpFromFirstHomeRailReturnsFocusToHomeTab() {
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
                                        title = "Avex TV",
                                        subtitle = "",
                                        supportingText = "",
                                        badge = "ATV",
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

        composeRule.onNodeWithTag("rail_card_home_app")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("rail_card_home_app")
            .performKeyInput {
                keyDown(Key.DirectionUp)
                keyUp(Key.DirectionUp)
            }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("top_nav_home").assertIsFocused()
    }

    @Test
    fun dpadUpFromFirstRoomServiceCardReturnsFocusToRoomServiceTab() {
        composeRule.setContent {
            HotelVisionTheme {
                LauncherScreen(
                    uiState = LauncherUiState(
                        selectedDestination = LauncherDestination.ROOM_SERVICE,
                        feedRows = listOf(
                            HomeFeedRow(
                                id = "room_service_row",
                                title = "Room Service",
                                subtitle = "",
                                rowType = HomeFeedRowType.HOTEL_FEATURES,
                                style = HomeSectionStyle.STANDARD,
                                cards = listOf(
                                    HomeCard(
                                        id = "room_service_card",
                                        title = "Butler Service",
                                        subtitle = "",
                                        supportingText = "",
                                        badge = "BS",
                                        accentColor = 0xFF365BDE,
                                        cardType = LauncherCardType.SERVICE,
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

        composeRule.onNodeWithTag("rail_card_room_service_card")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("rail_card_room_service_card")
            .performKeyInput {
                keyDown(Key.DirectionUp)
                keyUp(Key.DirectionUp)
            }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("top_nav_room_service").assertIsFocused()
    }

    @Test
    fun openingQuickSettingsMovesFocusToFirstPanelItem() {
        composeRule.setContent {
            HotelVisionTheme {
                LauncherScreen(
                    uiState = LauncherUiState(selectedDestination = LauncherDestination.HOME),
                    onDestinationSelected = {},
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("top_nav_settings_gear")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("top_nav_settings_gear").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quick_settings_item_all_settings").assertIsFocused()
    }

    @Test
    fun backFromQuickSettingsReturnsFocusToGear() {
        composeRule.setContent {
            HotelVisionTheme {
                LauncherScreen(
                    uiState = LauncherUiState(selectedDestination = LauncherDestination.HOME),
                    onDestinationSelected = {},
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("top_nav_settings_gear")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("top_nav_settings_gear").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quick_settings_item_all_settings")
            .performKeyInput {
                keyDown(Key.Back)
                keyUp(Key.Back)
            }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("top_nav_settings_gear").assertIsFocused()
    }

    @Test
    fun leftFromQuickSettingsReturnsFocusToGear() {
        composeRule.setContent {
            HotelVisionTheme {
                LauncherScreen(
                    uiState = LauncherUiState(selectedDestination = LauncherDestination.HOME),
                    onDestinationSelected = {},
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("top_nav_settings_gear")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("top_nav_settings_gear").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quick_settings_item_all_settings")
            .performKeyInput {
                keyDown(Key.DirectionLeft)
                keyUp(Key.DirectionLeft)
            }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("top_nav_settings_gear").assertIsFocused()
    }

    @Test
    fun dpadDownFromRoomServiceTabMovesFocusToFirstCard() {
        composeRule.setContent {
            HotelVisionTheme {
                LauncherScreen(
                    uiState = LauncherUiState(
                        selectedDestination = LauncherDestination.ROOM_SERVICE,
                        feedRows = listOf(
                            HomeFeedRow(
                                id = "room_service_row",
                                title = "Room Service",
                                subtitle = "",
                                rowType = HomeFeedRowType.HOTEL_FEATURES,
                                style = HomeSectionStyle.STANDARD,
                                cards = listOf(
                                    HomeCard(
                                        id = "room_service_card",
                                        title = "Butler Service",
                                        subtitle = "",
                                        supportingText = "",
                                        badge = "BS",
                                        accentColor = 0xFF365BDE,
                                        cardType = LauncherCardType.SERVICE,
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

        composeRule.onNodeWithTag("top_nav_room_service")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("top_nav_room_service")
            .performKeyInput {
                keyDown(Key.DirectionDown)
                keyUp(Key.DirectionDown)
            }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("rail_card_room_service_card").assertIsFocused()
    }

    @Test
    fun longPressOnInstalledAppShowsHotelSafeContextMenu() {
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
                                        id = "com.avex.tv",
                                        title = "Avex TV",
                                        subtitle = "",
                                        supportingText = "",
                                        packageName = "com.avex.tv",
                                        badge = "ATV",
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

        composeRule.onNodeWithTag("rail_card_com.avex.tv")
            .performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("installed_app_context_menu").assertIsDisplayed()
        composeRule.onNodeWithTag("installed_app_context_menu_open").assertIsDisplayed()
        composeRule.onNodeWithTag("installed_app_context_menu_move").assertIsDisplayed()
        composeRule.onNodeWithTag("installed_app_context_menu_app_info").assertIsDisplayed()
        composeRule.onAllNodesWithTag("installed_app_context_menu_uninstall").assertCountEquals(0)
    }

    @Test
    fun openingGuestMessagesMovesFocusToFirstMessage() {
        composeRule.setContent {
            HotelVisionTheme {
                LauncherScreen(
                    uiState = LauncherUiState(
                        guestMessages = listOf(
                            GuestMessageItem(
                                id = "welcome",
                                title = "Welcome",
                                body = "Your concierge is ready.",
                                timestampLabel = "Now",
                                badge = "VIP",
                                action = LauncherAction.None
                            )
                        )
                    ),
                    onDestinationSelected = {},
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("top_nav_notifications")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("top_nav_notifications").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("guest_message_item_welcome").assertIsFocused()
    }

    @Test
    fun backFromGuestMessagesReturnsFocusToNotifications() {
        composeRule.setContent {
            HotelVisionTheme {
                LauncherScreen(
                    uiState = LauncherUiState(
                        guestMessages = listOf(
                            GuestMessageItem(
                                id = "welcome",
                                title = "Welcome",
                                body = "Your concierge is ready.",
                                timestampLabel = "Now",
                                badge = "VIP",
                                action = LauncherAction.None
                            )
                        )
                    ),
                    onDestinationSelected = {},
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("top_nav_notifications")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("top_nav_notifications").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("guest_message_item_welcome")
            .performKeyInput {
                keyDown(Key.Back)
                keyUp(Key.Back)
            }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("top_nav_notifications").assertIsFocused()
    }
}
