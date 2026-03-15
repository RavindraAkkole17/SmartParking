package com.example.smartparking.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparking.R
import com.example.smartparking.ui.admin.AdminBookingsActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var fusedLocation: FusedLocationProviderClient

    private lateinit var rvSlots: RecyclerView
    private lateinit var slotAdapter: SlotAdapter

    private val slotList = mutableListOf<String>()
    private val bookedSlots = mutableListOf<String>()
    private var liveSlotStatus = mutableMapOf<String, Boolean>()

    private var parkingId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        fusedLocation = LocationServices.getFusedLocationProviderClient(this)

        val etName = findViewById<EditText>(R.id.etParkingName)
        val etOwner = findViewById<EditText>(R.id.etOwner)
        val etSlots = findViewById<EditText>(R.id.etSlots)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnSetLocation = findViewById<Button>(R.id.btnSetLocation)
        val btnViewBookings = findViewById<Button>(R.id.btnViewBookings)

        // -------------------------------
        // SETUP RECYCLER VIEW (Admin slot map)
        // -------------------------------
        rvSlots = findViewById(R.id.rvSlots)
        rvSlots.layoutManager = GridLayoutManager(this, 5)

        // Fill sample slot names (you can load dynamically too)
        for (i in 1..10) slotList.add("S$i")

        slotAdapter = SlotAdapter(slotList, bookedSlots) { }
        rvSlots.adapter = slotAdapter

        // -------------------------------
        // SAVE PARKING DETAILS
        // -------------------------------
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            parkingId = name.lowercase()

            val owner = etOwner.text.toString().trim()
            val slots = etSlots.text.toString().trim()

            if (name.isEmpty() || owner.isEmpty() || slots.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val parkingData = hashMapOf(
                "name" to name,
                "owner" to owner,
                "slots_total" to slots.toInt(),
                "slots_free" to slots.toInt(),
                "lat" to 0.0,
                "lng" to 0.0
            )

            db.collection("parkings").document(parkingId)
                .set(parkingData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Parking details saved!", Toast.LENGTH_SHORT).show()

                    // Start listeners after parking exists
                    startBookedSlotListener()
                    startLiveObstacleListener()
                }
        }

        // -------------------------------
        // VIEW BOOKINGS
        // -------------------------------
        btnViewBookings.setOnClickListener {
            startActivity(Intent(this, AdminBookingsActivity::class.java))
        }

        // -------------------------------
        // UPDATE ADMIN LOCATION
        // -------------------------------
        btnSetLocation.setOnClickListener {
            requestAdminLocation()
        }

        // -------------------------------
        // LOGOUT
        // -------------------------------
        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // -----------------------------------------------------------
    // REAL-TIME BOOKED SLOTS (GREY)
    // -----------------------------------------------------------
    private fun startBookedSlotListener() {
        if (parkingId.isEmpty()) return

        db.collection("bookings")
            .whereEqualTo("parkingId", parkingId)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    bookedSlots.clear()

                    for (doc in snap.documents) {
                        val slot = doc.getString("slot")
                        if (slot != null) bookedSlots.add(slot)
                    }

                    slotAdapter.notifyDataSetChanged()
                }
            }
    }

    // -----------------------------------------------------------
    // REAL-TIME OBSTACLE STATUS (RED)
    // -----------------------------------------------------------
    private fun startLiveObstacleListener() {
        if (parkingId.isEmpty()) return

        db.collection("parkings")
            .document(parkingId)
            .collection("slotsLive")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val newMap = mutableMapOf<String, Boolean>()

                    for (doc in snap.documents) {
                        newMap[doc.id] = doc.getBoolean("occupied") ?: false
                    }

                    liveSlotStatus = newMap
                    slotAdapter.updateLiveStatus(liveSlotStatus)
                }
            }
    }

    // -----------------------------------------------------------
    // SAVE ADMIN LOCATION
    // -----------------------------------------------------------
    private fun requestAdminLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        fusedLocation.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                saveLocationToFirestore(location.latitude, location.longitude)
            }
        }
    }

    private fun saveLocationToFirestore(lat: Double, lng: Double) {
        if (parkingId.isEmpty()) {
            Toast.makeText(this, "Enter Parking Name first", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("parkings").document(parkingId)
            .update(mapOf("lat" to lat, "lng" to lng))
            .addOnSuccessListener {
                Toast.makeText(this, "Location Updated Successfully!", Toast.LENGTH_SHORT).show()
            }
    }
}
