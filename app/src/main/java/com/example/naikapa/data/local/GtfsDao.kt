package com.example.naikapa.data.local

import android.content.ContentValues
import android.database.Cursor
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
        val selection: String
        val args: Array<String>
        if (agencyId.isNullOrBlank()) {
            selection = "${NaikApaDbContract.GtfsStops.STOP_NAME} LIKE ?"
            args = arrayOf("%$keyword%")
        } else {
            selection = "${NaikApaDbContract.GtfsStops.STOP_NAME} LIKE ? AND ${NaikApaDbContract.GtfsStops.AGENCY_ID} = ?"
            args = arrayOf("%$keyword%", agencyId)
        }
        dbHelper.readableDatabase.query(
            NaikApaDbContract.GtfsStops.TABLE,
            null,
            selection,
            args,
            null,
            null,
            "${NaikApaDbContract.GtfsStops.STOP_NAME} ASC",
            limit.toString()
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
}

private fun Cursor.getNullableString(columnName: String): String? {
    val index = getColumnIndexOrThrow(columnName)
    return if (isNull(index)) null else getString(index)
}
