package com.example.naikapa.data.model

data class SearchLocation(
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val source: String = SOURCE_TOMTOM
) {
    companion object {
        const val SOURCE_TOMTOM = "tomtom"
    }
}
