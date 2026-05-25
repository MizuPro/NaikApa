package com.example.naikapa.data.repository

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.local.RouteCacheDao
import com.example.naikapa.data.model.LocationPoint
import com.example.naikapa.data.model.RecommendationResult
import com.example.naikapa.data.model.RouteCache
import com.example.naikapa.data.model.SearchLocation
import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitMode
import com.example.naikapa.data.model.VehicleTypeFilter
import java.util.Locale
import kotlin.math.pow
import kotlin.math.round

data class RouteCacheLookup(
    val cacheKey: String,
    val originLat: Double,
    val originLon: Double,
    val destinationLat: Double,
    val destinationLon: Double,
    val mode: String,
    val priority: String
)

class RouteCacheRepository(
    private val findByKey: (String) -> RouteCache?,
    private val insertOrReplace: (RouteCache) -> Long,
    private val clearOlderThan: (Long) -> Int,
    private val serializer: RouteCacheSerializer = RouteCacheSerializer(),
    private val nowProvider: () -> Long = System::currentTimeMillis,
    private val ttlMillis: Long = AppConstants.ROUTE_CACHE_TTL_MILLIS
) {
    constructor(
        routeCacheDao: RouteCacheDao,
        serializer: RouteCacheSerializer = RouteCacheSerializer(),
        nowProvider: () -> Long = System::currentTimeMillis,
        ttlMillis: Long = AppConstants.ROUTE_CACHE_TTL_MILLIS
    ) : this(
        findByKey = routeCacheDao::findByKey,
        insertOrReplace = routeCacheDao::insertOrReplace,
        clearOlderThan = routeCacheDao::clearOlderThan,
        serializer = serializer,
        nowProvider = nowProvider,
        ttlMillis = ttlMillis
    )

    fun getRecommendation(
        origin: LocationPoint,
        destination: SearchLocation,
        transitMode: TransitMode,
        sortPreference: SortPreference,
        hasMotor: Boolean,
        hasCar: Boolean,
        vehicleTypeFilter: VehicleTypeFilter,
        originStopId: String?,
        destinationStopId: String?,
        avoidTollRoads: Boolean
    ): RecommendationResult? {
        val lookup = buildLookup(
            origin = origin,
            destination = destination,
            transitMode = transitMode,
            sortPreference = sortPreference,
            hasMotor = hasMotor,
            hasCar = hasCar,
            vehicleTypeFilter = vehicleTypeFilter,
            originStopId = originStopId,
            destinationStopId = destinationStopId,
            avoidTollRoads = avoidTollRoads
        )
        val cache = findByKey(lookup.cacheKey) ?: return null
        if (isExpired(cache)) return null
        return runCatching { serializer.deserialize(cache.resultJson) }.getOrNull()
    }

    fun saveRecommendation(
        origin: LocationPoint,
        destination: SearchLocation,
        transitMode: TransitMode,
        sortPreference: SortPreference,
        hasMotor: Boolean,
        hasCar: Boolean,
        vehicleTypeFilter: VehicleTypeFilter,
        originStopId: String?,
        destinationStopId: String?,
        avoidTollRoads: Boolean,
        recommendation: RecommendationResult
    ): Long {
        val lookup = buildLookup(
            origin = origin,
            destination = destination,
            transitMode = transitMode,
            sortPreference = sortPreference,
            hasMotor = hasMotor,
            hasCar = hasCar,
            vehicleTypeFilter = vehicleTypeFilter,
            originStopId = originStopId,
            destinationStopId = destinationStopId,
            avoidTollRoads = avoidTollRoads
        )
        val cache = RouteCache(
            cacheKey = lookup.cacheKey,
            originLat = lookup.originLat,
            originLon = lookup.originLon,
            destinationLat = lookup.destinationLat,
            destinationLon = lookup.destinationLon,
            mode = lookup.mode,
            priority = lookup.priority,
            resultJson = serializer.serialize(recommendation),
            createdAt = nowProvider()
        )
        return insertOrReplace(cache)
    }

    fun clearExpired(): Int {
        val cutoff = nowProvider() - AppConstants.ROUTE_CACHE_CLEANUP_AGE_MILLIS
        return clearOlderThan(cutoff)
    }

    fun buildLookup(
        origin: LocationPoint,
        destination: SearchLocation,
        transitMode: TransitMode,
        sortPreference: SortPreference,
        hasMotor: Boolean,
        hasCar: Boolean,
        vehicleTypeFilter: VehicleTypeFilter,
        originStopId: String?,
        destinationStopId: String?,
        avoidTollRoads: Boolean
    ): RouteCacheLookup {
        val originLat = roundCoordinate(origin.latitude)
        val originLon = roundCoordinate(origin.longitude)
        val destinationLat = roundCoordinate(destination.latitude)
        val destinationLon = roundCoordinate(destination.longitude)
        val mode = listOf(
            "transit=${transitMode.name}",
            "vehicleFilter=${vehicleTypeFilter.name}",
            "motor=$hasMotor",
            "car=$hasCar",
            "avoidToll=$avoidTollRoads"
        ).joinToString(";")
        val priority = sortPreference.name
        val cacheKey = listOf(
            "v1",
            formatCoordinate(originLat),
            formatCoordinate(originLon),
            formatCoordinate(destinationLat),
            formatCoordinate(destinationLon),
            mode,
            priority,
            "originStop=${originStopId.orEmptyKey()}",
            "destinationStop=${destinationStopId.orEmptyKey()}"
        ).joinToString("|")

        return RouteCacheLookup(
            cacheKey = cacheKey,
            originLat = originLat,
            originLon = originLon,
            destinationLat = destinationLat,
            destinationLon = destinationLon,
            mode = mode,
            priority = priority
        )
    }

    private fun isExpired(cache: RouteCache): Boolean =
        nowProvider() - cache.createdAt > ttlMillis

    private fun roundCoordinate(value: Double): Double {
        val multiplier = 10.0.pow(AppConstants.ROUTE_CACHE_COORDINATE_PRECISION)
        return round(value * multiplier) / multiplier
    }

    private fun formatCoordinate(value: Double): String {
        val format = "%.${AppConstants.ROUTE_CACHE_COORDINATE_PRECISION}f"
        return String.format(Locale.US, format, value)
    }

    private fun String?.orEmptyKey(): String =
        takeUnless { it.isNullOrBlank() } ?: "-"
}
