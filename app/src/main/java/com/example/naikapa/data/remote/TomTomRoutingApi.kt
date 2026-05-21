package com.example.naikapa.data.remote

import com.example.naikapa.data.model.TomTomRoutingResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TomTomRoutingApi {
    @GET("routing/1/calculateRoute/{from}:{to}/json")
    suspend fun calculateRoute(
        @Path("from", encoded = true) from: String,
        @Path("to", encoded = true) to: String,
        @Query("key") apiKey: String,
        @Query("travelMode") travelMode: String,
        @Query("routeType") routeType: String,
        @Query("maxAlternatives") maxAlternatives: Int,
        @Query("avoid") avoid: String? = null
    ): Response<TomTomRoutingResponse>
}
