package com.example.naikapa.domain.routing

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.model.GtfsStop
import com.example.naikapa.data.model.TransitEdge
import com.example.naikapa.data.model.TransitEdgeType
import kotlin.math.roundToInt

class WalkingTransferBuilder(
    private val radiusMeters: Int = AppConstants.WALKING_TRANSFER_RADIUS_M,
    private val walkingSpeedKmh: Double = AppConstants.WALKING_SPEED_KMH
) {
    private val gridSizeDegrees = 0.005

    fun build(stops: List<GtfsStop>): List<TransitEdge> {
        val edges = mutableListOf<TransitEdge>()
        val grid = stops.groupBy { GridCell.from(it, gridSizeDegrees) }
        val comparedPairs = mutableSetOf<String>()

        for ((cell, cellStops) in grid) {
            val nearbyStops = cell.neighbors().flatMap { grid[it].orEmpty() }
            cellStops.forEach { from ->
                nearbyStops.forEach { to ->
                    if (from.stopId == to.stopId) return@forEach
                    val pairKey = orderedPairKey(from.stopId, to.stopId)
                    if (!comparedPairs.add(pairKey)) return@forEach
                    if (from.agencyId == to.agencyId) return@forEach

                    val distanceMeters = GeoDistanceCalculator.haversineMeters(
                        from.stopLat,
                        from.stopLon,
                        to.stopLat,
                        to.stopLon
                    )
                    if (distanceMeters > radiusMeters) return@forEach

                    edges.add(createEdge(from.stopId, to.stopId, distanceMeters))
                    edges.add(createEdge(to.stopId, from.stopId, distanceMeters))
                }
            }
        }
        return edges
    }

    private fun createEdge(fromStopId: String, toStopId: String, distanceMeters: Double): TransitEdge {
        val durationSeconds = ((distanceMeters / 1000.0) / walkingSpeedKmh * 3600.0).roundToInt()
        return TransitEdge(
            fromStopId = fromStopId,
            toStopId = toStopId,
            routeId = AppConstants.WALKING_ROUTE_ID,
            routeShortName = AppConstants.WALKING_ROUTE_NAME,
            routeLongName = AppConstants.WALKING_ROUTE_NAME,
            routeColor = null,
            routeTextColor = null,
            agencyId = AppConstants.WALKING_ROUTE_ID,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            type = TransitEdgeType.WALKING
        )
    }

    private fun orderedPairKey(first: String, second: String): String =
        if (first <= second) "$first|$second" else "$second|$first"

    private data class GridCell(val latIndex: Int, val lonIndex: Int) {
        fun neighbors(): List<GridCell> {
            val result = mutableListOf<GridCell>()
            for (latOffset in -1..1) {
                for (lonOffset in -1..1) {
                    result.add(GridCell(latIndex + latOffset, lonIndex + lonOffset))
                }
            }
            return result
        }

        companion object {
            fun from(stop: GtfsStop, gridSizeDegrees: Double): GridCell = GridCell(
                latIndex = kotlin.math.floor(stop.stopLat / gridSizeDegrees).toInt(),
                lonIndex = kotlin.math.floor(stop.stopLon / gridSizeDegrees).toInt()
            )
        }
    }
}
