package com.example.naikapa

import com.example.naikapa.data.model.TransitEdge
import com.example.naikapa.data.model.TransitEdgeType
import com.example.naikapa.data.model.TransitGraph
import com.example.naikapa.data.model.TransitGraphStats
import com.example.naikapa.data.model.TransitNode
import com.example.naikapa.domain.routing.RouteStepBuilder
import com.example.naikapa.domain.routing.TransitRouteMetricsCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteStepBuilderTest {
    @Test
    fun buildMergesSameRouteEdgesAndKeepsWalkingSeparate() {
        val edges = listOf(
            edge("A", "B", "TJ1", "Tije", 100),
            edge("B", "C", "TJ1", "Tije", 120),
            edge("C", "D", "WALKING", "WALKING", 60, TransitEdgeType.WALKING)
        )
        val graph = graphOf(edges)

        val steps = RouteStepBuilder().build(graph, edges)

        assertEquals(2, steps.size)
        assertEquals("A", steps[0].fromStop.stopId)
        assertEquals("C", steps[0].toStop.stopId)
        assertEquals(3, steps[0].stopCount)
        assertEquals(220, steps[0].durationSeconds)
        assertEquals(TransitEdgeType.WALKING, steps[1].type)
    }

    @Test
    fun metricsCalculatorCountsDurationWalkingDistanceAndTransitChanges() {
        val edges = listOf(
            edge("A", "B", "TJ1", "Tije", 100, distanceMeters = 500.0),
            edge("B", "C", "TJ2", "Tije", 120, distanceMeters = 700.0),
            edge("C", "D", "WALKING", "WALKING", 60, TransitEdgeType.WALKING, 250.0)
        )

        val steps = RouteStepBuilder().build(graphOf(edges), edges)
        val metrics = TransitRouteMetricsCalculator().calculate(edges, steps)

        assertEquals(280, metrics.totalDurationSeconds)
        assertEquals(1450.0, metrics.totalDistanceMeters, 0.001)
        assertEquals(250.0, metrics.walkingDistanceMeters, 0.001)
        assertEquals(1, metrics.transitCount)
        assertEquals(3500, metrics.estimatedFare)
        assertEquals(0, metrics.estimatedBbm)
        assertEquals(3500, metrics.estimatedTotalCost)
    }

    private fun graphOf(edges: List<TransitEdge>): TransitGraph {
        val stopIds = edges.flatMap { listOf(it.fromStopId, it.toStopId) }.toSet()
        val nodes = stopIds.associateWith { TransitNode(it, "Stop $it", -6.0, 106.0, "Tije") }
        return TransitGraph(
            nodes = nodes,
            adjacency = edges.groupBy { it.fromStopId },
            stats = TransitGraphStats(nodes.size, edges.count { it.type == TransitEdgeType.TRANSIT }, edges.count { it.type == TransitEdgeType.WALKING })
        )
    }

    private fun edge(
        from: String,
        to: String,
        routeId: String,
        agencyId: String,
        duration: Int,
        type: TransitEdgeType = TransitEdgeType.TRANSIT,
        distanceMeters: Double = 100.0
    ): TransitEdge = TransitEdge(
        fromStopId = from,
        toStopId = to,
        routeId = routeId,
        routeShortName = routeId,
        routeLongName = routeId,
        routeColor = null,
        routeTextColor = null,
        agencyId = agencyId,
        distanceMeters = distanceMeters,
        durationSeconds = duration,
        type = type
    )
}
