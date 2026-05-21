package com.example.naikapa.data.remote

import com.example.naikapa.data.model.TomTomSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TomTomSearchApi {
    @GET("search/2/search/{query}.json")
    suspend fun search(
        @Path("query", encoded = false) query: String,
        @Query("key") apiKey: String,
        @Query("countrySet") countrySet: String,
        @Query("typeahead") typeahead: Boolean,
        @Query("limit") limit: Int,
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double
    ): Response<TomTomSearchResponse>
}
