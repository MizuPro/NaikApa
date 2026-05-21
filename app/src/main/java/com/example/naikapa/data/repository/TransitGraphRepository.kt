package com.example.naikapa.data.repository

import android.util.Log
import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.local.GtfsDao
import com.example.naikapa.data.model.TransitGraph
import com.example.naikapa.domain.routing.TransitGraphBuilder

class TransitGraphRepository(
    private val gtfsDao: GtfsDao,
    private val graphBuilder: TransitGraphBuilder = TransitGraphBuilder()
) {
    @Volatile
    private var cachedGraph: TransitGraph? = null

    fun getGraph(): TransitGraph {
        cachedGraph?.let { return it }
        return synchronized(this) {
            cachedGraph ?: buildGraph().also { cachedGraph = it }
        }
    }

    fun buildGraph(limitConnections: Int? = null): TransitGraph {
        val connections = gtfsDao.getAdjacentStopConnections(limitConnections)
        val allStops = gtfsDao.getAllStopsForGraph()
        val stops = if (limitConnections == null) {
            allStops
        } else {
            val relevantStopIds = connections.flatMap { listOf(it.fromStopId, it.toStopId) }.toSet()
            allStops.filter { it.stopId in relevantStopIds }
        }
        val graph = graphBuilder.build(stops, connections)
        Log.d(
            AppConstants.TRANSIT_GRAPH_LOG_TAG,
            "Graph built: nodes=${graph.stats.nodeCount}, transitEdges=${graph.stats.transitEdgeCount}, " +
                "walkingEdges=${graph.stats.walkingEdgeCount}, totalEdges=${graph.stats.totalEdgeCount}"
        )
        return graph
    }

    fun clearCache() {
        cachedGraph = null
    }
}
