package com.example.smartparking.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartparking.R
import com.example.smartparking.models.Booking
import com.example.smartparking.network.CreateOrderRequest
import com.example.smartparking.network.RetrofitClient
import com.example.smartparking.network.VerifyPaymentRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookingActivity : AppCompatActivity(), com.razorpay.PaymentResultListener {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var parkingId: String
    private lateinit var parkingName: String
    private lateinit var slotNumber: String
    private lateinit var dateStr: String
    private lateinit var btnPay: Button
    private lateinit var tvSummary: TextView

    private val amountPaise = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        parkingId = intent.getStringExtra("parkingId")!!
        parkingName = intent.getStringExtra("parkingName")!!
        slotNumber = intent.getStringExtra("slotNumber")!!
        dateStr = intent.getStringExtra("date")!!

        tvSummary = findViewById(R.id.tvSummary)
        btnPay = findViewById(R.id.btnPay)

        tvSummary.text = "Parking: $parkingName\nSlot: $slotNumber\nDate: $dateStr\nAmount: ₹20"

        btnPay.setOnClickListener { createOrderAndOpenCheckout() }
    }

    private fun createOrderAndOpenCheckout() {
        val req = CreateOrderRequest(amount = amountPaise)
        RetrofitClient.api.createOrder(req).enqueue(object: Callback<com.example.smartparking.network.CreateOrderResponse> {
            override fun onResponse(call: Call<com.example.smartparking.network.CreateOrderResponse>, response: Response<com.example.smartparking.network.CreateOrderResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val order = response.body()!!.order!!
                    val orderId = order["id"] as String
                    // save orderId for verification after payment
                    getSharedPreferences("sp", MODE_PRIVATE).edit().putString("last_order_id", orderId).apply()
                    openRazorpayCheckout(orderId, amountPaise)
                } else {
                    Toast.makeText(this@BookingActivity, "Failed to create order", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<com.example.smartparking.network.CreateOrderResponse>, t: Throwable) {
                Toast.makeText(this@BookingActivity, "Server error: ${'$'}{t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun openRazorpayCheckout(orderId: String, amount: Long) {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_R7ap4DMe5mgryT")
        try {
            val options = JSONObject()
            options.put("name", "SmartParking")
            options.put("description", "Slot Booking")
            options.put("order_id", orderId)
            options.put("currency", "INR")
            options.put("amount", amount.toString())
            val prefill = JSONObject()
            prefill.put("email", auth.currentUser?.email)
            options.put("prefill", prefill)
            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String) {
        val orderId = getSharedPreferences("sp", MODE_PRIVATE).getString("last_order_id", "") ?: ""
        val verReq = VerifyPaymentRequest(orderId, razorpayPaymentId, null)
        RetrofitClient.api.verifyPayment(verReq).enqueue(object: Callback<com.example.smartparking.network.VerifyPaymentResponse> {
            override fun onResponse(call: Call<com.example.smartparking.network.VerifyPaymentResponse>, response: Response<com.example.smartparking.network.VerifyPaymentResponse>) {
                val verified = response.body()?.verified ?: false
                if (verified) saveBooking(razorpayPaymentId, orderId, "confirmed") else {
                    saveBooking(razorpayPaymentId, orderId, "pending")
                    Toast.makeText(this@BookingActivity, "Payment verification failed", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<com.example.smartparking.network.VerifyPaymentResponse>, t: Throwable) {
                Toast.makeText(this@BookingActivity, "Verification error: ${'$'}{t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment failed: ${'$'}response", Toast.LENGTH_LONG).show()
    }

    private fun saveBooking(razorpayPaymentId: String, orderId: String?, status: String) {
        val b = Booking(
            parkingId = parkingId,
            parkingName = parkingName,
            userId = auth.currentUser!!.uid,
            userEmail = auth.currentUser!!.email ?: "",
            slotNumber = slotNumber,
            date = dateStr,
            amountPaise = amountPaise,
            status = status,
            razorpay_order_id = orderId,
            razorpay_payment_id = razorpayPaymentId
        )
        val docRef = db.collection("bookings").document()
        b.id = docRef.id
        docRef.set(b).addOnSuccessListener {
            Toast.makeText(this, "Booking saved: ${'$'}{b.id}", Toast.LENGTH_LONG).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Booking save failed: ${'$'}{it.message}", Toast.LENGTH_LONG).show()
        }
    }
}
