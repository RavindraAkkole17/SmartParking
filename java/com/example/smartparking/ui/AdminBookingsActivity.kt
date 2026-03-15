package com.example.smartparking.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparking.R
import com.example.smartparking.model.BookingModel
import com.google.firebase.firestore.FirebaseFirestore

class AdminBookingsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rvList: RecyclerView
    private lateinit var adapter: AdminBookingAdapter

    private val bookingList = mutableListOf<BookingModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_bookings)

        rvList = findViewById(R.id.rvAdminBookings)
        rvList.layoutManager = LinearLayoutManager(this)

        adapter = AdminBookingAdapter(bookingList)
        rvList.adapter = adapter

        loadAllBookings()
    }

    private fun loadAllBookings() {
        db.collection("parkings").get().addOnSuccessListener { parkingDocs ->

            for (parking in parkingDocs) {
                val parkingId = parking.id

                db.collection("parkings")
                    .document(parkingId)
                    .collection("bookings")
                    .get()
                    .addOnSuccessListener { dateDocs ->

                        for (dateDoc in dateDocs) {

                            val date = dateDoc.id

                            db.collection("parkings")
                                .document(parkingId)
                                .collection("bookings")
                                .document(date)
                                .collection("orders")
                                .get()
                                .addOnSuccessListener { orders ->

                                    for (order in orders) {
                                        val m = order.toObject(BookingModel::class.java)
                                        bookingList.add(m.copy(parkingId = parkingId))
                                    }

                                    adapter.notifyDataSetChanged()
                                }
                        }
                    }
            }

        }.addOnFailureListener {
            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
