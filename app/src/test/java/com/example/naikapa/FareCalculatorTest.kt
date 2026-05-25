package com.example.naikapa

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.model.RouteStep
import com.example.naikapa.data.model.TransitEdgeType
import com.example.naikapa.data.model.TransitNode
import com.example.naikapa.domain.routing.FareCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class FareCalculatorTest {
    private val calculator = FareCalculator()

    @Test
    fun calculateStepFareReturnsTransjakartaFlatFare() {
        assertEquals(AppConstants.FARE_TRANSJAKARTA_FLAT, calculator.calculateStepFare(step("Tije")))
    }

    @Test
    fun calculateKrlFareUsesProgressiveDistance() {
        assertEquals(3000, calculator.calculateKrlFare(20.0))
        assertEquals(4000, calculator.calculateKrlFare(30.0))
        assertEquals(6000, calculator.calculateKrlFare(55.0))
    }

    @Test
    fun calculateMrtFareIsCapped() {
        assertEquals(7000, calculator.calculateMrtFare(stopCount = 5))
        assertEquals(AppConstants.FARE_MRT_MAX, calculator.calculateMrtFare(stopCount = 20))
    }

    @Test
    fun calculateStepFareReturnsLrtJakartaFlatFare() {
        assertEquals(AppConstants.FARE_LRTJ_FLAT, calculator.calculateStepFare(step("LRTJ")))
    }

    @Test
    fun calculateLrtJabodebekFareIsDistanceBasedAndCapped() {
        assertEquals(5000, calculator.calculateLrtJabodebekFare(1.0))
        assertEquals(7800, calculator.calculateLrtJabodebekFare(5.0))
        assertEquals(AppConstants.FARE_LRTJB_MAX, calculator.calculateLrtJabodebekFare(40.0))
    }

    @Test
    fun calculateStepFareReturnsZeroForWalking() {
        assertEquals(0, calculator.calculateStepFare(step("WALKING", TransitEdgeType.WALKING)))
    }

    @Test
    fun calculateTransitFareSumsNonWalkingSteps() {
        val fare = calculator.calculateTransitFare(
            listOf(
                step("Tije"),
                step("KAIC", distanceMeters = 30_000.0),
                step("WALKING", TransitEdgeType.WALKING)
            )
        )

        assertEquals(7500, fare)
    }

    private fun step(
        agencyId: String,
        type: TransitEdgeType = TransitEdgeType.TRANSIT,
        distanceMeters: Double = 1000.0,
        stopCount: Int = 3
    ): RouteStep = RouteStep(
        routeId = agencyId,
        routeShortName = agencyId,
        routeLongName = agencyId,
        routeColor = null,
        routeTextColor = null,
        agencyId = agencyId,
        type = type,
        fromStop = TransitNode("A", "A", -6.0, 106.0, agencyId),
        toStop = TransitNode("B", "B", -6.1, 106.1, agencyId),
        stopCount = stopCount,
        distanceMeters = distanceMeters,
        durationSeconds = 600
    )
}
