package com.example.madecie3.faculty.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madecie3.R
import com.example.madecie3.databinding.ItemScheduleBinding
import com.example.madecie3.faculty.models.ScheduleItem

class ScheduleAdapter :
    ListAdapter<ScheduleItem, ScheduleAdapter.ScheduleViewHolder>(ScheduleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ScheduleViewHolder(private val binding: ItemScheduleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScheduleItem) {
            val ctx = binding.root.context

            binding.tvStartTime.text = item.startTime
            binding.tvEndTime.text   = item.endTime

            binding.tvClassName.text = item.className
                .replace(Regex("(?i)\\bSubject\\s*"), "")
                .trim()
                .ifEmpty { "Busy Period" }

            val roomValue = item.classRoom.trim()
            if (roomValue.isBlank() || roomValue.equals("N/A", ignoreCase = true)) {
                binding.tvClassRoom.visibility = View.GONE
            } else {
                binding.tvClassRoom.visibility = View.VISIBLE
                binding.tvClassRoom.text       = roomValue
            }

            binding.tvStatus.text = item.status.uppercase()

            val (drawableRes, tint) = when (item.statusColor) {
                "blue"  -> Pair(R.drawable.rounded_button,     ctx.getColor(R.color.ios_blue))
                "amber" -> Pair(R.drawable.availability_amber, ctx.getColor(R.color.amber_500))
                "green" -> Pair(R.drawable.availability_green, ctx.getColor(R.color.ios_green))
                "grey"  -> Pair(R.drawable.availability_grey,  ctx.getColor(R.color.ios_gray))
                else    -> Pair(R.drawable.rounded_button,     ctx.getColor(R.color.ios_blue))
            }

            binding.statusIndicator.setBackgroundResource(drawableRes)
            binding.statusIndicator.backgroundTintList = ColorStateList.valueOf(tint)
            binding.tvStatus.setTextColor(tint)
        }
    }
}

class ScheduleDiffCallback : DiffUtil.ItemCallback<ScheduleItem>() {
    override fun areItemsTheSame(oldItem: ScheduleItem, newItem: ScheduleItem) =
        oldItem.startTime == newItem.startTime && oldItem.day == newItem.day

    override fun areContentsTheSame(oldItem: ScheduleItem, newItem: ScheduleItem) =
        oldItem == newItem
}