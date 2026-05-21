package com.example.naikapa.data.model

data class SavedTrip(
    val idSaved: Long = 0,
    val idUser: Long,
    val namaPerjalanan: String,
    val originName: String,
    val originLat: Double,
    val originLon: Double,
    val destinationName: String,
    val destinationLat: Double,
    val destinationLon: Double,
    val mode: String,
    val priority: String,
    val catatan: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
