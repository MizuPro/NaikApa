package com.example.naikapa.data.repository

import com.example.naikapa.data.local.DisruptionReportDao
import com.example.naikapa.data.model.CreateDelayRequest
import com.example.naikapa.data.model.DisruptionReport
import com.example.naikapa.data.model.RouteDelaySummaryDataDto
import com.example.naikapa.data.remote.DelayApi
import com.example.naikapa.data.remote.ImageUploadHelper
import com.example.naikapa.data.remote.RemoteClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DelayRepository(
    private val delayApi: DelayApi = RemoteClient.delayApi,
    private val disruptionReportDao: DisruptionReportDao
) {

    /**
     * Fetch active delay reports from cloud API and sync with local SQLite cache.
     * Fallbacks to local SQLite cache if offline.
     */
    suspend fun getActiveReports(
        routeId: String? = null,
        stopId: String? = null,
        category: String? = null
    ): List<DisruptionReport> = withContext(Dispatchers.IO) {
        try {
            val response = delayApi.getActiveReports(
                routeId = routeId,
                stopId = stopId,
                category = category,
                status = "active"
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val dtoList = response.body()?.data.orEmpty()
                val domainReports = dtoList.map { it.toDomainModel() }

                // Sync with local SQLite cache
                domainReports.forEach { report ->
                    try {
                        val existing = disruptionReportDao.getById(report.idReport)
                        if (existing == null) {
                            disruptionReportDao.insert(report)
                        } else {
                            disruptionReportDao.update(report)
                        }
                    } catch (_: Exception) {}
                }

                return@withContext domainReports
            }
        } catch (_: Exception) {
            // Offline fallback
        }

        // Return local SQLite active reports
        return@withContext disruptionReportDao.getActiveReports()
    }

    /**
     * Submit a new disruption report with optional photo upload to Backblaze B2.
     */
    suspend fun createReport(
        idUser: Long,
        stopId: String?,
        routeId: String?,
        category: String,
        description: String,
        photoFile: File? = null,
        impactLevel: Int = 1,
        durationMinutes: Int = 60
    ): Result<DisruptionReport> = withContext(Dispatchers.IO) {
        var publicPhotoUrl: String? = null

        // 1. Upload photo to Backblaze B2 if file provided
        if (photoFile != null && photoFile.exists() && photoFile.length() > 0L) {
            try {
                val mimeType = if (photoFile.name.endsWith(".png", ignoreCase = true)) "image/png" else "image/jpeg"
                val urlRes = delayApi.getUploadUrl(photoFile.name, mimeType)

                if (urlRes.isSuccessful && urlRes.body()?.success == true) {
                    val uploadData = urlRes.body()?.data
                    if (uploadData != null) {
                        val isUploaded = ImageUploadHelper.uploadPhotoToB2(uploadData.uploadUrl, photoFile, mimeType)
                        if (isUploaded) {
                            publicPhotoUrl = uploadData.fileUrl
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Submit report payload to backend cloud
        try {
            val req = CreateDelayRequest(
                idUser = idUser,
                stopId = stopId,
                routeId = routeId,
                category = category,
                description = description,
                photoUrl = publicPhotoUrl, // Hanya kirim URL cloud yang valid ke backend, jangan path lokal
                impactLevel = impactLevel,
                durationMinutes = durationMinutes
            )

            val apiRes = delayApi.createReport(req)
            if (apiRes.isSuccessful && apiRes.body()?.success == true && apiRes.body()?.data != null) {
                val createdReport = apiRes.body()!!.data!!.toDomainModel()
                disruptionReportDao.insert(createdReport)
                return@withContext Result.success(createdReport)
            }
        } catch (e: Exception) {
            // Offline fallback creation
            e.printStackTrace()
        }

        // Offline / Local save fallback
        val now = System.currentTimeMillis()
        val localReport = DisruptionReport(
            idReport = 0,
            idUser = idUser,
            stopId = stopId,
            routeId = routeId,
            category = category,
            description = description,
            photoPath = publicPhotoUrl ?: photoFile?.absolutePath,
            impactLevel = impactLevel,
            status = DisruptionReport.STATUS_ACTIVE,
            createdAt = now,
            expiredAt = now + (durationMinutes * 60 * 1000L)
        )

        val insertedId = disruptionReportDao.insert(localReport)
        return@withContext Result.success(localReport.copy(idReport = insertedId))
    }

    /**
     * Mark a disruption report as resolved.
     */
    suspend fun resolveReport(idReport: Long, idUser: Long): Boolean = withContext(Dispatchers.IO) {
        disruptionReportDao.markResolvedByUser(idReport, idUser)

        try {
            val apiRes = delayApi.resolveReport(idReport, mapOf("id_user" to idUser))
            apiRes.isSuccessful
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Delete a disruption report.
     */
    suspend fun deleteReport(idReport: Long, idUser: Long): Boolean = withContext(Dispatchers.IO) {
        disruptionReportDao.deleteByUser(idReport, idUser)

        try {
            val apiRes = delayApi.deleteReport(idReport, idUser)
            apiRes.isSuccessful
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Fetch route delay summary from cloud API.
     */
    suspend fun getRouteDelaySummary(routeId: String): RouteDelaySummaryDataDto? = withContext(Dispatchers.IO) {
        try {
            val res = delayApi.getRouteDelaySummary(routeId)
            if (res.isSuccessful && res.body()?.success == true) {
                return@withContext res.body()?.data
            }
        } catch (_: Exception) {}
        null
    }
}
