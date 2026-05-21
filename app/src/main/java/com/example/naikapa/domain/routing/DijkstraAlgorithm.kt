package com.example.naikapa.domain.routing

import com.example.naikapa.data.model.TransitEdge
import com.example.naikapa.data.model.TransitEdgeType
import com.example.naikapa.data.model.TransitGraph
import com.example.naikapa.data.model.TransitMode
import com.example.naikapa.data.model.SortPreference
import java.util.PriorityQueue

class DijkstraAlgorithm(
    private val costCalculator: TransitEdgeCostCalculator = TransitEdgeCostCalculator()
) {
    fun findPath(
        graph: TransitGraph,
        startStopId: String,
        endStopId: String,
        mode: TransitMode,
        sortPreference: SortPreference
    ): List<TransitEdge>? {
        if (!graph.nodes.containsKey(startStopId) || !graph.nodes.containsKey(endStopId)) return null
        if (startStopId == endStopId) return emptyList()

        val distances = mutableMapOf(startStopId to 0.0)
        val previous = mutableMapOf<String, PreviousTrace>()
        val queue = PriorityQueue<QueueEntry>(compareBy { it.cost })
        queue.add(QueueEntry(startStopId, 0.0))

        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: break
            if (current.cost > distances.getOrDefault(current.stopId, Double.POSITIVE_INFINITY)) continue
            if (current.stopId == endStopId) break

            val previousEdge = previous[current.stopId]?.edge
            graph.adjacency[current.stopId].orEmpty()
                .asSequence()
                .filter { isEdgeAllowed(it, mode) }
                .forEach { edge ->
                    val newCost = current.cost + costCalculator.calculate(edge, previousEdge, sortPreference)
                    if (newCost < distances.getOrDefault(edge.toStopId, Double.POSITIVE_INFINITY)) {
                        distances[edge.toStopId] = newCost
                        previous[edge.toStopId] = PreviousTrace(fromStopId = current.stopId, edge = edge)
                        queue.add(QueueEntry(edge.toStopId, newCost))
                    }
                }
        }

        if (!previous.containsKey(endStopId)) return null
        return reconstructPath(startStopId, endStopId, previous)
    }

    private fun isEdgeAllowed(edge: TransitEdge, mode: TransitMode): Boolean {
        if (edge.type == TransitEdgeType.WALKING) return true
        return when (mode) {
            TransitMode.ALL -> true
            TransitMode.TRANSJAKARTA -> edge.agencyId == "Tije"
            TransitMode.KRL -> edge.agencyId == "KAIC"
            TransitMode.MRT -> edge.agencyId == "MRTJ"
            TransitMode.LRT -> edge.agencyId == "LRTJ" || edge.agencyId == "LRTJB"
        }
    }

    private fun reconstructPath(
        startStopId: String,
        endStopId: String,
        previous: Map<String, PreviousTrace>
    ): List<TransitEdge> {
        val edges = ArrayDeque<TransitEdge>()
        var currentStopId = endStopId
        while (currentStopId != startStopId) {
            val trace = previous[currentStopId] ?: return emptyList()
            edges.addFirst(trace.edge)
            currentStopId = trace.fromStopId
        }
        return edges.toList()
    }

    private data class QueueEntry(val stopId: String, val cost: Double)

    private data class PreviousTrace(
        val fromStopId: String,
        val edge: TransitEdge
    )
}
