package com.example.naikapa.domain.routing

import com.example.naikapa.data.model.RouteMetrics
import com.example.naikapa.data.model.RouteStep
import com.example.naikapa.data.model.TransitEdge
import com.example.naikapa.data.model.TransitEdgeType

class TransitRouteMetricsCalculator(
    private val fareCalculator: FareCalculator = FareCalculator()
) {
    fun calculate(edges: List<TransitEdge>, steps: List<RouteStep>): RouteMetrics {
        val totalDurationSeconds = edges.sumOf { it.durationSeconds }
        val totalDistanceMeters = edges.sumOf { it.distanceMeters }
        val walkingDistanceMeters = edges
            .filter { it.type == TransitEdgeType.WALKING }
            .sumOf { it.distanceMeters }
        val transitCount = countTransitChanges(edges)
        val estimatedFare = fareCalculator.calculateTransitFare(steps)
        val estimatedBbm = 0

        return RouteMetrics(
            totalDurationSeconds = totalDurationSeconds,
            totalDistanceMeters = totalDistanceMeters,
            walkingDistanceMeters = walkingDistanceMeters,
            transitCount = transitCount,
            estimatedFare = estimatedFare,
            estimatedBbm = estimatedBbm,
            estimatedTotalCost = estimatedFare + estimatedBbm
        )
    }

    private fun countTransitChanges(edges: List<TransitEdge>): Int {
        var previousTransit: TransitEdge? = null
        var changes = 0
        edges.filter { it.type == TransitEdgeType.TRANSIT }.forEach { edge ->
            val previous = previousTransit
            if (previous != null && (previous.routeId != edge.routeId || previous.agencyId != edge.agencyId)) {
                changes += 1
            }
            previousTransit = edge
        }
        return changes
    }
}
