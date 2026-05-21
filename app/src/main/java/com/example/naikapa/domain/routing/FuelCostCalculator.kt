package com.example.naikapa.domain.routing

import com.example.naikapa.common.AppConstants
import kotlin.math.roundToInt

class FuelCostCalculator {
    fun calculateMotorFuelCost(distanceKm: Double): Int =
        calculateFuelCost(distanceKm, AppConstants.MOTOR_CONSUMPTION_KM_PER_LITER)

    fun calculateCarFuelCost(distanceKm: Double): Int =
        calculateFuelCost(distanceKm, AppConstants.CAR_CONSUMPTION_KM_PER_LITER)

    private fun calculateFuelCost(distanceKm: Double, consumptionKmPerLiter: Double): Int {
        if (distanceKm <= 0.0) return 0
        val liters = distanceKm / consumptionKmPerLiter
        return (liters * AppConstants.FUEL_COST_PER_LITER).roundToInt()
    }
}
