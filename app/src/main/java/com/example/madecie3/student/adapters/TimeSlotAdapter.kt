package com.example.madecie3.student.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madecie3.databinding.ItemTimeSlotBinding
import com.example.madecie3.student.models.TimeSlot
import com.example.madecie3.R

class TimeSlotAdapter(
    private val onTimeSlotClick: (TimeSlot) -> Unit
) : ListAdapter<TimeSlot, TimeSlotAdapter.TimeSlotViewHolder>(TimeSlotDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val binding = ItemTimeSlotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimeSlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TimeSlotViewHolder(
        private val binding: ItemTimeSlotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(slot: TimeSlot) {
            binding.tvTime.text = slot.time
            binding.tvDuration.text = slot.duration

            val context = binding.root.context

            if (slot.isAvailable) {
                // 🟢 AVAILABLE SLOT
                if (slot.isSelected) {
                    // Selected state: Darker color (Indigo/Dark Blue)
                    binding.root.setCardBackgroundColor(context.getColor(R.color.ios_slate))
                    binding.tvTime.setTextColor(context.getColor(R.color.white))
                    binding.tvDuration.setTextColor(context.getColor(R.color.white))
                } else {
                    // Normal state: Light blue
                    binding.root.setCardBackgroundColor(context.getColor(R.color.ios_blue))
                    binding.tvTime.setTextColor(context.getColor(R.color.white))
                    binding.tvDuration.setTextColor(context.getColor(R.color.white))
                }

                binding.root.alpha = 1f
                binding.root.isClickable = true
                binding.root.setOnClickListener {
                    onTimeSlotClick(slot)
                }
            } else {
                // 🔴 BUSY SLOT
                binding.root.setCardBackgroundColor(context.getColor(R.color.ios_gray4))
                binding.tvTime.setTextColor(context.getColor(R.color.apple_secondary_label))
                binding.tvDuration.setTextColor(context.getColor(R.color.apple_secondary_label))
                binding.root.alpha = 0.5f
                binding.root.isClickable = false
                binding.root.setOnClickListener(null)
            }
        }
    }
}

class TimeSlotDiffCallback : DiffUtil.ItemCallback<TimeSlot>() {
    override fun areItemsTheSame(oldItem: TimeSlot, newItem: TimeSlot): Boolean {
        return oldItem.time == newItem.time
    }
    override fun areContentsTheSame(oldItem: TimeSlot, newItem: TimeSlot): Boolean {
        return oldItem == newItem
    }
}
