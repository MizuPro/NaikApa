package com.example.naikapa.domain.routing

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.model.RouteStep
import com.example.naikapa.data.model.TransitEdgeType
import kotlin.math.ceil
import kotlin.math.roundToInt

class FareCalculator {
    fun calculateTransitFare(steps: List<RouteStep>): Int {
        val transitSteps = steps.filter { it.type == TransitEdgeType.TRANSIT }
        return transitSteps.groupBy { it.agencyId }.entries.sumOf { (agencyId, agencySteps) ->
            when (agencyId) {
                "Tije" -> AppConstants.FARE_TRANSJAKARTA_FLAT
                "KAIC" -> calculateKrlFare(agencySteps.sumOf { it.distanceMeters } / 1000.0)
                "MRTJ" -> calculateMrtFare(agencySteps.sumOf { (it.stopCount - 1).coerceAtLeast(0) } + 1)
                "LRTJ" -> AppConstants.FARE_LRTJ_FLAT
                "LRTJB" -> calculateLrtJabodebekFare(agencySteps.sumOf { it.distanceMeters } / 1000.0)
                else -> 0
            }
        }
    }

    fun calculateStepFare(step: RouteStep): Int {
        if (step.type == TransitEdgeType.WALKING) return 0
        return when (step.agencyId) {
            "Tije" -> AppConstants.FARE_TRANSJAKARTA_FLAT
            "KAIC" -> calculateKrlFare(step.distanceMeters / 1000.0)
            "MRTJ" -> calculateMrtFare(step.stopCount)
            "LRTJ" -> AppConstants.FARE_LRTJ_FLAT
            "LRTJB" -> calculateLrtJabodebekFare(step.distanceMeters / 1000.0)
            else -> 0
        }
    }

    fun calculateKrlFare(distanceKm: Double): Int {
        if (distanceKm <= AppConstants.FARE_KRL_BASE_DISTANCE_KM) {
            return AppConstants.FARE_KRL_BASE
        }
        val extraBlocks = ceil(
            (distanceKm - AppConstants.FARE_KRL_BASE_DISTANCE_KM) /
                AppConstants.FARE_KRL_INCREMENT_DISTANCE_KM
        ).toInt()
        return AppConstants.FARE_KRL_BASE + extraBlocks * AppConstants.FARE_KRL_INCREMENT
    }

    fun calculateMrtFare(stopCount: Int): Int {
        val passedStops = (stopCount - 1).coerceAtLeast(0)
        val fare = AppConstants.FARE_MRT_BASE + passedStops * AppConstants.FARE_MRT_PER_STOP
        return fare.coerceAtMost(AppConstants.FARE_MRT_MAX)
    }

    fun calculateLrtJabodebekFare(distanceKm: Double): Int {
        val extraDistance = (distanceKm - AppConstants.FARE_LRTJB_BASE_DISTANCE_KM).coerceAtLeast(0.0)
        val fare = AppConstants.FARE_LRTJB_BASE + (extraDistance * AppConstants.FARE_LRTJB_PER_KM).roundToInt()
        return fare.coerceAtMost(AppConstants.FARE_LRTJB_MAX)
    }
}
