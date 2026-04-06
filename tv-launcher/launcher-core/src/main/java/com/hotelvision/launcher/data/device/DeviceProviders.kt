package com.hotelvision.launcher.data.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethod
import com.hotelvision.launcher.ui.InstalledAppItem
import com.hotelvision.launcher.ui.LauncherAction
import com.hotelvision.launcher.ui.SourceItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface AppsProvider {
    suspend fun getInstalledApps(): List<InstalledAppItem>
    fun observeInstalledApps(): Flow<List<InstalledAppItem>>
}

/**
 * Whitelist-based Apps Provider for VIP app filtering.
 * Only returns apps whose package names match the hardcoded whitelist.
 */
interface WhitelistAppsProvider {
    suspend fun getWhitelistedApps(): List<InstalledAppItem>
    fun observeWhitelistedApps(): Flow<List<InstalledAppItem>>
}

interface InputsProvider {
    suspend fun getSourceItems(): List<SourceItem>
}

@Singleton
class AndroidAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : AppsProvider {

    private val hiddenSystemPackages = setOf(
        "com.android.inputmethod.latin",
        "com.google.android.inputmethod.latin",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.android.settings",
        "com.android.tv.settings",
        "com.google.android.katniss",
        "com.google.android.tvlauncher",
        "com.google.android.apps.tv.launcherx",
        "com.android.provision",
        "com.google.android.setupwizard"
    )

    override suspend fun getInstalledApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        queryInstalledApps()
    }

    override fun observeInstalledApps(): Flow<List<InstalledAppItem>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                launch(Dispatchers.IO) {
                    trySend(queryInstalledApps())
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, intentFilter)
        }

        launch(Dispatchers.IO) {
            trySend(queryInstalledApps())
        }

        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }.distinctUntilChanged { old, new ->
        old.map(InstalledAppItem::packageName) == new.map(InstalledAppItem::packageName)
    }

    private fun queryInstalledApps(): List<InstalledAppItem> {
        val packageManager = context.packageManager
        val leanbackActivities = queryLeanbackActivities(packageManager)
        val launcherActivities = queryLauncherActivities(packageManager)
        val inputMethodPackages = queryInputMethodPackages(packageManager)

        val leanbackPackages = leanbackActivities
            .associateBy { it.activityInfo?.packageName }
            .filterKeys { it != null }
        val launcherPackages = launcherActivities
            .associateBy { it.activityInfo?.packageName }
            .filterKeys { it != null }

        return (leanbackActivities + launcherActivities)
            .mapNotNull { candidate ->
                val packageName = candidate.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null
                if (packageName in inputMethodPackages) return@mapNotNull null
                if (shouldHidePackage(packageName)) return@mapNotNull null

                val leanbackResolveInfo = leanbackPackages[packageName]
                val launcherResolveInfo = launcherPackages[packageName]

                val launchIntent = resolveLaunchIntent(
                    packageManager = packageManager,
                    packageName = packageName,
                    leanbackResolveInfo = leanbackResolveInfo
                )
                val resolvedIntent = launchIntent ?: return@mapNotNull null
                val preferredResolveInfo = leanbackResolveInfo ?: launcherResolveInfo ?: candidate

                val label = preferredResolveInfo
                    .loadLabel(packageManager)
                    ?.toString()
                    ?.trim()
                    .orEmpty()
                if (label.isBlank()) return@mapNotNull null

                val systemApp = preferredResolveInfo
                    .activityInfo
                    ?.applicationInfo
                    ?.let(::isSystemApp)
                    ?: false
                InstalledAppItem(
                    id = packageName,
                    packageName = packageName,
                    launchActivityClassName = preferredResolveInfo.activityInfo?.name,
                    title = label,
                    subtitle = when {
                        systemApp -> "System launcher app"
                        else -> "Installed TV app"
                    },
                    badge = badgeFrom(label),
                    isSystemApp = systemApp,
                    action = LauncherAction.LaunchIntent(resolvedIntent)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
    }

    private fun queryLauncherActivities(packageManager: PackageManager): List<ResolveInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return queryActivities(packageManager, launcherIntent)
    }

    private fun queryLeanbackActivities(packageManager: PackageManager): List<ResolveInfo> {
        val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        return queryActivities(packageManager, leanbackIntent)
    }

    private fun resolveLaunchIntent(
        packageManager: PackageManager,
        packageName: String,
        leanbackResolveInfo: ResolveInfo?
    ): Intent? {
        val leanbackLaunchIntent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
        if (leanbackLaunchIntent != null) {
            return leanbackLaunchIntent
        }

        leanbackResolveInfo?.let {
            return createExplicitLauncherIntent(
                resolveInfo = it,
                category = Intent.CATEGORY_LEANBACK_LAUNCHER
            )
        }

        return packageManager.getLaunchIntentForPackage(packageName)
    }

    private fun queryInputMethodPackages(packageManager: PackageManager): Set<String> {
        val intent = Intent(InputMethod.SERVICE_INTERFACE)
        return queryServices(packageManager, intent)
            .mapNotNull { it.serviceInfo?.packageName }
            .toSet()
    }

    private fun shouldHidePackage(packageName: String): Boolean {
        if (packageName in hiddenSystemPackages) return true

        val normalized = packageName.lowercase(Locale.getDefault())
        return normalized.contains("inputmethod") ||
            normalized.contains("keyboard") ||
            normalized.contains("packageinstaller") ||
            normalized.contains("setupwizard")
    }

    private fun createExplicitLauncherIntent(
        resolveInfo: ResolveInfo,
        category: String
    ): Intent? {
        val activityInfo = resolveInfo.activityInfo ?: return null
        return Intent(Intent.ACTION_MAIN)
            .addCategory(category)
            .setClassName(activityInfo.packageName, activityInfo.name)
    }

    @Suppress("DEPRECATION")
    private fun queryServices(
        packageManager: PackageManager,
        intent: Intent
    ): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentServices(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            packageManager.queryIntentServices(intent, PackageManager.MATCH_ALL)
        }
    }

    @Suppress("DEPRECATION")
    private fun queryActivities(
        packageManager: PackageManager,
        intent: Intent
    ): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
    }

    private fun isSystemApp(applicationInfo: ApplicationInfo): Boolean {
        return applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    private fun badgeFrom(label: String): String {
        return label
            .split(" ")
            .mapNotNull { part -> part.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { label.take(2).uppercase(Locale.getDefault()) }
            .take(3)
    }
}

@Singleton
class AndroidInputsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : InputsProvider {

    override suspend fun getSourceItems(): List<SourceItem> = withContext(Dispatchers.IO) {
        val tvInputManager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
        val systemInputs = tvInputManager
            ?.tvInputList
            ?.mapNotNull { inputInfo -> mapInput(inputInfo) }
            .orEmpty()

        if (systemInputs.isNotEmpty()) {
            systemInputs.sortedBy { it.title.lowercase(Locale.getDefault()) }
        } else {
            fallbackInputs()
        }
    }

    private fun mapInput(inputInfo: TvInputInfo): SourceItem? {
        val label = inputInfo.loadLabel(context)?.toString()?.trim().orEmpty()
        if (label.isBlank()) return null

        return SourceItem(
            id = inputInfo.id,
            title = label,
            subtitle = inputTypeSubtitle(inputInfo.type),
            badge = inputTypeBadge(inputInfo.type),
            isSystemProvided = true,
            action = LauncherAction.LaunchIntent(
                Intent(
                    Intent.ACTION_VIEW,
                    TvContract.buildChannelUriForPassthroughInput(inputInfo.id)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        )
    }

    private fun fallbackInputs(): List<SourceItem> {
        return listOf(
            SourceItem(
                id = "live_tv",
                title = "Live TV",
                subtitle = "Open hotel channel lineup",
                badge = "TV",
                isSystemProvided = false,
                action = LauncherAction.None
            ),
            SourceItem(
                id = "hdmi_1",
                title = "HDMI 1",
                subtitle = "External player or console",
                badge = "HD1",
                isSystemProvided = false,
                action = LauncherAction.None
            ),
            SourceItem(
                id = "hdmi_2",
                title = "HDMI 2",
                subtitle = "Secondary external source",
                badge = "HD2",
                isSystemProvided = false,
                action = LauncherAction.None
            ),
            SourceItem(
                id = "source_settings",
                title = "Source Settings",
                subtitle = "Open TV device settings",
                badge = "SET",
                isSystemProvided = false,
                action = LauncherAction.LaunchIntent(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            )
        )
    }

    private fun inputTypeSubtitle(type: Int): String {
        return when (type) {
            TvInputInfo.TYPE_TUNER -> "Broadcast or live channel input"
            TvInputInfo.TYPE_HDMI -> "HDMI device input"
            TvInputInfo.TYPE_COMPONENT -> "Component input"
            TvInputInfo.TYPE_DISPLAY_PORT -> "DisplayPort input"
            else -> "TV input source"
        }
    }

    private fun inputTypeBadge(type: Int): String {
        return when (type) {
            TvInputInfo.TYPE_TUNER -> "TV"
            TvInputInfo.TYPE_HDMI -> "HD"
            TvInputInfo.TYPE_COMPONENT -> "AV"
            TvInputInfo.TYPE_DISPLAY_PORT -> "DP"
            else -> "IN"
        }
    }
}

/**
 * VIP Whitelist Apps Provider - Only shows approved apps on home screen.
 * Filters PackageManager results by hardcoded whitelist.
 */
@Singleton
class WhitelistAppsProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WhitelistAppsProvider {

    companion object {
        // VIP App Whitelist - Strict filtering for home screen
        private val VIP_APP_WHITELIST = listOf(
            "com.google.android.youtube.tv",
            "com.avex.tv"
        )
    }

    override suspend fun getWhitelistedApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        queryWhitelistedApps()
    }

    override fun observeWhitelistedApps(): Flow<List<InstalledAppItem>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                launch(Dispatchers.IO) {
                    trySend(queryWhitelistedApps())
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, intentFilter)
        }

        launch(Dispatchers.IO) {
            trySend(queryWhitelistedApps())
        }

        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }.distinctUntilChanged { old, new ->
        old.map(InstalledAppItem::packageName) == new.map(InstalledAppItem::packageName)
    }

    private fun queryWhitelistedApps(): List<InstalledAppItem> {
        val packageManager = context.packageManager
        val whitelistedApps = mutableListOf<InstalledAppItem>()

        for (packageName in VIP_APP_WHITELIST) {
            try {
                val leanbackIntent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
                val launchIntent = leanbackIntent ?: packageManager.getLaunchIntentForPackage(packageName)
                
                if (launchIntent != null) {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val label = packageManager.getApplicationLabel(appInfo).toString().trim()
                    
                    if (label.isNotBlank()) {
                        whitelistedApps.add(
                            InstalledAppItem(
                                id = packageName,
                                packageName = packageName,
                                launchActivityClassName = launchIntent.component?.className,
                                title = label,
                                subtitle = "Available on this TV",
                                badge = badgeFrom(label),
                                isSystemApp = false,
                                action = LauncherAction.LaunchIntent(launchIntent)
                            )
                        )
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // App not installed - skip silently
            }
        }

        return whitelistedApps
    }

    private fun badgeFrom(label: String): String {
        return label
            .split(" ")
            .mapNotNull { part -> part.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { label.take(2).uppercase(Locale.getDefault()) }
            .take(3)
    }
}
