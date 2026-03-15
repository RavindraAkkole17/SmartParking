package com.example.smartparking.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class CreateOrderRequest(val amount: Long, val currency: String = "INR", val receipt: String? = null)
data class CreateOrderResponse(val success: Boolean, val order: Map<String, Any>?)
data class VerifyPaymentRequest(val razorpay_order_id: String, val razorpay_payment_id: String, val razorpay_signature: String?)
data class VerifyPaymentResponse(val success: Boolean, val verified: Boolean)

interface ServerApi {
    @POST("createOrder")
    fun createOrder(@Body body: CreateOrderRequest): Call<CreateOrderResponse>

    @POST("verifyPayment")
    fun verifyPayment(@Body body: VerifyPaymentRequest): Call<VerifyPaymentResponse>
}
