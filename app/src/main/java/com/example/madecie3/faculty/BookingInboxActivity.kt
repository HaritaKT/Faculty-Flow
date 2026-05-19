package com.example.madecie3.faculty

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.madecie3.databinding.ActivityBookingInboxBinding
import com.example.madecie3.faculty.adapters.BookingRequestAdapter
import com.example.madecie3.faculty.models.BookingRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// ✅ AI
import com.example.madecie3.ai.SmartEngine
import com.example.madecie3.ai.TimetableSlot

class BookingInboxActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingInboxBinding
    private lateinit var bookingAdapter: BookingRequestAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // ✅ FILE PICKER
    private val timetablePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                processTimetable(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingInboxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupUI()
        setupBookingsList()
        fetchBookings()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnUploadTimetable.setOnClickListener {
            pickTimetableFile()
        }
    }

    private fun setupBookingsList() {
        bookingAdapter = BookingRequestAdapter(
            onAccept = { booking, reply ->
                updateBookingStatus(booking.id, "confirmed", reply)
            },
            onDecline = { booking, reply ->
                updateBookingStatus(booking.id, "declined", reply)
            }
        )

        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(this@BookingInboxActivity)
            adapter = bookingAdapter
        }
    }

    private fun fetchBookings() {
        val facultyId = auth.currentUser?.uid ?: return

        db.collection("bookings")
            .whereEqualTo("facultyId", facultyId)
            .whereEqualTo("status", "pending")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    if (error.message?.contains("index") == true) {
                        fetchBookingsWithoutOrder(facultyId)
                    } else {
                        Toast.makeText(
                            this,
                            "Error fetching bookings: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@addSnapshotListener
                }

                val bookings = mutableListOf<BookingRequest>()
                value?.forEach { doc ->
                    val booking =
                        doc.toObject(BookingRequest::class.java).copy(id = doc.id)
                    bookings.add(booking)
                }

                updateUI(bookings)
            }
    }

    private fun fetchBookingsWithoutOrder(facultyId: String) {
        db.collection("bookings")
            .whereEqualTo("facultyId", facultyId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { value, error ->

                if (error != null) {
                    Toast.makeText(
                        this,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                val bookings = mutableListOf<BookingRequest>()
                value?.forEach { doc ->
                    val booking =
                        doc.toObject(BookingRequest::class.java).copy(id = doc.id)
                    bookings.add(booking)
                }

                updateUI(bookings)
            }
    }

    private fun updateUI(bookings: List<BookingRequest>) {

        val sortedList = SmartEngine.sortRequests(bookings)

        bookingAdapter.submitList(sortedList)

        if (sortedList.isEmpty()) {
            binding.rvBookings.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.rvBookings.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
    }

    private fun updateBookingStatus(
        bookingId: String,
        newStatus: String,
        reply: String
    ) {
        val updates = mapOf(
            "status" to newStatus,
            "facultyReply" to reply
        )

        db.collection("bookings").document(bookingId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Booking $newStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to update booking: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun pickTimetableFile() {
        timetablePicker.launch("*/*")
    }

    // 🔥 FINAL AI + STORAGE LOGIC
    private fun processTimetable(uri: Uri) {

        Toast.makeText(this, "Scanning timetable...", Toast.LENGTH_SHORT).show()

        SmartEngine.scanTimetable(this, uri) { busySlots ->

            val facultyId = auth.currentUser?.uid ?: return@scanTimetable

            // ✅ MAP TO STRING FOR COMPATIBILITY
            val slotStrings = busySlots.map { it.time }

            // ✅ SAVE TO FIRESTORE
            val data = mapOf(
                "slots" to slotStrings
            )

            db.collection("timetables")
                .document(facultyId)
                .set(data)
                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Saved ${busySlots.size} busy slots",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Failed to save timetable",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            // debug logs
            busySlots.forEach {
                Log.d("AI_TIMETABLE", it.time)
            }
        }
    }
}
