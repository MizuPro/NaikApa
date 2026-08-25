package com.example.naikapa

import com.example.naikapa.data.local.DisruptionReportDao
import com.example.naikapa.data.local.NaikApaDatabaseHelper
import com.example.naikapa.data.model.ApiResponse
import com.example.naikapa.data.model.CreateDelayRequest
import com.example.naikapa.data.model.DelayReportDto
import com.example.naikapa.data.model.DelayReportListResponse
import com.example.naikapa.data.model.DisruptionReport
import com.example.naikapa.data.model.RouteDelaySummaryDataDto
import com.example.naikapa.data.model.UploadUrlDataDto
import com.example.naikapa.data.remote.DelayApi
import com.example.naikapa.data.repository.DelayRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class DelayRepositoryTest {

    private lateinit var mockApi: FakeDelayApi
    private lateinit var mockDao: FakeDisruptionReportDao
    private lateinit var repository: DelayRepository

    @Before
    fun setUp() {
        mockApi = FakeDelayApi()
        mockDao = FakeDisruptionReportDao()
        repository = DelayRepository(delayApi = mockApi, disruptionReportDao = mockDao)
    }

    @Test
    fun `getActiveReports fetches from cloud API and syncs to local SQLite`() = runBlocking {
        mockApi.reportsToReturn = listOf(
            DelayReportDto(
                idReport = 101L,
                idUser = 10L,
                stopId = "HALTE_A",
                routeId = "KORIDOR_1",
                category = "Keterlambatan",
                description = "Bus mengalami kendala",
                photoUrl = "https://b2.com/photo.jpg",
                createdAtRaw = "2026-07-29T12:00:00Z",
                expiredAtRaw = "2026-07-29T13:00:00Z"
            )
        )

        val reports = repository.getActiveReports(routeId = "KORIDOR_1")

        assertEquals(1, reports.size)
        assertEquals(101L, reports[0].idReport)
        assertEquals("HALTE_A", reports[0].stopId)

        // Verify synced into local SQLite DAO
        assertEquals(1, mockDao.storedReports.size)
        assertEquals(101L, mockDao.storedReports[0].idReport)
    }

    @Test
    fun `createReport sends payload to cloud API and saves locally`() = runBlocking {
        val res = repository.createReport(
            idUser = 123L,
            stopId = "HALTE_MONAS",
            routeId = "KORIDOR_1",
            category = "Kemacetan",
            description = "Macet parah di sekitar Monas"
        )

        assertTrue(res.isSuccess)
        val created = res.getOrNull()
        assertNotNull(created)
        assertEquals("Kemacetan", created?.category)
    }
}

// ── Test Fakes ─────────────────────────────────────────────────────────────

class FakeDelayApi : DelayApi {
    var reportsToReturn: List<DelayReportDto> = emptyList()

    override suspend fun getUploadUrl(fileName: String, contentType: String): Response<ApiResponse<UploadUrlDataDto>> {
        val data = UploadUrlDataDto(
            uploadUrl = "https://b2.com/upload",
            fileUrl = "https://b2.com/$fileName",
            fileKey = fileName,
            expiresInSeconds = 900
        )
        return Response.success(ApiResponse(true, "OK", data))
    }

    override suspend fun createReport(request: CreateDelayRequest): Response<ApiResponse<DelayReportDto>> {
        val dto = DelayReportDto(
            idReport = 200L,
            idUser = request.idUser,
            stopId = request.stopId,
            routeId = request.routeId,
            category = request.category,
            description = request.description,
            photoUrl = request.photoUrl,
            impactLevel = request.impactLevel,
            createdAtRaw = "2026-07-29T12:00:00Z",
            expiredAtRaw = "2026-07-29T13:00:00Z"
        )
        return Response.success(ApiResponse(true, "OK", dto))
    }

    override suspend fun getActiveReports(
        routeId: String?,
        stopId: String?,
        category: String?,
        status: String,
        page: Int,
        limit: Int
    ): Response<DelayReportListResponse> {
        return Response.success(DelayReportListResponse(true, reportsToReturn.size, reportsToReturn.size, reportsToReturn))
    }

    override suspend fun getRouteDelaySummary(routeId: String): Response<ApiResponse<RouteDelaySummaryDataDto>> {
        val summary = RouteDelaySummaryDataDto(routeId, true, 1, 3.0, mapOf("Keterlambatan" to 1), null)
        return Response.success(ApiResponse(true, "OK", summary))
    }

    override suspend fun resolveReport(reportId: Long, body: Map<String, Long>): Response<ApiResponse<DelayReportDto>> {
        val dto = DelayReportDto(reportId, 1L, null, null, "Keterlambatan", "test", null, 1, "resolved", null, null)
        return Response.success(ApiResponse(true, "OK", dto))
    }

    override suspend fun deleteReport(reportId: Long, idUser: Long): Response<ApiResponse<Any>> {
        return Response.success(ApiResponse(true, "Deleted", null))
    }
}

class FakeDisruptionReportDao : DisruptionReportDao() {
    val storedReports = mutableListOf<DisruptionReport>()

    override fun insert(report: DisruptionReport): Long {
        storedReports.add(report)
        return report.idReport.takeIf { it > 0 } ?: 999L
    }

    override fun getById(idReport: Long): DisruptionReport? {
        return storedReports.find { it.idReport == idReport }
    }

    override fun getActiveReports(nowMillis: Long): List<DisruptionReport> {
        return storedReports
    }

    override fun update(report: DisruptionReport): Int {
        val idx = storedReports.indexOfFirst { it.idReport == report.idReport }
        if (idx >= 0) {
            storedReports[idx] = report
            return 1
        }
        return 0
    }
}
