package com.example.naikapa

import com.example.naikapa.data.model.LocationPoint
import com.example.naikapa.data.model.MapMarkerType
import com.example.naikapa.data.model.MapPoint
import com.example.naikapa.data.model.PrivateVehicleMode
import com.example.naikapa.data.model.PrivateVehicleRouteResult
import com.example.naikapa.data.model.RecommendationResult
import com.example.naikapa.data.model.RouteCache
import com.example.naikapa.data.model.RouteCandidate
import com.example.naikapa.data.model.ScoredRoute
import com.example.naikapa.data.model.SearchLocation
import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitMode
import com.example.naikapa.data.model.VehicleTypeFilter
import com.example.naikapa.data.repository.RouteCacheRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteCacheRepositoryTest {
    private val origin = LocationPoint("Asal", -6.2000049, 106.8000049, false)
    private val destination = SearchLocation("Tujuan", "Alamat", -6.3000049, 106.9000049)

    @Test
    fun buildLookupUsesRoundedCoordinatesAndRelevantInputs() {
        val repo = createRepository()

        val first = repo.buildLookup(
            origin = origin,
            destination = destination,
            transitMode = TransitMode.ALL,
            sortPreference = SortPreference.FASTEST,
            hasMotor = true,
            hasCar = false,
            vehicleTypeFilter = VehicleTypeFilter.ALL,
            originStopId = "STOP_A",
            destinationStopId = "STOP_B",
            avoidTollRoads = false
        )
        val second = repo.buildLookup(
            origin = origin,
            destination = destination,
            transitMode = TransitMode.ALL,
            sortPreference = SortPreference.FASTEST,
            hasMotor = false,
            hasCar = false,
            vehicleTypeFilter = VehicleTypeFilter.ALL,
            originStopId = "STOP_A",
            destinationStopId = "STOP_B",
            avoidTollRoads = false
        )

        assertEquals(-6.2, first.originLat, 0.000001)
        assertEquals(106.8, first.originLon, 0.000001)
        assertNotEquals(first.cacheKey, second.cacheKey)
    }

    @Test
    fun getRecommendationReturnsCachedResultWhenFresh() {
        val store = mutableMapOf<String, RouteCache>()
        var now = 1_000L
        val repo = createRepository(store = store, nowProvider = { now }, ttlMillis = 5_000L)
        val recommendation = privateRecommendation(score = 88)

        repo.saveRecommendation(
            origin = origin,
            destination = destination,
            transitMode = TransitMode.ALL,
            sortPreference = SortPreference.FASTEST,
            hasMotor = true,
            hasCar = false,
            vehicleTypeFilter = VehicleTypeFilter.ALL,
            originStopId = null,
            destinationStopId = null,
            avoidTollRoads = false,
            recommendation = recommendation
        )
        now = 2_000L

        val cached = repo.getRecommendation(
            origin = origin,
            destination = destination,
            transitMode = TransitMode.ALL,
            sortPreference = SortPreference.FASTEST,
            hasMotor = true,
            hasCar = false,
            vehicleTypeFilter = VehicleTypeFilter.ALL,
            originStopId = null,
            destinationStopId = null,
            avoidTollRoads = false
        )

        assertEquals(88, cached?.main?.score)
    }

    @Test
    fun getRecommendationReturnsNullWhenExpiredOrMissing() {
        val store = mutableMapOf<String, RouteCache>()
        var now = 1_000L
        val repo = createRepository(store = store, nowProvider = { now }, ttlMillis = 500L)

        assertNull(
            repo.getRecommendation(
                origin = origin,
                destination = destination,
                transitMode = TransitMode.ALL,
                sortPreference = SortPreference.FASTEST,
                hasMotor = true,
                hasCar = false,
                vehicleTypeFilter = VehicleTypeFilter.ALL,
                originStopId = null,
                destinationStopId = null,
                avoidTollRoads = false
            )
        )

        repo.saveRecommendation(
            origin = origin,
            destination = destination,
            transitMode = TransitMode.ALL,
            sortPreference = SortPreference.FASTEST,
            hasMotor = true,
            hasCar = false,
            vehicleTypeFilter = VehicleTypeFilter.ALL,
            originStopId = null,
            destinationStopId = null,
            avoidTollRoads = false,
            recommendation = privateRecommendation()
        )
        now = 2_000L

        assertNull(
            repo.getRecommendation(
                origin = origin,
                destination = destination,
                transitMode = TransitMode.ALL,
                sortPreference = SortPreference.FASTEST,
                hasMotor = true,
                hasCar = false,
                vehicleTypeFilter = VehicleTypeFilter.ALL,
                originStopId = null,
                destinationStopId = null,
                avoidTollRoads = false
            )
        )
    }

    @Test
    fun clearExpiredRemovesOldCacheEntries() {
        val store = mutableMapOf<String, RouteCache>()
        val repo = createRepository(store = store, nowProvider = { 100_000_000L })
        store["old"] = routeCache("old", createdAt = 1L)
        store["new"] = routeCache("new", createdAt = 99_999_999L)

        val removed = repo.clearExpired()

        assertEquals(1, removed)
        assertTrue("new" in store)
        assertTrue("old" !in store)
    }

    private fun createRepository(
        store: MutableMap<String, RouteCache> = mutableMapOf(),
        nowProvider: () -> Long = { 1_000L },
        ttlMillis: Long = 5_000L
    ): RouteCacheRepository = RouteCacheRepository(
        findByKey = { store[it] },
        insertOrReplace = { cache ->
            store[cache.cacheKey] = cache
            1L
        },
        clearOlderThan = { cutoff ->
            val oldKeys = store.filterValues { it.createdAt < cutoff }.keys.toList()
            oldKeys.forEach(store::remove)
            oldKeys.size
        },
        nowProvider = nowProvider,
        ttlMillis = ttlMillis
    )

    private fun privateRecommendation(score: Int = 90): RecommendationResult {
        val candidate = RouteCandidate.PrivateVehicle(
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
        )
        return RecommendationResult(
            main = ScoredRoute(candidate, score, "cached", rankLabel = "Rekomendasi Utama"),
            alternatives = emptyList(),
            sortPreference = SortPreference.FASTEST
        )
    }

    private fun routeCache(key: String, createdAt: Long): RouteCache = RouteCache(
        cacheKey = key,
        originLat = -6.2,
        originLon = 106.8,
        destinationLat = -6.3,
        destinationLon = 106.9,
        mode = "mode",
        priority = "FASTEST",
        resultJson = "{}",
        createdAt = createdAt
    )
}
