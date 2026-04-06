package com.hotelvision.launcher.data.device

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.hotelvision.launcher.ui.InstalledAppItem
import com.hotelvision.launcher.ui.LauncherAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidAppsProviderTest {

    @Test
    fun getInstalledApps_includesHomeLauncherOnlyOnceAndUsesHomeIntent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val shadowPackageManager = shadowOf(context.packageManager)
        val provider = AndroidAppsProvider(context)

        val homeResolveInfo = createResolveInfo(
            packageName = "com.example.systemlauncher",
            label = "System Launcher",
            isSystemApp = true
        )

        shadowPackageManager.addResolveInfoForIntent(homeIntent(), homeResolveInfo)
        shadowPackageManager.addResolveInfoForIntent(launcherIntent(), homeResolveInfo)

        val apps = provider.getInstalledApps()

        assertEquals(1, apps.count { it.packageName == "com.example.systemlauncher" })
        val homeLauncher = apps.single { it.packageName == "com.example.systemlauncher" }
        assertEquals("System home launcher", homeLauncher.subtitle)
        assertTrue(homeLauncher.isSystemApp)
        assertEquals("com.example.systemlauncher.MainActivity", homeLauncher.launchActivityClassName)

        val action = homeLauncher.action as LauncherAction.LaunchIntent
        assertTrue(action.intent.categories?.contains(Intent.CATEGORY_HOME) == true)
        assertEquals("com.example.systemlauncher", action.intent.component?.packageName)
        assertEquals("com.example.systemlauncher.MainActivity", action.intent.component?.className)
    }

    @Test
    fun observeInstalledApps_emitsUpdatedListWhenPackageAddedBroadcastArrives() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val packageManager = context.packageManager
        val shadowPackageManager = shadowOf(packageManager)
        val provider = AndroidAppsProvider(context)

        shadowPackageManager.addResolveInfoForIntent(
            leanbackIntent(),
            createResolveInfo(packageName = "com.example.initial", label = "Initial TV")
        )

        val emissions = mutableListOf<List<InstalledAppItem>>()
        val job = collectApps(provider, emissions)

        shadowOf(Looper.getMainLooper()).idle()
        waitUntil { emissions.size == 1 }
        assertTrue(emissions.first().any { it.packageName == "com.example.initial" })
        assertEquals(1, emissions.first().count { it.packageName == "com.example.initial" })

        shadowPackageManager.addResolveInfoForIntent(
            leanbackIntent(),
            createResolveInfo(packageName = "com.example.newapp", label = "New TV")
        )
        context.sendBroadcast(Intent(Intent.ACTION_PACKAGE_ADDED, Uri.parse("package:com.example.newapp")))

        shadowOf(Looper.getMainLooper()).idle()
        waitUntil { emissions.size >= 2 }
        assertTrue(emissions.last().any { it.packageName == "com.example.initial" })
        assertTrue(emissions.last().any { it.packageName == "com.example.newapp" })
        assertEquals(1, emissions.last().count { it.packageName == "com.example.initial" })
        assertEquals(1, emissions.last().count { it.packageName == "com.example.newapp" })

        job.cancel()
    }

    @Test
    fun observeInstalledApps_suppressesDuplicateEmissionWhenPackageBroadcastDoesNotChangeList() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val shadowPackageManager = shadowOf(context.packageManager)
        val provider = AndroidAppsProvider(context)

        shadowPackageManager.addResolveInfoForIntent(
            leanbackIntent(),
            createResolveInfo(packageName = "com.example.same", label = "Same TV")
        )

        val emissions = mutableListOf<List<InstalledAppItem>>()
        val job = collectApps(provider, emissions)

        shadowOf(Looper.getMainLooper()).idle()
        waitUntil { emissions.size == 1 }

        context.sendBroadcast(Intent(Intent.ACTION_PACKAGE_CHANGED, Uri.parse("package:com.example.same")))
        shadowOf(Looper.getMainLooper()).idle()
        delay(500)

        assertEquals(1, emissions.size)
        assertTrue(emissions.single().all { it.packageName != context.packageName })

        job.cancel()
    }

    private fun createResolveInfo(
        packageName: String,
        label: String,
        isSystemApp: Boolean = false
    ): ResolveInfo {
        val applicationInfo = ApplicationInfo().apply {
            this.packageName = packageName
            if (isSystemApp) {
                flags = flags or ApplicationInfo.FLAG_SYSTEM
            }
        }

        val activityInfo = ActivityInfo().apply {
            this.packageName = packageName
            name = "$packageName.MainActivity"
            this.applicationInfo = applicationInfo
        }

        return ResolveInfo().apply {
            this.activityInfo = activityInfo
            nonLocalizedLabel = label
        }
    }

    private fun leanbackIntent(): Intent {
        return Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    }

    private fun launcherIntent(): Intent {
        return Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    }

    private fun homeIntent(): Intent {
        return Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    }

    private fun CoroutineScope.collectApps(
        provider: AndroidAppsProvider,
        emissions: MutableList<List<InstalledAppItem>>
    ): Job {
        return launch {
            provider.observeInstalledApps()
                .onEach { emissions += it }
                .collect {}
        }
    }

    private suspend fun waitUntil(
        timeoutMs: Long = 5_000L,
        intervalMs: Long = 50L,
        condition: () -> Boolean
    ) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition()) return
            delay(intervalMs)
        }
        error("Condition was not met within ${timeoutMs}ms")
    }
}
