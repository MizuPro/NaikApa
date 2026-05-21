package com.example.naikapa.data.model

/**
 * Membungkus satu kandidat rute dari berbagai sumber (transit, kendaraan pribadi, gabungan).
 * Digunakan sebagai input RecommendationEngine sebelum diberi skor.
 */
sealed class RouteCandidate {

    abstract val metrics: RouteMetrics
    abstract val candidateLabel: String

    data class Transit(
        val result: TransitRouteResult
    ) : RouteCandidate() {
        override val metrics: RouteMetrics get() = result.metrics
        override val candidateLabel: String
            get() = buildLabel(result)

        private fun buildLabel(r: TransitRouteResult): String {
            val agencies = r.steps
                .filter { it.type == TransitEdgeType.TRANSIT }
                .map { it.agencyId }
                .distinct()
            return if (agencies.isEmpty()) "Transportasi Umum"
            else agencies.joinToString(" + ") { agencyIdToName(it) }
        }

        private fun agencyIdToName(id: String) = when (id) {
            "Tije"  -> "TransJakarta"
            "KAIC"  -> "KRL"
            "MRTJ"  -> "MRT"
            "LRTJ"  -> "LRT Jakarta"
            "LRTJB" -> "LRT Jabodebek"
            else    -> id
        }
    }

    data class PrivateVehicle(
        val result: PrivateVehicleRouteResult
    ) : RouteCandidate() {
        override val metrics: RouteMetrics
            get() = RouteMetrics(
                totalDurationSeconds = result.travelTimeSeconds,
                totalDistanceMeters = result.distanceMeters.toDouble(),
                walkingDistanceMeters = 0.0,
                transitCount = 0,
                estimatedFare = 0,
                estimatedBbm = result.estimatedBbm,
                estimatedTotalCost = result.estimatedTotalCost
            )
        override val candidateLabel: String
            get() = if (result.mode == PrivateVehicleMode.MOTOR) "Motor" else "Mobil"
    }

    data class Combined(
        val result: CombinedRouteResult
    ) : RouteCandidate() {
        override val metrics: RouteMetrics get() = result.metrics
        override val candidateLabel: String
            get() {
                val vehicle = if (result.privateVehicleMode == PrivateVehicleMode.MOTOR) "Motor" else "Mobil"
                return "$vehicle + Transit"
            }
    }
}

/**
 * Satu kandidat rute yang sudah diberi skor, alasan, dan info gangguan.
 */
data class ScoredRoute(
    val candidate: RouteCandidate,
    val score: Int,                     // 0–100
    val reason: String,                 // Alasan rekomendasi dalam Bahasa Indonesia
    val hasDisruptionWarning: Boolean = false,
    val disruptionWarningText: String? = null,
    val rankLabel: String = ""          // "Rekomendasi Utama", "Alternatif 1", dst.
)

/**
 * Hasil akhir dari RecommendationEngine: rekomendasi utama + daftar alternatif.
 */
data class RecommendationResult(
    val main: ScoredRoute,
    val alternatives: List<ScoredRoute>,  // Maks 2 alternatif
    val sortPreference: SortPreference
) {
    val all: List<ScoredRoute> get() = listOf(main) + alternatives
}
