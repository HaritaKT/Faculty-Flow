package com.example.madecie3.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.madecie3.R
import com.example.madecie3.databinding.ActivityFacultyProfileBinding
import com.example.madecie3.student.adapters.TimeSlotAdapter
import com.example.madecie3.student.models.TimeSlot
import com.google.firebase.firestore.FirebaseFirestore

class FacultyProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFacultyProfileBinding
    private lateinit var timeSlotAdapter: TimeSlotAdapter
    private lateinit var db: FirebaseFirestore
    private var facultyId: String = ""
    private var facultyName: String = ""
    private var facultyDesignation: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFacultyProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        facultyId = intent.getStringExtra("faculty_id") ?: ""

        setupUI()
        setupTimeSlots()
        setupClickListeners()
        loadFacultyData()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun setupTimeSlots() {
        timeSlotAdapter = TimeSlotAdapter { timeSlot ->
            navigateToBooking(timeSlot.time)
        }

        binding.rvAvailableSlots.apply {
            layoutManager = GridLayoutManager(this@FacultyProfileActivity, 3)
            adapter = timeSlotAdapter
        }

        // Sample time slots for now - in a full app these would come from faculty schedule
        val timeSlots = listOf(
            TimeSlot("9:00 AM", "30 min"),
            TimeSlot("9:30 AM", "30 min"),
            TimeSlot("10:00 AM", "30 min"),
            TimeSlot("10:30 AM", "30 min"),
            TimeSlot("11:00 AM", "30 min"),
            TimeSlot("11:30 AM", "30 min")
        )
        timeSlotAdapter.submitList(timeSlots)
    }

    private fun setupClickListeners() {
        binding.btnBookNow.setOnClickListener {
            navigateToBooking(null)
        }
    }

    private fun navigateToBooking(selectedTime: String?) {
        val intent = Intent(this, BookSlotActivity::class.java)
        intent.putExtra("faculty_id", facultyId)
        intent.putExtra("faculty_name", facultyName)
        intent.putExtra("faculty_designation", facultyDesignation)
        intent.putExtra("selected_time", selectedTime)
        startActivity(intent)
    }

    private fun loadFacultyData() {
        if (facultyId.isEmpty()) return

        db.collection("users").document(facultyId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    facultyName = doc.getString("name") ?: "Faculty Member"
                    facultyDesignation = doc.getString("designation") ?: "Professor"
                    
                    binding.tvFacultyName.text = facultyName
                    binding.tvFacultyDesignation.text = facultyDesignation
                    binding.tvCurrentLocation.text = doc.getString("roomBlock") ?: "Not specified"
                    binding.tvOfficeHours.text = doc.getString("officeHours") ?: "Not specified"
                    
                    val availability = doc.getString("availability") ?: "green"
                    updateAvailabilityUI(availability)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateAvailabilityUI(status: String) {
        when (status) {
            "green" -> {
                binding.tvAvailabilityStatus.text = "Available"
                binding.availabilityIndicator.setBackgroundResource(R.drawable.availability_green)
                binding.tvAvailabilityStatus.setTextColor(getColor(R.color.green_500))
            }
            "amber" -> {
                binding.tvAvailabilityStatus.text = "Busy"
                binding.availabilityIndicator.setBackgroundResource(R.drawable.availability_amber)
                binding.tvAvailabilityStatus.setTextColor(getColor(R.color.ios_orange))
            }
            else -> {
                binding.tvAvailabilityStatus.text = "In Class"
                binding.availabilityIndicator.setBackgroundResource(R.drawable.availability_grey)
                binding.tvAvailabilityStatus.setTextColor(getColor(R.color.apple_secondary_label))
            }
        }
    }
}
