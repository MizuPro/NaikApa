package com.example.naikapa.presentation.route_detail

import com.example.naikapa.data.model.ScoredRoute
import com.example.naikapa.data.model.LocationPoint
import com.example.naikapa.data.model.SearchLocation

object RouteDetailSharedState {
    var selectedRoute: ScoredRoute? = null
    var origin: LocationPoint? = null
    var destination: SearchLocation? = null
    /** Mode transportasi yang dipilih user saat pencarian rute (e.g. "Campur Semua", "Motor"). */
    var selectedMode: String? = null
    /** Prioritas yang dipilih user saat pencarian rute (e.g. "Tercepat", "Terhemat"). */
    var selectedPriority: String? = null
}
