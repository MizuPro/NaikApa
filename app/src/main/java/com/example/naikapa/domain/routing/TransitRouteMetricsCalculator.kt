package com.example.naikapa.domain.routing

import com.example.naikapa.data.model.RouteMetrics
import com.example.naikapa.data.model.TransitEdge
import com.example.naikapa.data.model.TransitEdgeType

class TransitRouteMetricsCalculator {
    fun calculate(edges: List<TransitEdge>): RouteMetrics {
        val totalDurationSeconds = edges.sumOf { it.durationSeconds }
        val totalDistanceMeters = edges.sumOf { it.distanceMeters }
        val walkingDistanceMeters = edges
            .filter { it.type == TransitEdgeType.WALKING }
            .sumOf { it.distanceMeters }
        val transitCount = countTransitChanges(edges)

        return RouteMetrics(
            totalDurationSeconds = totalDurationSeconds,
            totalDistanceMeters = totalDistanceMeters,
            walkingDistanceMeters = walkingDistanceMeters,
            transitCount = transitCount
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
