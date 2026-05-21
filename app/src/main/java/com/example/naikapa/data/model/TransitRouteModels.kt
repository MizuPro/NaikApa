package com.example.naikapa.data.model

enum class TransitMode {
    ALL,
    TRANSJAKARTA,
    KRL,
    MRT,
    LRT
}

enum class SortPreference {
    FASTEST,
    CHEAPEST,
    MIN_WALKING,
    FEWEST_TRANSFERS
}

data class RouteMetrics(
    val totalDurationSeconds: Int,
    val totalDistanceMeters: Double,
    val walkingDistanceMeters: Double,
    val transitCount: Int,
    val estimatedFare: Int = 0,
    val estimatedBbm: Int = 0,
    val estimatedTotalCost: Int = estimatedFare + estimatedBbm
)

data class RouteStep(
    val routeId: String,
    val routeShortName: String,
    val routeLongName: String?,
    val agencyId: String,
    val type: TransitEdgeType,
    val fromStop: TransitNode,
    val toStop: TransitNode,
    val stopCount: Int,
    val distanceMeters: Double,
    val durationSeconds: Int
)

data class TransitRouteResult(
    val startStop: TransitNode,
    val endStop: TransitNode,
    val mode: TransitMode,
    val sortPreference: SortPreference,
    val edges: List<TransitEdge>,
    val steps: List<RouteStep>,
    val metrics: RouteMetrics
)
