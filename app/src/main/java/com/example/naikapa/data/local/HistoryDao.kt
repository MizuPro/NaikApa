package com.example.naikapa.data.local

import android.content.ContentValues
import android.database.Cursor
import com.example.naikapa.data.model.RouteHistory
import com.example.naikapa.data.model.SearchHistory

class HistoryDao(private val dbHelper: NaikApaDatabaseHelper) {
    fun insertSearchHistory(history: SearchHistory): Long {
        val values = ContentValues().apply {
            put(NaikApaDbContract.SearchHistory.ID_USER, history.idUser)
            put(NaikApaDbContract.SearchHistory.KEYWORD, history.keyword)
            put(NaikApaDbContract.SearchHistory.SELECTED_NAME, history.selectedName)
            put(NaikApaDbContract.SearchHistory.SELECTED_ADDRESS, history.selectedAddress)
            put(NaikApaDbContract.SearchHistory.SELECTED_LAT, history.selectedLat)
            put(NaikApaDbContract.SearchHistory.SELECTED_LON, history.selectedLon)
            put(NaikApaDbContract.SearchHistory.SEARCHED_AT, history.searchedAt)
        }
        return dbHelper.writableDatabase.insert(NaikApaDbContract.SearchHistory.TABLE, null, values)
    }

    fun getSearchHistory(idUser: Long): List<SearchHistory> {
        dbHelper.readableDatabase.query(
            NaikApaDbContract.SearchHistory.TABLE,
            null,
            "${NaikApaDbContract.SearchHistory.ID_USER} = ?",
            arrayOf(idUser.toString()),
            null,
            null,
            "${NaikApaDbContract.SearchHistory.SEARCHED_AT} DESC"
        ).use { cursor ->
            return cursor.toSearchHistoryList()
        }
    }

    fun deleteSearchHistory(idSearch: Long): Int {
        return dbHelper.writableDatabase.delete(
            NaikApaDbContract.SearchHistory.TABLE,
            "${NaikApaDbContract.SearchHistory.ID} = ?",
            arrayOf(idSearch.toString())
        )
    }

    fun clearSearchHistory(idUser: Long): Int {
        return dbHelper.writableDatabase.delete(
            NaikApaDbContract.SearchHistory.TABLE,
            "${NaikApaDbContract.SearchHistory.ID_USER} = ?",
            arrayOf(idUser.toString())
        )
    }

    fun insertRouteHistory(history: RouteHistory): Long {
        val values = ContentValues().apply {
            put(NaikApaDbContract.RouteHistory.ID_USER, history.idUser)
            put(NaikApaDbContract.RouteHistory.ORIGIN_NAME, history.originName)
            put(NaikApaDbContract.RouteHistory.DESTINATION_NAME, history.destinationName)
            put(NaikApaDbContract.RouteHistory.MODE, history.mode)
            put(NaikApaDbContract.RouteHistory.PRIORITY, history.priority)
            put(NaikApaDbContract.RouteHistory.RECOMMENDATION_SUMMARY, history.recommendationSummary)
            put(NaikApaDbContract.RouteHistory.SCORE, history.score)
            put(NaikApaDbContract.RouteHistory.ESTIMATED_TIME, history.estimatedTime)
            put(NaikApaDbContract.RouteHistory.ESTIMATED_COST, history.estimatedCost)
            put(NaikApaDbContract.RouteHistory.ESTIMATED_BBM, history.estimatedBbm)
            put(NaikApaDbContract.RouteHistory.WALKING_DISTANCE, history.walkingDistance)
            put(NaikApaDbContract.RouteHistory.TRANSIT_COUNT, history.transitCount)
            put(NaikApaDbContract.RouteHistory.CREATED_AT, history.createdAt)
        }
        return dbHelper.writableDatabase.insert(NaikApaDbContract.RouteHistory.TABLE, null, values)
    }

    fun getRouteHistory(idUser: Long): List<RouteHistory> {
        dbHelper.readableDatabase.query(
            NaikApaDbContract.RouteHistory.TABLE,
            null,
            "${NaikApaDbContract.RouteHistory.ID_USER} = ?",
            arrayOf(idUser.toString()),
            null,
            null,
            "${NaikApaDbContract.RouteHistory.CREATED_AT} DESC"
        ).use { cursor ->
            return cursor.toRouteHistoryList()
        }
    }

    fun deleteRouteHistory(idHistory: Long): Int {
        return dbHelper.writableDatabase.delete(
            NaikApaDbContract.RouteHistory.TABLE,
            "${NaikApaDbContract.RouteHistory.ID} = ?",
            arrayOf(idHistory.toString())
        )
    }

    fun clearRouteHistory(idUser: Long): Int {
        return dbHelper.writableDatabase.delete(
            NaikApaDbContract.RouteHistory.TABLE,
            "${NaikApaDbContract.RouteHistory.ID_USER} = ?",
            arrayOf(idUser.toString())
        )
    }

    private fun Cursor.toSearchHistoryList(): List<SearchHistory> {
        val result = mutableListOf<SearchHistory>()
        while (moveToNext()) {
            result.add(
                SearchHistory(
                    idSearch = getLong(getColumnIndexOrThrow(NaikApaDbContract.SearchHistory.ID)),
                    idUser = getLong(getColumnIndexOrThrow(NaikApaDbContract.SearchHistory.ID_USER)),
                    keyword = getString(getColumnIndexOrThrow(NaikApaDbContract.SearchHistory.KEYWORD)),
                    selectedName = getString(getColumnIndexOrThrow(NaikApaDbContract.SearchHistory.SELECTED_NAME)),
                    selectedAddress = getNullableString(NaikApaDbContract.SearchHistory.SELECTED_ADDRESS),
                    selectedLat = getDouble(getColumnIndexOrThrow(NaikApaDbContract.SearchHistory.SELECTED_LAT)),
                    selectedLon = getDouble(getColumnIndexOrThrow(NaikApaDbContract.SearchHistory.SELECTED_LON)),
                    searchedAt = getLong(getColumnIndexOrThrow(NaikApaDbContract.SearchHistory.SEARCHED_AT))
                )
            )
        }
        return result
    }

    private fun Cursor.toRouteHistoryList(): List<RouteHistory> {
        val result = mutableListOf<RouteHistory>()
        while (moveToNext()) {
            result.add(
                RouteHistory(
                    idHistory = getLong(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.ID)),
                    idUser = getLong(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.ID_USER)),
                    originName = getString(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.ORIGIN_NAME)),
                    destinationName = getString(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.DESTINATION_NAME)),
                    mode = getString(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.MODE)),
                    priority = getString(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.PRIORITY)),
                    recommendationSummary = getString(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.RECOMMENDATION_SUMMARY)),
                    score = getInt(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.SCORE)),
                    estimatedTime = getInt(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.ESTIMATED_TIME)),
                    estimatedCost = getInt(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.ESTIMATED_COST)),
                    estimatedBbm = getInt(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.ESTIMATED_BBM)),
                    walkingDistance = getDouble(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.WALKING_DISTANCE)),
                    transitCount = getInt(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.TRANSIT_COUNT)),
                    createdAt = getLong(getColumnIndexOrThrow(NaikApaDbContract.RouteHistory.CREATED_AT))
                )
            )
        }
        return result
    }
}

private fun Cursor.getNullableString(columnName: String): String? {
    val index = getColumnIndexOrThrow(columnName)
    return if (isNull(index)) null else getString(index)
}
