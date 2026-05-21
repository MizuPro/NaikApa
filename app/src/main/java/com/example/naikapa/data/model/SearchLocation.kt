package com.example.naikapa.data.model

data class SearchLocation(
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val source: String = SOURCE_TOMTOM,
    // Metadata opsional untuk hasil GTFS lokal
    val stopId: String? = null,
    val agencyId: String? = null,
    val stopType: String? = null,
    val distanceMeters: Double? = null
) {
    companion object {
        const val SOURCE_TOMTOM = "tomtom"
        const val SOURCE_GTFS = "gtfs"
    }
}
