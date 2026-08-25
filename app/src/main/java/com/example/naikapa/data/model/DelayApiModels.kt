package com.example.naikapa.data.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

data class UploadUrlDataDto(
    @SerializedName("uploadUrl") val uploadUrl: String,
    @SerializedName("fileUrl") val fileUrl: String,
    @SerializedName("fileKey") val fileKey: String,
    @SerializedName("expiresInSeconds") val expiresInSeconds: Int
)

data class CreateDelayRequest(
    @SerializedName("id_user") val idUser: Long,
    @SerializedName("stop_id") val stopId: String? = null,
    @SerializedName("route_id") val routeId: String? = null,
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String,
    @SerializedName("photo_url") val photoUrl: String? = null,
    @SerializedName("impact_level") val impactLevel: Int = 1,
    @SerializedName("duration_minutes") val durationMinutes: Int = 60
)

data class DelayReportDto(
    @SerializedName("id_report") val idReport: Long,
    @SerializedName("id_user") val idUser: Long,
    @SerializedName("stop_id") val stopId: String?,
    @SerializedName("route_id") val routeId: String?,
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName("impact_level") val impactLevel: Int = 1,
    @SerializedName("status") val status: String = DisruptionReport.STATUS_ACTIVE,
    @SerializedName("created_at") val createdAtRaw: String?,
    @SerializedName("expired_at") val expiredAtRaw: String?,
    @SerializedName("remaining_seconds") val remainingSeconds: Long? = null
) {
    fun toDomainModel(): DisruptionReport {
        val createdMillis = parseIsoToMillis(createdAtRaw) ?: System.currentTimeMillis()
        val expiredMillis = parseIsoToMillis(expiredAtRaw)
            ?: (createdMillis + (remainingSeconds?.times(1000L) ?: DisruptionReport.ONE_HOUR_MILLIS))

        return DisruptionReport(
            idReport = idReport,
            idUser = idUser,
            stopId = stopId,
            routeId = routeId,
            category = category,
            description = description,
            photoPath = photoUrl, // Stores public Backblaze B2 photo URL
            impactLevel = impactLevel,
            status = status,
            createdAt = createdMillis,
            expiredAt = expiredMillis
        )
    }
}

data class DelayReportListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("data") val data: List<DelayReportDto>
)

data class RouteDelaySummaryDataDto(
    @SerializedName("route_id") val routeId: String,
    @SerializedName("is_delayed") val isDelayed: Boolean,
    @SerializedName("total_active_reports") val totalActiveReports: Int,
    @SerializedName("average_impact_level") val averageImpactLevel: Double,
    @SerializedName("category_breakdown") val categoryBreakdown: Map<String, Int>?,
    @SerializedName("latest_report") val latestReport: DelayReportDto?
)

private fun parseIsoToMillis(isoString: String?): Long? {
    if (isoString.isNullOrEmpty()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val cleanIso = isoString.take(19)
        sdf.parse(cleanIso)?.time
    } catch (_: Exception) {
        null
    }
}
