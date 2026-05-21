package com.example.naikapa.data.model

data class SearchHistory(
    val idSearch: Long = 0,
    val idUser: Long,
    val keyword: String,
    val selectedName: String,
    val selectedAddress: String?,
    val selectedLat: Double,
    val selectedLon: Double,
    val searchedAt: Long = System.currentTimeMillis()
)
