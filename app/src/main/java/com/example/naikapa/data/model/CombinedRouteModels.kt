package com.example.naikapa.data.model

enum class CombinedRouteSegmentType {
    PRIVATE_VEHICLE,
    TRANSIT,
    WALKING
}

data class CombinedRouteStopCandidate(
    val stopId: String,
    val stopName: String,
    val latitude: Double,
    val longitude: Double,
    val agencyId: String,
    val stopType: String?,
    val distanceMeters: Double
) {
    fun toTransitNode(): TransitNode = TransitNode(
        stopId = stopId,
        stopName = stopName,
        lat = latitude,
        lon = longitude,
        agencyId = agencyId,
        stopType = stopType
    )
}

data class CombinedRouteSegment(
    val type: CombinedRouteSegmentType,
    val title: String,
    val durationSeconds: Int,
    val distanceMeters: Double,
    val estimatedFare: Int = 0,
    val estimatedBbm: Int = 0,
    val points: List<MapPoint> = emptyList(),
    val transitResult: TransitRouteResult? = null,
    val privateVehicleResult: PrivateVehicleRouteResult? = null
)

data class CombinedRouteResult(
    val privateVehicleMode: PrivateVehicleMode,
    val originStop: CombinedRouteStopCandidate,
    val destinationStop: CombinedRouteStopCandidate,
    val segments: List<CombinedRouteSegment>,
    val metrics: RouteMetrics
) {
    val transitResult: TransitRouteResult?
        get() = segments.firstNotNullOfOrNull { it.transitResult }

    val privateVehicleResult: PrivateVehicleRouteResult?
        get() = segments.firstNotNullOfOrNull { it.privateVehicleResult }
}
