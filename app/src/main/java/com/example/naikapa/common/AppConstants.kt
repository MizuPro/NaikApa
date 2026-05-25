package com.example.naikapa.common

object AppConstants {
    const val DATABASE_NAME = "naikapa.db"
    const val DATABASE_VERSION = 2
    const val WALKING_SPEED_KMH = 4.0
    const val WALKING_TRANSFER_RADIUS_M = 350
    const val DISRUPTION_ACTIVE_DURATION_HOURS = 1
    const val MAX_NEARBY_STOPS = 5
    const val FUEL_COST_PER_LITER = 10000
    const val MOTOR_CONSUMPTION_KM_PER_LITER = 40.0
    const val CAR_CONSUMPTION_KM_PER_LITER = 12.0
    const val TOMTOM_SEARCH_LIMIT = 8
    const val TOMTOM_MAX_ALTERNATIVES = 2
    const val TOMTOM_BASE_URL = "https://api.tomtom.com/"
    const val TOMTOM_COUNTRY_SET_ID = "ID"
    const val TOMTOM_TYPEAHEAD = true
    const val TOMTOM_MIN_QUERY_LENGTH = 3
    const val TOMTOM_SEARCH_DEBOUNCE_MS = 300L
    const val TOMTOM_API_KEY_PLACEHOLDER = "isi_api_key_kamu_di_sini"
    const val TOMTOM_ROUTING_ROUTE_TYPE_FASTEST = "fastest"
    const val TOMTOM_ROUTING_TRAVEL_MODE_MOTORCYCLE = "motorcycle"
    const val TOMTOM_ROUTING_TRAVEL_MODE_CAR = "car"
    const val TOMTOM_ROUTING_TRAVEL_MODE_PEDESTRIAN = "pedestrian"
    const val TOMTOM_ROUTING_AVOID_TOLL_ROADS = "tollRoads"
    const val MAP_USER_AGENT = "NaikApa/1.0"
    const val MAP_DEFAULT_LAT = -6.2088
    const val MAP_DEFAULT_LON = 106.8456
    const val MAP_DEFAULT_ZOOM = 12.5
    const val MAP_LOCATION_ZOOM = 15.0
    const val MAP_TILE_POSITRON = "CartoDB_Positron"
    const val MAP_TILE_DARK_MATTER = "CartoDB_DarkMatter"

    // GTFS Local Stop Search
    const val GTFS_SEARCH_LIMIT = 10
    const val GTFS_MIN_QUERY_LENGTH = 2
    const val GTFS_SOURCE = "gtfs"

    // Transit graph
    const val TRANSIT_GRAPH_LOG_TAG = "TransitGraph"
    const val WALKING_ROUTE_ID = "WALKING"
    const val WALKING_ROUTE_NAME = "Jalan Kaki"
    const val ROUTING_TRANSFER_PENALTY_SECONDS = 300
    const val ROUTING_AGENCY_ENTRY_COST = 3500
    const val ROUTING_TRANSFER_COST = 500
    const val ROUTING_DURATION_TIEBREAKER = 0.00001
    const val ROUTING_WALKING_DISTANCE_WEIGHT = 10.0

    // Combined route
    const val COMBINED_ROUTE_LOG_TAG = "CombinedRoute"
    const val COMBINED_ROUTE_STOP_CANDIDATE_LIMIT = 3
    const val COMBINED_ROUTE_MAX_COMBINATIONS = 9
    const val COMBINED_ROUTE_ORIGIN_RADIUS_M = 8000.0
    const val COMBINED_ROUTE_DESTINATION_RADIUS_M = 2500.0
    const val WALKING_SECONDS_PER_METER = 0.9

    // Route cache
    const val ROUTE_CACHE_TTL_MILLIS = 10 * 60 * 1000L
    const val ROUTE_CACHE_CLEANUP_AGE_MILLIS = 24 * 60 * 60 * 1000L
    const val ROUTE_CACHE_COORDINATE_PRECISION = 5

    // ── Recommendation scoring ────────────────────────────────────────────────
    // Bobot per dimensi untuk setiap prioritas (total = 100)
    // Format: SCORE_W_<PRIORITAS>_<DIMENSI>
    const val SCORE_W_FASTEST_TIME      = 45
    const val SCORE_W_FASTEST_COST      = 15
    const val SCORE_W_FASTEST_WALKING   = 15
    const val SCORE_W_FASTEST_TRANSIT   = 15
    const val SCORE_W_FASTEST_DISRUPTION = 10

    const val SCORE_W_CHEAPEST_COST     = 45
    const val SCORE_W_CHEAPEST_TIME     = 20
    const val SCORE_W_CHEAPEST_WALKING  = 15
    const val SCORE_W_CHEAPEST_TRANSIT  = 10
    const val SCORE_W_CHEAPEST_DISRUPTION = 10

    const val SCORE_W_MINWALK_WALKING   = 45
    const val SCORE_W_MINWALK_TIME      = 20
    const val SCORE_W_MINWALK_COST      = 15
    const val SCORE_W_MINWALK_TRANSIT   = 10
    const val SCORE_W_MINWALK_DISRUPTION = 10

    const val SCORE_W_MINTRANSIT_TRANSIT  = 45
    const val SCORE_W_MINTRANSIT_TIME     = 20
    const val SCORE_W_MINTRANSIT_COST     = 15
    const val SCORE_W_MINTRANSIT_WALKING  = 10
    const val SCORE_W_MINTRANSIT_DISRUPTION = 10

    // Penalti gangguan aktif (dikurangi dari skor)
    const val SCORE_DISRUPTION_PENALTY  = 15

    // Referensi normalisasi (nilai "buruk" = skor 0 untuk dimensi tersebut)
    const val SCORE_REF_MAX_DURATION_SEC   = 7200   // 2 jam
    const val SCORE_REF_MAX_COST_IDR       = 50000  // Rp 50.000
    const val SCORE_REF_MAX_WALKING_M      = 3000   // 3 km
    const val SCORE_REF_MAX_TRANSIT_COUNT  = 5      // 5 kali transit

    // ── Disruption Report ─────────────────────────────────────────────────────
    const val DISRUPTION_ACTIVE_DURATION_MILLIS = DISRUPTION_ACTIVE_DURATION_HOURS * 60 * 60 * 1000L

    // Kategori laporan gangguan
    const val DISRUPTION_CATEGORY_CROWDED       = "Penuh / Antrean Panjang"
    const val DISRUPTION_CATEGORY_DELAY         = "Keterlambatan"
    const val DISRUPTION_CATEGORY_BREAKDOWN     = "Kerusakan / Gangguan Teknis"
    const val DISRUPTION_CATEGORY_ACCIDENT      = "Kecelakaan / Insiden"
    const val DISRUPTION_CATEGORY_CLOSURE       = "Penutupan Jalur / Halte"
    const val DISRUPTION_CATEGORY_OTHER         = "Lainnya"

    val DISRUPTION_CATEGORIES = listOf(
        DISRUPTION_CATEGORY_CROWDED,
        DISRUPTION_CATEGORY_DELAY,
        DISRUPTION_CATEGORY_BREAKDOWN,
        DISRUPTION_CATEGORY_ACCIDENT,
        DISRUPTION_CATEGORY_CLOSURE,
        DISRUPTION_CATEGORY_OTHER
    )

    // Foto laporan
    const val DISRUPTION_PHOTO_DIR              = "disruption_photos"
    const val DISRUPTION_PHOTO_PREFIX           = "report_"
    const val DISRUPTION_PHOTO_EXTENSION        = ".jpg"

    // Validasi deskripsi
    const val DISRUPTION_DESCRIPTION_MIN_LENGTH = 10
    const val DISRUPTION_DESCRIPTION_MAX_LENGTH = 500

    // Transit fares
    const val FARE_TRANSJAKARTA_FLAT = 3500
    const val FARE_KRL_BASE = 3000
    const val FARE_KRL_BASE_DISTANCE_KM = 25.0
    const val FARE_KRL_INCREMENT_DISTANCE_KM = 10.0
    const val FARE_KRL_INCREMENT = 1000
    const val FARE_MRT_BASE = 3000
    const val FARE_MRT_PER_STOP = 1000
    const val FARE_MRT_MAX = 14000
    const val FARE_LRTJ_FLAT = 5000
    const val FARE_LRTJB_BASE = 5000
    const val FARE_LRTJB_BASE_DISTANCE_KM = 1.0
    const val FARE_LRTJB_PER_KM = 700
    const val FARE_LRTJB_MAX = 20000
}
