package com.hotelvision.launcher.data.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * ADB-testable broadcast receiver that triggers a full guest checkout reset.
 *
 * Usage:
 *   adb shell am broadcast -a com.hotelvision.launcher.ACTION_CHECKOUT
 *
 * Registered in AndroidManifest.xml with exported=true for testing.
 * In production this will be locked to a signed PMS intent or removed.
 */
class CheckoutBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CHECKOUT = "com.hotelvision.launcher.ACTION_CHECKOUT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CHECKOUT) {
            Log.i("CheckoutReceiver", "Checkout broadcast received — resetting guest session.")
            CheckoutResetHandler.reset(context)
        }
    }
}
