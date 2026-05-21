package com.example.naikapa.data.local

import android.provider.BaseColumns

object NaikApaDbContract {
    object Users : BaseColumns {
        const val TABLE = "users"
        const val ID = "id_user"
        const val NAMA = "nama"
        const val EMAIL = "email"
        const val PASSWORD = "password"
        const val HAS_MOTOR = "has_motor"
        const val HAS_CAR = "has_car"
        const val CREATED_AT = "created_at"
    }

    object UserProfiles : BaseColumns {
        const val TABLE = "user_profiles"
        const val ID = "id_profile"
        const val ID_USER = "id_user"
        const val DEFAULT_MODE = "default_mode"
        const val DEFAULT_PRIORITY = "default_priority"
        const val HOME_LAT = "home_lat"
        const val HOME_LON = "home_lon"
        const val HOME_LABEL = "home_label"
    }

    object GtfsStops {
        const val TABLE = "gtfs_stops"
        const val STOP_ID = "stop_id"
        const val STOP_NAME = "stop_name"
        const val STOP_LAT = "stop_lat"
        const val STOP_LON = "stop_lon"
        const val AGENCY_ID = "agency_id"
        const val STOP_TYPE = "stop_type"
    }

    object GtfsRoutes {
        const val TABLE = "gtfs_routes"
        const val ROUTE_ID = "route_id"
        const val AGENCY_ID = "agency_id"
        const val ROUTE_SHORT_NAME = "route_short_name"
        const val ROUTE_LONG_NAME = "route_long_name"
        const val ROUTE_COLOR = "route_color"
        const val ROUTE_TEXT_COLOR = "route_text_color"
    }

    object GtfsTrips {
        const val TABLE = "gtfs_trips"
        const val TRIP_ID = "trip_id"
        const val ROUTE_ID = "route_id"
        const val SERVICE_ID = "service_id"
        const val DIRECTION_ID = "direction_id"
    }

    object GtfsStopTimes : BaseColumns {
        const val TABLE = "gtfs_stop_times"
        const val ID = "id_stop_time"
        const val TRIP_ID = "trip_id"
        const val ARRIVAL_TIME = "arrival_time"
        const val DEPARTURE_TIME = "departure_time"
        const val STOP_ID = "stop_id"
        const val STOP_SEQUENCE = "stop_sequence"
    }

    object SavedTrips : BaseColumns {
        const val TABLE = "saved_trips"
        const val ID = "id_saved"
        const val ID_USER = "id_user"
        const val NAMA_PERJALANAN = "nama_perjalanan"
        const val ORIGIN_NAME = "origin_name"
        const val ORIGIN_LAT = "origin_lat"
        const val ORIGIN_LON = "origin_lon"
        const val DESTINATION_NAME = "destination_name"
        const val DESTINATION_LAT = "destination_lat"
        const val DESTINATION_LON = "destination_lon"
        const val MODE = "mode"
        const val PRIORITY = "priority"
        const val CATATAN = "catatan"
        const val CREATED_AT = "created_at"
    }

    object SearchHistory : BaseColumns {
        const val TABLE = "search_history"
        const val ID = "id_search"
        const val ID_USER = "id_user"
        const val KEYWORD = "keyword"
        const val SELECTED_NAME = "selected_name"
        const val SELECTED_ADDRESS = "selected_address"
        const val SELECTED_LAT = "selected_lat"
        const val SELECTED_LON = "selected_lon"
        const val SEARCHED_AT = "searched_at"
    }

    object RouteHistory : BaseColumns {
        const val TABLE = "route_history"
        const val ID = "id_history"
        const val ID_USER = "id_user"
        const val ORIGIN_NAME = "origin_name"
        const val DESTINATION_NAME = "destination_name"
        const val MODE = "mode"
        const val PRIORITY = "priority"
        const val RECOMMENDATION_SUMMARY = "recommendation_summary"
        const val SCORE = "score"
        const val ESTIMATED_TIME = "estimated_time"
        const val ESTIMATED_COST = "estimated_cost"
        const val ESTIMATED_BBM = "estimated_bbm"
        const val WALKING_DISTANCE = "walking_distance"
        const val TRANSIT_COUNT = "transit_count"
        const val CREATED_AT = "created_at"
    }

    object DisruptionReports : BaseColumns {
        const val TABLE = "disruption_reports"
        const val ID = "id_report"
        const val ID_USER = "id_user"
        const val STOP_ID = "stop_id"
        const val ROUTE_ID = "route_id"
        const val CATEGORY = "category"
        const val DESCRIPTION = "description"
        const val PHOTO_PATH = "photo_path"
        const val IMPACT_LEVEL = "impact_level"
        const val STATUS = "status"
        const val CREATED_AT = "created_at"
        const val EXPIRED_AT = "expired_at"
    }

    object RouteCache : BaseColumns {
        const val TABLE = "route_cache"
        const val ID = "id_cache"
        const val ORIGIN_LAT = "origin_lat"
        const val ORIGIN_LON = "origin_lon"
        const val DESTINATION_LAT = "destination_lat"
        const val DESTINATION_LON = "destination_lon"
        const val MODE = "mode"
        const val PRIORITY = "priority"
        const val RESULT_JSON = "result_json"
        const val CREATED_AT = "created_at"
    }

    val createTableStatements = listOf(
        """
        CREATE TABLE ${Users.TABLE} (
            ${Users.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${Users.NAMA} TEXT NOT NULL,
            ${Users.EMAIL} TEXT NOT NULL UNIQUE,
            ${Users.PASSWORD} TEXT NOT NULL,
            ${Users.HAS_MOTOR} INTEGER NOT NULL DEFAULT 0,
            ${Users.HAS_CAR} INTEGER NOT NULL DEFAULT 0,
            ${Users.CREATED_AT} INTEGER NOT NULL
        )
        """.trimIndent(),
        """
        CREATE TABLE ${UserProfiles.TABLE} (
            ${UserProfiles.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${UserProfiles.ID_USER} INTEGER NOT NULL UNIQUE,
            ${UserProfiles.DEFAULT_MODE} TEXT,
            ${UserProfiles.DEFAULT_PRIORITY} TEXT,
            ${UserProfiles.HOME_LAT} REAL,
            ${UserProfiles.HOME_LON} REAL,
            ${UserProfiles.HOME_LABEL} TEXT,
            FOREIGN KEY(${UserProfiles.ID_USER}) REFERENCES ${Users.TABLE}(${Users.ID}) ON DELETE CASCADE
        )
        """.trimIndent(),
        """
        CREATE TABLE ${GtfsStops.TABLE} (
            ${GtfsStops.STOP_ID} TEXT PRIMARY KEY,
            ${GtfsStops.STOP_NAME} TEXT NOT NULL,
            ${GtfsStops.STOP_LAT} REAL NOT NULL,
            ${GtfsStops.STOP_LON} REAL NOT NULL,
            ${GtfsStops.AGENCY_ID} TEXT NOT NULL,
            ${GtfsStops.STOP_TYPE} TEXT
        )
        """.trimIndent(),
        """
        CREATE TABLE ${GtfsRoutes.TABLE} (
            ${GtfsRoutes.ROUTE_ID} TEXT PRIMARY KEY,
            ${GtfsRoutes.AGENCY_ID} TEXT NOT NULL,
            ${GtfsRoutes.ROUTE_SHORT_NAME} TEXT,
            ${GtfsRoutes.ROUTE_LONG_NAME} TEXT,
            ${GtfsRoutes.ROUTE_COLOR} TEXT,
            ${GtfsRoutes.ROUTE_TEXT_COLOR} TEXT
        )
        """.trimIndent(),
        """
        CREATE TABLE ${GtfsTrips.TABLE} (
            ${GtfsTrips.TRIP_ID} TEXT PRIMARY KEY,
            ${GtfsTrips.ROUTE_ID} TEXT NOT NULL,
            ${GtfsTrips.SERVICE_ID} TEXT,
            ${GtfsTrips.DIRECTION_ID} INTEGER,
            FOREIGN KEY(${GtfsTrips.ROUTE_ID}) REFERENCES ${GtfsRoutes.TABLE}(${GtfsRoutes.ROUTE_ID}) ON DELETE CASCADE
        )
        """.trimIndent(),
        """
        CREATE TABLE ${GtfsStopTimes.TABLE} (
            ${GtfsStopTimes.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${GtfsStopTimes.TRIP_ID} TEXT NOT NULL,
            ${GtfsStopTimes.ARRIVAL_TIME} TEXT NOT NULL,
            ${GtfsStopTimes.DEPARTURE_TIME} TEXT NOT NULL,
            ${GtfsStopTimes.STOP_ID} TEXT NOT NULL,
            ${GtfsStopTimes.STOP_SEQUENCE} INTEGER NOT NULL,
            FOREIGN KEY(${GtfsStopTimes.TRIP_ID}) REFERENCES ${GtfsTrips.TABLE}(${GtfsTrips.TRIP_ID}) ON DELETE CASCADE,
            FOREIGN KEY(${GtfsStopTimes.STOP_ID}) REFERENCES ${GtfsStops.TABLE}(${GtfsStops.STOP_ID}) ON DELETE CASCADE
        )
        """.trimIndent(),
        """
        CREATE TABLE ${SavedTrips.TABLE} (
            ${SavedTrips.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${SavedTrips.ID_USER} INTEGER NOT NULL,
            ${SavedTrips.NAMA_PERJALANAN} TEXT NOT NULL,
            ${SavedTrips.ORIGIN_NAME} TEXT NOT NULL,
            ${SavedTrips.ORIGIN_LAT} REAL NOT NULL,
            ${SavedTrips.ORIGIN_LON} REAL NOT NULL,
            ${SavedTrips.DESTINATION_NAME} TEXT NOT NULL,
            ${SavedTrips.DESTINATION_LAT} REAL NOT NULL,
            ${SavedTrips.DESTINATION_LON} REAL NOT NULL,
            ${SavedTrips.MODE} TEXT NOT NULL,
            ${SavedTrips.PRIORITY} TEXT NOT NULL,
            ${SavedTrips.CATATAN} TEXT,
            ${SavedTrips.CREATED_AT} INTEGER NOT NULL,
            FOREIGN KEY(${SavedTrips.ID_USER}) REFERENCES ${Users.TABLE}(${Users.ID}) ON DELETE CASCADE
        )
        """.trimIndent(),
        """
        CREATE TABLE ${SearchHistory.TABLE} (
            ${SearchHistory.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${SearchHistory.ID_USER} INTEGER NOT NULL,
            ${SearchHistory.KEYWORD} TEXT NOT NULL,
            ${SearchHistory.SELECTED_NAME} TEXT NOT NULL,
            ${SearchHistory.SELECTED_ADDRESS} TEXT,
            ${SearchHistory.SELECTED_LAT} REAL NOT NULL,
            ${SearchHistory.SELECTED_LON} REAL NOT NULL,
            ${SearchHistory.SEARCHED_AT} INTEGER NOT NULL,
            FOREIGN KEY(${SearchHistory.ID_USER}) REFERENCES ${Users.TABLE}(${Users.ID}) ON DELETE CASCADE
        )
        """.trimIndent(),
        """
        CREATE TABLE ${RouteHistory.TABLE} (
            ${RouteHistory.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${RouteHistory.ID_USER} INTEGER NOT NULL,
            ${RouteHistory.ORIGIN_NAME} TEXT NOT NULL,
            ${RouteHistory.DESTINATION_NAME} TEXT NOT NULL,
            ${RouteHistory.MODE} TEXT NOT NULL,
            ${RouteHistory.PRIORITY} TEXT NOT NULL,
            ${RouteHistory.RECOMMENDATION_SUMMARY} TEXT NOT NULL,
            ${RouteHistory.SCORE} INTEGER NOT NULL,
            ${RouteHistory.ESTIMATED_TIME} INTEGER NOT NULL,
            ${RouteHistory.ESTIMATED_COST} INTEGER NOT NULL,
            ${RouteHistory.ESTIMATED_BBM} INTEGER NOT NULL,
            ${RouteHistory.WALKING_DISTANCE} REAL NOT NULL,
            ${RouteHistory.TRANSIT_COUNT} INTEGER NOT NULL,
            ${RouteHistory.CREATED_AT} INTEGER NOT NULL,
            FOREIGN KEY(${RouteHistory.ID_USER}) REFERENCES ${Users.TABLE}(${Users.ID}) ON DELETE CASCADE
        )
        """.trimIndent(),
        """
        CREATE TABLE ${DisruptionReports.TABLE} (
            ${DisruptionReports.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${DisruptionReports.ID_USER} INTEGER NOT NULL,
            ${DisruptionReports.STOP_ID} TEXT,
            ${DisruptionReports.ROUTE_ID} TEXT,
            ${DisruptionReports.CATEGORY} TEXT NOT NULL,
            ${DisruptionReports.DESCRIPTION} TEXT NOT NULL,
            ${DisruptionReports.PHOTO_PATH} TEXT,
            ${DisruptionReports.IMPACT_LEVEL} INTEGER NOT NULL DEFAULT 1,
            ${DisruptionReports.STATUS} TEXT NOT NULL DEFAULT 'active',
            ${DisruptionReports.CREATED_AT} INTEGER NOT NULL,
            ${DisruptionReports.EXPIRED_AT} INTEGER NOT NULL,
            FOREIGN KEY(${DisruptionReports.ID_USER}) REFERENCES ${Users.TABLE}(${Users.ID}) ON DELETE CASCADE
        )
        """.trimIndent(),
        """
        CREATE TABLE ${RouteCache.TABLE} (
            ${RouteCache.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${RouteCache.ORIGIN_LAT} REAL NOT NULL,
            ${RouteCache.ORIGIN_LON} REAL NOT NULL,
            ${RouteCache.DESTINATION_LAT} REAL NOT NULL,
            ${RouteCache.DESTINATION_LON} REAL NOT NULL,
            ${RouteCache.MODE} TEXT NOT NULL,
            ${RouteCache.PRIORITY} TEXT NOT NULL,
            ${RouteCache.RESULT_JSON} TEXT NOT NULL,
            ${RouteCache.CREATED_AT} INTEGER NOT NULL
        )
        """.trimIndent()
    )

    val indexStatements = listOf(
        "CREATE INDEX IF NOT EXISTS idx_users_email ON ${Users.TABLE}(${Users.EMAIL})",
        "CREATE INDEX IF NOT EXISTS idx_saved_trips_user ON ${SavedTrips.TABLE}(${SavedTrips.ID_USER})",
        "CREATE INDEX IF NOT EXISTS idx_search_history_user_time ON ${SearchHistory.TABLE}(${SearchHistory.ID_USER}, ${SearchHistory.SEARCHED_AT})",
        "CREATE INDEX IF NOT EXISTS idx_route_history_user_time ON ${RouteHistory.TABLE}(${RouteHistory.ID_USER}, ${RouteHistory.CREATED_AT})",
        "CREATE INDEX IF NOT EXISTS idx_disruptions_status_expired ON ${DisruptionReports.TABLE}(${DisruptionReports.STATUS}, ${DisruptionReports.EXPIRED_AT})",
        "CREATE INDEX IF NOT EXISTS idx_disruptions_stop_route ON ${DisruptionReports.TABLE}(${DisruptionReports.STOP_ID}, ${DisruptionReports.ROUTE_ID})",
        "CREATE INDEX IF NOT EXISTS idx_gtfs_stops_name ON ${GtfsStops.TABLE}(${GtfsStops.STOP_NAME})",
        "CREATE INDEX IF NOT EXISTS idx_gtfs_stops_agency ON ${GtfsStops.TABLE}(${GtfsStops.AGENCY_ID})",
        "CREATE INDEX IF NOT EXISTS idx_gtfs_routes_agency ON ${GtfsRoutes.TABLE}(${GtfsRoutes.AGENCY_ID})",
        "CREATE INDEX IF NOT EXISTS idx_gtfs_trips_route ON ${GtfsTrips.TABLE}(${GtfsTrips.ROUTE_ID})",
        "CREATE INDEX IF NOT EXISTS idx_gtfs_stop_times_trip_sequence ON ${GtfsStopTimes.TABLE}(${GtfsStopTimes.TRIP_ID}, ${GtfsStopTimes.STOP_SEQUENCE})",
        "CREATE INDEX IF NOT EXISTS idx_gtfs_stop_times_stop ON ${GtfsStopTimes.TABLE}(${GtfsStopTimes.STOP_ID})",
        "CREATE INDEX IF NOT EXISTS idx_route_cache_lookup ON ${RouteCache.TABLE}(${RouteCache.ORIGIN_LAT}, ${RouteCache.ORIGIN_LON}, ${RouteCache.DESTINATION_LAT}, ${RouteCache.DESTINATION_LON}, ${RouteCache.MODE}, ${RouteCache.PRIORITY})"
    )

    val dropTableStatements = listOf(
        RouteCache.TABLE,
        DisruptionReports.TABLE,
        RouteHistory.TABLE,
        SearchHistory.TABLE,
        SavedTrips.TABLE,
        GtfsStopTimes.TABLE,
        GtfsTrips.TABLE,
        GtfsRoutes.TABLE,
        GtfsStops.TABLE,
        UserProfiles.TABLE,
        Users.TABLE
    ).map { "DROP TABLE IF EXISTS $it" }
}
