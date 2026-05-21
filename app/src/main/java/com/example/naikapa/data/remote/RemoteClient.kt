package com.example.naikapa.data.remote

import com.example.naikapa.common.AppConstants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RemoteClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(AppConstants.TOMTOM_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val tomTomSearchApi: TomTomSearchApi = retrofit.create(TomTomSearchApi::class.java)
    val tomTomRoutingApi: TomTomRoutingApi = retrofit.create(TomTomRoutingApi::class.java)
}
