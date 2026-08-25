package com.example.naikapa.data.repository

import com.example.naikapa.common.SessionManager
import com.example.naikapa.data.local.UserDao
import com.example.naikapa.data.model.LoginRequest
import com.example.naikapa.data.model.RegisterRequest
import com.example.naikapa.data.model.User
import com.example.naikapa.data.remote.AuthApi
import com.example.naikapa.data.remote.RemoteClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * AuthRepository — strategi Cloud-First, Offline Fallback.
 *
 * Register → hanya via cloud (tidak ada offline fallback).
 * Login   → coba cloud, jika offline fallback ke SQLite lokal.
 */
class AuthRepository(
    private val authApi: AuthApi = RemoteClient.authApi,
    private val sessionManager: SessionManager,
    private val userDao: UserDao? = null // Opsional untuk offline fallback login
) {

    /**
     * Register user baru ke backend cloud.
     * Jika berhasil, sesi (JWT token + user data) disimpan ke SessionManager.
     *
     * @return Result.success(Unit) jika berhasil, Result.failure(Exception) dengan pesan error yang jelas.
     */
    suspend fun register(
        nama: String,
        email: String,
        password: String,
        hasMotor: Boolean,
        hasCar: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = authApi.register(
                RegisterRequest(
                    nama = nama,
                    email = email,
                    password = password,
                    hasMotor = hasMotor,
                    hasCar = hasCar
                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()?.data
                if (authData != null) {
                    sessionManager.saveFullSession(
                        userId = authData.user.idUser,
                        name = authData.user.nama,
                        email = authData.user.email,
                        token = authData.token,
                        hasMotor = authData.user.hasMotor,
                        hasCar = authData.user.hasCar
                    )
                    userDao?.saveOrUpdateCloudUser(
                        User(
                            idUser = authData.user.idUser,
                            nama = authData.user.nama,
                            email = authData.user.email,
                            password = password,
                            hasMotor = authData.user.hasMotor,
                            hasCar = authData.user.hasCar
                        )
                    )
                    return@withContext Result.success(Unit)
                }
            }

            // Parse error body untuk mendapatkan pesan yang informatif
            val errorBody = response.errorBody()?.string()
            val errorMessage = parseErrorMessage(errorBody, response.code())
            return@withContext Result.failure(Exception(errorMessage))

        } catch (e: Exception) {
            return@withContext Result.failure(
                Exception("Tidak dapat terhubung ke server. Periksa koneksi internet Anda.")
            )
        }
    }

    /**
     * Login user — Cloud-First, Offline Fallback ke SQLite lokal jika tidak ada koneksi.
     *
     * @return Result.success(Unit) jika berhasil, Result.failure(Exception) dengan pesan error yang jelas.
     */
    suspend fun login(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        // ── Coba login via Cloud ──────────────────────────────────────────
        try {
            val response = authApi.login(LoginRequest(email = email, password = password))

            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()?.data
                if (authData != null) {
                    sessionManager.saveFullSession(
                        userId = authData.user.idUser,
                        name = authData.user.nama,
                        email = authData.user.email,
                        token = authData.token,
                        hasMotor = authData.user.hasMotor,
                        hasCar = authData.user.hasCar
                    )
                    userDao?.saveOrUpdateCloudUser(
                        User(
                            idUser = authData.user.idUser,
                            nama = authData.user.nama,
                            email = authData.user.email,
                            password = password,
                            hasMotor = authData.user.hasMotor,
                            hasCar = authData.user.hasCar
                        )
                    )
                    return@withContext Result.success(Unit)
                }
            }

            // Server menjawab tapi dengan error (401, 422, dll)
            val errorBody = response.errorBody()?.string()
            val errorMessage = parseErrorMessage(errorBody, response.code())
            return@withContext Result.failure(Exception(errorMessage))

        } catch (_: Exception) {
            // Network error — lanjut ke offline fallback
        }

        // ── Offline Fallback: login via SQLite lokal ──────────────────────
        val dao = userDao
        if (dao != null) {
            val localUser = dao.login(email, password)
            if (localUser != null) {
                // Login berhasil secara offline — simpan sesi tanpa token JWT
                sessionManager.saveSession(localUser.idUser, localUser.nama, localUser.email)
                return@withContext Result.success(Unit)
            }
        }

        return@withContext Result.failure(
            Exception("Tidak dapat terhubung ke server dan data akun tidak tersedia secara offline.")
        )
    }

    /** Parse pesan error dari response body JSON backend. */
    private fun parseErrorMessage(errorBodyString: String?, httpCode: Int): String {
        if (!errorBodyString.isNullOrBlank()) {
            try {
                val json = JSONObject(errorBodyString)
                val message = json.optString("message")
                if (message.isNotBlank()) return message
            } catch (_: Exception) {}
        }
        return when (httpCode) {
            401 -> "Email atau password salah."
            409 -> "Email sudah terdaftar. Gunakan email lain atau login."
            422 -> "Data yang dimasukkan tidak valid."
            500 -> "Terjadi kesalahan pada server. Coba beberapa saat lagi."
            else -> "Terjadi kesalahan (kode: $httpCode). Coba lagi."
        }
    }
}
