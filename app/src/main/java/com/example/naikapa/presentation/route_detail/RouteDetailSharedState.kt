package com.example.naikapa.presentation.route_detail

import com.example.naikapa.data.model.ScoredRoute
import com.example.naikapa.data.model.LocationPoint
import com.example.naikapa.data.model.SearchLocation

object RouteDetailSharedState {
    var selectedRoute: ScoredRoute? = null
    var origin: LocationPoint? = null
    var destination: SearchLocation? = null
}
