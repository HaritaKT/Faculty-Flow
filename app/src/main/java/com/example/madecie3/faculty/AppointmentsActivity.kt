package com.example.madecie3.faculty

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.madecie3.databinding.ActivityAppointmentsBinding
import com.example.madecie3.faculty.adapters.AppointmentsAdapter
import com.example.madecie3.faculty.models.BookingRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class AppointmentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppointmentsBinding
    private lateinit var adapter: AppointmentsAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppointmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupUI()
        setupRecyclerView()
        fetchAppointments()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = AppointmentsAdapter()
        binding.rvAppointments.layoutManager = LinearLayoutManager(this)
        binding.rvAppointments.adapter = adapter
    }

    private fun fetchAppointments() {
        val facultyId = auth.currentUser?.uid ?: return

        db.collection("bookings")
            .whereEqualTo("facultyId", facultyId)
            .whereEqualTo("status", "confirmed")
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    // Fallback if index is still not quite ready or there's an issue with sorting
                    if (error.message?.contains("index") == true) {
                        fetchAppointmentsWithoutSort(facultyId)
                    } else {
                        Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                    return@addSnapshotListener
                }

                processBookings(value?.toObjects(BookingRequest::class.java) ?: emptyList())
            }
    }

    private fun fetchAppointmentsWithoutSort(facultyId: String) {
        db.collection("bookings")
            .whereEqualTo("facultyId", facultyId)
            .whereEqualTo("status", "confirmed")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                processBookings(value?.toObjects(BookingRequest::class.java) ?: emptyList())
            }
    }

    private fun processBookings(bookings: List<BookingRequest>) {
        val currentTime = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        
        val upcomingAppointments = bookings.filter { booking ->
            isUpcoming(booking, currentTime, dateFormat)
        }.sortedWith(compareBy({ 
            try { dateFormat.parse(it.date)?.time ?: 0L } catch (e: Exception) { 0L }
        }, {
            parseTimeToMinutes(it.timeSlot)
        }))

        adapter.submitList(upcomingAppointments)
        updateUI(upcomingAppointments.isEmpty())
    }

    private fun isUpcoming(booking: BookingRequest, now: Calendar, dateFormat: SimpleDateFormat): Boolean {
        return try {
            val bookingDate = dateFormat.parse(booking.date) ?: return false
            val startMinutes = parseTimeToMinutes(booking.timeSlot) ?: return false
            
            // Assume 30 min duration as per BookSlotActivity
            val endMinutes = startMinutes + 30
            
            val bookingEndTime = Calendar.getInstance().apply {
                time = bookingDate
                set(Calendar.HOUR_OF_DAY, endMinutes / 60)
                set(Calendar.MINUTE, endMinutes % 60)
            }

            bookingEndTime.after(now)
        } catch (e: Exception) {
            true // Show if unsure
        }
    }

    private fun parseTimeToMinutes(raw: String): Int? {
        return try {
            val upper = raw.uppercase(Locale.ENGLISH).trim()
            val hasPm = upper.contains("PM")
            val hasAm = upper.contains("AM")
            val clean = upper.replace("AM", "").replace("PM", "").trim()
            val parts = clean.split(":")
            var hour = parts[0].trim().toInt()
            val minute = if (parts.size > 1) parts[1].trim().toInt() else 0

            when {
                hasPm && hour != 12 -> hour += 12
                hasAm && hour == 12 -> hour = 0
            }
            hour * 60 + minute
        } catch (e: Exception) {
            null
        }
    }

    private fun updateUI(isEmpty: Boolean) {
        if (isEmpty) {
            binding.rvAppointments.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.rvAppointments.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
    }
}
