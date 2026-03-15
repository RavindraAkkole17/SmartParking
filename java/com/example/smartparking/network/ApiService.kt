package com.example.smartparking.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Body

interface ApiService {

    @GET("slots")
    fun getSlots(): Call<List<SlotStatus>>

    @Headers("Content-Type: application/json")
    @POST("create_order")
    fun createOrder(@Body request: CreateOrderRequest): Call<CreateOrderResponse>

    @Headers("Content-Type: application/json")
    @POST("verify_payment")
    fun verifyPayment(@Body request: VerifyPaymentRequest): Call<VerifyPaymentResponse>
}
