package com.example.naikapa

import com.example.naikapa.domain.routing.FuelCostCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class FuelCostCalculatorTest {
    private val calculator = FuelCostCalculator()

    @Test
    fun calculateMotorFuelCostUsesMotorConsumption() {
        assertEquals(2500, calculator.calculateMotorFuelCost(10.0))
    }

    @Test
    fun calculateCarFuelCostUsesCarConsumption() {
        assertEquals(8333, calculator.calculateCarFuelCost(10.0))
    }

    @Test
    fun calculateFuelCostReturnsZeroForNonPositiveDistance() {
        assertEquals(0, calculator.calculateMotorFuelCost(0.0))
        assertEquals(0, calculator.calculateCarFuelCost(-1.0))
    }
}
