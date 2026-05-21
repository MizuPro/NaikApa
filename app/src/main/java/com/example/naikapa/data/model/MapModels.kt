package com.example.naikapa.data.model

data class MapPoint(
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val markerType: MapMarkerType = MapMarkerType.TRANSIT
)

data class RoutePolyline(
    val points: List<MapPoint>,
    val color: Int
)

enum class MapMarkerType {
    ORIGIN,
    DESTINATION,
    TRANSIT
}

enum class MapStyle {
    POSITRON,
    DARK_MATTER
}
