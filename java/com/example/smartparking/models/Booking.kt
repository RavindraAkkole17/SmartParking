package com.example.smartparking.models

data class Booking(
    var id: String = "",
    var parkingId: String = "",
    var parkingName: String = "",
    var userId: String = "",
    var userEmail: String = "",
    var slotNumber: String = "",
    var date: String = "",
    var amountPaise: Long = 0,
    var status: String = "pending",
    var razorpay_order_id: String? = null,
    var razorpay_payment_id: String? = null,
    var createdAt: Long = System.currentTimeMillis()
)
