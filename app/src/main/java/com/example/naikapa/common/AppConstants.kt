package com.example.naikapa.common

object AppConstants {
    const val DATABASE_NAME = "naikapa.db"
    const val DATABASE_VERSION = 1
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
    const val MAP_USER_AGENT = "NaikApa/1.0"
    const val MAP_DEFAULT_LAT = -6.2088
    const val MAP_DEFAULT_LON = 106.8456
    const val MAP_DEFAULT_ZOOM = 12.5
    const val MAP_LOCATION_ZOOM = 15.0
    const val MAP_TILE_POSITRON = "CartoDB_Positron"
    const val MAP_TILE_DARK_MATTER = "CartoDB_DarkMatter"
}
