package com.example.naikapa

import com.example.naikapa.data.model.CombinedRouteResult
import com.example.naikapa.data.model.CombinedRouteSegment
import com.example.naikapa.data.model.CombinedRouteSegmentType
import com.example.naikapa.data.model.CombinedRouteStopCandidate
import com.example.naikapa.data.model.MapMarkerType
import com.example.naikapa.data.model.MapPoint
import com.example.naikapa.data.model.PrivateVehicleMode
import com.example.naikapa.data.model.PrivateVehicleRouteResult
import com.example.naikapa.data.model.RecommendationResult
import com.example.naikapa.data.model.RouteCandidate
import com.example.naikapa.data.model.RouteMetrics
import com.example.naikapa.data.model.RouteStep
import com.example.naikapa.data.model.ScoredRoute
import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitEdgeType
import com.example.naikapa.data.model.TransitMode
import com.example.naikapa.data.model.TransitNode
import com.example.naikapa.data.model.TransitRouteResult
import com.example.naikapa.data.repository.RouteCacheSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteCacheSerializerTest {
    private val serializer = RouteCacheSerializer()

    @Test
    fun roundTripKeepsPrivateVehicleCandidate() {
        val result = recommendation(RouteCandidate.PrivateVehicle(privateVehicleResult()), score = 91)

        val restored = serializer.deserialize(serializer.serialize(result))

        assertEquals(SortPreference.FASTEST, restored.sortPreference)
        assertEquals(91, restored.main.score)
        val candidate = restored.main.candidate
        assertTrue(candidate is RouteCandidate.PrivateVehicle)
        assertEquals(10_000.0, candidate.metrics.totalDistanceMeters, 0.0001)
        assertEquals(2_500, candidate.metrics.estimatedTotalCost)
    }

    @Test
    fun roundTripKeepsTransitCandidate() {
        val result = recommendation(RouteCandidate.Transit(transitRouteResult()), score = 82)

        val restored = serializer.deserialize(serializer.serialize(result))

        assertEquals(82, restored.main.score)
        val candidate = restored.main.candidate
        assertTrue(candidate is RouteCandidate.Transit)
        assertEquals(1, candidate.metrics.transitCount)
        assertEquals(3_500, candidate.metrics.estimatedFare)
    }

    @Test
    fun roundTripKeepsCombinedCandidate() {
        val result = recommendation(RouteCandidate.Combined(combinedRouteResult()), score = 76)

        val restored = serializer.deserialize(serializer.serialize(result))

        assertEquals(76, restored.main.score)
        val candidate = restored.main.candidate
        assertTrue(candidate is RouteCandidate.Combined)
        assertEquals(2_500, candidate.metrics.estimatedBbm)
        assertEquals(6_000, candidate.metrics.estimatedTotalCost)
    }

    private fun recommendation(candidate: RouteCandidate, score: Int): RecommendationResult =
        RecommendationResult(
            main = ScoredRoute(
                candidate = candidate,
                score = score,
                reason = "Alasan cache",
                hasDisruptionWarning = false,
                rankLabel = "Rekomendasi Utama"
            ),
            alternatives = emptyList(),
            sortPreference = SortPreference.FASTEST
        )

    private fun privateVehicleResult(): PrivateVehicleRouteResult =
        PrivateVehicleRouteResult(
            mode = PrivateVehicleMode.MOTOR,
            distanceMeters = 10_000,
            travelTimeSeconds = 1_200,
            estimatedBbm = 2_500,
            estimatedTotalCost = 2_500,
            points = listOf(
                MapPoint("Asal", -6.2, 106.8, markerType = MapMarkerType.ORIGIN),
                MapPoint("Tujuan", -6.3, 106.9, markerType = MapMarkerType.DESTINATION)
            ),
            alternativeIndex = 0
        )

    private fun transitRouteResult(): TransitRouteResult {
        val start = TransitNode("A", "Halte A", -6.2, 106.8, "Tije", "halte")
        val end = TransitNode("B", "Halte B", -6.25, 106.85, "Tije", "halte")
        return TransitRouteResult(
            startStop = start,
            endStop = end,
            mode = TransitMode.ALL,
            sortPreference = SortPreference.FASTEST,
            edges = emptyList(),
            steps = listOf(
                RouteStep(
                    routeId = "TJ1",
                    routeShortName = "TJ1",
                    routeLongName = "TransJakarta 1",
                    routeColor = null,
                    routeTextColor = null,
                    agencyId = "Tije",
                    type = TransitEdgeType.TRANSIT,
                    fromStop = start,
                    toStop = end,
                    stopCount = 2,
                    distanceMeters = 5_000.0,
                    durationSeconds = 900
                )
            ),
            metrics = RouteMetrics(
                totalDurationSeconds = 900,
                totalDistanceMeters = 5_000.0,
                walkingDistanceMeters = 0.0,
                transitCount = 1,
                estimatedFare = 3_500,
                estimatedTotalCost = 3_500
            )
        )
    }

    private fun combinedRouteResult(): CombinedRouteResult {
        val transitRoute = transitRouteResult()
        val vehicleRoute = privateVehicleResult()
        val originStop = CombinedRouteStopCandidate("A", "Halte A", -6.2, 106.8, "Tije", "halte", 0.0)
        val destinationStop = CombinedRouteStopCandidate("B", "Halte B", -6.25, 106.85, "Tije", "halte", 0.0)
        return CombinedRouteResult(
            privateVehicleMode = PrivateVehicleMode.MOTOR,
            originStop = originStop,
            destinationStop = destinationStop,
            segments = listOf(
                CombinedRouteSegment(
                    type = CombinedRouteSegmentType.PRIVATE_VEHICLE,
                    title = "Motor menuju Halte",
                    durationSeconds = vehicleRoute.travelTimeSeconds,
                    distanceMeters = vehicleRoute.distanceMeters.toDouble(),
                    estimatedBbm = vehicleRoute.estimatedBbm,
                    points = vehicleRoute.points,
                    privateVehicleResult = vehicleRoute
                ),
                CombinedRouteSegment(
                    type = CombinedRouteSegmentType.TRANSIT,
                    title = "Transportasi umum",
                    durationSeconds = transitRoute.metrics.totalDurationSeconds,
                    distanceMeters = transitRoute.metrics.totalDistanceMeters,
                    estimatedFare = transitRoute.metrics.estimatedFare,
                    transitResult = transitRoute
                )
            ),
            metrics = RouteMetrics(
                totalDurationSeconds = 2_100,
                totalDistanceMeters = 15_000.0,
                walkingDistanceMeters = 0.0,
                transitCount = 1,
                estimatedFare = 3_500,
                estimatedBbm = 2_500,
                estimatedTotalCost = 6_000
            )
        )
    }
}
