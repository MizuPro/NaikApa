package com.example.naikapa.domain.routing

import com.example.naikapa.data.model.RouteStep
import com.example.naikapa.data.model.TransitEdge
import com.example.naikapa.data.model.TransitEdgeType
import com.example.naikapa.data.model.TransitGraph

class RouteStepBuilder {
    fun build(graph: TransitGraph, edges: List<TransitEdge>): List<RouteStep> {
        if (edges.isEmpty()) return emptyList()

        val result = mutableListOf<RouteStep>()
        val currentGroup = mutableListOf<TransitEdge>()

        edges.forEach { edge ->
            if (currentGroup.isEmpty()) {
                currentGroup.add(edge)
                return@forEach
            }
            val previous = currentGroup.last()
            if (canMerge(previous, edge)) {
                currentGroup.add(edge)
            } else {
                result.add(currentGroup.toRouteStep(graph))
                currentGroup.clear()
                currentGroup.add(edge)
            }
        }

        if (currentGroup.isNotEmpty()) result.add(currentGroup.toRouteStep(graph))
        return result
    }

    private fun canMerge(previous: TransitEdge, next: TransitEdge): Boolean {
        if (previous.type == TransitEdgeType.WALKING || next.type == TransitEdgeType.WALKING) return false
        return previous.routeId == next.routeId &&
            previous.agencyId == next.agencyId &&
            previous.toStopId == next.fromStopId
    }

    private fun List<TransitEdge>.toRouteStep(graph: TransitGraph): RouteStep {
        val first = first()
        val last = last()
        return RouteStep(
            routeId = first.routeId,
            routeShortName = first.routeShortName,
            routeLongName = first.routeLongName,
            routeColor = first.routeColor,
            routeTextColor = first.routeTextColor,
            agencyId = first.agencyId,
            type = first.type,
            fromStop = requireNotNull(graph.nodes[first.fromStopId]) { "Missing node ${first.fromStopId}" },
            toStop = requireNotNull(graph.nodes[last.toStopId]) { "Missing node ${last.toStopId}" },
            stopCount = size + 1,
            distanceMeters = sumOf { it.distanceMeters },
            durationSeconds = sumOf { it.durationSeconds }
        )
    }
}
