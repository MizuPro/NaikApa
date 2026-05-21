package com.example.naikapa.data.model

data class TomTomSearchResponse(
    val results: List<TomTomSearchResult> = emptyList()
)

data class TomTomSearchResult(
    val poi: TomTomPoi? = null,
    val address: TomTomAddress? = null,
    val position: TomTomPosition? = null
)

data class TomTomPoi(
    val name: String? = null
)

data class TomTomAddress(
    val freeformAddress: String? = null
)

data class TomTomPosition(
    val lat: Double? = null,
    val lon: Double? = null
)
