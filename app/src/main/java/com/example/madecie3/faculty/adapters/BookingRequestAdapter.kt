package com.example.madecie3.faculty.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madecie3.databinding.ItemBookingRequestBinding
import com.example.madecie3.faculty.models.BookingRequest
import com.example.madecie3.R

class BookingRequestAdapter(
    private val onAccept: (BookingRequest, String) -> Unit,
    private val onDecline: (BookingRequest, String) -> Unit
) : androidx.recyclerview.widget.ListAdapter<BookingRequest, BookingRequestAdapter.BookingViewHolder>(BookingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookingViewHolder(private val binding: ItemBookingRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private var isExpanded = false
        
        fun bind(bookingRequest: BookingRequest) {
            binding.tvStudentName.text = bookingRequest.studentName
            binding.tvRequestedTime.text = bookingRequest.requestedTime
            binding.tvNotePreview.text = if (bookingRequest.note.length > 50) {
                bookingRequest.note.take(50) + "..."
            } else {
                bookingRequest.note
            }
            binding.tvFullNote.text = bookingRequest.note

            // Set expand/collapse functionality
            binding.ivExpand.setOnClickListener {
                isExpanded = !isExpanded
                toggleExpandedState()
            }

            binding.root.setOnClickListener {
                isExpanded = !isExpanded
                toggleExpandedState()
            }

            binding.btnAccept.setOnClickListener {
                showReplyDialog(bookingRequest, true)
            }

            binding.btnDecline.setOnClickListener {
                showReplyDialog(bookingRequest, false)
            }
        }

        private fun showReplyDialog(bookingRequest: BookingRequest, isAccepting: Boolean) {
            val context = binding.root.context
            val builder = AlertDialog.Builder(context)
            builder.setTitle(if (isAccepting) "Accept Booking" else "Decline Booking")
            
            val container = FrameLayout(context)
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Use 20dp margin
            val margin = (20 * context.resources.displayMetrics.density).toInt()
            params.setMargins(margin, margin / 2, margin, 0)
            
            val input = EditText(context)
            input.hint = "Add an optional reply..."
            input.layoutParams = params
            container.addView(input)
            
            builder.setView(container)

            builder.setPositiveButton("Confirm") { _, _ ->
                val reply = input.text.toString().trim()
                if (isAccepting) {
                    onAccept(bookingRequest, reply)
                } else {
                    onDecline(bookingRequest, reply)
                }
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
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

class BookingDiffCallback : DiffUtil.ItemCallback<BookingRequest>() {
    override fun areItemsTheSame(oldItem: BookingRequest, newItem: BookingRequest): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BookingRequest, newItem: BookingRequest): Boolean {
        return oldItem == newItem
    }
}
