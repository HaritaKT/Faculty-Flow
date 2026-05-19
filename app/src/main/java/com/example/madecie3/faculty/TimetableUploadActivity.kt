package com.example.madecie3.faculty

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.madecie3.ai.SmartEngine
import com.example.madecie3.ai.TimetableSlot
import com.example.madecie3.R
import com.example.madecie3.databinding.ActivityTimetableUploadBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TimetableUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimetableUploadBinding
    private var detectedSlots = mutableListOf<TimetableSlot>()

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleFile(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimetableUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupClickListeners()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.rvDetectedSlots.layoutManager = LinearLayoutManager(this)
        binding.rvDetectedSlots.adapter = DetectedSlotsAdapter(detectedSlots)
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener { finish() }
        binding.uploadArea.setOnClickListener { pickFileLauncher.launch("*/*") }
        binding.cameraOption.setOnClickListener { pickFileLauncher.launch("image/*") }
        binding.btnSaveSlots.setOnClickListener { saveToFirestore() }
        binding.btnRetry.setOnClickListener { resetUI() }
    }

    private fun handleFile(uri: Uri) {
        showLoadingState(true)
        SmartEngine.scanTimetable(this, uri) { slots ->
            if (isFinishing) return@scanTimetable
            showLoadingState(false)
            if (slots.isEmpty()) {
                Toast.makeText(
                    this,
                    "No timetable slots detected. Please try a clearer image of the full weekly grid.",
                    Toast.LENGTH_LONG
                ).show()
                resetUI()
            } else {
                displayResults(slots)
            }
        }
    }

    private fun displayResults(slots: List<TimetableSlot>) {
        detectedSlots.clear()
        detectedSlots.addAll(slots)
        binding.rvDetectedSlots.adapter?.notifyDataSetChanged()
        binding.selectionArea.visibility = View.GONE
        binding.resultsArea.visibility   = View.VISIBLE
        binding.tvHeader.text    = "Review Slots"
        binding.tvSubHeader.text = "Found ${slots.size} class slots across ${
            slots.mapNotNull { it.day.ifEmpty { null } }.toSet().size
        } days. Sync to your schedule?"
    }

    /**
     * Saves timetable using the per-day Firestore structure:
     *
     *   timetables/{uid} → {
     *     "monday"    : ["09:10 - 10:10 | Dynamics and Controls | N/A", ...],
     *     "tuesday"   : [...],
     *     ...
     *   }
     *
     * Slots with no day (single-day or unrecognised image) go into every weekday
     * as a fallback so the faculty sees something immediately.
     */
    private fun saveToFirestore() {
        val facultyId = FirebaseAuth.getInstance().currentUser?.uid
        if (facultyId == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_LONG).show()
            return
        }
        binding.btnSaveSlots.isEnabled = false
        binding.btnSaveSlots.text      = "Syncing..."

        val weekdays = FacultyHomeActivity.DAY_KEYS   // ["monday"…"friday"]

        val perDay: MutableMap<String, MutableList<String>> =
            weekdays.associateWith { mutableListOf<String>() }.toMutableMap()

        for (slot in detectedSlots) {
            val slotString = "${slot.time} | ${slot.subject.ifEmpty { "Busy Period" }} | ${slot.room.ifEmpty { "N/A" }}"
            val day = slot.day.lowercase().trim()
            if (day.isNotEmpty() && perDay.containsKey(day)) {
                perDay[day]!!.add(slotString)
            } else {
                // Day unknown — broadcast to all so nothing is silently lost
                for (d in weekdays) perDay[d]!!.add(slotString)
            }
        }

        val firestoreData: Map<String, Any> =
            perDay.mapValues { (_, list) -> list as List<String> }

        FirebaseFirestore.getInstance()
            .collection("timetables")
            .document(facultyId)
            .set(firestoreData)
            .addOnSuccessListener { showSuccessState() }
            .addOnFailureListener { e ->
                binding.btnSaveSlots.isEnabled = true
                binding.btnSaveSlots.text      = "Sync to My Schedule"
                Toast.makeText(this, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoadingState(isLoading: Boolean) {
        binding.selectionArea.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.progressArea.visibility  = if (isLoading) View.VISIBLE else View.GONE
        binding.resultsArea.visibility   = View.GONE
    }

    private fun resetUI() {
        binding.selectionArea.visibility = View.VISIBLE
        binding.resultsArea.visibility   = View.GONE
        binding.progressArea.visibility  = View.GONE
        binding.successState.visibility  = View.GONE
        binding.tvHeader.text    = "Update Your Schedule"
        binding.tvSubHeader.text = "Upload your weekly timetable image to sync your availability day by day."
    }

    private fun showSuccessState() {
        binding.resultsArea.visibility  = View.GONE
        binding.successState.visibility = View.VISIBLE
        binding.tvHeader.visibility     = View.GONE
        binding.tvSubHeader.visibility  = View.GONE
        binding.root.postDelayed({ finish() }, 2500)
    }

    // ── Inner adapter ─────────────────────────────────────────────────────────

    private class DetectedSlotsAdapter(private val slots: List<TimetableSlot>) :
        RecyclerView.Adapter<DetectedSlotsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val timeText:    TextView = view.findViewById(android.R.id.text1)
            val subjectText: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val slot = slots[position]
            val dayLabel = if (slot.day.isNotEmpty())
                " [${slot.day.replaceFirstChar { it.uppercase() }}]" else " [All days]"
            holder.timeText.text    = "${slot.time}$dayLabel"
            holder.subjectText.text = slot.subject.ifEmpty { "Busy Period" }
            holder.timeText.setTextColor(
                holder.itemView.context.getColor(R.color.apple_label))
            holder.subjectText.setTextColor(
                holder.itemView.context.getColor(R.color.apple_secondary_label))
        }

        override fun getItemCount() = slots.size
    }
}