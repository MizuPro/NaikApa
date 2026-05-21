package com.example.naikapa.data.model

enum class PrivateVehicleMode {
    MOTOR,
    CAR
}

data class TomTomRoutingResponse(
    val routes: List<TomTomRoute> = emptyList()
)

data class TomTomRoute(
    val summary: TomTomRouteSummary? = null,
    val legs: List<TomTomRouteLeg> = emptyList()
)

data class TomTomRouteSummary(
    val lengthInMeters: Int? = null,
    val travelTimeInSeconds: Int? = null
)

data class TomTomRouteLeg(
    val points: List<TomTomRoutePoint> = emptyList()
)

data class TomTomRoutePoint(
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class PrivateVehicleRouteResult(
    val mode: PrivateVehicleMode,
    val distanceMeters: Int,
    val travelTimeSeconds: Int,
    val estimatedBbm: Int,
    val estimatedTotalCost: Int,
    val points: List<MapPoint>,
    val alternativeIndex: Int
)
