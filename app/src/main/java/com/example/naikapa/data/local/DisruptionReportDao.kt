package com.example.naikapa.data.local

import android.content.ContentValues
import android.database.Cursor
import com.example.naikapa.data.model.DisruptionReport

open class DisruptionReportDao(private val dbHelper: NaikApaDatabaseHelper? = null) {

    private val writableDb get() = checkNotNull(dbHelper) { "DatabaseHelper null" }.writableDatabase
    private val readableDb get() = checkNotNull(dbHelper) { "DatabaseHelper null" }.readableDatabase

    open fun insert(report: DisruptionReport): Long {
        return writableDb.insert(
            NaikApaDbContract.DisruptionReports.TABLE,
            null,
            report.toValues()
        )
    }

    open fun getById(idReport: Long): DisruptionReport? {
        readableDb.query(
            NaikApaDbContract.DisruptionReports.TABLE,
            null,
            "${NaikApaDbContract.DisruptionReports.ID} = ?",
            arrayOf(idReport.toString()),
            null, null, null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toDisruptionReport() else null
        }
    }

    open fun getActiveReports(nowMillis: Long = System.currentTimeMillis()): List<DisruptionReport> {
        readableDb.query(
            NaikApaDbContract.DisruptionReports.TABLE,
            null,
            "${NaikApaDbContract.DisruptionReports.STATUS} = ? AND ${NaikApaDbContract.DisruptionReports.EXPIRED_AT} > ?",
            arrayOf(DisruptionReport.STATUS_ACTIVE, nowMillis.toString()),
            null,
            null,
            "${NaikApaDbContract.DisruptionReports.CREATED_AT} DESC"
        ).use { cursor ->
            return cursor.toDisruptionReportList()
        }
    }

    fun getByUser(idUser: Long): List<DisruptionReport> {
        readableDb.query(
            NaikApaDbContract.DisruptionReports.TABLE,
            null,
            "${NaikApaDbContract.DisruptionReports.ID_USER} = ?",
            arrayOf(idUser.toString()),
            null,
            null,
            "${NaikApaDbContract.DisruptionReports.CREATED_AT} DESC"
        ).use { cursor ->
            return cursor.toDisruptionReportList()
        }
    }

    fun getActiveByUser(idUser: Long, nowMillis: Long = System.currentTimeMillis()): List<DisruptionReport> {
        readableDb.query(
            NaikApaDbContract.DisruptionReports.TABLE,
            null,
            "${NaikApaDbContract.DisruptionReports.ID_USER} = ? AND " +
            "${NaikApaDbContract.DisruptionReports.STATUS} = ? AND " +
            "${NaikApaDbContract.DisruptionReports.EXPIRED_AT} > ?",
            arrayOf(idUser.toString(), DisruptionReport.STATUS_ACTIVE, nowMillis.toString()),
            null,
            null,
            "${NaikApaDbContract.DisruptionReports.CREATED_AT} DESC"
        ).use { cursor ->
            return cursor.toDisruptionReportList()
        }
    }

    /** Update laporan — hanya jika idReport dan idUser cocok (ownership check). */
    fun updateByUser(report: DisruptionReport): Int {
        return writableDb.update(
            NaikApaDbContract.DisruptionReports.TABLE,
            report.toValues(),
            "${NaikApaDbContract.DisruptionReports.ID} = ? AND ${NaikApaDbContract.DisruptionReports.ID_USER} = ?",
            arrayOf(report.idReport.toString(), report.idUser.toString())
        )
    }

    open fun update(report: DisruptionReport): Int {
        return writableDb.update(
            NaikApaDbContract.DisruptionReports.TABLE,
            report.toValues(),
            "${NaikApaDbContract.DisruptionReports.ID} = ?",
            arrayOf(report.idReport.toString())
        )
    }

    fun markResolved(idReport: Long): Int {
        val values = ContentValues().apply {
            put(NaikApaDbContract.DisruptionReports.STATUS, DisruptionReport.STATUS_RESOLVED)
        }
        return writableDb.update(
            NaikApaDbContract.DisruptionReports.TABLE,
            values,
            "${NaikApaDbContract.DisruptionReports.ID} = ?",
            arrayOf(idReport.toString())
        )
    }

    /** Tandai resolved — hanya jika idReport dan idUser cocok (ownership check). */
    open fun markResolvedByUser(idReport: Long, idUser: Long): Int {
        val values = ContentValues().apply {
            put(NaikApaDbContract.DisruptionReports.STATUS, DisruptionReport.STATUS_RESOLVED)
        }
        return writableDb.update(
            NaikApaDbContract.DisruptionReports.TABLE,
            values,
            "${NaikApaDbContract.DisruptionReports.ID} = ? AND ${NaikApaDbContract.DisruptionReports.ID_USER} = ?",
            arrayOf(idReport.toString(), idUser.toString())
        )
    }

    open fun delete(idReport: Long): Int {
        return writableDb.delete(
            NaikApaDbContract.DisruptionReports.TABLE,
            "${NaikApaDbContract.DisruptionReports.ID} = ?",
            arrayOf(idReport.toString())
        )
    }

    /** Hapus laporan — hanya jika idReport dan idUser cocok (ownership check). */
    open fun deleteByUser(idReport: Long, idUser: Long): Int {
        return writableDb.delete(
            NaikApaDbContract.DisruptionReports.TABLE,
            "${NaikApaDbContract.DisruptionReports.ID} = ? AND ${NaikApaDbContract.DisruptionReports.ID_USER} = ?",
            arrayOf(idReport.toString(), idUser.toString())
        )
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun DisruptionReport.toValues(): ContentValues = ContentValues().apply {
        put(NaikApaDbContract.DisruptionReports.ID_USER, idUser)
        put(NaikApaDbContract.DisruptionReports.STOP_ID, stopId)
        put(NaikApaDbContract.DisruptionReports.ROUTE_ID, routeId)
        put(NaikApaDbContract.DisruptionReports.CATEGORY, category)
        put(NaikApaDbContract.DisruptionReports.DESCRIPTION, description)
        put(NaikApaDbContract.DisruptionReports.PHOTO_PATH, photoPath)
        put(NaikApaDbContract.DisruptionReports.IMPACT_LEVEL, impactLevel)
        put(NaikApaDbContract.DisruptionReports.STATUS, status)
        put(NaikApaDbContract.DisruptionReports.CREATED_AT, createdAt)
        put(NaikApaDbContract.DisruptionReports.EXPIRED_AT, expiredAt)
    }

    private fun Cursor.toDisruptionReport(): DisruptionReport {
        return DisruptionReport(
            idReport    = getLong(getColumnIndexOrThrow(NaikApaDbContract.DisruptionReports.ID)),
            idUser      = getLong(getColumnIndexOrThrow(NaikApaDbContract.DisruptionReports.ID_USER)),
            stopId      = getNullableString(NaikApaDbContract.DisruptionReports.STOP_ID),
            routeId     = getNullableString(NaikApaDbContract.DisruptionReports.ROUTE_ID),
            category    = getString(getColumnIndexOrThrow(NaikApaDbContract.DisruptionReports.CATEGORY)),
            description = getString(getColumnIndexOrThrow(NaikApaDbContract.DisruptionReports.DESCRIPTION)),
            photoPath   = getNullableString(NaikApaDbContract.DisruptionReports.PHOTO_PATH),
            impactLevel = getInt(getColumnIndexOrThrow(NaikApaDbContract.DisruptionReports.IMPACT_LEVEL)),
            status      = getString(getColumnIndexOrThrow(NaikApaDbContract.DisruptionReports.STATUS)),
            createdAt   = getLong(getColumnIndexOrThrow(NaikApaDbContract.DisruptionReports.CREATED_AT)),
            expiredAt   = getLong(getColumnIndexOrThrow(NaikApaDbContract.DisruptionReports.EXPIRED_AT))
        )
    }

    private fun Cursor.toDisruptionReportList(): List<DisruptionReport> {
        val result = mutableListOf<DisruptionReport>()
        while (moveToNext()) {
            result.add(toDisruptionReport())
        }
        return result
    }
}

private fun Cursor.getNullableString(columnName: String): String? {
    val index = getColumnIndexOrThrow(columnName)
    return if (isNull(index)) null else getString(index)
}
