package com.example.naikapa.data.repository

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.model.SearchLocation
import com.example.naikapa.data.model.TomTomSearchResult
import com.example.naikapa.data.remote.TomTomSearchApi

class TomTomSearchRepository(
    private val api: TomTomSearchApi
) {
    suspend fun search(
        query: String,
        apiKey: String,
        latitudeBias: Double = AppConstants.MAP_DEFAULT_LAT,
        longitudeBias: Double = AppConstants.MAP_DEFAULT_LON
    ): Result<List<SearchLocation>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < AppConstants.TOMTOM_MIN_QUERY_LENGTH) {
            return Result.success(emptyList())
        }
        if (apiKey.isBlank() || apiKey == AppConstants.TOMTOM_API_KEY_PLACEHOLDER) {
            return Result.failure(IllegalStateException("TomTom API key belum diisi"))
        }

        return runCatching {
            val response = api.search(
                query = trimmedQuery,
                apiKey = apiKey,
                countrySet = AppConstants.TOMTOM_COUNTRY_SET_ID,
                typeahead = AppConstants.TOMTOM_TYPEAHEAD,
                limit = AppConstants.TOMTOM_SEARCH_LIMIT,
                latitude = latitudeBias,
                longitude = longitudeBias
            )
            if (!response.isSuccessful) {
                error("TomTom Search gagal: HTTP ${response.code()}")
            }
            response.body()?.results.orEmpty().mapNotNull(::toSearchLocation)
        }
    }

    fun toSearchLocation(result: TomTomSearchResult): SearchLocation? {
        val position = result.position ?: return null
        val latitude = position.lat ?: return null
        val longitude = position.lon ?: return null
        val address = result.address?.freeformAddress
        val name = result.poi?.name?.takeIf { it.isNotBlank() }
            ?: address?.takeIf { it.isNotBlank() }
            ?: return null

        return SearchLocation(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude
        )
    }
}
