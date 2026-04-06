package com.hotelvision.launcher.data.session

import android.content.Context
import android.content.SharedPreferences
import com.hotelvision.launcher.ui.RoomSession

/**
 * Manages the active guest session for the current room.
 * Backed by SharedPreferences — no network or account needed.
 * Cleared on checkout via [CheckoutResetHandler].
 */
object SessionManager {

    private const val PREFS_NAME = "hotel_session"
    private const val KEY_ROOM_ID = "room_id"
    private const val KEY_GUEST_NAME = "guest_name"
    private const val KEY_CHECK_IN = "check_in_ms"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun startSession(context: Context, roomId: String, guestName: String? = null) {
        prefs(context).edit()
            .putString(KEY_ROOM_ID, roomId)
            .putString(KEY_GUEST_NAME, guestName)
            .putLong(KEY_CHECK_IN, System.currentTimeMillis())
            .apply()
    }

    fun getCurrentSession(context: Context): RoomSession? {
        val p = prefs(context)
        val roomId = p.getString(KEY_ROOM_ID, null) ?: return null
        return RoomSession(
            roomId = roomId,
            checkInTimeMs = p.getLong(KEY_CHECK_IN, System.currentTimeMillis()),
            guestName = p.getString(KEY_GUEST_NAME, null)
        )
    }

    /** Call on guest checkout — wipes all session data. */
    fun clearSession(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
