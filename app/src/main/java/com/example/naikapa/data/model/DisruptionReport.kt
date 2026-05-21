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
    /** Apakah laporan ini masih aktif (belum expired dan statusnya active). */
    fun isActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return status == STATUS_ACTIVE && expiredAt > nowMillis
    }

    /** Sisa waktu aktif dalam milidetik. Negatif jika sudah expired. */
    fun remainingMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        return expiredAt - nowMillis
    }

    /** Sisa waktu aktif dalam format "X j Y mnt" atau "Z mnt". */
    fun remainingTimeLabel(nowMillis: Long = System.currentTimeMillis()): String {
        val remaining = remainingMillis(nowMillis)
        if (remaining <= 0) return "Expired"
        val totalMinutes = (remaining / 60_000).toInt()
        return if (totalMinutes >= 60) {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (minutes > 0) "$hours j $minutes mnt" else "$hours j"
        } else {
            "$totalMinutes mnt"
        }
    }

    companion object {
        const val STATUS_ACTIVE   = "active"
        const val STATUS_RESOLVED = "resolved"
        const val ONE_HOUR_MILLIS = 60 * 60 * 1000L
    }
}
