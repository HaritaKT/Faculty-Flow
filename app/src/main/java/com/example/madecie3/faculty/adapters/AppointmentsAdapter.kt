package com.example.madecie3.faculty.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madecie3.databinding.ItemAppointmentCardBinding
import com.example.madecie3.faculty.models.BookingRequest

class AppointmentsAdapter : ListAdapter<BookingRequest, AppointmentsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppointmentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemAppointmentCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(appointment: BookingRequest) {
            binding.tvStudentName.text = appointment.studentName
            binding.tvDateTime.text = "${appointment.date} • ${appointment.timeSlot}"
            binding.tvNote.text = "Reason: ${appointment.note}"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BookingRequest>() {
        override fun areItemsTheSame(oldItem: BookingRequest, newItem: BookingRequest) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BookingRequest, newItem: BookingRequest) = oldItem == newItem
    }
}
