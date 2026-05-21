package com.example.naikapa

import com.example.naikapa.common.AppConstants
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FASE 19 — UI Resource Contract Test
 *
 * Lightweight test to verify that key UI constants and configurations
 * are properly defined and accessible. This does not test Android resources
 * directly (those require instrumentation), but validates the Kotlin-side
 * constants that drive UI behavior.
 */
class UiResourceContractTest {

    @Test
    fun `gtfs search constants are defined`() {
        assertTrue(
            "GTFS_SEARCH_LIMIT should be positive",
            AppConstants.GTFS_SEARCH_LIMIT > 0
        )
        assertTrue(
            "GTFS_MIN_QUERY_LENGTH should be at least 1",
            AppConstants.GTFS_MIN_QUERY_LENGTH >= 1
        )
        assertNotNull(
            "GTFS_SOURCE should not be null",
            AppConstants.GTFS_SOURCE
        )
        assertTrue(
            "GTFS_SOURCE should not be empty",
            AppConstants.GTFS_SOURCE.isNotEmpty()
        )
    }

    @Test
    fun `disruption report constants are defined`() {
        assertTrue(
            "DISRUPTION_ACTIVE_DURATION_MILLIS should be positive",
            AppConstants.DISRUPTION_ACTIVE_DURATION_MILLIS > 0
        )
        assertTrue(
            "DISRUPTION_CATEGORIES should not be empty",
            AppConstants.DISRUPTION_CATEGORIES.isNotEmpty()
        )
        assertTrue(
            "DISRUPTION_DESCRIPTION_MIN_LENGTH should be positive",
            AppConstants.DISRUPTION_DESCRIPTION_MIN_LENGTH > 0
        )
        assertTrue(
            "DISRUPTION_DESCRIPTION_MAX_LENGTH should be greater than min",
            AppConstants.DISRUPTION_DESCRIPTION_MAX_LENGTH > AppConstants.DISRUPTION_DESCRIPTION_MIN_LENGTH
        )
        assertNotNull(
            "DISRUPTION_PHOTO_DIR should not be null",
            AppConstants.DISRUPTION_PHOTO_DIR
        )
    }

    @Test
    fun `disruption categories contain expected entries`() {
        val categories = AppConstants.DISRUPTION_CATEGORIES
        assertTrue("Should have at least 3 categories", categories.size >= 3)
        // Verify no empty category names
        categories.forEach { category ->
            assertTrue("Category name should not be blank: '$category'", category.isNotBlank())
        }
    }

    @Test
    fun `disruption active duration is one hour`() {
        val oneHourMillis = 60L * 60L * 1000L
        assertTrue(
            "DISRUPTION_ACTIVE_DURATION_MILLIS should be >= 1 hour",
            AppConstants.DISRUPTION_ACTIVE_DURATION_MILLIS >= oneHourMillis
        )
    }

    @Test
    fun `gtfs search limit is within reasonable bounds`() {
        assertTrue(
            "GTFS_SEARCH_LIMIT should be at most 100",
            AppConstants.GTFS_SEARCH_LIMIT <= 100
        )
        assertTrue(
            "GTFS_SEARCH_LIMIT should be at least 5",
            AppConstants.GTFS_SEARCH_LIMIT >= 5
        )
    }
}
