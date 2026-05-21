package com.example.naikapa.data.model

enum class TransitEdgeType {
    TRANSIT,
    WALKING
}

data class TransitNode(
    val stopId: String,
    val stopName: String,
    val lat: Double,
    val lon: Double,
    val agencyId: String,
    val stopType: String? = null
)

data class TransitEdge(
    val fromStopId: String,
    val toStopId: String,
    val routeId: String,
    val routeShortName: String,
    val routeLongName: String?,
    val routeColor: String?,
    val routeTextColor: String?,
    val agencyId: String,
    val distanceMeters: Double,
    val durationSeconds: Int,
    val type: TransitEdgeType
)

data class TransitGraphStats(
    val nodeCount: Int,
    val transitEdgeCount: Int,
    val walkingEdgeCount: Int
) {
    val totalEdgeCount: Int
        get() = transitEdgeCount + walkingEdgeCount
}

data class TransitGraph(
    val nodes: Map<String, TransitNode>,
    val adjacency: Map<String, List<TransitEdge>>,
    val stats: TransitGraphStats
)
