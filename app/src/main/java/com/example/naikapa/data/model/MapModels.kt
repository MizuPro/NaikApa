package com.example.naikapa.data.model

data class MapPoint(
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val markerType: MapMarkerType = MapMarkerType.TRANSIT,
    val transitRole: TransitMarkerRole = TransitMarkerRole.NONE
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

enum class TransitMarkerRole {
    NONE,
    BOARD,   // Naik di halte/stasiun ini
    ALIGHT   // Turun di halte/stasiun ini
}

enum class MapStyle {
    POSITRON,
    DARK_MATTER
}
