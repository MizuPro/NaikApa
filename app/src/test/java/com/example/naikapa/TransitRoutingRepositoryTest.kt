package com.example.naikapa

import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitEdge
import com.example.naikapa.data.model.TransitEdgeType
import com.example.naikapa.data.model.TransitGraph
import com.example.naikapa.data.model.TransitGraphStats
import com.example.naikapa.data.model.TransitMode
import com.example.naikapa.data.model.TransitNode
import com.example.naikapa.domain.routing.DijkstraAlgorithm
import com.example.naikapa.domain.routing.RouteStepBuilder
import com.example.naikapa.domain.routing.TransitRouteMetricsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TransitRoutingRepositoryTest {
    @Test
    fun routeResultContainsFareAndTotalCost() {
        val graph = graphOf(
            edge("A", "B", "TJ1", "Tije", 100),
            edge("B", "C", "TJ1", "Tije", 100)
        )
        val edges = DijkstraAlgorithm().findPath(graph, "A", "C", TransitMode.ALL, SortPreference.FASTEST)

        assertNotNull(edges)
        val steps = RouteStepBuilder().build(graph, edges.orEmpty())
        val metrics = TransitRouteMetricsCalculator().calculate(edges.orEmpty(), steps)

        assertEquals(3500, metrics.estimatedFare)
        assertEquals(0, metrics.estimatedBbm)
        assertEquals(3500, metrics.estimatedTotalCost)
    }

    private fun graphOf(vararg edges: TransitEdge): TransitGraph {
        val stopIds = edges.flatMap { listOf(it.fromStopId, it.toStopId) }.toSet()
        val nodes = stopIds.associateWith { TransitNode(it, "Stop $it", -6.0, 106.0, "Tije") }
        return TransitGraph(
            nodes = nodes,
            adjacency = edges.groupBy { it.fromStopId },
            stats = TransitGraphStats(nodes.size, edges.count { it.type == TransitEdgeType.TRANSIT }, edges.count { it.type == TransitEdgeType.WALKING })
        )
    }

    private fun edge(from: String, to: String, routeId: String, agencyId: String, duration: Int): TransitEdge =
        TransitEdge(
            fromStopId = from,
            toStopId = to,
            routeId = routeId,
            routeShortName = routeId,
            routeLongName = routeId,
            routeColor = null,
            routeTextColor = null,
            agencyId = agencyId,
            distanceMeters = 1000.0,
            durationSeconds = duration,
            type = TransitEdgeType.TRANSIT
        )
}
