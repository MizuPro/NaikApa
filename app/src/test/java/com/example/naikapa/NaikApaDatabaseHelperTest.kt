package com.example.naikapa

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.local.NaikApaDbContract
import com.example.naikapa.data.local.PrebuiltDatabaseCopier
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

    @Test
    fun gtfsIndexesAndPrebuiltAssetPathAreConfigured() {
        val expectedGtfsIndexes = listOf(
            "idx_gtfs_stops_name",
            "idx_gtfs_stops_agency",
            "idx_gtfs_routes_agency",
            "idx_gtfs_trips_route",
            "idx_gtfs_stop_times_trip_sequence",
            "idx_gtfs_stop_times_stop"
        )

        assertEquals("databases/naikapa_gtfs.db", PrebuiltDatabaseCopier.ASSET_DATABASE_PATH)
        expectedGtfsIndexes.forEach { indexName ->
            assertTrue(
                "Missing GTFS index $indexName",
                NaikApaDbContract.indexStatements.any { it.contains(indexName) }
            )
        }
    }
}
