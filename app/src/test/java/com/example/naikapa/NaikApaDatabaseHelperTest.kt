package com.example.naikapa

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.local.NaikApaDbContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaikApaDatabaseHelperTest {
    @Test
    fun contractContainsAllMvpTables() {
        val expectedTables = listOf(
            "users",
            "user_profiles",
            "gtfs_stops",
            "gtfs_routes",
            "gtfs_trips",
            "gtfs_stop_times",
            "saved_trips",
            "search_history",
            "route_history",
            "disruption_reports",
            "route_cache"
        )

        assertEquals("naikapa.db", AppConstants.DATABASE_NAME)
        assertTrue(AppConstants.DATABASE_VERSION >= 1)
        expectedTables.forEach { table ->
            assertTrue(
                "Missing CREATE TABLE for $table",
                NaikApaDbContract.createTableStatements.any { it.contains("CREATE TABLE $table") }
            )
            assertTrue(
                "Missing DROP TABLE for $table",
                NaikApaDbContract.dropTableStatements.any { it.contains("DROP TABLE IF EXISTS $table") }
            )
        }
    }
}
