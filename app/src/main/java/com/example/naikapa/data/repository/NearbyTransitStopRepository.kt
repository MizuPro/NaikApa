package com.example.naikapa.data.repository

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.local.GtfsDao
import com.example.naikapa.data.model.CombinedRouteStopCandidate
import com.example.naikapa.data.model.GtfsStop
import com.example.naikapa.domain.routing.GeoDistanceCalculator

class NearbyTransitStopRepository(
    private val loadStops: () -> List<GtfsStop>
) {
    constructor(gtfsDao: GtfsDao) : this({ gtfsDao.getStopsForNearestSearch() })

    fun findNearestStops(
        latitude: Double,
        longitude: Double,
        maxDistanceMeters: Double,
        limit: Int = AppConstants.COMBINED_ROUTE_STOP_CANDIDATE_LIMIT,
        agencyId: String? = null
    ): List<CombinedRouteStopCandidate> {
        return rankStops(
            stops = loadStops(),
            latitude = latitude,
            longitude = longitude,
            maxDistanceMeters = maxDistanceMeters,
            limit = limit,
            agencyId = agencyId
        )
    }

    fun rankStops(
        stops: List<GtfsStop>,
        latitude: Double,
        longitude: Double,
        maxDistanceMeters: Double,
        limit: Int = AppConstants.COMBINED_ROUTE_STOP_CANDIDATE_LIMIT,
        agencyId: String? = null
    ): List<CombinedRouteStopCandidate> {
        val safeLimit = limit.coerceAtLeast(1)
        val normalizedAgency = agencyId?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        return stops
            .asSequence()
            .filter { stop ->
                normalizedAgency == null || stop.agencyId.equals(normalizedAgency, ignoreCase = true)
            }
            .map { stop ->
                val distance = GeoDistanceCalculator.haversineMeters(
                    latitude,
                    longitude,
                    stop.stopLat,
                    stop.stopLon
                )
                stop to distance
            }
            .filter { (_, distance) -> distance <= maxDistanceMeters }
            .sortedWith(compareBy<Pair<GtfsStop, Double>> { it.second }.thenBy { it.first.stopName })
            .take(safeLimit)
            .map { (stop, distance) ->
                CombinedRouteStopCandidate(
                    stopId = stop.stopId,
                    stopName = stop.stopName,
                    latitude = stop.stopLat,
                    longitude = stop.stopLon,
                    agencyId = stop.agencyId,
                    stopType = stop.stopType,
                    distanceMeters = distance
                )
            }
            .toList()
    }
}
