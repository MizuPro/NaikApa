package com.example.naikapa.domain.recommendation

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.model.DisruptionReport
import com.example.naikapa.data.model.RouteCandidate
import com.example.naikapa.data.model.RouteMetrics
import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitEdgeType
import kotlin.math.roundToInt

/**
 * Menghitung skor kecocokan 0–100 untuk satu kandidat rute.
 *
 * Rumus dasar:
 *   skor = Σ (bobot_dimensi × nilai_dimensi_ternormalisasi)
 *   nilai_dimensi = 1.0 − (aktual / referensi_maksimum)  → semakin kecil aktual, semakin tinggi nilai
 *   Penalti gangguan dikurangi di akhir.
 */
class RecommendationScorer {

    /**
     * @param candidate  Kandidat rute yang akan diberi skor.
     * @param sortPreference  Prioritas user yang menentukan bobot tiap dimensi.
     * @param activeDisruptions  Daftar laporan gangguan aktif saat ini.
     * @return Skor 0–100 (sudah di-clamp).
     */
    fun score(
        candidate: RouteCandidate,
        sortPreference: SortPreference,
        activeDisruptions: List<DisruptionReport>
    ): Int {
        val m = candidate.metrics
        val weights = weightsFor(sortPreference)

        val timeScore    = normalizeInverse(m.totalDurationSeconds.toDouble(), AppConstants.SCORE_REF_MAX_DURATION_SEC.toDouble())
        val costScore    = normalizeInverse(m.estimatedTotalCost.toDouble(), AppConstants.SCORE_REF_MAX_COST_IDR.toDouble())
        val walkScore    = normalizeInverse(m.walkingDistanceMeters, AppConstants.SCORE_REF_MAX_WALKING_M.toDouble())
        val transitScore = normalizeInverse(m.transitCount.toDouble(), AppConstants.SCORE_REF_MAX_TRANSIT_COUNT.toDouble())

        val rawScore = (weights.time     * timeScore    +
                        weights.cost     * costScore    +
                        weights.walking  * walkScore    +
                        weights.transit  * transitScore).roundToInt()

        val disruptionPenalty = if (hasDisruption(candidate, activeDisruptions))
            AppConstants.SCORE_DISRUPTION_PENALTY else 0

        return (rawScore - disruptionPenalty).coerceIn(0, 100)
    }

    /**
     * Apakah kandidat ini terdampak gangguan aktif?
     * Cek berdasarkan stopId yang dilalui rute transit.
     */
    fun hasDisruption(
        candidate: RouteCandidate,
        activeDisruptions: List<DisruptionReport>
    ): Boolean {
        if (activeDisruptions.isEmpty()) return false
        val disruptedStops = activeDisruptions.mapNotNull { it.stopId }.toSet()
        val disruptedRoutes = activeDisruptions.mapNotNull { it.routeId }.toSet()

        return when (candidate) {
            is RouteCandidate.Transit -> {
                candidate.result.steps.any { step ->
                    step.fromStop.stopId in disruptedStops ||
                    step.toStop.stopId in disruptedStops ||
                    step.routeId in disruptedRoutes
                }
            }
            is RouteCandidate.Combined -> {
                val transitResult = candidate.result.transitResult
                transitResult?.steps?.any { step ->
                    step.fromStop.stopId in disruptedStops ||
                    step.toStop.stopId in disruptedStops ||
                    step.routeId in disruptedRoutes
                } ?: false
            }
            is RouteCandidate.PrivateVehicle -> false
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Normalisasi terbalik: nilai kecil → skor tinggi.
     * Hasil: 0.0 (aktual ≥ max) sampai 1.0 (aktual = 0).
     */
    private fun normalizeInverse(actual: Double, max: Double): Double {
        if (max <= 0) return 1.0
        return (1.0 - (actual / max)).coerceIn(0.0, 1.0)
    }

    private fun weightsFor(pref: SortPreference): DimensionWeights = when (pref) {
        SortPreference.FASTEST -> DimensionWeights(
            time    = AppConstants.SCORE_W_FASTEST_TIME,
            cost    = AppConstants.SCORE_W_FASTEST_COST,
            walking = AppConstants.SCORE_W_FASTEST_WALKING,
            transit = AppConstants.SCORE_W_FASTEST_TRANSIT
        )
        SortPreference.CHEAPEST -> DimensionWeights(
            time    = AppConstants.SCORE_W_CHEAPEST_TIME,
            cost    = AppConstants.SCORE_W_CHEAPEST_COST,
            walking = AppConstants.SCORE_W_CHEAPEST_WALKING,
            transit = AppConstants.SCORE_W_CHEAPEST_TRANSIT
        )
        SortPreference.MIN_WALKING -> DimensionWeights(
            time    = AppConstants.SCORE_W_MINWALK_TIME,
            cost    = AppConstants.SCORE_W_MINWALK_COST,
            walking = AppConstants.SCORE_W_MINWALK_WALKING,
            transit = AppConstants.SCORE_W_MINWALK_TRANSIT
        )
        SortPreference.FEWEST_TRANSFERS -> DimensionWeights(
            time    = AppConstants.SCORE_W_MINTRANSIT_TIME,
            cost    = AppConstants.SCORE_W_MINTRANSIT_COST,
            walking = AppConstants.SCORE_W_MINTRANSIT_WALKING,
            transit = AppConstants.SCORE_W_MINTRANSIT_TRANSIT
        )
    }

    private data class DimensionWeights(
        val time: Int,
        val cost: Int,
        val walking: Int,
        val transit: Int
    )
}
