package com.hotelvision.launcher.setup

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

enum class DefaultLauncherFlow {
    ROLE_MANAGER,
    HOME_CHOOSER,
    VENDOR_SETTINGS,
    ADB_PROVISIONING
}

data class DefaultLauncherUiState(
    val isDefaultLauncher: Boolean,
    val recommendedFlow: DefaultLauncherFlow,
    val recommendedActionLabel: String,
    val statusText: String,
    val vendorSettingsLabel: String?,
    val adbCommand: String
)

data class VendorSettingsOption(
    val label: String,
    val intent: Intent
)

sealed interface DefaultLauncherRequest {
    data class RoleRequest(val intent: Intent) : DefaultLauncherRequest
    data class HomeChooserRequest(val intent: Intent) : DefaultLauncherRequest
    data class VendorSettingsRequest(val option: VendorSettingsOption) : DefaultLauncherRequest
    data object ProvisioningInstructions : DefaultLauncherRequest
}

internal data class DefaultLauncherProgress(
    val roleAttempted: Boolean,
    val homeChooserAttempted: Boolean,
    val vendorSettingsAttempted: Boolean
)

internal fun chooseDefaultLauncherFlow(
    roleAvailable: Boolean,
    progress: DefaultLauncherProgress,
    hasVendorSettings: Boolean
): DefaultLauncherFlow {
    return when {
        roleAvailable && !progress.roleAttempted -> DefaultLauncherFlow.ROLE_MANAGER
        !progress.homeChooserAttempted -> DefaultLauncherFlow.HOME_CHOOSER
        hasVendorSettings && !progress.vendorSettingsAttempted -> DefaultLauncherFlow.VENDOR_SETTINGS
        else -> DefaultLauncherFlow.ADB_PROVISIONING
    }
}

internal fun isResolvedHomeActivity(
    resolvedPackageName: String?,
    resolvedClassName: String?,
    launcherPackageName: String,
    launcherActivityClassName: String
): Boolean {
    return resolvedPackageName == launcherPackageName &&
        (resolvedClassName == launcherActivityClassName ||
            resolvedClassName == ComponentName(launcherPackageName, launcherActivityClassName).className)
}

internal fun buildSetHomeActivityCommand(
    launcherPackageName: String,
    launcherActivityClassName: String
): String {
    return "adb shell cmd package set-home-activity --user 0 $launcherPackageName/$launcherActivityClassName"
}

class DefaultLauncherCoordinator(
    private val context: Context,
    private val launcherPackageName: String,
    private val launcherActivityClassName: String
) {
    private val packageManager: PackageManager
        get() = context.packageManager

    fun buildUiState(): DefaultLauncherUiState {
        val isDefault = isLauncherDefaultHome()
        val progress = DefaultLauncherPromptStore.progress(context)
        val vendorOption = resolveVendorSettingsOption()
        val recommendedFlow = if (isDefault) {
            DefaultLauncherFlow.ADB_PROVISIONING
        } else {
            chooseDefaultLauncherFlow(
                roleAvailable = canRequestHomeRole(progress.roleAttempted),
                progress = progress,
                hasVendorSettings = vendorOption != null
            )
        }

        return DefaultLauncherUiState(
            isDefaultLauncher = isDefault,
            recommendedFlow = recommendedFlow,
            recommendedActionLabel = when {
                isDefault -> "Launcher is active"
                recommendedFlow == DefaultLauncherFlow.ROLE_MANAGER -> "Set as default"
                recommendedFlow == DefaultLauncherFlow.HOME_CHOOSER -> "Open launcher chooser"
                recommendedFlow == DefaultLauncherFlow.VENDOR_SETTINGS -> vendorOption?.label ?: "Open TV settings"
                else -> "Show ADB command"
            },
            statusText = if (isDefault) {
                "Hotel Vision Launcher is already the active home app on this TV."
            } else {
                "This TV is still using another home app. Use the guided flow below or provision it over ADB."
            },
            vendorSettingsLabel = vendorOption?.label,
            adbCommand = buildSetHomeActivityCommand(launcherPackageName, launcherActivityClassName)
        )
    }

    fun resolveRequest(): DefaultLauncherRequest {
        if (isLauncherDefaultHome()) {
            return DefaultLauncherRequest.ProvisioningInstructions
        }

        val progress = DefaultLauncherPromptStore.progress(context)
        val vendorOption = resolveVendorSettingsOption()
        return when (
            chooseDefaultLauncherFlow(
                roleAvailable = canRequestHomeRole(progress.roleAttempted),
                progress = progress,
                hasVendorSettings = vendorOption != null
            )
        ) {
            DefaultLauncherFlow.ROLE_MANAGER -> {
                DefaultLauncherRequest.RoleRequest(
                    intent = requireNotNull(buildRoleRequestIntent()) {
                        "ROLE_HOME request should be available before choosing ROLE_MANAGER flow."
                    }
                )
            }

            DefaultLauncherFlow.HOME_CHOOSER -> {
                DefaultLauncherRequest.HomeChooserRequest(buildHomeChooserIntent())
            }

            DefaultLauncherFlow.VENDOR_SETTINGS -> {
                DefaultLauncherRequest.VendorSettingsRequest(
                    option = requireNotNull(vendorOption) {
                        "Vendor settings should be available before choosing VENDOR_SETTINGS flow."
                    }
                )
            }

            DefaultLauncherFlow.ADB_PROVISIONING -> DefaultLauncherRequest.ProvisioningInstructions
        }
    }

    fun markFlowAttempted(flow: DefaultLauncherFlow) {
        when (flow) {
            DefaultLauncherFlow.ROLE_MANAGER -> DefaultLauncherPromptStore.markRoleAttempted(context)
            DefaultLauncherFlow.HOME_CHOOSER -> DefaultLauncherPromptStore.markHomeChooserAttempted(context)
            DefaultLauncherFlow.VENDOR_SETTINGS -> DefaultLauncherPromptStore.markVendorSettingsAttempted(context)
            DefaultLauncherFlow.ADB_PROVISIONING -> Unit
        }
        DefaultLauncherPromptStore.markPromptPending(context, true)
    }

    fun clearPromptState() {
        DefaultLauncherPromptStore.clear(context)
    }

    fun buildRoleRequestIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME) || roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
            return null
        }
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }

    fun buildHomeChooserIntent(): Intent {
        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun resolveVendorSettingsIntent(): Intent? = resolveVendorSettingsOption()?.intent

    fun isLauncherDefaultHome(): Boolean {
        val resolvedHome = resolveHomeComponent(packageManager)
        return isResolvedHomeActivity(
            resolvedPackageName = resolvedHome?.packageName,
            resolvedClassName = resolvedHome?.className,
            launcherPackageName = launcherPackageName,
            launcherActivityClassName = launcherActivityClassName
        )
    }

    fun resolveVendorSettingsOption(): VendorSettingsOption? {
        return vendorSettingsCandidates()
            .firstNotNullOfOrNull { candidate ->
                candidate(packageManager)
            }
    }

    private fun canRequestHomeRole(roleAttempted: Boolean): Boolean {
        return !roleAttempted && buildRoleRequestIntent() != null
    }

    private fun vendorSettingsCandidates(): List<(PackageManager) -> VendorSettingsOption?> {
        return listOf(
            { packageManager ->
                buildResolvedIntentOption(
                    packageManager = packageManager,
                    label = "Open default app settings",
                    intent = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            { packageManager ->
                buildPackageSettingsOption(
                    packageManager = packageManager,
                    packageName = "com.droidlogic.tv.settings",
                    label = "Open Droidlogic TV settings"
                )
            },
            { packageManager ->
                buildPackageSettingsOption(
                    packageManager = packageManager,
                    packageName = "com.android.tv.settings",
                    label = "Open Android TV settings"
                )
            },
            { packageManager ->
                buildPackageSettingsOption(
                    packageManager = packageManager,
                    packageName = "com.google.android.tvlauncher",
                    label = "Open TV launcher settings"
                )
            },
            { packageManager ->
                buildResolvedIntentOption(
                    packageManager = packageManager,
                    label = "Open system settings",
                    intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )
    }

    private fun buildPackageSettingsOption(
        packageManager: PackageManager,
        packageName: String,
        label: String
    ): VendorSettingsOption? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launchIntent?.let {
            buildResolvedIntentOption(
                packageManager = packageManager,
                label = label,
                intent = it
            )
        }
    }

    private fun buildResolvedIntentOption(
        packageManager: PackageManager,
        label: String,
        intent: Intent
    ): VendorSettingsOption? {
        val resolved = resolveActivity(packageManager, intent) ?: return null
        return VendorSettingsOption(
            label = label,
            intent = intent.setClassName(resolved.packageName, resolved.className)
        )
    }
}

private fun resolveHomeComponent(packageManager: PackageManager): ComponentName? {
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return resolveActivity(packageManager, homeIntent)
}

private fun resolveActivity(
    packageManager: PackageManager,
    intent: Intent
): ComponentName? {
    val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
    val activityInfo = resolveInfo?.activityInfo ?: return null
    if (activityInfo.packageName == "android") return null
    return ComponentName(activityInfo.packageName, activityInfo.name)
}
