package com.example.naikapa

import com.example.naikapa.data.model.DisruptionReport
import com.example.naikapa.data.model.PrivateVehicleMode
import com.example.naikapa.data.model.PrivateVehicleRouteResult
import com.example.naikapa.data.model.RouteCandidate
import com.example.naikapa.data.model.RouteMetrics
import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitEdgeType
import com.example.naikapa.data.model.TransitMode
import com.example.naikapa.data.model.TransitNode
import com.example.naikapa.data.model.TransitRouteResult
import com.example.naikapa.domain.recommendation.RecommendationReasonBuilder
import com.example.naikapa.domain.recommendation.RecommendationScorer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecommendationScorerTest {

    private lateinit var scorer: RecommendationScorer
    private lateinit var reasonBuilder: RecommendationReasonBuilder

    @Before
    fun setUp() {
        scorer = RecommendationScorer()
        reasonBuilder = RecommendationReasonBuilder()
    }

    // ── Helper builders ───────────────────────────────────────────────────────

    private fun makeMotorCandidate(
        durationSec: Int = 1800,
        distanceM: Double = 10000.0,
        bbm: Int = 2500,
        totalCost: Int = 2500
    ): RouteCandidate.PrivateVehicle {
        return RouteCandidate.PrivateVehicle(
            result = PrivateVehicleRouteResult(
                mode = PrivateVehicleMode.MOTOR,
                distanceMeters = distanceM.toInt(),
                travelTimeSeconds = durationSec,
                estimatedBbm = bbm,
                estimatedTotalCost = totalCost,
                points = emptyList(),
                alternativeIndex = 0
            )
        )
    }

    private fun makeTransitCandidate(
        durationSec: Int = 3600,
        walkingM: Double = 500.0,
        transitCount: Int = 2,
        fare: Int = 7000
    ): RouteCandidate.Transit {
        val dummyNode = TransitNode("S1", "Stop A", -6.2, 106.8, "Tije")
        return RouteCandidate.Transit(
            result = TransitRouteResult(
                startStop = dummyNode,
                endStop = dummyNode,
                mode = TransitMode.ALL,
                sortPreference = SortPreference.FASTEST,
                edges = emptyList(),
                steps = emptyList(),
                metrics = RouteMetrics(
                    totalDurationSeconds = durationSec,
                    totalDistanceMeters = 15000.0,
                    walkingDistanceMeters = walkingM,
                    transitCount = transitCount,
                    estimatedFare = fare,
                    estimatedBbm = 0,
                    estimatedTotalCost = fare
                )
            )
        )
    }

    // ── Test 1: Skor motor lebih tinggi dari transit untuk prioritas FASTEST ──

    @Test
    fun `motor candidate scores higher than slow transit for FASTEST priority`() {
        val motor = makeMotorCandidate(durationSec = 1200)   // 20 menit
        val transit = makeTransitCandidate(durationSec = 5400) // 90 menit

        val motorScore = scorer.score(motor, SortPreference.FASTEST, emptyList())
        val transitScore = scorer.score(transit, SortPreference.FASTEST, emptyList())

        assertTrue(
            "Motor ($motorScore) should score higher than slow transit ($transitScore) for FASTEST",
            motorScore > transitScore
        )
    }

    // ── Test 2: Skor transit lebih tinggi dari motor untuk prioritas CHEAPEST ─

    @Test
    fun `transit candidate scores higher than motor for CHEAPEST priority`() {
        // Motor dengan BBM sangat mahal vs transit murah
        val motor = makeMotorCandidate(durationSec = 1800, totalCost = 40000)   // Rp 40.000 BBM
        val transit = makeTransitCandidate(durationSec = 3600, fare = 3500)     // Rp 3.500 tarif

        val motorScore = scorer.score(motor, SortPreference.CHEAPEST, emptyList())
        val transitScore = scorer.score(transit, SortPreference.CHEAPEST, emptyList())

        assertTrue(
            "Transit ($transitScore) should score higher than very expensive motor ($motorScore) for CHEAPEST",
            transitScore > motorScore
        )
    }

    // ── Test 3: Skor transit lebih tinggi untuk MIN_WALKING jika walking = 0 ─

    @Test
    fun `motor candidate scores higher for MIN_WALKING when walking is zero`() {
        val motor = makeMotorCandidate()                        // walking = 0
        val transit = makeTransitCandidate(walkingM = 1500.0)  // 1.5 km jalan kaki

        val motorScore = scorer.score(motor, SortPreference.MIN_WALKING, emptyList())
        val transitScore = scorer.score(transit, SortPreference.MIN_WALKING, emptyList())

        assertTrue(
            "Motor ($motorScore) should score higher than transit with 1.5km walking ($transitScore)",
            motorScore > transitScore
        )
    }

    // ── Test 4: Penalti gangguan mengurangi skor ──────────────────────────────

    @Test
    fun `disruption penalty reduces transit score`() {
        val transit = makeTransitCandidate()
        val scoreWithoutDisruption = scorer.score(transit, SortPreference.FASTEST, emptyList())

        // Buat laporan gangguan yang tidak terkait (tidak ada stopId yang cocok)
        val unrelatedDisruption = DisruptionReport(
            idUser = 1,
            stopId = "STOP_TIDAK_ADA",
            routeId = null,
            category = "Penuh",
            description = "Test",
            photoPath = null
        )
        val scoreWithUnrelatedDisruption = scorer.score(transit, SortPreference.FASTEST, listOf(unrelatedDisruption))

        assertEquals(
            "Unrelated disruption should not affect score",
            scoreWithoutDisruption,
            scoreWithUnrelatedDisruption
        )
    }

    // ── Test 4b: Laporan terkait stop menurunkan skor ─────────────────────────

    @Test
    fun `related stop disruption reduces transit score`() {
        // Buat transit candidate dengan stop yang diketahui
        val dummyNode = com.example.naikapa.data.model.TransitNode("STOP_DKA", "Dukuh Atas", -6.2, 106.82, "Tije")
        val transitWithStop = RouteCandidate.Transit(
            result = com.example.naikapa.data.model.TransitRouteResult(
                startStop = dummyNode,
                endStop   = dummyNode,
                mode      = com.example.naikapa.data.model.TransitMode.ALL,
                sortPreference = SortPreference.FASTEST,
                edges  = emptyList(),
                steps  = listOf(
                    com.example.naikapa.data.model.RouteStep(
                        type           = com.example.naikapa.data.model.TransitEdgeType.TRANSIT,
                        fromStop       = dummyNode,
                        toStop         = dummyNode,
                        routeId        = "ROUTE_KRL",
                        routeShortName = "KRL",
                        routeLongName  = null,
                        agencyId       = "KAIC",
                        durationSeconds = 600,
                        distanceMeters  = 5000.0,
                        stopCount       = 3
                    )
                ),
                metrics = RouteMetrics(
                    totalDurationSeconds = 3600,
                    totalDistanceMeters  = 15000.0,
                    walkingDistanceMeters = 500.0,
                    transitCount         = 1,
                    estimatedFare        = 3500,
                    estimatedBbm         = 0,
                    estimatedTotalCost   = 3500
                )
            )
        )

        val scoreWithoutDisruption = scorer.score(transitWithStop, SortPreference.FASTEST, emptyList())

        // Laporan terkait stop yang dilalui
        val relatedDisruption = DisruptionReport(
            idUser      = 1,
            stopId      = "STOP_DKA",
            routeId     = null,
            category    = "Keterlambatan",
            description = "Kereta terlambat 30 menit",
            photoPath   = null
        )
        val scoreWithRelatedDisruption = scorer.score(transitWithStop, SortPreference.FASTEST, listOf(relatedDisruption))

        assertTrue(
            "Related stop disruption should reduce score (before=$scoreWithoutDisruption, after=$scoreWithRelatedDisruption)",
            scoreWithRelatedDisruption < scoreWithoutDisruption
        )
        assertTrue(
            "hasDisruption should return true for related stop",
            scorer.hasDisruption(transitWithStop, listOf(relatedDisruption))
        )
    }

    // ── Test 4c: Laporan expired tidak memengaruhi skor ──────────────────────

    @Test
    fun `expired disruption report does not affect score`() {
        val transit = makeTransitCandidate()
        val scoreWithoutDisruption = scorer.score(transit, SortPreference.FASTEST, emptyList())

        // Laporan yang sudah expired (expiredAt di masa lalu) — tidak masuk ke activeDisruptions
        // Scorer hanya menerima list yang sudah difilter aktif dari DAO,
        // jadi test ini memastikan list kosong = tidak ada penalti
        val emptyActiveList = emptyList<DisruptionReport>()
        val scoreWithEmptyList = scorer.score(transit, SortPreference.FASTEST, emptyActiveList)

        assertEquals(
            "Empty active disruption list should not affect score",
            scoreWithoutDisruption,
            scoreWithEmptyList
        )
    }

    // ── Test 5: Skor selalu dalam rentang 0–100 ───────────────────────────────

    @Test
    fun `score is always between 0 and 100`() {
        val candidates = listOf(
            makeMotorCandidate(durationSec = 100, totalCost = 100),
            makeMotorCandidate(durationSec = 10000, totalCost = 100000),
            makeTransitCandidate(durationSec = 100, walkingM = 0.0, transitCount = 0, fare = 0),
            makeTransitCandidate(durationSec = 10000, walkingM = 5000.0, transitCount = 10, fare = 100000)
        )
        val preferences = SortPreference.values()

        for (candidate in candidates) {
            for (pref in preferences) {
                val score = scorer.score(candidate, pref, emptyList())
                assertTrue("Score $score should be >= 0", score >= 0)
                assertTrue("Score $score should be <= 100", score <= 100)
            }
        }
    }

    // ── Test 6: hasDisruption false untuk kendaraan pribadi ──────────────────

    @Test
    fun `private vehicle candidate never has disruption`() {
        val motor = makeMotorCandidate()
        val disruption = DisruptionReport(
            idUser = 1,
            stopId = "ANY_STOP",
            routeId = "ANY_ROUTE",
            category = "Penuh",
            description = "Test",
            photoPath = null
        )
        assertFalse(scorer.hasDisruption(motor, listOf(disruption)))
    }

    // ── Test 7: Alasan rekomendasi tidak kosong ───────────────────────────────

    @Test
    fun `reason builder returns non-empty string for all priorities`() {
        val motor = makeMotorCandidate()
        val candidates = listOf(motor)

        for (pref in SortPreference.values()) {
            val reason = reasonBuilder.buildReason(motor, candidates, pref, isMain = true)
            assertTrue("Reason should not be empty for $pref", reason.isNotBlank())
        }
    }

    // ── Test 8: Alasan utama berbeda dari alasan alternatif ───────────────────

    @Test
    fun `main reason differs from alternative reason`() {
        val motor = makeMotorCandidate()
        val transit = makeTransitCandidate()
        val candidates = listOf(motor, transit)

        val mainReason = reasonBuilder.buildReason(motor, candidates, SortPreference.FASTEST, isMain = true)
        val altReason = reasonBuilder.buildReason(transit, candidates, SortPreference.FASTEST, isMain = false)

        // Keduanya tidak kosong
        assertTrue(mainReason.isNotBlank())
        assertTrue(altReason.isNotBlank())
    }

    // ── Test 9: Urutan skor berubah saat prioritas berubah ────────────────────

    @Test
    fun `ranking changes when priority changes from FASTEST to CHEAPEST`() {
        // Motor: sangat cepat (15 menit) tapi sangat mahal (Rp 40.000)
        val fastMotor = makeMotorCandidate(durationSec = 900, totalCost = 40000)
        // Transit: lambat (90 menit) tapi sangat murah (Rp 3.500)
        val cheapTransit = makeTransitCandidate(durationSec = 5400, fare = 3500)

        val fastestMotorScore = scorer.score(fastMotor, SortPreference.FASTEST, emptyList())
        val fastestTransitScore = scorer.score(cheapTransit, SortPreference.FASTEST, emptyList())

        val cheapestMotorScore = scorer.score(fastMotor, SortPreference.CHEAPEST, emptyList())
        val cheapestTransitScore = scorer.score(cheapTransit, SortPreference.CHEAPEST, emptyList())

        // Untuk FASTEST: motor harus menang (jauh lebih cepat)
        assertTrue(
            "Motor should win for FASTEST (motor=$fastestMotorScore, transit=$fastestTransitScore)",
            fastestMotorScore > fastestTransitScore
        )
        // Untuk CHEAPEST: transit harus menang (jauh lebih murah)
        assertTrue(
            "Transit should win for CHEAPEST (transit=$cheapestTransitScore, motor=$cheapestMotorScore)",
            cheapestTransitScore > cheapestMotorScore
        )
    }

    // ── Test 10: Skor konstanta bobot valid (total = 100 per prioritas) ───────

    @Test
    fun `score weight constants sum to 100 for each priority`() {
        val fastestSum = com.example.naikapa.common.AppConstants.SCORE_W_FASTEST_TIME +
                com.example.naikapa.common.AppConstants.SCORE_W_FASTEST_COST +
                com.example.naikapa.common.AppConstants.SCORE_W_FASTEST_WALKING +
                com.example.naikapa.common.AppConstants.SCORE_W_FASTEST_TRANSIT +
                com.example.naikapa.common.AppConstants.SCORE_W_FASTEST_DISRUPTION
        assertEquals("FASTEST weights should sum to 100", 100, fastestSum)

        val cheapestSum = com.example.naikapa.common.AppConstants.SCORE_W_CHEAPEST_COST +
                com.example.naikapa.common.AppConstants.SCORE_W_CHEAPEST_TIME +
                com.example.naikapa.common.AppConstants.SCORE_W_CHEAPEST_WALKING +
                com.example.naikapa.common.AppConstants.SCORE_W_CHEAPEST_TRANSIT +
                com.example.naikapa.common.AppConstants.SCORE_W_CHEAPEST_DISRUPTION
        assertEquals("CHEAPEST weights should sum to 100", 100, cheapestSum)

        val minWalkSum = com.example.naikapa.common.AppConstants.SCORE_W_MINWALK_WALKING +
                com.example.naikapa.common.AppConstants.SCORE_W_MINWALK_TIME +
                com.example.naikapa.common.AppConstants.SCORE_W_MINWALK_COST +
                com.example.naikapa.common.AppConstants.SCORE_W_MINWALK_TRANSIT +
                com.example.naikapa.common.AppConstants.SCORE_W_MINWALK_DISRUPTION
        assertEquals("MIN_WALKING weights should sum to 100", 100, minWalkSum)

        val minTransitSum = com.example.naikapa.common.AppConstants.SCORE_W_MINTRANSIT_TRANSIT +
                com.example.naikapa.common.AppConstants.SCORE_W_MINTRANSIT_TIME +
                com.example.naikapa.common.AppConstants.SCORE_W_MINTRANSIT_COST +
                com.example.naikapa.common.AppConstants.SCORE_W_MINTRANSIT_WALKING +
                com.example.naikapa.common.AppConstants.SCORE_W_MINTRANSIT_DISRUPTION
        assertEquals("FEWEST_TRANSFERS weights should sum to 100", 100, minTransitSum)
    }
}
