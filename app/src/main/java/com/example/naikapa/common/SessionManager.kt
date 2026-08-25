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
        private const val KEY_AUTH_TOKEN = "authToken"
        private const val KEY_HAS_MOTOR = "hasMotor"
        private const val KEY_HAS_CAR = "hasCar"
    }

    /** Simpan sesi setelah auth backend berhasil (dengan JWT token). */
    fun saveFullSession(
        userId: Long,
        name: String,
        email: String,
        token: String,
        hasMotor: Boolean = false,
        hasCar: Boolean = false
    ) {
        prefs.edit().apply {
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_AUTH_TOKEN, token)
            putBoolean(KEY_HAS_MOTOR, hasMotor)
            putBoolean(KEY_HAS_CAR, hasCar)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /** Simpan sesi tanpa token (fallback offline / SQLite lokal). */
    fun saveSession(userId: Int, name: String, email: String) {
        saveSession(userId.toLong(), name, email)
    }

    /** Simpan sesi tanpa token (fallback offline / SQLite lokal). */
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

    /** Kembalikan JWT token sebagai header "Bearer <token>", atau null jika belum login via backend. */
    fun getBearerToken(): String? {
        val token = prefs.getString(KEY_AUTH_TOKEN, null) ?: return null
        return "Bearer $token"
    }

    /** Kembalikan raw JWT token string. */
    fun getToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    fun hasMotor(): Boolean = prefs.getBoolean(KEY_HAS_MOTOR, false)

    fun hasCar(): Boolean = prefs.getBoolean(KEY_HAS_CAR, false)

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
}

