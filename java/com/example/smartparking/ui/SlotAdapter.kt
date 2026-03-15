package com.example.smartparking.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparking.R

class SlotAdapter(
    private val slots: List<String>,
    private val bookedSlots: MutableList<String>,
    private val onSlotSelected: (String) -> Unit
) : RecyclerView.Adapter<SlotAdapter.SlotViewHolder>() {

    var selectedSlot: String? = null

    // slotId -> occupied (real-time motion detection)
    private var liveStatus: Map<String, Boolean> = emptyMap()

    /** 🔥 NEW — update Booked Slots */
    fun updateBookedSlots(newBooked: List<String>) {
        bookedSlots.clear()
        bookedSlots.addAll(newBooked)
        notifyDataSetChanged()
    }

    /** 🔥 Live status from camera */
    fun updateLiveStatus(statusMap: Map<String, Boolean>) {
        liveStatus = statusMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.slot_item, parent, false)
        return SlotViewHolder(view)
    }

    override fun getItemCount(): Int = slots.size

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slotName = slots[position]
        val context = holder.itemView.context
        holder.tvSlot.text = slotName

        val isOccupied = liveStatus[slotName] == true
        val isBooked = bookedSlots.contains(slotName)

        when {
            // 🔴 1. Occupied in live detection → RED
            isOccupied -> {
                holder.tvSlot.isEnabled = false
                holder.tvSlot.background =
                    ContextCompat.getDrawable(context, R.drawable.slot_occupied)
            }

            // ⚪ 2. Booked earlier → GREY
            isBooked -> {
                holder.tvSlot.isEnabled = false
                holder.tvSlot.background =
                    ContextCompat.getDrawable(context, R.drawable.slot_booked)
            }

            // 🟩 3. User selected → DARK GREEN
            selectedSlot == slotName -> {
                holder.tvSlot.isEnabled = true
                holder.tvSlot.background =
                    ContextCompat.getDrawable(context, R.drawable.slot_selected)
            }

            // 🟦 4. Free slot → LIGHT GREEN
            else -> {
                holder.tvSlot.isEnabled = true
                holder.tvSlot.background =
                    ContextCompat.getDrawable(context, R.drawable.slot_available)
            }
        }

        // Handle click only when slot is actually free
        holder.itemView.setOnClickListener {
            if (!isBooked && !isOccupied) {
                selectedSlot = slotName
                onSlotSelected(slotName)
                notifyDataSetChanged()
            }
        }
    }

    class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSlot: TextView = itemView.findViewById(R.id.tvSlot)
    }
}
