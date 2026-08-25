package com.example.naikapa.data.remote

import com.example.naikapa.data.model.ApiResponse
import com.example.naikapa.data.model.CreateDelayRequest
import com.example.naikapa.data.model.DelayReportDto
import com.example.naikapa.data.model.DelayReportListResponse
import com.example.naikapa.data.model.RouteDelaySummaryDataDto
import com.example.naikapa.data.model.UploadUrlDataDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DelayApi {

    @GET("api/v1/delays/upload-url")
    suspend fun getUploadUrl(
        @Query("fileName") fileName: String,
        @Query("contentType") contentType: String
    ): Response<ApiResponse<UploadUrlDataDto>>

    @POST("api/v1/delays")
    suspend fun createReport(
        @Body request: CreateDelayRequest
    ): Response<ApiResponse<DelayReportDto>>

    @GET("api/v1/delays")
    suspend fun getActiveReports(
        @Query("route_id") routeId: String? = null,
        @Query("stop_id") stopId: String? = null,
        @Query("category") category: String? = null,
        @Query("status") status: String = "active",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<DelayReportListResponse>

    @GET("api/v1/delays/routes/{routeId}")
    suspend fun getRouteDelaySummary(
        @Path("routeId") routeId: String
    ): Response<ApiResponse<RouteDelaySummaryDataDto>>

    @PATCH("api/v1/delays/{id}/resolve")
    suspend fun resolveReport(
        @Path("id") reportId: Long,
        @Body body: Map<String, Long> = emptyMap()
    ): Response<ApiResponse<DelayReportDto>>

    @DELETE("api/v1/delays/{id}")
    suspend fun deleteReport(
        @Path("id") reportId: Long,
        @Query("id_user") idUser: Long
    ): Response<ApiResponse<Any>>
}
