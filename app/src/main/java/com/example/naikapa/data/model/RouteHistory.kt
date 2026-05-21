package com.example.naikapa.data.model

data class RouteHistory(
    val idHistory: Long = 0,
    val idUser: Long,
    val originName: String,
    val destinationName: String,
    val mode: String,
    val priority: String,
    val recommendationSummary: String,
    val score: Int,
    val estimatedTime: Int,
    val estimatedCost: Int,
    val estimatedBbm: Int,
    val walkingDistance: Double,
    val transitCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)
