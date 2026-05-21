package com.example.naikapa.domain.routing

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitEdge
import com.example.naikapa.data.model.TransitEdgeType

class TransitEdgeCostCalculator {
    fun calculate(edge: TransitEdge, previousEdge: TransitEdge?, sortPreference: SortPreference): Double {
        val transferPenalty = transferPenalty(edge, previousEdge)
        return when (sortPreference) {
            SortPreference.FASTEST -> edge.durationSeconds + transferPenalty
            SortPreference.CHEAPEST -> cheapestCost(edge, previousEdge)
            SortPreference.MIN_WALKING -> minWalkingCost(edge) + transferPenalty * 0.25
            SortPreference.FEWEST_TRANSFERS -> fewestTransfersCost(edge, previousEdge)
        }
    }

    private fun cheapestCost(edge: TransitEdge, previousEdge: TransitEdge?): Double {
        val agencyEntryCost = if (
            edge.type == TransitEdgeType.TRANSIT &&
            previousEdge?.agencyId != edge.agencyId
        ) {
            AppConstants.ROUTING_AGENCY_ENTRY_COST.toDouble()
        } else {
            0.0
        }
        val routeTransferCost = if (isTransfer(edge, previousEdge)) {
            AppConstants.ROUTING_TRANSFER_COST.toDouble()
        } else {
            0.0
        }
        return agencyEntryCost + routeTransferCost + edge.durationSeconds * AppConstants.ROUTING_DURATION_TIEBREAKER
    }

    private fun minWalkingCost(edge: TransitEdge): Double {
        val walkingCost = if (edge.type == TransitEdgeType.WALKING) {
            edge.distanceMeters * AppConstants.ROUTING_WALKING_DISTANCE_WEIGHT
        } else {
            0.0
        }
        return walkingCost + edge.durationSeconds * AppConstants.ROUTING_DURATION_TIEBREAKER
    }

    private fun fewestTransfersCost(edge: TransitEdge, previousEdge: TransitEdge?): Double {
        val transferCost = if (isTransfer(edge, previousEdge)) {
            1.0
        } else {
            0.0
        }
        return transferCost + edge.durationSeconds * AppConstants.ROUTING_DURATION_TIEBREAKER
    }

    private fun transferPenalty(edge: TransitEdge, previousEdge: TransitEdge?): Double =
        if (isTransfer(edge, previousEdge)) {
            AppConstants.ROUTING_TRANSFER_PENALTY_SECONDS.toDouble()
        } else {
            0.0
        }

    private fun isTransfer(edge: TransitEdge, previousEdge: TransitEdge?): Boolean {
        if (previousEdge == null) return false
        if (edge.type == TransitEdgeType.WALKING || previousEdge.type == TransitEdgeType.WALKING) return true
        return edge.routeId != previousEdge.routeId || edge.agencyId != previousEdge.agencyId
    }
}
