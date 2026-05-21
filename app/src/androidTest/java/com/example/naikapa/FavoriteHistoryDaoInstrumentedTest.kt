package com.example.naikapa

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.naikapa.data.local.HistoryDao
import com.example.naikapa.data.local.NaikApaDatabaseHelper
import com.example.naikapa.data.local.SavedTripDao
import com.example.naikapa.data.model.RouteHistory
import com.example.naikapa.data.model.SavedTrip
import com.example.naikapa.data.model.SearchHistory
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteHistoryDaoInstrumentedTest {

    private lateinit var dbHelper: NaikApaDatabaseHelper
    private lateinit var savedTripDao: SavedTripDao
    private lateinit var historyDao: HistoryDao

    private val testUserId = 1L

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        dbHelper = NaikApaDatabaseHelper(context)
        savedTripDao = SavedTripDao(dbHelper)
        historyDao = HistoryDao(dbHelper)
    }

    @After
    fun tearDown() {
        dbHelper.close()
    }

    // ── SavedTrip CRUD ────────────────────────────────────────────────────────

    @Test
    fun savedTrip_insertAndRead() {
        val trip = SavedTrip(
            idUser = testUserId,
            namaPerjalanan = "Rumah ke Kantor",
            originName = "Rumah",
            originLat = -6.2,
            originLon = 106.8,
            destinationName = "Kantor",
            destinationLat = -6.18,
            destinationLon = 106.82,
            mode = "Campur Semua",
            priority = "Tercepat"
        )
        val id = savedTripDao.insert(trip)
        assertTrue("Insert harus berhasil", id > 0)

        val trips = savedTripDao.getByUser(testUserId)
        val found = trips.firstOrNull { it.idSaved == id }
        assertNotNull("Trip yang baru diinsert harus ditemukan", found)
        assertEquals("Rumah ke Kantor", found?.namaPerjalanan)
        assertEquals("Campur Semua", found?.mode)
    }

    @Test
    fun savedTrip_updateNameAndNote() {
        val trip = SavedTrip(
            idUser = testUserId,
            namaPerjalanan = "Trip Awal",
            originName = "A",
            originLat = -6.2,
            originLon = 106.8,
            destinationName = "B",
            destinationLat = -6.18,
            destinationLon = 106.82,
            mode = "Motor",
            priority = "Terhemat"
        )
        val id = savedTripDao.insert(trip)
        assertTrue(id > 0)

        val updated = savedTripDao.updateNameAndNote(id, "Trip Diperbarui", "Catatan baru")
        assertEquals(1, updated)

        val trips = savedTripDao.getByUser(testUserId)
        val found = trips.firstOrNull { it.idSaved == id }
        assertEquals("Trip Diperbarui", found?.namaPerjalanan)
        assertEquals("Catatan baru", found?.catatan)
    }

    @Test
    fun savedTrip_delete() {
        val trip = SavedTrip(
            idUser = testUserId,
            namaPerjalanan = "Trip Hapus",
            originName = "X",
            originLat = -6.2,
            originLon = 106.8,
            destinationName = "Y",
            destinationLat = -6.18,
            destinationLon = 106.82,
            mode = "KRL",
            priority = "Minim Transit"
        )
        val id = savedTripDao.insert(trip)
        assertTrue(id > 0)

        val deleted = savedTripDao.delete(id)
        assertEquals(1, deleted)

        val trips = savedTripDao.getByUser(testUserId)
        assertNull("Trip yang dihapus tidak boleh ditemukan", trips.firstOrNull { it.idSaved == id })
    }

    // ── SearchHistory CRUD ────────────────────────────────────────────────────

    @Test
    fun searchHistory_insertReadDeleteClear() {
        val history1 = SearchHistory(
            idUser = testUserId,
            keyword = "Dukuh Atas",
            selectedName = "Halte Dukuh Atas",
            selectedAddress = "Jakarta Selatan",
            selectedLat = -6.2,
            selectedLon = 106.82
        )
        val history2 = SearchHistory(
            idUser = testUserId,
            keyword = "Stasiun Tangerang",
            selectedName = "Stasiun Tangerang",
            selectedAddress = null,
            selectedLat = -6.17,
            selectedLon = 106.63
        )

        val id1 = historyDao.insertSearchHistory(history1)
        val id2 = historyDao.insertSearchHistory(history2)
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)

        val list = historyDao.getSearchHistory(testUserId)
        assertTrue("Harus ada minimal 2 riwayat pencarian", list.size >= 2)

        // Delete satu
        val deleted = historyDao.deleteSearchHistory(id1)
        assertEquals(1, deleted)
        val afterDelete = historyDao.getSearchHistory(testUserId)
        assertNull(afterDelete.firstOrNull { it.idSearch == id1 })

        // Clear semua
        historyDao.clearSearchHistory(testUserId)
        val afterClear = historyDao.getSearchHistory(testUserId)
        assertTrue("Setelah clear, tidak boleh ada riwayat pencarian user ini", afterClear.isEmpty())
    }

    // ── RouteHistory CRUD ─────────────────────────────────────────────────────

    @Test
    fun routeHistory_insertReadDeleteClear() {
        val history = RouteHistory(
            idUser = testUserId,
            originName = "Lokasi saya",
            destinationName = "Kantor",
            mode = "Campur Semua",
            priority = "Tercepat",
            recommendationSummary = "Naik KRL dari Stasiun A ke Stasiun B",
            score = 85,
            estimatedTime = 1800,
            estimatedCost = 3500,
            estimatedBbm = 0,
            walkingDistance = 350.0,
            transitCount = 1
        )

        val id = historyDao.insertRouteHistory(history)
        assertTrue("Insert route history harus berhasil", id > 0)

        val list = historyDao.getRouteHistory(testUserId)
        val found = list.firstOrNull { it.idHistory == id }
        assertNotNull("Route history yang diinsert harus ditemukan", found)
        assertEquals("Campur Semua", found?.mode)
        assertEquals(85, found?.score)
        assertEquals(1800, found?.estimatedTime)

        // Delete satu
        val deleted = historyDao.deleteRouteHistory(id)
        assertEquals(1, deleted)
        val afterDelete = historyDao.getRouteHistory(testUserId)
        assertNull(afterDelete.firstOrNull { it.idHistory == id })

        // Insert lagi lalu clear
        historyDao.insertRouteHistory(history)
        historyDao.clearRouteHistory(testUserId)
        val afterClear = historyDao.getRouteHistory(testUserId)
        assertTrue("Setelah clear, tidak boleh ada riwayat perjalanan user ini", afterClear.isEmpty())
    }
}
