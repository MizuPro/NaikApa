package com.example.naikapa.data.model

data class User(
    val idUser: Long = 0,
    val nama: String,
    val email: String,
    val password: String,
    val hasMotor: Boolean,
    val hasCar: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
