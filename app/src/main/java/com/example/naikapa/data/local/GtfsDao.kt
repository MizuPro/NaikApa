package com.example.naikapa.data.local

import android.content.ContentValues
import android.database.Cursor
import com.example.naikapa.data.model.GtfsAdjacentStopConnection
import com.example.naikapa.data.model.GtfsAgencyCount
import com.example.naikapa.data.model.GtfsRoute
import com.example.naikapa.data.model.GtfsStop
import com.example.naikapa.data.model.GtfsStopTime
import com.example.naikapa.data.model.GtfsTableCount
import com.example.naikapa.data.model.GtfsTrip

class GtfsDao(private val dbHelper: NaikApaDatabaseHelper) {
    fun insertStop(stop: GtfsStop): Long {
        val values = ContentValues().apply {
            put(NaikApaDbContract.GtfsStops.STOP_ID, stop.stopId)
            put(NaikApaDbContract.GtfsStops.STOP_NAME, stop.stopName)
            put(NaikApaDbContract.GtfsStops.STOP_LAT, stop.stopLat)
            put(NaikApaDbContract.GtfsStops.STOP_LON, stop.stopLon)
            put(NaikApaDbContract.GtfsStops.AGENCY_ID, stop.agencyId)
            put(NaikApaDbContract.GtfsStops.STOP_TYPE, stop.stopType)
        }
        return dbHelper.writableDatabase.insertWithOnConflict(
            NaikApaDbContract.GtfsStops.TABLE,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun insertRoute(route: GtfsRoute): Long {
        val values = ContentValues().apply {
            put(NaikApaDbContract.GtfsRoutes.ROUTE_ID, route.routeId)
            put(NaikApaDbContract.GtfsRoutes.AGENCY_ID, route.agencyId)
            put(NaikApaDbContract.GtfsRoutes.ROUTE_SHORT_NAME, route.routeShortName)
            put(NaikApaDbContract.GtfsRoutes.ROUTE_LONG_NAME, route.routeLongName)
            put(NaikApaDbContract.GtfsRoutes.ROUTE_COLOR, route.routeColor)
            put(NaikApaDbContract.GtfsRoutes.ROUTE_TEXT_COLOR, route.routeTextColor)
        }
        return dbHelper.writableDatabase.insertWithOnConflict(
            NaikApaDbContract.GtfsRoutes.TABLE,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun insertTrip(trip: GtfsTrip): Long {
        val values = ContentValues().apply {
            put(NaikApaDbContract.GtfsTrips.TRIP_ID, trip.tripId)
            put(NaikApaDbContract.GtfsTrips.ROUTE_ID, trip.routeId)
            put(NaikApaDbContract.GtfsTrips.SERVICE_ID, trip.serviceId)
            if (trip.directionId == null) putNull(NaikApaDbContract.GtfsTrips.DIRECTION_ID) else put(
                NaikApaDbContract.GtfsTrips.DIRECTION_ID,
                trip.directionId
            )
        }
        return dbHelper.writableDatabase.insertWithOnConflict(
            NaikApaDbContract.GtfsTrips.TABLE,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun insertStopTime(stopTime: GtfsStopTime): Long {
        val values = ContentValues().apply {
            put(NaikApaDbContract.GtfsStopTimes.TRIP_ID, stopTime.tripId)
            put(NaikApaDbContract.GtfsStopTimes.ARRIVAL_TIME, stopTime.arrivalTime)
            put(NaikApaDbContract.GtfsStopTimes.DEPARTURE_TIME, stopTime.departureTime)
            put(NaikApaDbContract.GtfsStopTimes.STOP_ID, stopTime.stopId)
            put(NaikApaDbContract.GtfsStopTimes.STOP_SEQUENCE, stopTime.stopSequence)
        }
        return dbHelper.writableDatabase.insert(NaikApaDbContract.GtfsStopTimes.TABLE, null, values)
    }

    fun searchStops(keyword: String, agencyId: String? = null, limit: Int = 20): List<GtfsStop> {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return emptyList()
        val safeLimit = limit.coerceIn(1, 50)

        val selection: String
        val args: Array<String>
        if (agencyId.isNullOrBlank()) {
            selection = "${NaikApaDbContract.GtfsStops.STOP_NAME} LIKE ?"
            args = arrayOf("%$trimmed%")
        } else {
            selection = "${NaikApaDbContract.GtfsStops.STOP_NAME} LIKE ? AND ${NaikApaDbContract.GtfsStops.AGENCY_ID} = ?"
            args = arrayOf("%$trimmed%", agencyId.trim())
        }
        dbHelper.readableDatabase.query(
            NaikApaDbContract.GtfsStops.TABLE,
            null,
            selection,
            args,
            null,
            null,
            "${NaikApaDbContract.GtfsStops.STOP_NAME} ASC",
            safeLimit.toString()
        ).use { cursor ->
            return cursor.toStopList()
        }
    }

    fun getStop(stopId: String): GtfsStop? {
        dbHelper.readableDatabase.query(
            NaikApaDbContract.GtfsStops.TABLE,
            null,
            "${NaikApaDbContract.GtfsStops.STOP_ID} = ?",
            arrayOf(stopId),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toStop() else null
        }
    }

    fun getRoute(routeId: String): GtfsRoute? {
        dbHelper.readableDatabase.query(
            NaikApaDbContract.GtfsRoutes.TABLE,
            null,
            "${NaikApaDbContract.GtfsRoutes.ROUTE_ID} = ?",
            arrayOf(routeId),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toRoute() else null
        }
    }

    fun getAllStopsForGraph(): List<GtfsStop> {
        dbHelper.readableDatabase.query(
            NaikApaDbContract.GtfsStops.TABLE,
            null,
            null,
            null,
            null,
            null,
            "${NaikApaDbContract.GtfsStops.STOP_ID} ASC"
        ).use { cursor ->
            return cursor.toStopList()
        }
    }

    fun getStopsForNearestSearch(limit: Int? = null): List<GtfsStop> {
        val safeLimit = limit?.coerceAtLeast(1)
        val limitClause = safeLimit?.toString()
        dbHelper.readableDatabase.query(
            NaikApaDbContract.GtfsStops.TABLE,
            null,
            "${NaikApaDbContract.GtfsStops.STOP_LAT} BETWEEN -90 AND 90 AND " +
                "${NaikApaDbContract.GtfsStops.STOP_LON} BETWEEN -180 AND 180",
            null,
            null,
            null,
            "${NaikApaDbContract.GtfsStops.STOP_ID} ASC",
            limitClause
        ).use { cursor ->
            return cursor.toStopList()
        }
    }

    fun getAdjacentStopConnections(limit: Int? = null): List<GtfsAdjacentStopConnection> {
        val safeLimit = limit?.coerceAtLeast(1)
        val limitClause = safeLimit?.let { " LIMIT $it" }.orEmpty()
        dbHelper.readableDatabase.rawQuery(
            """
            SELECT
                from_stop.${NaikApaDbContract.GtfsStops.STOP_ID} AS from_stop_id,
                from_stop.${NaikApaDbContract.GtfsStops.STOP_NAME} AS from_stop_name,
                from_stop.${NaikApaDbContract.GtfsStops.STOP_LAT} AS from_stop_lat,
                from_stop.${NaikApaDbContract.GtfsStops.STOP_LON} AS from_stop_lon,
                from_stop.${NaikApaDbContract.GtfsStops.AGENCY_ID} AS from_agency_id,
                from_stop.${NaikApaDbContract.GtfsStops.STOP_TYPE} AS from_stop_type,
                to_stop.${NaikApaDbContract.GtfsStops.STOP_ID} AS to_stop_id,
                to_stop.${NaikApaDbContract.GtfsStops.STOP_NAME} AS to_stop_name,
                to_stop.${NaikApaDbContract.GtfsStops.STOP_LAT} AS to_stop_lat,
                to_stop.${NaikApaDbContract.GtfsStops.STOP_LON} AS to_stop_lon,
                to_stop.${NaikApaDbContract.GtfsStops.AGENCY_ID} AS to_agency_id,
                to_stop.${NaikApaDbContract.GtfsStops.STOP_TYPE} AS to_stop_type,
                st1.${NaikApaDbContract.GtfsStopTimes.TRIP_ID} AS trip_id,
                routes.${NaikApaDbContract.GtfsRoutes.ROUTE_ID} AS route_id,
                routes.${NaikApaDbContract.GtfsRoutes.ROUTE_SHORT_NAME} AS route_short_name,
                routes.${NaikApaDbContract.GtfsRoutes.ROUTE_LONG_NAME} AS route_long_name,
                routes.${NaikApaDbContract.GtfsRoutes.ROUTE_COLOR} AS route_color,
                routes.${NaikApaDbContract.GtfsRoutes.ROUTE_TEXT_COLOR} AS route_text_color,
                routes.${NaikApaDbContract.GtfsRoutes.AGENCY_ID} AS agency_id,
                st1.${NaikApaDbContract.GtfsStopTimes.DEPARTURE_TIME} AS departure_time,
                st2.${NaikApaDbContract.GtfsStopTimes.ARRIVAL_TIME} AS arrival_time
            FROM ${NaikApaDbContract.GtfsStopTimes.TABLE} st1
            JOIN ${NaikApaDbContract.GtfsStopTimes.TABLE} st2
                ON st1.${NaikApaDbContract.GtfsStopTimes.TRIP_ID} = st2.${NaikApaDbContract.GtfsStopTimes.TRIP_ID}
                AND st2.${NaikApaDbContract.GtfsStopTimes.STOP_SEQUENCE} = st1.${NaikApaDbContract.GtfsStopTimes.STOP_SEQUENCE} + 1
            JOIN ${NaikApaDbContract.GtfsTrips.TABLE} trips
                ON st1.${NaikApaDbContract.GtfsStopTimes.TRIP_ID} = trips.${NaikApaDbContract.GtfsTrips.TRIP_ID}
            JOIN ${NaikApaDbContract.GtfsRoutes.TABLE} routes
                ON trips.${NaikApaDbContract.GtfsTrips.ROUTE_ID} = routes.${NaikApaDbContract.GtfsRoutes.ROUTE_ID}
            JOIN ${NaikApaDbContract.GtfsStops.TABLE} from_stop
                ON st1.${NaikApaDbContract.GtfsStopTimes.STOP_ID} = from_stop.${NaikApaDbContract.GtfsStops.STOP_ID}
            JOIN ${NaikApaDbContract.GtfsStops.TABLE} to_stop
                ON st2.${NaikApaDbContract.GtfsStopTimes.STOP_ID} = to_stop.${NaikApaDbContract.GtfsStops.STOP_ID}
            ORDER BY st1.${NaikApaDbContract.GtfsStopTimes.TRIP_ID} ASC,
                st1.${NaikApaDbContract.GtfsStopTimes.STOP_SEQUENCE} ASC
            $limitClause
            """.trimIndent(),
            null
        ).use { cursor ->
            val result = mutableListOf<GtfsAdjacentStopConnection>()
            while (cursor.moveToNext()) result.add(cursor.toAdjacentStopConnection())
            return result
        }
    }

    fun hasGtfsData(): Boolean = countRows(NaikApaDbContract.GtfsStops.TABLE) > 0 &&
        countRows(NaikApaDbContract.GtfsRoutes.TABLE) > 0 &&
        countRows(NaikApaDbContract.GtfsTrips.TABLE) > 0 &&
        countRows(NaikApaDbContract.GtfsStopTimes.TABLE) > 0

    fun getGtfsTableCounts(): List<GtfsTableCount> = listOf(
        NaikApaDbContract.GtfsStops.TABLE,
        NaikApaDbContract.GtfsRoutes.TABLE,
        NaikApaDbContract.GtfsTrips.TABLE,
        NaikApaDbContract.GtfsStopTimes.TABLE
    ).map { table -> GtfsTableCount(table, countRows(table)) }

    fun getStopCountsByAgency(): List<GtfsAgencyCount> {
        dbHelper.readableDatabase.rawQuery(
            """
            SELECT ${NaikApaDbContract.GtfsStops.AGENCY_ID}, COUNT(*)
            FROM ${NaikApaDbContract.GtfsStops.TABLE}
            GROUP BY ${NaikApaDbContract.GtfsStops.AGENCY_ID}
            ORDER BY ${NaikApaDbContract.GtfsStops.AGENCY_ID} ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val result = mutableListOf<GtfsAgencyCount>()
            while (cursor.moveToNext()) {
                result.add(
                    GtfsAgencyCount(
                        agencyId = cursor.getString(0),
                        totalStops = cursor.getInt(1)
                    )
                )
            }
            return result
        }
    }

    private fun countRows(tableName: String): Int {
        dbHelper.readableDatabase.rawQuery("SELECT COUNT(*) FROM $tableName", null).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    private fun Cursor.toStopList(): List<GtfsStop> {
        val result = mutableListOf<GtfsStop>()
        while (moveToNext()) result.add(toStop())
        return result
    }

    private fun Cursor.toStop(): GtfsStop = GtfsStop(
        stopId = getString(getColumnIndexOrThrow(NaikApaDbContract.GtfsStops.STOP_ID)),
        stopName = getString(getColumnIndexOrThrow(NaikApaDbContract.GtfsStops.STOP_NAME)),
        stopLat = getDouble(getColumnIndexOrThrow(NaikApaDbContract.GtfsStops.STOP_LAT)),
        stopLon = getDouble(getColumnIndexOrThrow(NaikApaDbContract.GtfsStops.STOP_LON)),
        agencyId = getString(getColumnIndexOrThrow(NaikApaDbContract.GtfsStops.AGENCY_ID)),
        stopType = getNullableString(NaikApaDbContract.GtfsStops.STOP_TYPE)
    )

    private fun Cursor.toRoute(): GtfsRoute = GtfsRoute(
        routeId = getString(getColumnIndexOrThrow(NaikApaDbContract.GtfsRoutes.ROUTE_ID)),
        agencyId = getString(getColumnIndexOrThrow(NaikApaDbContract.GtfsRoutes.AGENCY_ID)),
        routeShortName = getNullableString(NaikApaDbContract.GtfsRoutes.ROUTE_SHORT_NAME),
        routeLongName = getNullableString(NaikApaDbContract.GtfsRoutes.ROUTE_LONG_NAME),
        routeColor = getNullableString(NaikApaDbContract.GtfsRoutes.ROUTE_COLOR),
        routeTextColor = getNullableString(NaikApaDbContract.GtfsRoutes.ROUTE_TEXT_COLOR)
    )

    private fun Cursor.toAdjacentStopConnection(): GtfsAdjacentStopConnection = GtfsAdjacentStopConnection(
        fromStopId = getString(getColumnIndexOrThrow("from_stop_id")),
        fromStopName = getString(getColumnIndexOrThrow("from_stop_name")),
        fromStopLat = getDouble(getColumnIndexOrThrow("from_stop_lat")),
        fromStopLon = getDouble(getColumnIndexOrThrow("from_stop_lon")),
        fromAgencyId = getString(getColumnIndexOrThrow("from_agency_id")),
        fromStopType = getNullableString("from_stop_type"),
        toStopId = getString(getColumnIndexOrThrow("to_stop_id")),
        toStopName = getString(getColumnIndexOrThrow("to_stop_name")),
        toStopLat = getDouble(getColumnIndexOrThrow("to_stop_lat")),
        toStopLon = getDouble(getColumnIndexOrThrow("to_stop_lon")),
        toAgencyId = getString(getColumnIndexOrThrow("to_agency_id")),
        toStopType = getNullableString("to_stop_type"),
        tripId = getString(getColumnIndexOrThrow("trip_id")),
        routeId = getString(getColumnIndexOrThrow("route_id")),
        routeShortName = getNullableString("route_short_name"),
        routeLongName = getNullableString("route_long_name"),
        routeColor = getNullableString("route_color"),
        routeTextColor = getNullableString("route_text_color"),
        agencyId = getString(getColumnIndexOrThrow("agency_id")),
        departureTime = getString(getColumnIndexOrThrow("departure_time")),
        arrivalTime = getString(getColumnIndexOrThrow("arrival_time"))
    )
}

private fun Cursor.getNullableString(columnName: String): String? {
    val index = getColumnIndexOrThrow(columnName)
    return if (isNull(index)) null else getString(index)
}
