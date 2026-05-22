package com.example.naikapa.domain.recommendation

import com.example.naikapa.data.local.DisruptionReportDao
import com.example.naikapa.data.model.CombinedRouteResult
import com.example.naikapa.data.model.DisruptionReport
import com.example.naikapa.data.model.LocationPoint
import com.example.naikapa.data.model.PrivateVehicleMode
import com.example.naikapa.data.model.PrivateVehicleRouteResult
import com.example.naikapa.data.model.RecommendationResult
import com.example.naikapa.data.model.RouteCandidate
import com.example.naikapa.data.model.ScoredRoute
import com.example.naikapa.data.model.SearchLocation
import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitMode
import com.example.naikapa.data.model.TransitRouteResult
import com.example.naikapa.data.model.VehicleTypeFilter
import com.example.naikapa.data.repository.CombinedRouteRepository
import com.example.naikapa.data.repository.TomTomRoutingRepository
import com.example.naikapa.data.repository.TransitRoutingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Orkestrasi utama sistem rekomendasi NaikApa.
 *
 * Alur:
 * 1. Jalankan semua sumber rute secara paralel (transit, kendaraan pribadi, gabungan).
 * 2. Kumpulkan semua kandidat.
 * 3. Ambil gangguan aktif dari database.
 * 4. Beri skor setiap kandidat.
 * 5. Urutkan berdasarkan skor.
 * 6. Kembalikan RecommendationResult (utama + maks 2 alternatif).
 */
class RecommendationEngine(
    private val transitRoutingRepository: TransitRoutingRepository,
    private val tomTomRoutingRepository: TomTomRoutingRepository,
    private val combinedRouteRepository: CombinedRouteRepository,
    private val disruptionReportDao: DisruptionReportDao,
    private val scorer: RecommendationScorer = RecommendationScorer(),
    private val reasonBuilder: RecommendationReasonBuilder = RecommendationReasonBuilder()
) {

    /**
     * Hitung rekomendasi rute berdasarkan input user.
     *
     * @param origin  Titik asal (dari GPS atau pilihan user).
     * @param destination  Titik tujuan (dari TomTom Search atau GTFS).
     * @param transitMode  Moda transit yang dipilih.
     * @param sortPreference  Prioritas perjalanan.
     * @param hasMotor  Apakah user memiliki motor.
     * @param hasCar  Apakah user memiliki mobil.
     * @param tomTomApiKey  API key TomTom untuk routing kendaraan pribadi.
     * @param vehicleTypeFilter  Filter tipe kendaraan: ALL, TRANSIT_ONLY, atau PRIVATE_ONLY.
     * @param originStopId  Stop ID GTFS asal jika user memilih dari GTFS (opsional).
     * @param destinationStopId  Stop ID GTFS tujuan jika user memilih dari GTFS (opsional).
     */
    suspend fun recommend(
        origin: LocationPoint,
        destination: SearchLocation,
        transitMode: TransitMode,
        sortPreference: SortPreference,
        hasMotor: Boolean,
        hasCar: Boolean,
        tomTomApiKey: String,
        vehicleTypeFilter: VehicleTypeFilter = VehicleTypeFilter.ALL,
        originStopId: String? = null,
        destinationStopId: String? = null
    ): Result<RecommendationResult> = runCatching {

        val candidates = mutableListOf<RouteCandidate>()

        // Tentukan apakah masing-masing tipe kandidat perlu dijalankan berdasarkan filter
        val includeTransit = vehicleTypeFilter != VehicleTypeFilter.PRIVATE_ONLY
        val includePrivate = vehicleTypeFilter != VehicleTypeFilter.TRANSIT_ONLY
        val includeCombined = vehicleTypeFilter == VehicleTypeFilter.ALL

        coroutineScope {
            // ── 1. Rute transit (jika ada stop GTFS asal & tujuan) ──────────
            val transitDeferred = async(Dispatchers.IO) {
                if (includeTransit && !originStopId.isNullOrBlank() && !destinationStopId.isNullOrBlank()) {
                    transitRoutingRepository.findRoute(
                        startStopId = originStopId,
                        endStopId = destinationStopId,
                        mode = transitMode,
                        sortPreference = sortPreference
                    )
                } else null
            }

            // ── 2. Rute kendaraan pribadi ────────────────────────────────────
            val motorDeferred = async(Dispatchers.IO) {
                if (includePrivate && hasMotor && tomTomApiKey.isNotBlank()) {
                    tomTomRoutingRepository.calculateRoute(
                        originLat = origin.latitude,
                        originLon = origin.longitude,
                        destinationLat = destination.latitude,
                        destinationLon = destination.longitude,
                        mode = PrivateVehicleMode.MOTOR,
                        apiKey = tomTomApiKey
                    ).getOrNull()?.firstOrNull()
                } else null
            }

            val carDeferred = async(Dispatchers.IO) {
                if (includePrivate && hasCar && tomTomApiKey.isNotBlank()) {
                    tomTomRoutingRepository.calculateRoute(
                        originLat = origin.latitude,
                        originLon = origin.longitude,
                        destinationLat = destination.latitude,
                        destinationLon = destination.longitude,
                        mode = PrivateVehicleMode.CAR,
                        apiKey = tomTomApiKey
                    ).getOrNull()?.firstOrNull()
                } else null
            }

            // ── 3. Rute gabungan (motor/mobil ke transit) ────────────────────
            val combinedMotorDeferred = async(Dispatchers.IO) {
                if (includeCombined && hasMotor && tomTomApiKey.isNotBlank()) {
                    combinedRouteRepository.findCombinedRoutes(
                        originLat = origin.latitude,
                        originLon = origin.longitude,
                        destinationLat = destination.latitude,
                        destinationLon = destination.longitude,
                        privateVehicleMode = PrivateVehicleMode.MOTOR,
                        transitMode = transitMode,
                        sortPreference = sortPreference
                    ).getOrNull()?.firstOrNull()
                } else null
            }

            val combinedCarDeferred = async(Dispatchers.IO) {
                if (includeCombined && hasCar && tomTomApiKey.isNotBlank()) {
                    combinedRouteRepository.findCombinedRoutes(
                        originLat = origin.latitude,
                        originLon = origin.longitude,
                        destinationLat = destination.latitude,
                        destinationLon = destination.longitude,
                        privateVehicleMode = PrivateVehicleMode.CAR,
                        transitMode = transitMode,
                        sortPreference = sortPreference
                    ).getOrNull()?.firstOrNull()
                } else null
            }

            val combinedTransitOnlyDeferred = async(Dispatchers.IO) {
                if (includeTransit && (originStopId.isNullOrBlank() || destinationStopId.isNullOrBlank())) {
                    combinedRouteRepository.findCombinedRoutes(
                        originLat = origin.latitude,
                        originLon = origin.longitude,
                        destinationLat = destination.latitude,
                        destinationLon = destination.longitude,
                        privateVehicleMode = null,
                        transitMode = transitMode,
                        sortPreference = sortPreference
                    ).getOrNull()?.firstOrNull()
                } else null
            }

            // ── Kumpulkan hasil ──────────────────────────────────────────────
            transitDeferred.await()?.let { candidates.add(RouteCandidate.Transit(it)) }
            motorDeferred.await()?.let { candidates.add(RouteCandidate.PrivateVehicle(it)) }
            carDeferred.await()?.let { candidates.add(RouteCandidate.PrivateVehicle(it)) }
            combinedMotorDeferred.await()?.let { candidates.add(RouteCandidate.Combined(it)) }
            combinedCarDeferred.await()?.let { candidates.add(RouteCandidate.Combined(it)) }
            combinedTransitOnlyDeferred.await()?.let { candidates.add(RouteCandidate.Combined(it)) }
        }

        if (candidates.isEmpty()) error("Tidak ada rute yang ditemukan untuk kombinasi input ini.")

        // ── 4. Ambil gangguan aktif ──────────────────────────────────────────
        val activeDisruptions = withContext(Dispatchers.IO) {
            disruptionReportDao.getActiveReports()
        }

        // ── 5. Skor dan urutkan ──────────────────────────────────────────────
        val scored = candidates
            .map { candidate ->
                val s = scorer.score(candidate, sortPreference, activeDisruptions)
                val hasDisruption = scorer.hasDisruption(candidate, activeDisruptions)
                ScoredRoute(
                    candidate = candidate,
                    score = s,
                    reason = reasonBuilder.buildReason(candidate, candidates, sortPreference, isMain = false),
                    hasDisruptionWarning = hasDisruption,
                    disruptionWarningText = if (hasDisruption) reasonBuilder.buildDisruptionWarning(candidate) else null
                )
            }
            .sortedByDescending { it.score }

        // ── 6. Susun hasil ───────────────────────────────────────────────────
        val main = scored.first().let { sr ->
            sr.copy(
                reason = reasonBuilder.buildReason(sr.candidate, candidates, sortPreference, isMain = true),
                rankLabel = "Rekomendasi Utama"
            )
        }
        val alternatives = scored.drop(1).take(2).mapIndexed { idx, sr ->
            sr.copy(
                reason = reasonBuilder.buildReason(sr.candidate, candidates, sortPreference, isMain = false),
                rankLabel = "Alternatif ${idx + 1}"
            )
        }

        RecommendationResult(
            main = main,
            alternatives = alternatives,
            sortPreference = sortPreference
        )
    }
}
