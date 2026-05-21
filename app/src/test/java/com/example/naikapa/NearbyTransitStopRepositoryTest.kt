package com.example.naikapa

import com.example.naikapa.data.model.GtfsStop
import com.example.naikapa.data.repository.NearbyTransitStopRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyTransitStopRepositoryTest {
    @Test
    fun findNearestStopsSortsByDistanceAndRespectsLimit() {
        val repository = NearbyTransitStopRepository {
            listOf(
                GtfsStop("far", "Far Stop", -6.3000, 106.9000, "tj"),
                GtfsStop("near", "Near Stop", -6.2010, 106.8010, "tj"),
                GtfsStop("mid", "Mid Stop", -6.2200, 106.8200, "tj")
            )
        }

        val result = repository.findNearestStops(
            latitude = -6.2000,
            longitude = 106.8000,
            maxDistanceMeters = 20_000.0,
            limit = 2
        )

        assertEquals(2, result.size)
        assertEquals("near", result[0].stopId)
        assertEquals("mid", result[1].stopId)
    }

    @Test
    fun findNearestStopsFiltersByRadius() {
        val repository = NearbyTransitStopRepository {
            listOf(
                GtfsStop("near", "Near Stop", -6.2001, 106.8001, "tj"),
                GtfsStop("far", "Far Stop", -6.5000, 107.1000, "tj")
            )
        }

        val result = repository.findNearestStops(
            latitude = -6.2000,
            longitude = 106.8000,
            maxDistanceMeters = 1000.0,
            limit = 5
        )

        assertEquals(1, result.size)
        assertEquals("near", result.first().stopId)
        assertTrue(result.first().distanceMeters < 1000.0)
    }

    @Test
    fun findNearestStopsFiltersByAgency() {
        val repository = NearbyTransitStopRepository {
            listOf(
                GtfsStop("tj", "TJ Stop", -6.2001, 106.8001, "tj"),
                GtfsStop("krl", "KRL Stop", -6.2002, 106.8002, "krl")
            )
        }

        val result = repository.findNearestStops(
            latitude = -6.2000,
            longitude = 106.8000,
            maxDistanceMeters = 1000.0,
            limit = 5,
            agencyId = "krl"
        )

        assertEquals(1, result.size)
        assertEquals("krl", result.first().stopId)
    }
}
