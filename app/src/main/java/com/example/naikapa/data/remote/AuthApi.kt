package com.example.naikapa.data.remote

import com.example.naikapa.data.model.AuthApiResponse
import com.example.naikapa.data.model.AuthResponseDto
import com.example.naikapa.data.model.LoginRequest
import com.example.naikapa.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthApiResponse<AuthResponseDto>>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthApiResponse<AuthResponseDto>>

    @GET("api/v1/auth/me")
    suspend fun getMe(
        @Header("Authorization") bearerToken: String
    ): Response<AuthApiResponse<AuthResponseDto>>
}
