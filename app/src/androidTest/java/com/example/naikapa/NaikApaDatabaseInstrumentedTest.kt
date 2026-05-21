package com.example.naikapa

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.local.DisruptionReportDao
import com.example.naikapa.data.local.GtfsDao
import com.example.naikapa.data.local.HistoryDao
import com.example.naikapa.data.local.NaikApaDatabaseHelper
import com.example.naikapa.data.local.RouteCacheDao
import com.example.naikapa.data.local.SavedTripDao
import com.example.naikapa.data.local.UserDao
import com.example.naikapa.data.model.DisruptionReport
import com.example.naikapa.data.model.GtfsStop
import com.example.naikapa.data.model.RouteCache
import com.example.naikapa.data.model.RouteHistory
import com.example.naikapa.data.model.SavedTrip
import com.example.naikapa.data.model.SearchHistory
import com.example.naikapa.data.model.User
import com.example.naikapa.data.model.UserProfile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NaikApaDatabaseInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var dbHelper: NaikApaDatabaseHelper

    @Before
    fun setUp() {
        context.deleteDatabase(AppConstants.DATABASE_NAME)
        dbHelper = NaikApaDatabaseHelper(context)
        dbHelper.writableDatabase
    }

    @After
    fun tearDown() {
        dbHelper.close()
        context.deleteDatabase(AppConstants.DATABASE_NAME)
    }

    @Test
    fun userProfileFavoriteHistoryReportCacheAndGtfsCrudWorks() {
        val userDao = UserDao(dbHelper)
        val savedTripDao = SavedTripDao(dbHelper)
        val historyDao = HistoryDao(dbHelper)
        val reportDao = DisruptionReportDao(dbHelper)
        val cacheDao = RouteCacheDao(dbHelper)
        val gtfsDao = GtfsDao(dbHelper)

        val userId = userDao.insertUser(
            User(
                nama = "Tester NaikApa",
                email = "tester@naikapa.local",
                password = "secret",
                hasMotor = true,
                hasCar = false
            )
        )
        assertTrue(userId > 0)
        assertTrue(userDao.isEmailExists("tester@naikapa.local"))
        assertTrue(userDao.isEmailExists("TESTER@naikapa.local"))
        assertNotNull(userDao.login("tester@naikapa.local", "secret"))
        assertNotNull(userDao.login("TESTER@naikapa.local", "secret"))

        userDao.upsertProfile(
            UserProfile(
                idUser = userId,
                defaultMode = "all",
                defaultPriority = "fastest",
                homeLat = -6.2,
                homeLon = 106.8,
                homeLabel = "Rumah"
            )
        )
        assertEquals("Rumah", userDao.getProfile(userId)?.homeLabel)

        val otherUserId = userDao.insertUser(
            User(
                nama = "Akun Lain",
                email = "other@naikapa.local",
                password = "secret",
                hasMotor = false,
                hasCar = true
            )
        )
        assertTrue(otherUserId > 0)
        assertTrue(userDao.isEmailUsedByOtherUser("other@naikapa.local", userId))
        assertEquals(1, userDao.updateUser(userDao.getUserById(userId)!!.copy(nama = "Tester Update", hasCar = true)))
        assertEquals("Tester Update", userDao.getUserById(userId)?.nama)
        assertEquals(true, userDao.getUserById(userId)?.hasCar)

        val savedId = savedTripDao.insert(
            SavedTrip(
                idUser = userId,
                namaPerjalanan = "Kampus",
                originName = "Rumah",
                originLat = -6.2,
                originLon = 106.8,
                destinationName = "Kampus",
                destinationLat = -6.3,
                destinationLon = 106.9,
                mode = "all",
                priority = "fastest"
            )
        )
        assertTrue(savedId > 0)
        assertEquals(1, savedTripDao.getByUser(userId).size)
        assertEquals(1, savedTripDao.updateNameAndNote(savedId, "Kampus Pagi", "Jam sibuk"))

        val searchId = historyDao.insertSearchHistory(
            SearchHistory(
                idUser = userId,
                keyword = "Dukuh Atas",
                selectedName = "Dukuh Atas BNI",
                selectedAddress = "Jakarta",
                selectedLat = -6.2,
                selectedLon = 106.82
            )
        )
        val routeHistoryId = historyDao.insertRouteHistory(
            RouteHistory(
                idUser = userId,
                originName = "Rumah",
                destinationName = "Kampus",
                mode = "all",
                priority = "fastest",
                recommendationSummary = "Naik KRL",
                score = 90,
                estimatedTime = 1800,
                estimatedCost = 3500,
                estimatedBbm = 0,
                walkingDistance = 0.5,
                transitCount = 1
            )
        )
        assertTrue(searchId > 0)
        assertTrue(routeHistoryId > 0)
        assertEquals(1, historyDao.getSearchHistory(userId).size)
        assertEquals(1, historyDao.getRouteHistory(userId).size)

        val reportId = reportDao.insert(
            DisruptionReport(
                idUser = userId,
                stopId = "STOP_DKA",
                routeId = null,
                category = "Antrean panjang",
                description = "Antrean tap in panjang",
                photoPath = "/tmp/report.jpg"
            )
        )
        assertTrue(reportId > 0)
        assertEquals(1, reportDao.getActiveReports().size)
        assertEquals(1, reportDao.markResolved(reportId))
        assertEquals(0, reportDao.getActiveReports().size)

        val cacheId = cacheDao.insert(
            RouteCache(
                originLat = -6.2,
                originLon = 106.8,
                destinationLat = -6.3,
                destinationLon = 106.9,
                mode = "motor",
                priority = "fastest",
                resultJson = """{"ok":true}"""
            )
        )
        assertTrue(cacheId > 0)
        assertEquals("""{"ok":true}""", cacheDao.find(-6.2, 106.8, -6.3, 106.9, "motor", "fastest")?.resultJson)

        gtfsDao.insertStop(
            GtfsStop(
                stopId = "STOP_DKA",
                stopName = "Dukuh Atas BNI",
                stopLat = -6.2,
                stopLon = 106.82,
                agencyId = "Tije",
                stopType = "halte"
            )
        )
        assertTrue(gtfsDao.searchStops("Dukuh", "Tije").isNotEmpty())
        assertEquals("Dukuh Atas BNI", gtfsDao.getStop("STOP_DKA")?.stopName)

        assertEquals(1, savedTripDao.delete(savedId))
        assertEquals(1, historyDao.deleteSearchHistory(searchId))
        assertEquals(1, historyDao.deleteRouteHistory(routeHistoryId))
        assertEquals(1, reportDao.delete(reportId))
        assertEquals(1, userDao.deleteUser(userId))
        assertEquals(1, userDao.deleteUser(otherUserId))
    }

    @Test
    fun prebuiltGtfsDatabaseCanBeReadAndSearched() {
        val gtfsDao = GtfsDao(dbHelper)

        assertTrue(gtfsDao.hasGtfsData())
        gtfsDao.getGtfsTableCounts().forEach { tableCount ->
            assertTrue("${tableCount.tableName} should not be empty", tableCount.totalRows > 0)
        }
        assertTrue(gtfsDao.getStopCountsByAgency().isNotEmpty())

        val dukuhResults = gtfsDao.searchStops("Dukuh", limit = 10)
        val tangerangResults = gtfsDao.searchStops("Tangerang", limit = 10)

        assertTrue(dukuhResults.isNotEmpty())
        assertTrue(tangerangResults.isNotEmpty())
    }
}
