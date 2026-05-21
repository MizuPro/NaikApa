package com.example.naikapa

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.model.GtfsStop
import com.example.naikapa.data.model.TransitEdgeType
import com.example.naikapa.domain.routing.WalkingTransferBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkingTransferBuilderTest {
    private val builder = WalkingTransferBuilder()

    @Test
    fun buildCreatesBidirectionalWalkingEdgesForNearbyDifferentAgencies() {
        val edges = builder.build(
            listOf(
                stop("TJ_DUKUH", "Tije", -6.2000, 106.8200),
                stop("KRL_SUDIRMAN", "KAIC", -6.2008, 106.8200)
            )
        )

        assertEquals(2, edges.size)
        assertTrue(edges.any { it.fromStopId == "TJ_DUKUH" && it.toStopId == "KRL_SUDIRMAN" })
        assertTrue(edges.any { it.fromStopId == "KRL_SUDIRMAN" && it.toStopId == "TJ_DUKUH" })
        edges.forEach { edge ->
            assertEquals(TransitEdgeType.WALKING, edge.type)
            assertEquals(AppConstants.WALKING_ROUTE_ID, edge.routeId)
            assertTrue(edge.distanceMeters <= AppConstants.WALKING_TRANSFER_RADIUS_M)
            assertTrue(edge.durationSeconds > 0)
        }
    }

    @Test
    fun buildSkipsStopsFromSameAgency() {
        val edges = builder.build(
            listOf(
                stop("TJ_A", "Tije", -6.2000, 106.8200),
                stop("TJ_B", "Tije", -6.2008, 106.8200)
            )
        )

        assertTrue(edges.isEmpty())
    }

    @Test
    fun buildSkipsStopsOutsideRadius() {
        val edges = builder.build(
            listOf(
                stop("TJ_A", "Tije", -6.2000, 106.8200),
                stop("KRL_B", "KAIC", -6.2100, 106.8200)
            )
        )

        assertTrue(edges.isEmpty())
    }

    private fun stop(id: String, agencyId: String, lat: Double, lon: Double): GtfsStop =
        GtfsStop(
            stopId = id,
            stopName = id,
            stopLat = lat,
            stopLon = lon,
            agencyId = agencyId,
            stopType = "station"
        )
}
