package com.example.naikapa

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitEdge
import com.example.naikapa.data.model.TransitEdgeType
import com.example.naikapa.domain.routing.TransitEdgeCostCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitEdgeCostCalculatorTest {
    private val calculator = TransitEdgeCostCalculator()

    @Test
    fun fastestAddsTransferPenaltyWhenRouteChanges() {
        val previous = edge("A", "B", "TJ1", 100)
        val next = edge("B", "C", "TJ2", 120)

        val cost = calculator.calculate(next, previous, SortPreference.FASTEST)

        assertEquals(120.0 + AppConstants.ROUTING_TRANSFER_PENALTY_SECONDS, cost, 0.0001)
    }

    @Test
    fun cheapestChargesAgencyEntryOnlyForFirstAgencyEdge() {
        val first = edge("A", "B", "TJ1", 100)
        val second = edge("B", "C", "TJ1", 100)

        val firstCost = calculator.calculate(first, null, SortPreference.CHEAPEST)
        val secondCost = calculator.calculate(second, first, SortPreference.CHEAPEST)

        assertTrue(firstCost > secondCost)
        assertEquals(AppConstants.ROUTING_AGENCY_ENTRY_COST.toDouble(), firstCost, 1.0)
    }

    @Test
    fun minWalkingPenalizesWalkingDistanceMoreThanTransitDuration() {
        val walking = edge("A", "B", "WALKING", 60, TransitEdgeType.WALKING, distanceMeters = 300.0)
        val transit = edge("A", "B", "TJ1", 600)

        val walkingCost = calculator.calculate(walking, null, SortPreference.MIN_WALKING)
        val transitCost = calculator.calculate(transit, null, SortPreference.MIN_WALKING)

        assertTrue(walkingCost > transitCost)
    }

    @Test
    fun fewestTransfersAddsOneWhenRouteChanges() {
        val previous = edge("A", "B", "TJ1", 100)
        val next = edge("B", "C", "TJ2", 100)

        val cost = calculator.calculate(next, previous, SortPreference.FEWEST_TRANSFERS)

        assertEquals(1.0, cost, 0.01)
    }

    private fun edge(
        from: String,
        to: String,
        routeId: String,
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
        agencyId = if (type == TransitEdgeType.WALKING) "WALKING" else "Tije",
        distanceMeters = distanceMeters,
        durationSeconds = duration,
        type = type
    )
}
