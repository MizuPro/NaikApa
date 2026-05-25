package com.example.naikapa.data.repository

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.model.CombinedRouteResult
import com.example.naikapa.data.model.CombinedRouteSegment
import com.example.naikapa.data.model.CombinedRouteSegmentType
import com.example.naikapa.data.model.CombinedRouteStopCandidate
import com.example.naikapa.data.model.MapMarkerType
import com.example.naikapa.data.model.MapPoint
import com.example.naikapa.data.model.PrivateVehicleMode
import com.example.naikapa.data.model.PrivateVehicleRouteResult
import com.example.naikapa.data.model.RouteMetrics
import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitMode
import com.example.naikapa.data.model.TransitRouteResult
import com.example.naikapa.domain.routing.GeoDistanceCalculator
import kotlin.math.roundToInt

class CombinedRouteRepository(
    private val nearbyTransitStopRepository: NearbyTransitStopRepository,
    private val privateVehicleRouteProvider: suspend (
        originLat: Double,
        originLon: Double,
        destinationLat: Double,
        destinationLon: Double,
        mode: PrivateVehicleMode,
        avoidTollRoads: Boolean
    ) -> Result<List<PrivateVehicleRouteResult>>,
    private val transitRouteProvider: (
        startStopId: String,
        endStopId: String,
        mode: TransitMode,
        sortPreference: SortPreference
    ) -> TransitRouteResult?,
    private val walkingRouteProvider: suspend (
        originLat: Double,
        originLon: Double,
        destinationLat: Double,
        destinationLon: Double,
        originLabel: String,
        destinationLabel: String
    ) -> Result<WalkingRouteResult>
) {
    constructor(
        nearbyTransitStopRepository: NearbyTransitStopRepository,
        tomTomRoutingRepository: TomTomRoutingRepository,
        transitRoutingRepository: TransitRoutingRepository,
        apiKey: String,
        avoidTollRoads: Boolean = false
    ) : this(
        nearbyTransitStopRepository = nearbyTransitStopRepository,
        privateVehicleRouteProvider = { originLat, originLon, destinationLat, destinationLon, mode, avoidTolls ->
            tomTomRoutingRepository.calculateRoute(
                originLat = originLat,
                originLon = originLon,
                destinationLat = destinationLat,
                destinationLon = destinationLon,
                mode = mode,
                apiKey = apiKey,
                avoidTollRoads = avoidTolls
            )
        },
        transitRouteProvider = { startStopId, endStopId, mode, sortPreference ->
            transitRoutingRepository.findRoute(startStopId, endStopId, mode, sortPreference)
        },
        walkingRouteProvider = { originLat, originLon, destinationLat, destinationLon, originLabel, destinationLabel ->
            tomTomRoutingRepository.calculateWalkingRoute(
                originLat = originLat,
                originLon = originLon,
                destinationLat = destinationLat,
                destinationLon = destinationLon,
                originLabel = originLabel,
                destinationLabel = destinationLabel,
                apiKey = apiKey
            )
        }
    )

    suspend fun findCombinedRoutes(
        originLat: Double,
        originLon: Double,
        destinationLat: Double,
        destinationLon: Double,
        privateVehicleMode: PrivateVehicleMode?,
        transitMode: TransitMode,
        sortPreference: SortPreference,
        agencyId: String? = null,
        limit: Int = AppConstants.COMBINED_ROUTE_STOP_CANDIDATE_LIMIT,
        avoidTollRoads: Boolean = false
    ): Result<List<CombinedRouteResult>> = runCatching {
        val originStops = nearbyTransitStopRepository.findNearestStops(
            latitude = originLat,
            longitude = originLon,
            maxDistanceMeters = AppConstants.COMBINED_ROUTE_ORIGIN_RADIUS_M,
            limit = limit,
            agencyId = agencyId
        )
        val destinationStops = nearbyTransitStopRepository.findNearestStops(
            latitude = destinationLat,
            longitude = destinationLon,
            maxDistanceMeters = AppConstants.COMBINED_ROUTE_DESTINATION_RADIUS_M,
            limit = limit,
            agencyId = agencyId
        )

        if (originStops.isEmpty() || destinationStops.isEmpty()) {
            error("Tidak ada titik transit terdekat yang sesuai")
        }

        val candidates = mutableListOf<CombinedRouteResult>()
        var checkedCombinations = 0
        for (originStop in originStops) {
            val vehicleRoute = if (privateVehicleMode != null) {
                privateVehicleRouteProvider(
                    originLat,
                    originLon,
                    originStop.latitude,
                    originStop.longitude,
                    privateVehicleMode,
                    avoidTollRoads
                ).getOrNull()?.firstOrNull() ?: continue
            } else {
                null
            }

            for (destinationStop in destinationStops) {
                if (checkedCombinations >= AppConstants.COMBINED_ROUTE_MAX_COMBINATIONS) break
                checkedCombinations += 1

                val transitRoute = transitRouteProvider(
                    originStop.stopId,
                    destinationStop.stopId,
                    transitMode,
                    sortPreference
                ) ?: continue

                candidates.add(
                    buildResult(
                        privateVehicleMode = privateVehicleMode,
                        originLat = originLat,
                        originLon = originLon,
                        destinationLat = destinationLat,
                        destinationLon = destinationLon,
                        originStop = originStop,
                        destinationStop = destinationStop,
                        vehicleRoute = vehicleRoute,
                        transitRoute = transitRoute
                    )
                )
            }
        }

        if (candidates.isEmpty()) error("Rute gabungan belum ditemukan")
        candidates.sortedWith(
            compareBy<CombinedRouteResult> { sortPrimaryMetric(it, sortPreference) }
                .thenBy { it.metrics.totalDurationSeconds }
        )
    }

    private suspend fun buildResult(
        privateVehicleMode: PrivateVehicleMode?,
        originLat: Double,
        originLon: Double,
        destinationLat: Double,
        destinationLon: Double,
        originStop: CombinedRouteStopCandidate,
        destinationStop: CombinedRouteStopCandidate,
        vehicleRoute: PrivateVehicleRouteResult?,
        transitRoute: TransitRouteResult
    ): CombinedRouteResult {
        // ── Last-mile: halte tujuan → destinasi ──────────────────────────────
        val lastMileWalking = walkingRouteProvider(
            destinationStop.latitude,
            destinationStop.longitude,
            destinationLat,
            destinationLon,
            destinationStop.stopName,
            "Tujuan"
        ).getOrNull()

        val walkingDistanceMeters: Double
        val walkingDurationSeconds: Int
        val walkingPoints: List<MapPoint>

        if (lastMileWalking != null) {
            walkingDistanceMeters = lastMileWalking.distanceMeters
            walkingDurationSeconds = lastMileWalking.durationSeconds
            walkingPoints = lastMileWalking.points
        } else {
            // Fallback ke Haversine jika TomTom gagal
            val haversineMeters = GeoDistanceCalculator.haversineMeters(
                destinationStop.latitude,
                destinationStop.longitude,
                destinationLat,
                destinationLon
            )
            walkingDistanceMeters = haversineMeters
            walkingDurationSeconds = (haversineMeters * AppConstants.WALKING_SECONDS_PER_METER).roundToInt()
            walkingPoints = listOf(
                MapPoint(
                    label = destinationStop.stopName,
                    latitude = destinationStop.latitude,
                    longitude = destinationStop.longitude,
                    description = destinationStop.agencyId,
                    markerType = MapMarkerType.TRANSIT
                ),
                MapPoint(
                    label = "Tujuan",
                    latitude = destinationLat,
                    longitude = destinationLon,
                    description = "Jalan kaki",
                    markerType = MapMarkerType.DESTINATION
                )
            )
        }

        // ── First-mile: origin → halte terdekat (hanya jika tidak ada kendaraan) ──
        val originSegment = if (privateVehicleMode != null && vehicleRoute != null) {
            val vehicleName = if (privateVehicleMode == PrivateVehicleMode.MOTOR) "Motor" else "Mobil"
            val stopLabel = stopTypeLabel(
                stopType = originStop.stopType,
                agencyId = originStop.agencyId
            )
            CombinedRouteSegment(
                type = CombinedRouteSegmentType.PRIVATE_VEHICLE,
                title = "$vehicleName menuju $stopLabel",
                durationSeconds = vehicleRoute.travelTimeSeconds,
                distanceMeters = vehicleRoute.distanceMeters.toDouble(),
                estimatedBbm = vehicleRoute.estimatedBbm,
                points = vehicleRoute.points,
                privateVehicleResult = vehicleRoute
            )
        } else {
            // Tidak ada kendaraan → jalan kaki ke halte, gunakan TomTom pedestrian
            val firstMileWalking = walkingRouteProvider(
                originLat,
                originLon,
                originStop.latitude,
                originStop.longitude,
                "Asal",
                originStop.stopName
            ).getOrNull()

            if (firstMileWalking != null) {
                CombinedRouteSegment(
                    type = CombinedRouteSegmentType.WALKING,
                    title = "Jalan kaki ke transit",
                    durationSeconds = firstMileWalking.durationSeconds,
                    distanceMeters = firstMileWalking.distanceMeters,
                    points = firstMileWalking.points
                )
            } else {
                // Fallback ke Haversine jika TomTom gagal
                val firstDistanceMeters = GeoDistanceCalculator.haversineMeters(
                    originLat,
                    originLon,
                    originStop.latitude,
                    originStop.longitude
                )
                val firstDurationSeconds = (firstDistanceMeters * AppConstants.WALKING_SECONDS_PER_METER).roundToInt()
                CombinedRouteSegment(
                    type = CombinedRouteSegmentType.WALKING,
                    title = "Jalan kaki ke transit",
                    durationSeconds = firstDurationSeconds,
                    distanceMeters = firstDistanceMeters,
                    points = listOf(
                        MapPoint(
                            label = "Asal",
                            latitude = originLat,
                            longitude = originLon,
                            description = "Jalan kaki",
                            markerType = MapMarkerType.ORIGIN
                        ),
                        MapPoint(
                            label = originStop.stopName,
                            latitude = originStop.latitude,
                            longitude = originStop.longitude,
                            description = originStop.agencyId,
                            markerType = MapMarkerType.TRANSIT
                        )
                    )
                )
            }
        }

        val transitSegment = CombinedRouteSegment(
            type = CombinedRouteSegmentType.TRANSIT,
            title = "Transportasi umum",
            durationSeconds = transitRoute.metrics.totalDurationSeconds,
            distanceMeters = transitRoute.metrics.totalDistanceMeters,
            estimatedFare = transitRoute.metrics.estimatedFare,
            points = transitRoute.steps.toTransitPoints(),
            transitResult = transitRoute
        )
        val walkingSegment = CombinedRouteSegment(
            type = CombinedRouteSegmentType.WALKING,
            title = "Jalan kaki ke tujuan",
            durationSeconds = walkingDurationSeconds,
            distanceMeters = walkingDistanceMeters,
            points = walkingPoints
        )
        val segments = listOf(originSegment, transitSegment, walkingSegment)
        val transitWalkingMeters = transitRoute.metrics.walkingDistanceMeters
        val originWalkingMeters = if (privateVehicleMode == null) originSegment.distanceMeters else 0.0
        val totalWalkingMeters = transitWalkingMeters + walkingDistanceMeters + originWalkingMeters
        val estimatedFare = transitRoute.metrics.estimatedFare
        val estimatedBbm = vehicleRoute?.estimatedBbm ?: 0

        return CombinedRouteResult(
            privateVehicleMode = privateVehicleMode,
            originStop = originStop,
            destinationStop = destinationStop,
            segments = segments,
            metrics = RouteMetrics(
                totalDurationSeconds = segments.sumOf { it.durationSeconds },
                totalDistanceMeters = segments.sumOf { it.distanceMeters },
                walkingDistanceMeters = totalWalkingMeters,
                transitCount = transitRoute.metrics.transitCount,
                estimatedFare = estimatedFare,
                estimatedBbm = estimatedBbm,
                estimatedTotalCost = estimatedFare + estimatedBbm
            )
        )
    }

    private fun List<com.example.naikapa.data.model.RouteStep>.toTransitPoints(): List<MapPoint> {
        if (isEmpty()) return emptyList()
        val points = mutableListOf<MapPoint>()
        points.add(
            MapPoint(
                label = first().fromStop.stopName,
                latitude = first().fromStop.lat,
                longitude = first().fromStop.lon,
                description = first().fromStop.agencyId,
                markerType = MapMarkerType.TRANSIT
            )
        )
        forEach { step ->
            points.add(
                MapPoint(
                    label = step.toStop.stopName,
                    latitude = step.toStop.lat,
                    longitude = step.toStop.lon,
                    description = step.routeShortName,
                    markerType = MapMarkerType.TRANSIT
                )
            )
        }
        return points
    }

    private fun sortPrimaryMetric(result: CombinedRouteResult, sortPreference: SortPreference): Double =
        when (sortPreference) {
            SortPreference.CHEAPEST -> result.metrics.estimatedTotalCost.toDouble()
            SortPreference.MIN_WALKING -> result.metrics.walkingDistanceMeters
            SortPreference.FEWEST_TRANSFERS -> result.metrics.transitCount.toDouble()
            SortPreference.FASTEST -> result.metrics.totalDurationSeconds.toDouble()
        }

    private fun stopTypeLabel(stopType: String?, agencyId: String): String {
        return when {
            stopType?.lowercase() == "station" -> "Stasiun"
            stopType?.lowercase() == "halte"   -> "Halte"
            agencyId == "Tije"                 -> "Halte"
            agencyId in listOf("KAIC", "MRTJ", "LRTJ", "LRTJB") -> "Stasiun"
            else                               -> "Titik Transit"
        }
    }
}
