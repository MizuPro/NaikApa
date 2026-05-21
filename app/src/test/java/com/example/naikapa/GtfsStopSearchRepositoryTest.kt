package com.example.naikapa

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.local.GtfsDao
import com.example.naikapa.data.model.GtfsStop
import com.example.naikapa.data.model.SearchLocation
import com.example.naikapa.data.repository.GtfsStopSearchRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class GtfsStopSearchRepositoryTest {

    private val mockDao: GtfsDao = mock(GtfsDao::class.java)
    private val repo = GtfsStopSearchRepository(mockDao)

    // ---- Helper data ----
    private val stopTJ = GtfsStop("tj_001", "Halte Blok M",    -6.2441, 106.7975, "tj",  "halte")
    private val stopKRL = GtfsStop("krl_001", "Stasiun Tangerang", -6.1767, 106.6319, "krl", "stasiun")
    private val stopMRT = GtfsStop("mrt_001", "Bundaran HI MRT", -6.1934, 106.8231, "mrt", "stasiun")
    private val stopLRT = GtfsStop("lrt_001", "Dukuh Atas LRT",  -6.2009, 106.8229, "lrt", "stasiun")

    // ---- search() ----

    @Test
    fun `search returns empty list when keyword shorter than min query length`() {
        val result = repo.search("a", null, null)
        assertTrue("Keyword terlalu pendek harus mengembalikan list kosong", result.isEmpty())
    }

    @Test
    fun `search returns empty list when keyword is blank`() {
        val result = repo.search("  ", null, null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `search maps GtfsStop to SearchLocation with SOURCE_GTFS`() {
        `when`(mockDao.searchStops("Blok M", null, AppConstants.GTFS_SEARCH_LIMIT * 3))
            .thenReturn(listOf(stopTJ))

        val results = repo.search("Blok M", null, null)
        assertEquals(1, results.size)
        val loc = results[0]
        assertEquals("Halte Blok M", loc.name)
        assertEquals(SearchLocation.SOURCE_GTFS, loc.source)
        assertEquals("tj_001", loc.stopId)
        assertEquals("tj", loc.agencyId)
    }

    @Test
    fun `search returns null distanceMeters when user location unavailable`() {
        `when`(mockDao.searchStops("Tangerang", null, AppConstants.GTFS_SEARCH_LIMIT * 3))
            .thenReturn(listOf(stopKRL))

        val results = repo.search("Tangerang", null, null)
        assertNull(results[0].distanceMeters)
    }

    @Test
    fun `search calculates distanceMeters when user location available`() {
        `when`(mockDao.searchStops("Tangerang", null, AppConstants.GTFS_SEARCH_LIMIT * 3))
            .thenReturn(listOf(stopKRL))

        // Koordinat user dekat Stasiun Tangerang
        val results = repo.search("Tangerang", -6.1770, 106.6320, null)
        val dist = results[0].distanceMeters
        assertTrue("Jarak harus positif", dist != null && dist > 0.0)
        assertTrue("Jarak ke stasiun Tangerang dari titik dekat harus < 1 km", dist!! < 1000.0)
    }

    @Test
    fun `search prioritizes stops whose name starts with keyword`() {
        val stopA = GtfsStop("a", "Dukuh Atas BRT",  -6.201, 106.822, "tj", null)
        val stopB = GtfsStop("b", "Halte Dukuh Atas", -6.202, 106.823, "tj", null)
        `when`(mockDao.searchStops("Dukuh", null, AppConstants.GTFS_SEARCH_LIMIT * 3))
            .thenReturn(listOf(stopB, stopA))  // DAO returns B first (alpha)

        val results = repo.search("Dukuh", null, null)
        // "Dukuh Atas BRT" starts with "Dukuh" → should be first
        assertEquals("Dukuh Atas BRT", results[0].name)
    }

    @Test
    fun `search respects agencyFilter parameter`() {
        `when`(mockDao.searchStops("Stasiun", "krl", AppConstants.GTFS_SEARCH_LIMIT * 3))
            .thenReturn(listOf(stopKRL))

        val results = repo.search("Stasiun", null, null, "krl")
        assertEquals(1, results.size)
        assertEquals("krl", results[0].agencyId)
    }

    @Test
    fun `search limits results to GTFS_SEARCH_LIMIT`() {
        val manyStops = (1..25).map { i ->
            GtfsStop("id_$i", "Stop $i", -6.0 + i * 0.01, 106.0 + i * 0.01, "tj", null)
        }
        `when`(mockDao.searchStops("Stop", null, AppConstants.GTFS_SEARCH_LIMIT * 3))
            .thenReturn(manyStops)

        val results = repo.search("Stop", null, null)
        assertTrue(results.size <= AppConstants.GTFS_SEARCH_LIMIT)
    }

    // ---- agencyIdToLabel() ----

    @Test
    fun agencyIdToLabel_returnsCorrectLabels() {
        assertEquals("TransJakarta",     GtfsStopSearchRepository.agencyIdToLabel("tj"))
        assertEquals("KRL Commuter Line",GtfsStopSearchRepository.agencyIdToLabel("krl"))
        assertEquals("MRT Jakarta",      GtfsStopSearchRepository.agencyIdToLabel("mrt"))
        assertEquals("LRT Jakarta",      GtfsStopSearchRepository.agencyIdToLabel("lrt"))
        assertEquals("UNKNOWN",          GtfsStopSearchRepository.agencyIdToLabel("unknown"))
        assertEquals("Halte / Stasiun",  GtfsStopSearchRepository.agencyIdToLabel(null))
    }

    @Test
    fun agencyIdToLabel_caseInsensitive() {
        assertEquals("TransJakarta", GtfsStopSearchRepository.agencyIdToLabel("TJ"))
        assertEquals("KRL Commuter Line", GtfsStopSearchRepository.agencyIdToLabel("KRL"))
    }

    // ---- haversineMeters() ----

    @Test
    fun haversineMeters_samePointReturnsZero() {
        val dist = repo.haversineMeters(-6.2088, 106.8456, -6.2088, 106.8456)
        assertEquals(0.0, dist, 0.1)
    }

    @Test
    fun haversineMeters_knownDistanceIsReasonable() {
        // Jakarta Pusat ke Stasiun Tangerang ~30 km
        val dist = repo.haversineMeters(-6.2088, 106.8456, -6.1767, 106.6319)
        assertTrue("Jarak Jakarta-Tangerang harus antara 20–40 km", dist in 20_000.0..40_000.0)
    }
}
