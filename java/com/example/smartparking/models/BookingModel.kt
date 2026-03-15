package com.example.smartparking.model

data class BookingModel(
    val userId: String = "",
    val slot: String = "",
    val date: String = "",
    val amount: Long = 0,
    val parkingId: String = ""
)
