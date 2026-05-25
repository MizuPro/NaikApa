package com.example.naikapa.data.repository

import com.example.naikapa.data.model.CombinedRouteResult
import com.example.naikapa.data.model.PrivateVehicleRouteResult
import com.example.naikapa.data.model.RecommendationResult
import com.example.naikapa.data.model.RouteCandidate
import com.example.naikapa.data.model.ScoredRoute
import com.example.naikapa.data.model.SortPreference
import com.example.naikapa.data.model.TransitRouteResult
import com.google.gson.Gson
import com.google.gson.JsonElement

class RouteCacheSerializer(
    private val gson: Gson = Gson()
) {
    fun serialize(result: RecommendationResult): String =
        gson.toJson(result.toDto())

    fun deserialize(json: String): RecommendationResult =
        gson.fromJson(json, RecommendationResultDto::class.java).toModel()

    private fun RecommendationResult.toDto(): RecommendationResultDto =
        RecommendationResultDto(
            main = main.toDto(),
            alternatives = alternatives.map { it.toDto() },
            sortPreference = sortPreference
        )

    private fun ScoredRoute.toDto(): ScoredRouteDto {
        val type = when (candidate) {
            is RouteCandidate.Transit -> CANDIDATE_TRANSIT
            is RouteCandidate.PrivateVehicle -> CANDIDATE_PRIVATE
            is RouteCandidate.Combined -> CANDIDATE_COMBINED
        }
        val payload = when (val routeCandidate = candidate) {
            is RouteCandidate.Transit -> gson.toJsonTree(routeCandidate.result)
            is RouteCandidate.PrivateVehicle -> gson.toJsonTree(routeCandidate.result)
            is RouteCandidate.Combined -> gson.toJsonTree(routeCandidate.result)
        }
        return ScoredRouteDto(
            candidateType = type,
            candidatePayload = payload,
            score = score,
            reason = reason,
            hasDisruptionWarning = hasDisruptionWarning,
            disruptionWarningText = disruptionWarningText,
            rankLabel = rankLabel
        )
    }

    private fun RecommendationResultDto.toModel(): RecommendationResult =
        RecommendationResult(
            main = main.toModel(),
            alternatives = alternatives.map { it.toModel() },
            sortPreference = sortPreference
        )

    private fun ScoredRouteDto.toModel(): ScoredRoute {
        val candidate = when (candidateType) {
            CANDIDATE_TRANSIT -> RouteCandidate.Transit(
                gson.fromJson(candidatePayload, TransitRouteResult::class.java)
            )
            CANDIDATE_PRIVATE -> RouteCandidate.PrivateVehicle(
                gson.fromJson(candidatePayload, PrivateVehicleRouteResult::class.java)
            )
            CANDIDATE_COMBINED -> RouteCandidate.Combined(
                gson.fromJson(candidatePayload, CombinedRouteResult::class.java)
            )
            else -> error("Unknown cached route candidate type: $candidateType")
        }
        return ScoredRoute(
            candidate = candidate,
            score = score,
            reason = reason,
            hasDisruptionWarning = hasDisruptionWarning,
            disruptionWarningText = disruptionWarningText,
            rankLabel = rankLabel
        )
    }

    private data class RecommendationResultDto(
        val main: ScoredRouteDto,
        val alternatives: List<ScoredRouteDto>,
        val sortPreference: SortPreference
    )

    private data class ScoredRouteDto(
        val candidateType: String,
        val candidatePayload: JsonElement,
        val score: Int,
        val reason: String,
        val hasDisruptionWarning: Boolean,
        val disruptionWarningText: String?,
        val rankLabel: String
    )

    private companion object {
        const val CANDIDATE_TRANSIT = "TRANSIT"
        const val CANDIDATE_PRIVATE = "PRIVATE"
        const val CANDIDATE_COMBINED = "COMBINED"
    }
}
