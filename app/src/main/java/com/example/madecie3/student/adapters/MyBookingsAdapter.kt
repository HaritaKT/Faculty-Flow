package com.example.madecie3.student.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madecie3.databinding.ItemBookingCardBinding
import com.example.madecie3.student.models.Booking
import com.example.madecie3.R // Add this line

class MyBookingsAdapter : androidx.recyclerview.widget.ListAdapter<Booking, MyBookingsAdapter.BookingViewHolder>(BookingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookingViewHolder(private val binding: ItemBookingCardBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private var isExpanded = false
        
        fun bind(booking: Booking) {
            binding.tvFacultyName.text = booking.facultyName
            binding.tvBookingTime.text = "${booking.date}, ${booking.timeSlot}"
            binding.tvStudentNote.text = booking.studentNote

            // Set status badge
            binding.tvStatus.text =
                booking.status.replaceFirstChar { it.uppercaseChar() }
            
            // Set status color
            val (statusColor, statusBg) = when (booking.status) {
                "pending" -> Pair(binding.root.context.getColor(R.color.amber_500), R.drawable.rounded_button_secondary)
                "confirmed" -> Pair(binding.root.context.getColor(R.color.green_500), R.drawable.rounded_button_secondary)
                "declined" -> Pair(binding.root.context.getColor(R.color.ios_red), R.drawable.rounded_button_secondary)
                "done" -> Pair(binding.root.context.getColor(R.color.grey_500), R.drawable.rounded_button_secondary)
                else -> Pair(binding.root.context.getColor(R.color.amber_500), R.drawable.rounded_button_secondary)
            }
            binding.tvStatus.setTextColor(statusColor)

            // Set faculty reply if available
            if (booking.facultyReply.isNotBlank()) {
                binding.tvFacultyReply.text = booking.facultyReply
                binding.facultyReplySection.visibility = View.VISIBLE
            } else {
                binding.facultyReplySection.visibility = View.GONE
            }

            // Set expand/collapse functionality
            binding.ivExpand.setOnClickListener {
                isExpanded = !isExpanded
                toggleExpandedState()
            }

            // Set card click to expand/collapse
            binding.root.setOnClickListener {
                isExpanded = !isExpanded
                toggleExpandedState()
            }
        }

        private fun toggleExpandedState() {
            if (isExpanded) {
                binding.expandedSection.visibility = View.VISIBLE
                binding.ivExpand.rotation = 180f
            } else {
                binding.expandedSection.visibility = View.GONE
                binding.ivExpand.rotation = 0f
            }
        }
    }
}

class BookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
    override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean {
        return oldItem == newItem
    }
}
