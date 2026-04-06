package com.hotelvision.launcher.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultLauncherCoordinatorTest {

    @Test
    fun chooseDefaultLauncherFlow_prefersRoleThenChooserThenVendorThenAdb() {
        assertEquals(
            DefaultLauncherFlow.ROLE_MANAGER,
            chooseDefaultLauncherFlow(
                roleAvailable = true,
                progress = DefaultLauncherProgress(
                    roleAttempted = false,
                    homeChooserAttempted = false,
                    vendorSettingsAttempted = false
                ),
                hasVendorSettings = true
            )
        )

        assertEquals(
            DefaultLauncherFlow.HOME_CHOOSER,
            chooseDefaultLauncherFlow(
                roleAvailable = false,
                progress = DefaultLauncherProgress(
                    roleAttempted = true,
                    homeChooserAttempted = false,
                    vendorSettingsAttempted = false
                ),
                hasVendorSettings = true
            )
        )

        assertEquals(
            DefaultLauncherFlow.VENDOR_SETTINGS,
            chooseDefaultLauncherFlow(
                roleAvailable = false,
                progress = DefaultLauncherProgress(
                    roleAttempted = true,
                    homeChooserAttempted = true,
                    vendorSettingsAttempted = false
                ),
                hasVendorSettings = true
            )
        )

        assertEquals(
            DefaultLauncherFlow.ADB_PROVISIONING,
            chooseDefaultLauncherFlow(
                roleAvailable = false,
                progress = DefaultLauncherProgress(
                    roleAttempted = true,
                    homeChooserAttempted = true,
                    vendorSettingsAttempted = true
                ),
                hasVendorSettings = true
            )
        )
    }

    @Test
    fun isResolvedHomeActivity_matchesLauncherComponent() {
        assertTrue(
            isResolvedHomeActivity(
                resolvedPackageName = "com.hotelvision.launcher",
                resolvedClassName = "com.hotelvision.launcher.MainActivity",
                launcherPackageName = "com.hotelvision.launcher",
                launcherActivityClassName = "com.hotelvision.launcher.MainActivity"
            )
        )

        assertFalse(
            isResolvedHomeActivity(
                resolvedPackageName = "com.other.launcher",
                resolvedClassName = "com.other.launcher.HomeActivity",
                launcherPackageName = "com.hotelvision.launcher",
                launcherActivityClassName = "com.hotelvision.launcher.MainActivity"
            )
        )
    }

    @Test
    fun buildSetHomeActivityCommand_formatsAdbProvisioningCommand() {
        assertEquals(
            "adb shell cmd package set-home-activity --user 0 com.hotelvision.launcher/com.hotelvision.launcher.MainActivity",
            buildSetHomeActivityCommand(
                launcherPackageName = "com.hotelvision.launcher",
                launcherActivityClassName = "com.hotelvision.launcher.MainActivity"
            )
        )
    }
}
