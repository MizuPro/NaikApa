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
        assertTrue(
            "Missing user email index",
            NaikApaDbContract.indexStatements.any { it.contains("idx_users_email") }
        )
    }

    @Test
    fun mapConstantsAreConfiguredForJabodetabekPreview() {
        assertEquals("NaikApa/1.0", AppConstants.MAP_USER_AGENT)
        assertEquals(-6.2088, AppConstants.MAP_DEFAULT_LAT, 0.0001)
        assertEquals(106.8456, AppConstants.MAP_DEFAULT_LON, 0.0001)
        assertTrue(AppConstants.MAP_DEFAULT_ZOOM > 0.0)
        assertTrue(AppConstants.MAP_LOCATION_ZOOM > AppConstants.MAP_DEFAULT_ZOOM)
        assertEquals("CartoDB_Positron", AppConstants.MAP_TILE_POSITRON)
        assertEquals("CartoDB_DarkMatter", AppConstants.MAP_TILE_DARK_MATTER)
    }

    @Test
    fun tomTomSearchConstantsAreConfigured() {
        assertEquals("https://api.tomtom.com/", AppConstants.TOMTOM_BASE_URL)
        assertEquals("ID", AppConstants.TOMTOM_COUNTRY_SET_ID)
        assertTrue(AppConstants.TOMTOM_TYPEAHEAD)
        assertEquals(8, AppConstants.TOMTOM_SEARCH_LIMIT)
        assertEquals(3, AppConstants.TOMTOM_MIN_QUERY_LENGTH)
        assertEquals(300L, AppConstants.TOMTOM_SEARCH_DEBOUNCE_MS)
        assertEquals("isi_api_key_kamu_di_sini", AppConstants.TOMTOM_API_KEY_PLACEHOLDER)
    }

    @Test
    fun tomTomRoutingConstantsAreConfigured() {
        assertEquals("fastest", AppConstants.TOMTOM_ROUTING_ROUTE_TYPE_FASTEST)
        assertEquals("motorcycle", AppConstants.TOMTOM_ROUTING_TRAVEL_MODE_MOTORCYCLE)
        assertEquals("car", AppConstants.TOMTOM_ROUTING_TRAVEL_MODE_CAR)
        assertEquals("tollRoads", AppConstants.TOMTOM_ROUTING_AVOID_TOLL_ROADS)
        assertEquals(2, AppConstants.TOMTOM_MAX_ALTERNATIVES)
    }

    @Test
    fun gtfsSearchConstantsAreConfigured() {
        // Limit hasil lokal cukup besar untuk menampilkan pilihan yang relevan
        assertTrue("GTFS_SEARCH_LIMIT harus >= 5", AppConstants.GTFS_SEARCH_LIMIT >= 5)
        // Min query length GTFS harus <= TomTom agar pencarian lokal tetap bisa dimulai lebih awal
        assertTrue(
            "GTFS_MIN_QUERY_LENGTH harus <= TOMTOM_MIN_QUERY_LENGTH",
            AppConstants.GTFS_MIN_QUERY_LENGTH <= AppConstants.TOMTOM_MIN_QUERY_LENGTH
        )
        assertEquals("Nilai source GTFS harus 'gtfs'", "gtfs", AppConstants.GTFS_SOURCE)
        assertEquals(10, AppConstants.GTFS_SEARCH_LIMIT)
        assertEquals(2, AppConstants.GTFS_MIN_QUERY_LENGTH)
    }
}
