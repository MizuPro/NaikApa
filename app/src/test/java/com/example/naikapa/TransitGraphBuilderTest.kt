package com.example.naikapa

import com.example.naikapa.data.model.GtfsAdjacentStopConnection
import com.example.naikapa.data.model.GtfsStop
import com.example.naikapa.data.model.TransitEdgeType
import com.example.naikapa.domain.routing.TransitGraphBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitGraphBuilderTest {
    private val builder = TransitGraphBuilder()

    @Test
    fun buildCreatesNodesTransitEdgesWalkingEdgesAndStats() {
        val stops = listOf(
            stop("A", "Tije", -6.2000, 106.8200),
            stop("B", "Tije", -6.2010, 106.8200),
            stop("C", "KAIC", -6.2012, 106.8200)
        )
        val graph = builder.build(
            stops = stops,
            connections = listOf(
                connection("A", "B", "T1", "08:00:00", "08:05:00"),
                connection("A", "B", "T2", "08:10:00", "08:17:00")
            )
        )

        assertEquals(3, graph.stats.nodeCount)
        assertEquals(1, graph.stats.transitEdgeCount)
        assertEquals(4, graph.stats.walkingEdgeCount)
        assertEquals(5, graph.stats.totalEdgeCount)

        val transitEdge = graph.adjacency["A"]?.first { it.type == TransitEdgeType.TRANSIT }
        assertNotNull(transitEdge)
        assertEquals("B", transitEdge?.toStopId)
        assertEquals(360, transitEdge?.durationSeconds)
        assertTrue(graph.adjacency["B"].orEmpty().any { it.type == TransitEdgeType.WALKING && it.toStopId == "C" })
    }

    private fun stop(id: String, agencyId: String, lat: Double, lon: Double): GtfsStop =
        GtfsStop(
            stopId = id,
            stopName = "Stop $id",
            stopLat = lat,
            stopLon = lon,
            agencyId = agencyId,
            stopType = "station"
        )

    private fun connection(
        fromStopId: String,
        toStopId: String,
        tripId: String,
        departureTime: String,
        arrivalTime: String
    ): GtfsAdjacentStopConnection = GtfsAdjacentStopConnection(
        fromStopId = fromStopId,
        fromStopName = "Stop $fromStopId",
        fromStopLat = if (fromStopId == "A") -6.2000 else -6.2010,
        fromStopLon = 106.8200,
        fromAgencyId = "Tije",
        fromStopType = "station",
        toStopId = toStopId,
        toStopName = "Stop $toStopId",
        toStopLat = -6.2010,
        toStopLon = 106.8200,
        toAgencyId = "Tije",
        toStopType = "station",
        tripId = tripId,
        routeId = "R1",
        routeShortName = "1",
        routeLongName = "Route 1",
        routeColor = "E31E24",
        routeTextColor = "FFFFFF",
        agencyId = "Tije",
        departureTime = departureTime,
        arrivalTime = arrivalTime
    )
}
