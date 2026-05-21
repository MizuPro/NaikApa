package com.example.naikapa.data.repository

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.model.MapMarkerType
import com.example.naikapa.data.model.MapPoint
import com.example.naikapa.data.model.PrivateVehicleMode
import com.example.naikapa.data.model.PrivateVehicleRouteResult
import com.example.naikapa.data.model.TomTomRoute
import com.example.naikapa.data.remote.TomTomRoutingApi
import com.example.naikapa.domain.routing.FuelCostCalculator
import java.util.Locale

class TomTomRoutingRepository(
    private val api: TomTomRoutingApi,
    private val fuelCostCalculator: FuelCostCalculator = FuelCostCalculator()
) {
    suspend fun calculateRoute(
        originLat: Double,
        originLon: Double,
        destinationLat: Double,
        destinationLon: Double,
        mode: PrivateVehicleMode,
        apiKey: String,
        avoidTollRoads: Boolean = false
    ): Result<List<PrivateVehicleRouteResult>> {
        if (apiKey.isBlank() || apiKey == AppConstants.TOMTOM_API_KEY_PLACEHOLDER) {
            return Result.failure(IllegalStateException("TomTom API key belum diisi"))
        }

        return runCatching {
            val response = api.calculateRoute(
                from = formatCoordinatePair(originLat, originLon),
                to = formatCoordinatePair(destinationLat, destinationLon),
                apiKey = apiKey,
                travelMode = mode.toTravelMode(),
                routeType = AppConstants.TOMTOM_ROUTING_ROUTE_TYPE_FASTEST,
                maxAlternatives = AppConstants.TOMTOM_MAX_ALTERNATIVES,
                avoid = if (mode == PrivateVehicleMode.CAR && avoidTollRoads) {
                    AppConstants.TOMTOM_ROUTING_AVOID_TOLL_ROADS
                } else {
                    null
                }
            )
            if (!response.isSuccessful) {
                error("TomTom Routing gagal: HTTP ${response.code()}")
            }
            val results = response.body()?.routes.orEmpty().mapIndexedNotNull { index, route ->
                toPrivateVehicleRouteResult(route, mode, index)
            }
            if (results.isEmpty()) error("TomTom Routing tidak mengembalikan rute")
            results
        }
    }

    fun toPrivateVehicleRouteResult(
        route: TomTomRoute,
        mode: PrivateVehicleMode,
        alternativeIndex: Int
    ): PrivateVehicleRouteResult? {
        val summary = route.summary ?: return null
        val distanceMeters = summary.lengthInMeters ?: return null
        val travelTimeSeconds = summary.travelTimeInSeconds ?: return null
        val points = route.legs
            .flatMap { it.points }
            .mapNotNull { point ->
                val latitude = point.latitude ?: return@mapNotNull null
                val longitude = point.longitude ?: return@mapNotNull null
                MapPoint(
                    label = if (alternativeIndex == 0) "Rute utama" else "Alternatif $alternativeIndex",
                    latitude = latitude,
                    longitude = longitude,
                    description = mode.name,
                    markerType = MapMarkerType.TRANSIT
                )
            }
        if (points.size < 2) return null

        val distanceKm = distanceMeters / 1000.0
        val estimatedBbm = fuelCostCalculator.calculateFuelCost(distanceKm, mode)
        return PrivateVehicleRouteResult(
            mode = mode,
            distanceMeters = distanceMeters,
            travelTimeSeconds = travelTimeSeconds,
            estimatedBbm = estimatedBbm,
            estimatedTotalCost = estimatedBbm,
            points = points,
            alternativeIndex = alternativeIndex
        )
    }

    private fun formatCoordinatePair(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "%.6f,%.6f", latitude, longitude)

    private fun PrivateVehicleMode.toTravelMode(): String = when (this) {
        PrivateVehicleMode.MOTOR -> AppConstants.TOMTOM_ROUTING_TRAVEL_MODE_MOTORCYCLE
        PrivateVehicleMode.CAR -> AppConstants.TOMTOM_ROUTING_TRAVEL_MODE_CAR
    }
}
