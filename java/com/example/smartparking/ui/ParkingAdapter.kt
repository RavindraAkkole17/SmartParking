package com.example.smartparking.ui.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparking.R
import com.example.smartparking.ui.ParkingDetailsActivity
import com.example.smartparking.ui.models.Parking
import kotlin.random.Random

class ParkingAdapter(
    private val parkingList: List<Parking>
) : RecyclerView.Adapter<ParkingAdapter.ParkingViewHolder>() {

    // List of backgrounds (make sure these XML files exist in drawable)
    private val bgList = listOf(
        R.drawable.card_bg_car1,
        R.drawable.card_bg_car2,
        R.drawable.card_bg_car3,
        R.drawable.card_bg_car4,
        R.drawable.card_bg_car5,
        R.drawable.card_bg_car6
    )

    class ParkingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvParkingName_card)
        val tvOwner: TextView = itemView.findViewById(R.id.tvOwner_card)
        val tvSlots: TextView = itemView.findViewById(R.id.tvSlots_card)
        val rootLayout: View = itemView.findViewById(R.id.cardRoot) // IMPORTANT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.parking_card, parent, false)
        return ParkingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParkingViewHolder, position: Int) {
        val parking = parkingList[position]

        // Set text
        holder.tvName.text = parking.name
        holder.tvOwner.text = "Owner: ${parking.owner}"
        holder.tvSlots.text = "Total Slots: ${parking.slots_total}"

        // Apply random background to card
        val randomBg = bgList[Random.nextInt(bgList.size)]
        holder.rootLayout.setBackgroundResource(randomBg)

        // Click listener → open details
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ParkingDetailsActivity::class.java)
            intent.putExtra("parkingId", parking.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return parkingList.size
    }
}
