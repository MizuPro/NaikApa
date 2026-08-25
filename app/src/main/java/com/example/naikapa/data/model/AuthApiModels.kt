package com.example.naikapa.data.model

import com.google.gson.annotations.SerializedName

// ── Request DTOs ───────────────────────────────────────────────────────────

data class RegisterRequest(
    @SerializedName("nama") val nama: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("has_motor") val hasMotor: Boolean = false,
    @SerializedName("has_car") val hasCar: Boolean = false
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

// ── Response DTOs ──────────────────────────────────────────────────────────

data class AuthUserDto(
    @SerializedName("id_user") val idUser: Long,
    @SerializedName("nama") val nama: String,
    @SerializedName("email") val email: String,
    @SerializedName("has_motor") val hasMotor: Boolean,
    @SerializedName("has_car") val hasCar: Boolean
)

data class AuthResponseDto(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: AuthUserDto
)

data class AuthApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)
