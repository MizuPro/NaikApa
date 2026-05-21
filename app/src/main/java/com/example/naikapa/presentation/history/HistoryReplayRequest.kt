package com.example.naikapa.presentation.history

import com.example.naikapa.data.model.LocationPoint
import com.example.naikapa.data.model.SearchLocation

/**
 * Shared request ringan untuk mengirim data favorit/riwayat pencarian
 * dari RiwayatFragment ke HomeFragment tanpa Safe Args.
 */
object HistoryReplayRequest {
    var pendingOrigin: LocationPoint? = null
    var pendingDestination: SearchLocation? = null
    var pendingMode: String? = null
    var pendingPriority: String? = null

    fun clear() {
        pendingOrigin = null
        pendingDestination = null
        pendingMode = null
        pendingPriority = null
    }

    fun hasPending(): Boolean = pendingDestination != null
}
