package com.example.naikapa

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.model.PrivateVehicleMode
import com.example.naikapa.data.model.TomTomRoute
import com.example.naikapa.data.model.TomTomRouteLeg
import com.example.naikapa.data.model.TomTomRoutePoint
import com.example.naikapa.data.model.TomTomRouteSummary
import com.example.naikapa.data.model.TomTomRoutingResponse
import com.example.naikapa.data.remote.TomTomRoutingApi
import com.example.naikapa.data.repository.TomTomRoutingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class TomTomRoutingRepositoryTest {
    @Test
    fun toPrivateVehicleRouteResultMapsSummaryPointsAndFuelCost() {
        val repository = TomTomRoutingRepository(FakeTomTomRoutingApi())
        val result = repository.toPrivateVehicleRouteResult(
            route = route(lengthMeters = 10_000, travelSeconds = 1_800),
            mode = PrivateVehicleMode.MOTOR,
            alternativeIndex = 0
        )

        requireNotNull(result)
        assertEquals(PrivateVehicleMode.MOTOR, result.mode)
        assertEquals(10_000, result.distanceMeters)
        assertEquals(1_800, result.travelTimeSeconds)
        assertEquals(2_500, result.estimatedBbm)
        assertEquals(2_500, result.estimatedTotalCost)
        assertEquals(2, result.points.size)
    }

    @Test
    fun calculateRouteReturnsFailureForPlaceholderApiKey() = runBlocking {
        val result = TomTomRoutingRepository(FakeTomTomRoutingApi()).calculateRoute(
            originLat = -6.2,
            originLon = 106.8,
            destinationLat = -6.3,
            destinationLon = 106.9,
            mode = PrivateVehicleMode.CAR,
            apiKey = AppConstants.TOMTOM_API_KEY_PLACEHOLDER
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun calculateRouteMapsSuccessfulApiResponse() = runBlocking {
        val api = FakeTomTomRoutingApi(
            response = TomTomRoutingResponse(
                routes = listOf(
                    route(lengthMeters = 12_000, travelSeconds = 2_100),
                    route(lengthMeters = 13_000, travelSeconds = 2_300)
                )
            )
        )

        val result = TomTomRoutingRepository(api).calculateRoute(
            originLat = -6.2,
            originLon = 106.8,
            destinationLat = -6.3,
            destinationLon = 106.9,
            mode = PrivateVehicleMode.CAR,
            apiKey = "valid-key"
        )

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        assertEquals(10_000, result.getOrThrow().first().estimatedBbm)
    }

    @Test
    fun toPrivateVehicleRouteResultReturnsNullForEmptyPolyline() {
        val result = TomTomRoutingRepository(FakeTomTomRoutingApi()).toPrivateVehicleRouteResult(
            route = TomTomRoute(summary = TomTomRouteSummary(1000, 100), legs = emptyList()),
            mode = PrivateVehicleMode.MOTOR,
            alternativeIndex = 0
        )

        assertEquals(null, result)
    }

    private class FakeTomTomRoutingApi(
        private val response: TomTomRoutingResponse = TomTomRoutingResponse(
            routes = listOf(defaultRoute(10_000, 1_800))
        )
    ) : TomTomRoutingApi {
        override suspend fun calculateRoute(
            from: String,
            to: String,
            apiKey: String,
            travelMode: String,
            routeType: String,
            maxAlternatives: Int,
            avoid: String?
        ): Response<TomTomRoutingResponse> = Response.success(response)
    }

    companion object {
        private fun route(lengthMeters: Int, travelSeconds: Int): TomTomRoute =
            defaultRoute(lengthMeters, travelSeconds)

        private fun defaultRoute(lengthMeters: Int, travelSeconds: Int): TomTomRoute = TomTomRoute(
            summary = TomTomRouteSummary(
                lengthInMeters = lengthMeters,
                travelTimeInSeconds = travelSeconds
            ),
            legs = listOf(
                TomTomRouteLeg(
                    points = listOf(
                        TomTomRoutePoint(latitude = -6.2, longitude = 106.8),
                        TomTomRoutePoint(latitude = -6.3, longitude = 106.9)
                    )
                )
            )
        )
    }
}
