package com.example.naikapa.data.repository

import com.example.naikapa.common.AppConstants
import com.example.naikapa.data.local.GtfsDao
import com.example.naikapa.data.model.GtfsStop
import com.example.naikapa.data.model.SearchLocation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Repository pencarian stop lokal berbasis database GTFS.
 * Bekerja sepenuhnya offline tanpa API key eksternal.
 */
class GtfsStopSearchRepository(private val gtfsDao: GtfsDao) {

    /**
     * Cari stop GTFS berdasarkan keyword.
     *
     * @param keyword       kata kunci pencarian (minimal [AppConstants.GTFS_MIN_QUERY_LENGTH] karakter)
     * @param userLat       latitude user untuk perhitungan jarak (null = tidak dihitung)
     * @param userLon       longitude user untuk perhitungan jarak (null = tidak dihitung)
     * @param agencyFilter  filter agency ("tj", "krl", "mrt", "lrt"), null = semua agency
     * @return list [SearchLocation] yang sudah diurutkan (relevansi keyword, lalu jarak)
     */
    fun search(
        keyword: String,
        userLat: Double?,
        userLon: Double?,
        agencyFilter: String? = null
    ): List<SearchLocation> {
        val trimmed = keyword.trim()
        if (trimmed.length < AppConstants.GTFS_MIN_QUERY_LENGTH) return emptyList()

        val rawStops = gtfsDao.searchStops(
            keyword = trimmed,
            agencyId = agencyFilter,
            limit = AppConstants.GTFS_SEARCH_LIMIT * 3   // ambil lebih banyak, sorting di Kotlin
        )

        return rawStops
            .map { stop -> stop.toSearchLocation(trimmed, userLat, userLon) }
            .sortedWith(compareBy(
                // Prioritas 1: nama stop dimulai dengan keyword (lebih relevan)
                { !it.name.startsWith(trimmed, ignoreCase = true) },
                // Prioritas 2: jarak dari user (null → paling akhir)
                { it.distanceMeters ?: Double.MAX_VALUE },
                // Prioritas 3: nama alfabet
                { it.name }
            ))
            .take(AppConstants.GTFS_SEARCH_LIMIT)
    }

    // ---- Private helpers ----

    private fun GtfsStop.toSearchLocation(
        keyword: String,
        userLat: Double?,
        userLon: Double?
    ): SearchLocation {
        val distanceMeters = if (userLat != null && userLon != null) {
            haversineMeters(userLat, userLon, stopLat, stopLon)
        } else null

        val agencyLabel = agencyIdToLabel(agencyId)
        val address = buildAddress(agencyLabel, distanceMeters)

        return SearchLocation(
            name = stopName,
            address = address,
            latitude = stopLat,
            longitude = stopLon,
            source = SearchLocation.SOURCE_GTFS,
            stopId = stopId,
            agencyId = agencyId,
            stopType = stopType,
            distanceMeters = distanceMeters
        )
    }

    private fun buildAddress(agencyLabel: String, distanceMeters: Double?): String {
        return if (distanceMeters != null) {
            val distanceStr = if (distanceMeters < 1000) {
                "${distanceMeters.toInt()} m"
            } else {
                String.format("%.1f km", distanceMeters / 1000.0)
            }
            "$agencyLabel · $distanceStr"
        } else {
            agencyLabel
        }
    }

    /**
     * Hitung jarak dua koordinat menggunakan formula Haversine (dalam meter).
     */
    internal fun haversineMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadiusM = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusM * c
    }

    companion object {
        /**
         * Konversi agencyId GTFS ke label yang ramah UI.
         */
        fun agencyIdToLabel(agencyId: String?): String = when (agencyId?.lowercase()) {
            "tj"  -> "TransJakarta"
            "krl" -> "KRL Commuter Line"
            "mrt" -> "MRT Jakarta"
            "lrt" -> "LRT Jakarta"
            else  -> agencyId?.uppercase() ?: "Halte / Stasiun"
        }
    }
}
