package com.example.concordmobile_android.data.local

import android.content.Context
import com.example.concordmobile_android.data.model.Session

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("concord_session", Context.MODE_PRIVATE)

    fun read(): Session? {
        val token = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        return Session(
            userId = prefs.getInt(KEY_USER_ID, 0),
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            token = token
        )
    }

    fun save(session: Session) {
        prefs.edit()
            .putInt(KEY_USER_ID, session.userId)
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_TOKEN, session.token)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_TOKEN = "token"
    }
}
