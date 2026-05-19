package com.example.madecie3.student.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madecie3.databinding.ItemFacultyCardBinding
import com.example.madecie3.student.models.Faculty
import com.example.madecie3.R // Add this line

class FacultyAdapter(
    private val onFacultyClick: (Faculty) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Faculty, FacultyAdapter.FacultyViewHolder>(FacultyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FacultyViewHolder {
        val binding = ItemFacultyCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FacultyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FacultyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FacultyViewHolder(private val binding: ItemFacultyCardBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(faculty: Faculty) {
            binding.tvFacultyName.text = faculty.name
            binding.tvFacultyDesignation.text = faculty.designation
            binding.tvFacultyDepartment.text = faculty.department

            // Set availability indicator color
            val availabilityRes = when (faculty.availability) {
                "green" -> R.drawable.availability_green
                "amber" -> R.drawable.availability_amber
                "grey" -> R.drawable.availability_grey
                else -> R.drawable.availability_green
            }
            binding.availabilityIndicator.setBackgroundResource(availabilityRes)

            // Set click listener
            binding.root.setOnClickListener {
                onFacultyClick(faculty)
            }
        }
    }
}

class FacultyDiffCallback : DiffUtil.ItemCallback<Faculty>() {
    override fun areItemsTheSame(oldItem: Faculty, newItem: Faculty): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Faculty, newItem: Faculty): Boolean {
        return oldItem == newItem
    }
}
