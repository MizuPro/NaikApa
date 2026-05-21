package com.example.naikapa.domain.routing

import com.example.naikapa.data.model.GtfsAdjacentStopConnection
import com.example.naikapa.data.model.GtfsStop
import com.example.naikapa.data.model.TransitEdge
import com.example.naikapa.data.model.TransitEdgeType
import com.example.naikapa.data.model.TransitGraph
import com.example.naikapa.data.model.TransitGraphStats
import com.example.naikapa.data.model.TransitNode
import kotlin.math.roundToInt

class TransitGraphBuilder(
    private val walkingTransferBuilder: WalkingTransferBuilder = WalkingTransferBuilder()
) {
    fun build(
        stops: List<GtfsStop>,
        connections: List<GtfsAdjacentStopConnection>
    ): TransitGraph {
        val nodes = buildNodes(stops, connections)
        val transitEdges = buildTransitEdges(connections)
        val walkingEdges = walkingTransferBuilder.build(stops.filter { nodes.containsKey(it.stopId) })
        val allEdges = transitEdges + walkingEdges
        val adjacency = allEdges.groupBy { it.fromStopId }
            .mapValues { (_, edges) -> edges.sortedWith(compareBy<TransitEdge> { it.type }.thenBy { it.durationSeconds }) }

        return TransitGraph(
            nodes = nodes,
            adjacency = adjacency,
            stats = TransitGraphStats(
                nodeCount = nodes.size,
                transitEdgeCount = transitEdges.size,
                walkingEdgeCount = walkingEdges.size
            )
        )
    }

    private fun buildNodes(
        stops: List<GtfsStop>,
        connections: List<GtfsAdjacentStopConnection>
    ): Map<String, TransitNode> {
        val result = linkedMapOf<String, TransitNode>()
        stops.forEach { stop ->
            result[stop.stopId] = TransitNode(
                stopId = stop.stopId,
                stopName = stop.stopName,
                lat = stop.stopLat,
                lon = stop.stopLon,
                agencyId = stop.agencyId,
                stopType = stop.stopType
            )
        }
        connections.forEach { connection ->
            result.putIfAbsent(
                connection.fromStopId,
                TransitNode(
                    stopId = connection.fromStopId,
                    stopName = connection.fromStopName,
                    lat = connection.fromStopLat,
                    lon = connection.fromStopLon,
                    agencyId = connection.fromAgencyId,
                    stopType = connection.fromStopType
                )
            )
            result.putIfAbsent(
                connection.toStopId,
                TransitNode(
                    stopId = connection.toStopId,
                    stopName = connection.toStopName,
                    lat = connection.toStopLat,
                    lon = connection.toStopLon,
                    agencyId = connection.toAgencyId,
                    stopType = connection.toStopType
                )
            )
        }
        return result
    }

    private fun buildTransitEdges(connections: List<GtfsAdjacentStopConnection>): List<TransitEdge> {
        return connections
            .groupBy { TransitEdgeKey(it.fromStopId, it.toStopId, it.routeId, it.agencyId) }
            .map { (_, group) ->
                val first = group.first()
                val averageDuration = group.map {
                    GtfsTimeParser.durationSeconds(it.departureTime, it.arrivalTime)
                }.average().roundToInt().coerceAtLeast(0)
                TransitEdge(
                    fromStopId = first.fromStopId,
                    toStopId = first.toStopId,
                    routeId = first.routeId,
                    routeShortName = first.routeShortName ?: first.routeId,
                    routeLongName = first.routeLongName,
                    routeColor = first.routeColor,
                    routeTextColor = first.routeTextColor,
                    agencyId = first.agencyId,
                    distanceMeters = GeoDistanceCalculator.haversineMeters(
                        first.fromStopLat,
                        first.fromStopLon,
                        first.toStopLat,
                        first.toStopLon
                    ),
                    durationSeconds = averageDuration,
                    type = TransitEdgeType.TRANSIT
                )
            }
    }

    private data class TransitEdgeKey(
        val fromStopId: String,
        val toStopId: String,
        val routeId: String,
        val agencyId: String
    )
}
