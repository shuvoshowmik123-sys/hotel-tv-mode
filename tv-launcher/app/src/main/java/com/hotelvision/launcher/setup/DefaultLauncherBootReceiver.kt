package com.hotelvision.launcher.setup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hotelvision.launcher.MainActivity

class DefaultLauncherBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
            return
        }

        val coordinator = DefaultLauncherCoordinator(
            context = context,
            launcherPackageName = context.packageName,
            launcherActivityClassName = MainActivity::class.java.name
        )

        if (!coordinator.isLauncherDefaultHome()) {
            DefaultLauncherPromptStore.markPromptPending(context, true)
        } else {
            coordinator.clearPromptState()
        }
    }
}
