package com.example.naikapa.data.local

import android.content.ContentValues
import android.database.Cursor
import com.example.naikapa.data.model.SavedTrip

class SavedTripDao(private val dbHelper: NaikApaDatabaseHelper) {
    fun insert(savedTrip: SavedTrip): Long {
        return dbHelper.writableDatabase.insert(
            NaikApaDbContract.SavedTrips.TABLE,
            null,
            savedTrip.toValues()
        )
    }

    fun getByUser(idUser: Long): List<SavedTrip> {
        dbHelper.readableDatabase.query(
            NaikApaDbContract.SavedTrips.TABLE,
            null,
            "${NaikApaDbContract.SavedTrips.ID_USER} = ?",
            arrayOf(idUser.toString()),
            null,
            null,
            "${NaikApaDbContract.SavedTrips.CREATED_AT} DESC"
        ).use { cursor ->
            return cursor.toSavedTripList()
        }
    }

    fun updateNameAndNote(idSaved: Long, namaPerjalanan: String, catatan: String?): Int {
        val values = ContentValues().apply {
            put(NaikApaDbContract.SavedTrips.NAMA_PERJALANAN, namaPerjalanan)
            put(NaikApaDbContract.SavedTrips.CATATAN, catatan)
        }
        return dbHelper.writableDatabase.update(
            NaikApaDbContract.SavedTrips.TABLE,
            values,
            "${NaikApaDbContract.SavedTrips.ID} = ?",
            arrayOf(idSaved.toString())
        )
    }

    fun delete(idSaved: Long): Int {
        return dbHelper.writableDatabase.delete(
            NaikApaDbContract.SavedTrips.TABLE,
            "${NaikApaDbContract.SavedTrips.ID} = ?",
            arrayOf(idSaved.toString())
        )
    }

    private fun SavedTrip.toValues(): ContentValues = ContentValues().apply {
        put(NaikApaDbContract.SavedTrips.ID_USER, idUser)
        put(NaikApaDbContract.SavedTrips.NAMA_PERJALANAN, namaPerjalanan)
        put(NaikApaDbContract.SavedTrips.ORIGIN_NAME, originName)
        put(NaikApaDbContract.SavedTrips.ORIGIN_LAT, originLat)
        put(NaikApaDbContract.SavedTrips.ORIGIN_LON, originLon)
        put(NaikApaDbContract.SavedTrips.DESTINATION_NAME, destinationName)
        put(NaikApaDbContract.SavedTrips.DESTINATION_LAT, destinationLat)
        put(NaikApaDbContract.SavedTrips.DESTINATION_LON, destinationLon)
        put(NaikApaDbContract.SavedTrips.MODE, mode)
        put(NaikApaDbContract.SavedTrips.PRIORITY, priority)
        put(NaikApaDbContract.SavedTrips.CATATAN, catatan)
        put(NaikApaDbContract.SavedTrips.CREATED_AT, createdAt)
    }

    private fun Cursor.toSavedTripList(): List<SavedTrip> {
        val result = mutableListOf<SavedTrip>()
        while (moveToNext()) {
            result.add(
                SavedTrip(
                    idSaved = getLong(getColumnIndexOrThrow(NaikApaDbContract.SavedTrips.ID)),
                    idUser = getLong(getColumnIndexOrThrow(NaikApaDbContract.SavedTrips.ID_USER)),
                    namaPerjalanan = getString(getColumnIndexOrThrow(NaikApaDbContract.SavedTrips.NAMA_PERJALANAN)),
                    originName = getString(getColumnIndexOrThrow(NaikApaDbContract.SavedTrips.ORIGIN_NAME)),
                    originLat = getDouble(getColumnIndexOrThrow(NaikApaDbContract.SavedTrips.ORIGIN_LAT)),
                    originLon = getDouble(getColumnIndexOrThrow(NaikApaDbContract.SavedTrips.ORIGIN_LON)),
                    destinationName = getString(getColumnIndexOrThrow(NaikApaDbContract.SavedTrips.DESTINATION_NAME)),
                    destinationLat = getDouble(getColumnIndexOrThrow(NaikApaDbContract.SavedTrips.DESTINATION_LAT)),
                    destinationLon = getDouble(getColumnIndexOrThrow(NaikApaDbContract.SavedTrips.DESTINATION_LON)),
                    mode = getString(getColumnIndexOrThrow(NaikApaDbContract.SavedTrips.MODE)),
                    priority = getString(getColumnIndexOrThrow(NaikApaDbContract.SavedTrips.PRIORITY)),
                    catatan = getNullableString(NaikApaDbContract.SavedTrips.CATATAN),
                    createdAt = getLong(getColumnIndexOrThrow(NaikApaDbContract.SavedTrips.CREATED_AT))
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
