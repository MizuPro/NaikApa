package com.example.naikapa

import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitEdge
import com.example.naikapa.data.model.TransitEdgeType
import com.example.naikapa.data.model.TransitGraph
import com.example.naikapa.data.model.TransitGraphStats
import com.example.naikapa.data.model.TransitMode
import com.example.naikapa.data.model.TransitNode
import com.example.naikapa.domain.routing.DijkstraAlgorithm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DijkstraAlgorithmTest {
    private val algorithm = DijkstraAlgorithm()

    @Test
    fun findPathReturnsFastestPath() {
        val graph = graphOf(
            edge("A", "B", "TJ1", "Tije", 100),
            edge("B", "D", "TJ1", "Tije", 100),
            edge("A", "C", "TJ2", "Tije", 400),
            edge("C", "D", "TJ2", "Tije", 400)
        )

        val path = algorithm.findPath(graph, "A", "D", TransitMode.ALL, SortPreference.FASTEST)

        assertEquals(listOf("B", "D"), path?.map { it.toStopId })
    }

    @Test
    fun findPathReturnsNullWhenUnreachable() {
        val graph = graphOf(edge("A", "B", "TJ1", "Tije", 100))

        val path = algorithm.findPath(graph, "A", "D", TransitMode.ALL, SortPreference.FASTEST)

        assertNull(path)
    }

    @Test
    fun findPathFiltersTransitModeButAllowsWalkingTransfer() {
        val graph = graphOf(
            edge("A", "B", "TJ1", "Tije", 100),
            edge("A", "C", "KRL1", "KAIC", 120),
            edge("C", "D", "WALKING", "WALKING", 60, TransitEdgeType.WALKING)
        )

        val path = algorithm.findPath(graph, "A", "D", TransitMode.KRL, SortPreference.FASTEST)

        assertEquals(listOf("KRL1", "WALKING"), path?.map { it.routeId })
        assertTrue(path.orEmpty().none { it.routeId == "TJ1" })
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

    private fun edge(
        from: String,
        to: String,
        routeId: String,
        agencyId: String,
        duration: Int,
        type: TransitEdgeType = TransitEdgeType.TRANSIT
    ): TransitEdge = TransitEdge(
        fromStopId = from,
        toStopId = to,
        routeId = routeId,
        routeShortName = routeId,
        routeLongName = routeId,
        routeColor = null,
        routeTextColor = null,
        agencyId = agencyId,
        distanceMeters = duration.toDouble(),
        durationSeconds = duration,
        type = type
    )
}
