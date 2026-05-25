package com.example.naikapa.data.local

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.naikapa.data.model.RouteCache

class RouteCacheDao(private val dbHelper: NaikApaDatabaseHelper) {
    fun insert(cache: RouteCache): Long {
        val values = ContentValues().apply {
            put(NaikApaDbContract.RouteCache.CACHE_KEY, cache.cacheKey)
            put(NaikApaDbContract.RouteCache.ORIGIN_LAT, cache.originLat)
            put(NaikApaDbContract.RouteCache.ORIGIN_LON, cache.originLon)
            put(NaikApaDbContract.RouteCache.DESTINATION_LAT, cache.destinationLat)
            put(NaikApaDbContract.RouteCache.DESTINATION_LON, cache.destinationLon)
            put(NaikApaDbContract.RouteCache.MODE, cache.mode)
            put(NaikApaDbContract.RouteCache.PRIORITY, cache.priority)
            put(NaikApaDbContract.RouteCache.RESULT_JSON, cache.resultJson)
            put(NaikApaDbContract.RouteCache.CREATED_AT, cache.createdAt)
        }
        return dbHelper.writableDatabase.insert(NaikApaDbContract.RouteCache.TABLE, null, values)
    }

    fun insertOrReplace(cache: RouteCache): Long {
        val values = ContentValues().apply {
            put(NaikApaDbContract.RouteCache.CACHE_KEY, cache.cacheKey)
            put(NaikApaDbContract.RouteCache.ORIGIN_LAT, cache.originLat)
            put(NaikApaDbContract.RouteCache.ORIGIN_LON, cache.originLon)
            put(NaikApaDbContract.RouteCache.DESTINATION_LAT, cache.destinationLat)
            put(NaikApaDbContract.RouteCache.DESTINATION_LON, cache.destinationLon)
            put(NaikApaDbContract.RouteCache.MODE, cache.mode)
            put(NaikApaDbContract.RouteCache.PRIORITY, cache.priority)
            put(NaikApaDbContract.RouteCache.RESULT_JSON, cache.resultJson)
            put(NaikApaDbContract.RouteCache.CREATED_AT, cache.createdAt)
        }
        return dbHelper.writableDatabase.insertWithOnConflict(
            NaikApaDbContract.RouteCache.TABLE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun findByKey(cacheKey: String): RouteCache? {
        dbHelper.readableDatabase.query(
            NaikApaDbContract.RouteCache.TABLE,
            null,
            "${NaikApaDbContract.RouteCache.CACHE_KEY} = ?",
            arrayOf(cacheKey),
            null,
            null,
            "${NaikApaDbContract.RouteCache.CREATED_AT} DESC",
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toRouteCache() else null
        }
    }

    fun find(
        originLat: Double,
        originLon: Double,
        destinationLat: Double,
        destinationLon: Double,
        mode: String,
        priority: String
    ): RouteCache? {
        dbHelper.readableDatabase.query(
            NaikApaDbContract.RouteCache.TABLE,
            null,
            """
            ${NaikApaDbContract.RouteCache.ORIGIN_LAT} = ? AND
            ${NaikApaDbContract.RouteCache.ORIGIN_LON} = ? AND
            ${NaikApaDbContract.RouteCache.DESTINATION_LAT} = ? AND
            ${NaikApaDbContract.RouteCache.DESTINATION_LON} = ? AND
            ${NaikApaDbContract.RouteCache.MODE} = ? AND
            ${NaikApaDbContract.RouteCache.PRIORITY} = ?
            """.trimIndent(),
            arrayOf(
                originLat.toString(),
                originLon.toString(),
                destinationLat.toString(),
                destinationLon.toString(),
                mode,
                priority
            ),
            null,
            null,
            "${NaikApaDbContract.RouteCache.CREATED_AT} DESC",
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toRouteCache() else null
        }
    }

    fun clearOlderThan(cutoffMillis: Long): Int {
        return dbHelper.writableDatabase.delete(
            NaikApaDbContract.RouteCache.TABLE,
            "${NaikApaDbContract.RouteCache.CREATED_AT} < ?",
            arrayOf(cutoffMillis.toString())
        )
    }

    fun clearAll(): Int {
        return dbHelper.writableDatabase.delete(NaikApaDbContract.RouteCache.TABLE, null, null)
    }

    private fun Cursor.toRouteCache(): RouteCache = RouteCache(
        idCache = getLong(getColumnIndexOrThrow(NaikApaDbContract.RouteCache.ID)),
        cacheKey = getString(getColumnIndexOrThrow(NaikApaDbContract.RouteCache.CACHE_KEY)),
        originLat = getDouble(getColumnIndexOrThrow(NaikApaDbContract.RouteCache.ORIGIN_LAT)),
        originLon = getDouble(getColumnIndexOrThrow(NaikApaDbContract.RouteCache.ORIGIN_LON)),
        destinationLat = getDouble(getColumnIndexOrThrow(NaikApaDbContract.RouteCache.DESTINATION_LAT)),
        destinationLon = getDouble(getColumnIndexOrThrow(NaikApaDbContract.RouteCache.DESTINATION_LON)),
        mode = getString(getColumnIndexOrThrow(NaikApaDbContract.RouteCache.MODE)),
        priority = getString(getColumnIndexOrThrow(NaikApaDbContract.RouteCache.PRIORITY)),
        resultJson = getString(getColumnIndexOrThrow(NaikApaDbContract.RouteCache.RESULT_JSON)),
        createdAt = getLong(getColumnIndexOrThrow(NaikApaDbContract.RouteCache.CREATED_AT))
    )
}
