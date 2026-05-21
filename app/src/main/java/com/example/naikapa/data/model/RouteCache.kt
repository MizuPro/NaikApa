package com.example.naikapa.data.model

data class RouteCache(
    val idCache: Long = 0,
    val originLat: Double,
    val originLon: Double,
    val destinationLat: Double,
    val destinationLon: Double,
    val mode: String,
    val priority: String,
    val resultJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
