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
import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitMode
import com.example.naikapa.data.model.User
import com.example.naikapa.data.model.UserProfile
import com.example.naikapa.data.repository.TransitGraphRepository
import com.example.naikapa.domain.routing.DijkstraAlgorithm
import com.example.naikapa.domain.routing.RouteStepBuilder
import com.example.naikapa.domain.routing.TransitRouteMetricsCalculator
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

    @Test
    fun prebuiltGtfsDatabaseCanBuildLimitedTransitGraph() {
        val gtfsDao = GtfsDao(dbHelper)
        val connections = gtfsDao.getAdjacentStopConnections(limit = 20)

        assertTrue(connections.isNotEmpty())
        connections.forEach { connection ->
            assertTrue(connection.fromStopId.isNotBlank())
            assertTrue(connection.toStopId.isNotBlank())
            assertTrue(connection.routeId.isNotBlank())
            assertTrue(connection.agencyId.isNotBlank())
        }

        val graph = TransitGraphRepository(gtfsDao).buildGraph(limitConnections = 20)
        assertTrue(graph.stats.nodeCount > 0)
        assertTrue(graph.stats.transitEdgeCount > 0)
        assertTrue(graph.stats.totalEdgeCount >= graph.stats.transitEdgeCount)

        val firstConnection = connections.first()
        val path = DijkstraAlgorithm().findPath(
            graph = graph,
            startStopId = firstConnection.fromStopId,
            endStopId = firstConnection.toStopId,
            mode = TransitMode.ALL,
            sortPreference = SortPreference.FASTEST
        )
        assertTrue(path?.isNotEmpty() == true)

        val steps = RouteStepBuilder().build(graph, path.orEmpty())
        val metrics = TransitRouteMetricsCalculator().calculate(path.orEmpty(), steps)
        assertTrue(metrics.estimatedFare >= 0)
        assertEquals(metrics.estimatedFare + metrics.estimatedBbm, metrics.estimatedTotalCost)
    }

    @Test
    fun disruptionReportCrudWithPhotoPathAndOwnershipWorks() {
        val userDao   = UserDao(dbHelper)
        val reportDao = DisruptionReportDao(dbHelper)

        // Buat dua user
        val userId1 = userDao.insertUser(
            User(nama = "User Satu", email = "user1@naikapa.local", password = "pass", hasMotor = false, hasCar = false)
        )
        val userId2 = userDao.insertUser(
            User(nama = "User Dua", email = "user2@naikapa.local", password = "pass", hasMotor = false, hasCar = false)
        )
        assertTrue(userId1 > 0)
        assertTrue(userId2 > 0)

        val now = System.currentTimeMillis()

        // Insert laporan aktif dengan photo path
        val reportId = reportDao.insert(
            DisruptionReport(
                idUser      = userId1,
                stopId      = "STOP_TEST",
                routeId     = "ROUTE_TEST",
                category    = "Keterlambatan",
                description = "Kereta terlambat 20 menit di stasiun ini.",
                photoPath   = "/data/local/tmp/report_test.jpg",
                createdAt   = now,
                expiredAt   = now + DisruptionReport.ONE_HOUR_MILLIS
            )
        )
        assertTrue("Insert laporan harus berhasil", reportId > 0)

        // getById
        val fetched = reportDao.getById(reportId)
        assertNotNull("getById harus mengembalikan laporan", fetched)
        assertEquals("STOP_TEST", fetched?.stopId)
        assertEquals("/data/local/tmp/report_test.jpg", fetched?.photoPath)

        // Laporan aktif muncul di getActiveReports
        val activeReports = reportDao.getActiveReports(now)
        assertTrue("Laporan aktif harus muncul", activeReports.any { it.idReport == reportId })

        // Laporan muncul di getByUser
        assertEquals(1, reportDao.getByUser(userId1).size)
        assertEquals(0, reportDao.getByUser(userId2).size)

        // getActiveByUser
        assertEquals(1, reportDao.getActiveByUser(userId1, now).size)

        // Update oleh user yang benar (ownership check)
        val updated = fetched!!.copy(description = "Deskripsi diperbarui oleh user yang benar.")
        assertEquals("updateByUser harus berhasil untuk owner", 1, reportDao.updateByUser(updated))

        // Update oleh user yang salah harus gagal
        val wrongOwnerUpdate = fetched.copy(idUser = userId2, description = "Coba update oleh user lain")
        assertEquals("updateByUser harus gagal untuk non-owner", 0, reportDao.updateByUser(wrongOwnerUpdate))

        // Laporan expired tidak muncul di getActiveReports
        val expiredReportId = reportDao.insert(
            DisruptionReport(
                idUser      = userId1,
                stopId      = "STOP_EXPIRED",
                routeId     = null,
                category    = "Lainnya",
                description = "Laporan yang sudah expired.",
                photoPath   = null,
                createdAt   = now - 2 * DisruptionReport.ONE_HOUR_MILLIS,
                expiredAt   = now - DisruptionReport.ONE_HOUR_MILLIS // sudah expired
            )
        )
        assertTrue(expiredReportId > 0)
        val activeAfterExpired = reportDao.getActiveReports(now)
        assertTrue(
            "Laporan expired tidak boleh muncul di active reports",
            activeAfterExpired.none { it.idReport == expiredReportId }
        )

        // markResolvedByUser oleh user yang salah harus gagal
        assertEquals("markResolvedByUser harus gagal untuk non-owner", 0, reportDao.markResolvedByUser(reportId, userId2))

        // deleteByUser oleh user yang salah harus gagal
        assertEquals("deleteByUser harus gagal untuk non-owner", 0, reportDao.deleteByUser(reportId, userId2))

        // deleteByUser oleh owner harus berhasil
        assertEquals("deleteByUser harus berhasil untuk owner", 1, reportDao.deleteByUser(reportId, userId1))

        // Cleanup
        reportDao.delete(expiredReportId)
        userDao.deleteUser(userId1)
        userDao.deleteUser(userId2)
    }
}
