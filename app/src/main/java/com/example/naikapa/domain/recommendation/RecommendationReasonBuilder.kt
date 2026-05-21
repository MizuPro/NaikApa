package com.example.naikapa.domain.recommendation

import com.example.naikapa.data.model.RouteCandidate
import com.example.naikapa.data.model.SortPreference

/**
 * Menghasilkan kalimat alasan rekomendasi dalam Bahasa Indonesia
 * berdasarkan prioritas user, kondisi rute, dan perbandingan antar kandidat.
 */
class RecommendationReasonBuilder {

    /**
     * @param candidate  Kandidat yang sedang diberi alasan.
     * @param allCandidates  Semua kandidat (untuk perbandingan relatif).
     * @param sortPreference  Prioritas user.
     * @param isMain  True jika ini rekomendasi utama.
     * @return Kalimat alasan yang bisa langsung ditampilkan ke user.
     */
    fun buildReason(
        candidate: RouteCandidate,
        allCandidates: List<RouteCandidate>,
        sortPreference: SortPreference,
        isMain: Boolean
    ): String {
        val m = candidate.metrics

        // Cek apakah kandidat ini terbaik di dimensi utama prioritas
        val isBestTime    = allCandidates.all { m.totalDurationSeconds <= it.metrics.totalDurationSeconds }
        val isBestCost    = allCandidates.all { m.estimatedTotalCost <= it.metrics.estimatedTotalCost }
        val isBestWalking = allCandidates.all { m.walkingDistanceMeters <= it.metrics.walkingDistanceMeters }
        val isBestTransit = allCandidates.all { m.transitCount <= it.metrics.transitCount }

        return when (sortPreference) {
            SortPreference.FASTEST -> buildFastestReason(candidate, isBestTime, isBestCost, isMain)
            SortPreference.CHEAPEST -> buildCheapestReason(candidate, isBestCost, isBestTime, isMain)
            SortPreference.MIN_WALKING -> buildMinWalkingReason(candidate, isBestWalking, isBestTime, isMain)
            SortPreference.FEWEST_TRANSFERS -> buildFewestTransfersReason(candidate, isBestTransit, isBestTime, isMain)
        }
    }

    /**
     * Teks peringatan gangguan untuk ditampilkan di bawah alasan.
     */
    fun buildDisruptionWarning(candidate: RouteCandidate): String {
        return when (candidate) {
            is RouteCandidate.Transit ->
                "⚠ Ada laporan gangguan aktif pada salah satu titik perjalanan ini. Rute tetap ditampilkan dengan penalti skor."
            is RouteCandidate.Combined ->
                "⚠ Ada laporan gangguan aktif pada segmen transit rute ini. Pertimbangkan alternatif lain."
            is RouteCandidate.PrivateVehicle ->
                "⚠ Ada laporan gangguan di sekitar rute ini."
        }
    }

    // ── Builders per prioritas ────────────────────────────────────────────────

    private fun buildFastestReason(
        candidate: RouteCandidate,
        isBestTime: Boolean,
        isBestCost: Boolean,
        isMain: Boolean
    ): String {
        val m = candidate.metrics
        val durationMin = m.totalDurationSeconds / 60

        return when {
            isMain && isBestTime ->
                "Rute ini dipilih karena memiliki waktu tempuh paling singkat, sekitar $durationMin menit."
            isMain ->
                "Rute ini dipilih karena kombinasi waktu dan biaya paling seimbang untuk prioritas tercepat."
            isBestCost ->
                "Alternatif ini lebih hemat biaya dibanding rekomendasi utama, meski sedikit lebih lama."
            else ->
                "Alternatif ini menawarkan rute berbeda dengan estimasi waktu sekitar $durationMin menit."
        }
    }

    private fun buildCheapestReason(
        candidate: RouteCandidate,
        isBestCost: Boolean,
        isBestTime: Boolean,
        isMain: Boolean
    ): String {
        val m = candidate.metrics
        val costStr = formatRupiah(m.estimatedTotalCost)

        return when {
            isMain && isBestCost ->
                "Rute ini dipilih karena memiliki total biaya paling rendah, sekitar $costStr."
            isMain ->
                "Rute ini dipilih karena kombinasi biaya dan waktu paling efisien untuk prioritas terhemat."
            isBestTime ->
                "Alternatif ini lebih cepat dibanding rekomendasi utama, meski biayanya sedikit lebih tinggi."
            else ->
                "Alternatif ini menawarkan pilihan lain dengan estimasi biaya sekitar $costStr."
        }
    }

    private fun buildMinWalkingReason(
        candidate: RouteCandidate,
        isBestWalking: Boolean,
        isBestTime: Boolean,
        isMain: Boolean
    ): String {
        val m = candidate.metrics
        val walkingM = m.walkingDistanceMeters.toInt()

        return when {
            isMain && isBestWalking && walkingM == 0 ->
                "Rute ini dipilih karena tidak memerlukan jalan kaki sama sekali."
            isMain && isBestWalking ->
                "Rute ini dipilih karena memiliki jarak jalan kaki paling pendek, hanya sekitar $walkingM meter."
            isMain ->
                "Rute ini dipilih karena meminimalkan jalan kaki dengan tetap menjaga waktu tempuh yang wajar."
            isBestTime ->
                "Alternatif ini lebih cepat, meski jarak jalan kakinya sedikit lebih jauh."
            else ->
                "Alternatif ini menawarkan pilihan lain dengan jarak jalan kaki sekitar $walkingM meter."
        }
    }

    private fun buildFewestTransfersReason(
        candidate: RouteCandidate,
        isBestTransit: Boolean,
        isBestTime: Boolean,
        isMain: Boolean
    ): String {
        val m = candidate.metrics

        return when {
            isMain && isBestTransit && m.transitCount == 0 ->
                "Rute ini dipilih karena tidak memerlukan perpindahan moda sama sekali."
            isMain && isBestTransit ->
                "Rute ini dipilih karena memiliki jumlah transit paling sedikit, hanya ${m.transitCount}x perpindahan."
            isMain ->
                "Rute ini dipilih karena meminimalkan perpindahan moda dengan waktu tempuh yang masuk akal."
            isBestTime ->
                "Alternatif ini lebih cepat, meski memerlukan lebih banyak perpindahan moda."
            else ->
                "Alternatif ini menawarkan pilihan lain dengan ${m.transitCount}x perpindahan moda."
        }
    }

    // ── Util ─────────────────────────────────────────────────────────────────

    private fun formatRupiah(amount: Int): String {
        return "Rp ${String.format("%,d", amount).replace(',', '.')}"
    }
}
