package com.example.madecie3.student

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.madecie3.R
import com.example.madecie3.databinding.ActivityMyBookingsBinding
import com.example.madecie3.student.adapters.MyBookingsAdapter
import com.example.madecie3.student.models.Booking
import com.example.madecie3.ai.SmartEngine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyBookingsBinding
    private lateinit var adapter: MyBookingsAdapter
    private var allBookings: List<Booking> = emptyList()
    private var selectedFilter: String = FILTER_ALL
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val FILTER_ALL = "all"
        private const val FILTER_PENDING = "pending"
        private const val FILTER_CONFIRMED = "confirmed"
        private const val FILTER_DONE = "done"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMyBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        setupListeners()
        loadBookings()
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.tabAll.setOnClickListener { applyFilter(FILTER_ALL) }
        binding.tabPending.setOnClickListener { applyFilter(FILTER_PENDING) }
        binding.tabConfirmed.setOnClickListener { applyFilter(FILTER_CONFIRMED) }
        binding.tabDone.setOnClickListener { applyFilter(FILTER_DONE) }
    }

    private fun setupRecycler() {
        adapter = MyBookingsAdapter()
        binding.rvBookings.layoutManager = LinearLayoutManager(this)
        binding.rvBookings.adapter = adapter
    }

    private fun loadBookings() {

        val studentId = auth.currentUser?.uid ?: return

        db.collection("bookings")
            .whereEqualTo("studentId", studentId)
            .get()
            .addOnSuccessListener { result ->

                allBookings = result.map {
                    it.toObject(Booking::class.java).copy(id = it.id)
                }

                applyFilter(selectedFilter)
            }
    }

    private fun applyFilter(filter: String) {
        selectedFilter = filter
        updateTabs()

        val filteredBookings = when (filter) {
            FILTER_PENDING -> allBookings.filter { it.status == "pending" }
            FILTER_CONFIRMED -> allBookings.filter { it.status == "confirmed" }
            FILTER_DONE -> allBookings.filter { it.status == "done" }
            else -> allBookings
        }

        val sortedBookings = SmartEngine.sortStudentBookings(filteredBookings)
        adapter.submitList(sortedBookings)
        binding.emptyState.visibility = if (sortedBookings.isEmpty()) View.VISIBLE else View.GONE
        binding.rvBookings.visibility = if (sortedBookings.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateTabs() {
        listOf(
            binding.tabAll to FILTER_ALL,
            binding.tabPending to FILTER_PENDING,
            binding.tabConfirmed to FILTER_CONFIRMED,
            binding.tabDone to FILTER_DONE
        ).forEach { (tab, filter) ->
            val selected = filter == selectedFilter
            tab.setBackgroundResource(if (selected) R.drawable.segmented_control_selected else 0)
            tab.setTextColor(
                getColor(if (selected) R.color.apple_label else R.color.apple_secondary_label)
            )
        }
    }
}
