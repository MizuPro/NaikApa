package com.example.naikapa.data.model

data class GtfsStop(
    val stopId: String,
    val stopName: String,
    val stopLat: Double,
    val stopLon: Double,
    val agencyId: String,
    val stopType: String? = null
)

data class GtfsRoute(
    val routeId: String,
    val agencyId: String,
    val routeShortName: String?,
    val routeLongName: String?,
    val routeColor: String?,
    val routeTextColor: String?
)

data class GtfsTrip(
    val tripId: String,
    val routeId: String,
    val serviceId: String?,
    val directionId: Int?
)

data class GtfsStopTime(
    val id: Long = 0,
    val tripId: String,
    val arrivalTime: String,
    val departureTime: String,
    val stopId: String,
    val stopSequence: Int
)

data class GtfsAgencyCount(
    val agencyId: String,
    val totalStops: Int
)

data class GtfsTableCount(
    val tableName: String,
    val totalRows: Int
)
