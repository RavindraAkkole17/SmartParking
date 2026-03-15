package com.example.smartparking.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparking.R
import com.example.smartparking.model.BookingModel

class AdminBookingAdapter(
    private val list: List<BookingModel>
) : RecyclerView.Adapter<AdminBookingAdapter.BookingVH>() {

    inner class BookingVH(item: View) : RecyclerView.ViewHolder(item) {
        val tvInfo: TextView = item.findViewById(R.id.tvBookingInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_booking, parent, false)
        return BookingVH(v)
    }

    override fun onBindViewHolder(holder: BookingVH, pos: Int) {
        val b = list[pos]

        holder.tvInfo.text =
            "User: ${b.userId}\nParking: ${b.parkingId}\nSlot: ${b.slot}\nDate: ${b.date}\nAmount: ₹${b.amount}"
    }

    override fun getItemCount() = list.size
}
