package com.example.naikapa.data.remote

import com.example.naikapa.common.AppConstants
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RemoteClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val fallbackInterceptor = Interceptor { chain ->
        val request = chain.request()
        try {
            chain.proceed(request)
        } catch (e: Exception) {
            val host = request.url.host
            if (host.contains("michaelk.fun") || host.contains("herokuapp.com")) {
                val fallbackHost = if (host.contains("michaelk.fun")) {
                    "naikapa-backend-56cbdd1ec8a0.herokuapp.com"
                } else {
                    "naikapa-backend.michaelk.fun"
                }
                val newUrl = request.url.newBuilder()
                    .host(fallbackHost)
                    .build()
                val newRequest = request.newBuilder()
                    .url(newUrl)
                    .build()
                chain.proceed(newRequest)
            } else {
                throw e
            }
        }
    }

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(fallbackInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofitTomTom = Retrofit.Builder()
        .baseUrl(AppConstants.TOMTOM_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val retrofitBackend = Retrofit.Builder()
        .baseUrl(AppConstants.BACKEND_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val tomTomSearchApi: TomTomSearchApi = retrofitTomTom.create(TomTomSearchApi::class.java)
    val tomTomRoutingApi: TomTomRoutingApi = retrofitTomTom.create(TomTomRoutingApi::class.java)
    val delayApi: DelayApi = retrofitBackend.create(DelayApi::class.java)
    val authApi: AuthApi = retrofitBackend.create(AuthApi::class.java)
}
