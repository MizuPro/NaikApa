package com.example.naikapa.data.model

data class DisruptionReport(
    val idReport: Long = 0,
    val idUser: Long,
    val stopId: String?,
    val routeId: String?,
    val category: String,
    val description: String,
    val photoPath: String?,
    val impactLevel: Int = 1,
    val status: String = STATUS_ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val expiredAt: Long = createdAt + ONE_HOUR_MILLIS
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_RESOLVED = "resolved"
        private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L
    }
}
