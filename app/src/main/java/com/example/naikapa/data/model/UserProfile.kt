package com.example.naikapa.data.model

data class UserProfile(
    val idProfile: Long = 0,
    val idUser: Long,
    val defaultMode: String? = null,
    val defaultPriority: String? = null,
    val homeLat: Double? = null,
    val homeLon: Double? = null,
    val homeLabel: String? = null
)
