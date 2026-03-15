package com.example.smartparking.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartparking.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserDashboardActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        val parkingContainer = findViewById<LinearLayout>(R.id.parkingContainer)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val menuButton = findViewById<ImageView>(R.id.menuBtn)

        // Display username
        val userEmail = auth.currentUser?.email ?: "User"
        tvWelcome.text = "Welcome, $userEmail"

        // Logout Menu
        menuButton.setOnClickListener {
            val popup = PopupMenu(this, menuButton)
            popup.menuInflater.inflate(R.menu.user_dashboard_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.logout -> {
                        auth.signOut()
                        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // Fetch ALL parking cards
        db.collection("parkings").get()
            .addOnSuccessListener { snap ->

                parkingContainer.removeAllViews()

                for (doc in snap.documents) {

                    val name = doc.getString("name") ?: "-"
                    val owner = doc.getString("owner") ?: "-"
                    val totalSlots = doc.getLong("slots_total") ?: 0
                    val freeSlots = doc.getLong("slots_free") ?: 0

                    val card = layoutInflater.inflate(R.layout.parking_card, null)

                    // ❗❗ Correct IDs
                    val tvName = card.findViewById<TextView>(R.id.tvParkingName_card)
                    val tvOwner = card.findViewById<TextView>(R.id.tvOwner_card)
                    val tvSlots = card.findViewById<TextView>(R.id.tvSlots_card)

                    tvName.text = name
                    tvOwner.text = "Owner: $owner"
                    tvSlots.text = "Slots: $freeSlots / $totalSlots"

                    card.setOnClickListener {
                        val intent = Intent(this, ParkingDetailsActivity::class.java)
                        intent.putExtra("parkingId", doc.id)
                        startActivity(intent)
                    }

                    parkingContainer.addView(card)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
    }
}
