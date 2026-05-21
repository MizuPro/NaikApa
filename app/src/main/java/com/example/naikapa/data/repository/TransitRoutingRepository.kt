package com.example.naikapa.data.repository

import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitMode
import com.example.naikapa.data.model.TransitRouteResult
import com.example.naikapa.domain.routing.DijkstraAlgorithm
import com.example.naikapa.domain.routing.RouteStepBuilder
import com.example.naikapa.domain.routing.TransitRouteMetricsCalculator

class TransitRoutingRepository(
    private val graphRepository: TransitGraphRepository,
    private val dijkstraAlgorithm: DijkstraAlgorithm = DijkstraAlgorithm(),
    private val stepBuilder: RouteStepBuilder = RouteStepBuilder(),
    private val metricsCalculator: TransitRouteMetricsCalculator = TransitRouteMetricsCalculator()
) {
    fun findRoute(
        startStopId: String,
        endStopId: String,
        mode: TransitMode,
        sortPreference: SortPreference
    ): TransitRouteResult? {
        val graph = graphRepository.getGraph()
        val edges = dijkstraAlgorithm.findPath(graph, startStopId, endStopId, mode, sortPreference) ?: return null
        val startStop = graph.nodes[startStopId] ?: return null
        val endStop = graph.nodes[endStopId] ?: return null
        val steps = stepBuilder.build(graph, edges)

        return TransitRouteResult(
            startStop = startStop,
            endStop = endStop,
            mode = mode,
            sortPreference = sortPreference,
            edges = edges,
            steps = steps,
            metrics = metricsCalculator.calculate(edges, steps)
        )
    }
}
