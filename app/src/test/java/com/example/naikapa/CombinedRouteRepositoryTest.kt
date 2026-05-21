package com.example.naikapa

import com.example.naikapa.data.model.GtfsStop
import com.example.naikapa.data.model.MapMarkerType
import com.example.naikapa.data.model.MapPoint
import com.example.naikapa.data.model.PrivateVehicleMode
import com.example.naikapa.data.model.PrivateVehicleRouteResult
import com.example.naikapa.data.model.RouteMetrics
import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitMode
import com.example.naikapa.data.model.TransitNode
import com.example.naikapa.data.model.TransitRouteResult
import com.example.naikapa.data.repository.CombinedRouteRepository
import com.example.naikapa.data.repository.NearbyTransitStopRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedRouteRepositoryTest {
    @Test
    fun findCombinedRoutesAggregatesVehicleTransitAndWalkingMetrics() = runBlocking {
        val repository = createRepository(
            stops = listOf(
                GtfsStop("origin_stop", "Stasiun Awal", -6.2010, 106.8010, "krl"),
                GtfsStop("dest_stop", "Stasiun Akhir", -6.2500, 106.8500, "krl")
            )
        )

        val result = repository.findCombinedRoutes(
            originLat = -6.2000,
            originLon = 106.8000,
            destinationLat = -6.2510,
            destinationLon = 106.8510,
            privateVehicleMode = PrivateVehicleMode.MOTOR,
            transitMode = TransitMode.KRL,
            sortPreference = SortPreference.FASTEST,
            agencyId = "krl"
        )

        assertTrue(result.isSuccess)
        val route = result.getOrThrow().first()
        assertEquals(PrivateVehicleMode.MOTOR, route.privateVehicleMode)
        assertEquals(2_500, route.metrics.estimatedBbm)
        assertEquals(3_000, route.metrics.estimatedFare)
        assertEquals(5_500, route.metrics.estimatedTotalCost)
        assertTrue(route.metrics.totalDurationSeconds > 1_900)
        assertTrue(route.metrics.walkingDistanceMeters > 0.0)
        assertEquals(3, route.segments.size)
    }

    @Test
    fun findCombinedRoutesReturnsFailureWhenNoNearbyStops() = runBlocking {
        val repository = createRepository(stops = emptyList())

        val result = repository.findCombinedRoutes(
            originLat = -6.2000,
            originLon = 106.8000,
            destinationLat = -6.2510,
            destinationLon = 106.8510,
            privateVehicleMode = PrivateVehicleMode.MOTOR,
            transitMode = TransitMode.ALL,
            sortPreference = SortPreference.FASTEST
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun findCombinedRoutesSkipsCandidateWhenTransitRouteMissing() = runBlocking {
        val nearbyRepository = NearbyTransitStopRepository {
            listOf(
                GtfsStop("origin_stop", "Stasiun Awal", -6.2010, 106.8010, "krl"),
                GtfsStop("dest_stop", "Stasiun Akhir", -6.2500, 106.8500, "krl")
            )
        }
        val repository = CombinedRouteRepository(
            nearbyTransitStopRepository = nearbyRepository,
            privateVehicleRouteProvider = { _, _, _, _, _ -> Result.success(listOf(vehicleRoute())) },
            transitRouteProvider = { _, _, _, _ -> null }
        )

        val result = repository.findCombinedRoutes(
            originLat = -6.2000,
            originLon = 106.8000,
            destinationLat = -6.2510,
            destinationLon = 106.8510,
            privateVehicleMode = PrivateVehicleMode.MOTOR,
            transitMode = TransitMode.ALL,
            sortPreference = SortPreference.FASTEST
        )

        assertTrue(result.isFailure)
    }

    private fun createRepository(stops: List<GtfsStop>): CombinedRouteRepository {
        val nearbyRepository = NearbyTransitStopRepository { stops }
        return CombinedRouteRepository(
            nearbyTransitStopRepository = nearbyRepository,
            privateVehicleRouteProvider = { _, _, _, _, _ -> Result.success(listOf(vehicleRoute())) },
            transitRouteProvider = { startStopId, endStopId, mode, sortPreference ->
                TransitRouteResult(
                    startStop = TransitNode(startStopId, "Stasiun Awal", -6.2010, 106.8010, "krl"),
                    endStop = TransitNode(endStopId, "Stasiun Akhir", -6.2500, 106.8500, "krl"),
                    mode = mode,
                    sortPreference = sortPreference,
                    edges = emptyList(),
                    steps = emptyList(),
                    metrics = RouteMetrics(
                        totalDurationSeconds = 1_800,
                        totalDistanceMeters = 15_000.0,
                        walkingDistanceMeters = 100.0,
                        transitCount = 1,
                        estimatedFare = 3_000,
                        estimatedBbm = 0,
                        estimatedTotalCost = 3_000
                    )
                )
            }
        )
    }

    private fun vehicleRoute(): PrivateVehicleRouteResult = PrivateVehicleRouteResult(
        mode = PrivateVehicleMode.MOTOR,
        distanceMeters = 10_000,
        travelTimeSeconds = 1_200,
        estimatedBbm = 2_500,
        estimatedTotalCost = 2_500,
        points = listOf(
            MapPoint("Origin", -6.2000, 106.8000, null, MapMarkerType.ORIGIN),
            MapPoint("Transit", -6.2010, 106.8010, null, MapMarkerType.TRANSIT)
        ),
        alternativeIndex = 0
    )
}
