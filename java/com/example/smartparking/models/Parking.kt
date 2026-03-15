package com.example.smartparking.ui.models

data class Parking(
    val id: String = "",
    val name: String = "",
    val owner: String = "",
    val slots_total: Int = 0,
    val slots_free: Int = 0
)
