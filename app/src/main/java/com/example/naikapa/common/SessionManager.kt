package com.example.naikapa.common

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "NaikApaSession"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USER_EMAIL = "userEmail"
        private const val KEY_USER_NAME = "userName"
        private const val KEY_THEME_MODE = "themeMode"

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }

    fun saveSession(userId: Int, name: String, email: String) {
        saveSession(userId.toLong(), name, email)
    }

    fun saveSession(userId: Long, name: String, email: String) {
        prefs.edit().apply {
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserId(): Long {
        return if (prefs.contains(KEY_USER_ID)) {
            try {
                prefs.getLong(KEY_USER_ID, -1L)
            } catch (_: ClassCastException) {
                prefs.getInt(KEY_USER_ID, -1).toLong()
            }
        } else {
            -1L
        }
    }

    fun updateSessionUser(name: String, email: String) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun saveThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, THEME_LIGHT) ?: THEME_LIGHT
    }

    fun isDarkMode(): Boolean = getThemeMode() == THEME_DARK
}
