package com.hotelvision.launcher.data.session

import android.content.Context

/**
 * Handles the full guest checkout reset sequence.
 *
 * Call on guest checkout to wipe the active session.
 * Trigger via [CheckoutBroadcastReceiver] during testing:
 *   adb shell am broadcast -a com.hotelvision.launcher.ACTION_CHECKOUT
 */
object CheckoutResetHandler {

    fun reset(context: Context) {
        // 1. Clear session prefs (guest name, room id, check-in time)
        SessionManager.clearSession(context)
        GuestPersonalizationStore.clear(context)

        // 2. Future phase: clear DB guest usage via injected DAO
        android.util.Log.i("CheckoutReset", "Guest session cleared. Room ready for next guest.")
    }
}
