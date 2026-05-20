package com.example.madecie3.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import com.example.madecie3.R
import com.example.madecie3.databinding.ActivityBookSlotBinding
import com.example.madecie3.databinding.ItemDateChipBinding
import com.example.madecie3.student.adapters.TimeSlotAdapter
import com.example.madecie3.student.models.TimeSlot
import com.example.madecie3.utils.PreferencesManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

class BookSlotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookSlotBinding
    private lateinit var timeSlotAdapter: TimeSlotAdapter

    private var selectedTimeSlot: TimeSlot? = null
    private var selectedDate: String = ""
    private var selectedDayKey: String = ""
    private var preselectedTime: String? = null

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var preferencesManager: PreferencesManager

    private var facultyId: String = ""
    private var facultyName: String = ""
    private var facultyDesignation: String = ""

    private var currentTimeSlots: List<TimeSlot> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBookSlotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        preferencesManager = PreferencesManager(this)

        facultyId = intent.getStringExtra("faculty_id") ?: ""
        facultyName = intent.getStringExtra("faculty_name") ?: "Dr. John Smith"
        facultyDesignation = intent.getStringExtra("faculty_designation") ?: "Professor"
        preselectedTime = intent.getStringExtra("selected_time")

        setupUI()
        setupTimeSlots()      // MUST come before date chips
        setupDateChips()
        setupClickListeners()
        loadFacultyData()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener { finish() }
    }

    // ================= DATE CHIPS =================
    private fun setupDateChips() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val dayKeyFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)

        for (i in 0..6) {
            val chip = ItemDateChipBinding.inflate(LayoutInflater.from(this))

            val date = calendar.time
            val formattedDate = dayFormat.format(date)
            val dayKey = dayKeyFormat.format(date).lowercase(Locale.ENGLISH)

            chip.tvDay.text = dateFormat.format(date).uppercase()
            chip.tvDate.text = SimpleDateFormat("d", Locale.getDefault()).format(date)

            chip.root.setOnClickListener {
                selectDate(chip, formattedDate, dayKey)
            }

            if (i == 0) {
                selectDate(chip, formattedDate, dayKey)
            }

            binding.dateContainer.addView(chip.root)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun selectDate(chip: ItemDateChipBinding, date: String, dayKey: String) {

        // reset all chips
        for (i in 0 until binding.dateContainer.childCount) {
            val child = binding.dateContainer.getChildAt(i)

            if (child is CardView) {
                child.setCardBackgroundColor(getColor(R.color.ios_gray6))

                val day = child.findViewById<TextView>(R.id.tvDay)
                val d = child.findViewById<TextView>(R.id.tvDate)

                day.setTextColor(getColor(R.color.apple_secondary_label))
                d.setTextColor(getColor(R.color.apple_label))
            }
        }

        // highlight selected
        chip.root.setCardBackgroundColor(getColor(R.color.ios_blue))
        chip.tvDay.setTextColor(getColor(R.color.white))
        chip.tvDate.setTextColor(getColor(R.color.white))

        selectedDate = date
        selectedDayKey = dayKey
        selectedTimeSlot = null
        binding.btnConfirmBooking.isEnabled = false
        binding.selectedTimeDisplay.visibility = View.GONE
        binding.tvSelectedTime.visibility = View.GONE
        loadTimeSlotsForDate()
    }

    // ================= TIME SLOTS =================
    private fun setupTimeSlots() {

        timeSlotAdapter = TimeSlotAdapter { slot ->
            updateSelectedTimeSlot(slot)
        }

        binding.rvTimeSlots.apply {
            layoutManager = GridLayoutManager(this@BookSlotActivity, 3)
            adapter = timeSlotAdapter
        }
    }

    private fun updateSelectedTimeSlot(slot: TimeSlot) {
        // Deselect previous
        currentTimeSlots.forEach { it.isSelected = false }
        
        // Select new
        val clickedSlot = currentTimeSlots.find { it.time == slot.time }
        clickedSlot?.isSelected = true
        
        // Update adapter
        timeSlotAdapter.notifyDataSetChanged()
        
        selectedTimeSlot = clickedSlot

        binding.selectedTimeDisplay.visibility = View.VISIBLE
        binding.tvSelectedTime.visibility = View.VISIBLE
        binding.tvSelectedTime.text = "Slot selected: ${slot.time}"

        binding.btnConfirmBooking.isEnabled = true
    }

    // 🔥 MAIN AI + FIREBASE FILTER
    private fun loadTimeSlotsForDate() {

        if (facultyId.isEmpty()) return

        db.collection("timetables")
            .document(facultyId)
            .get()
            .addOnSuccessListener { doc ->

                val daySlots = doc.get(selectedDayKey) as? List<*> ?: emptyList<Any>()
                val legacySlots = doc.get("slots") as? List<*> ?: emptyList<Any>()
                val busySlotsString = (daySlots.ifEmpty { legacySlots }).mapNotNull { it as? String }
                val busyRanges = busySlotsString.mapNotNull { parseBusyRange(it) }

                val allSlots = generateTimeSlots()

                val todayStr = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date())
                val isToday = selectedDate == todayStr
                val now = Calendar.getInstance()
                val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

                currentTimeSlots = allSlots.map { slot ->
                    TimeSlot(
                        time = slot,
                        duration = "30 min",
                        isAvailable = !overlapsBusyRange(slot, busyRanges),
                        isSelected = false
                    )
                }.filter { slot ->
                    if (!isToday) return@filter true
                    val slotStartMinutes = parseTimeToMinutes(slot.time) ?: return@filter false
                    val slotEndMinutes = slotStartMinutes + 30
                    // Filter out if current time is within 15 mins of end or past end
                    currentMinutes + 15 <= slotEndMinutes
                }

                timeSlotAdapter.submitList(currentTimeSlots)
                binding.noSlotsState.visibility =
                    if (currentTimeSlots.none { it.isAvailable }) View.VISIBLE else View.GONE

                val matchingPreselected = preselectedTime?.let { selected ->
                    currentTimeSlots.firstOrNull { it.time == selected && it.isAvailable }
                }
                if (matchingPreselected != null) {
                    updateSelectedTimeSlot(matchingPreselected)
                    preselectedTime = null
                }

                if (currentTimeSlots.none { it.isAvailable } && currentTimeSlots.isNotEmpty()) {
                    Toast.makeText(
                        this,
                        "No slots available (faculty busy)",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (currentTimeSlots.isEmpty()) {
                    Toast.makeText(
                        this,
                        "No more slots available for today",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load timetable", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔥 DYNAMIC SLOT GENERATOR (9AM → 5PM)
    private fun generateTimeSlots(): List<String> {

        val slots = mutableListOf<String>()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)

        val end = Calendar.getInstance()
        end.set(Calendar.HOUR_OF_DAY, 17)
        end.set(Calendar.MINUTE, 0)

        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())

        while (calendar.before(end)) {
            slots.add(formatter.format(calendar.time))
            calendar.add(Calendar.MINUTE, 30)
        }

        return slots
    }

    private fun parseBusyRange(rawSlot: String): Pair<Int, Int>? {
        val timeRange = rawSlot.substringBefore("|").trim()
        val times = timeRange.split(Regex("\\s*[-–]\\s*"))
        if (times.size < 2) return null

        val start = parseTimeToMinutes(times[0]) ?: return null
        val end = parseTimeToMinutes(times[1]) ?: return null
        return if (end > start) start to end else null
    }

    private fun overlapsBusyRange(slot: String, busyRanges: List<Pair<Int, Int>>): Boolean {
        val slotStart = parseTimeToMinutes(slot) ?: return false
        val slotEnd = slotStart + 30
        return busyRanges.any { (busyStart, busyEnd) ->
            max(slotStart, busyStart) < min(slotEnd, busyEnd)
        }
    }

    private fun parseTimeToMinutes(raw: String): Int? {
        return try {
            val upper = raw.uppercase(Locale.ENGLISH).trim()
            val hasPm = upper.contains("PM")
            val hasAm = upper.contains("AM")
            val clean = upper
                .replace("AM", "")
                .replace("PM", "")
                .trim()
            val parts = clean.split(":")
            var hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
            val minute = parts.getOrNull(1)?.trim()?.take(2)?.toIntOrNull() ?: 0

            when {
                hasPm && hour != 12 -> hour += 12
                hasAm && hour == 12 -> hour = 0
                !hasPm && !hasAm && hour in 1..7 -> hour += 12
            }

            hour * 60 + minute
        } catch (_: Exception) {
            null
        }
    }

    private fun setupClickListeners() {
        binding.btnConfirmBooking.setOnClickListener {
            confirmBooking()
        }
    }

    private fun loadFacultyData() {
        binding.tvFacultyName.text = facultyName
        binding.tvFacultyDesignation.text = facultyDesignation
    }

    // ================= BOOKING =================
    private fun confirmBooking() {

        val slot = selectedTimeSlot ?: return
        val studentId = auth.currentUser?.uid ?: return
        val studentName = preferencesManager.userName
        val note = binding.etNote.text.toString().trim()

        binding.btnConfirmBooking.isEnabled = false
        binding.btnConfirmBooking.text = "Sending Request..."

        val bookingData = hashMapOf(
            "studentId" to studentId,
            "studentName" to studentName,
            "facultyId" to facultyId,
            "facultyName" to facultyName,
            "facultyDesignation" to facultyDesignation,
            "date" to selectedDate,
            "timeSlot" to slot.time,
            "status" to "pending",
            "studentNote" to note,
            "timestamp" to Timestamp.now()
        )

        db.collection("bookings")
            .add(bookingData)
            .addOnSuccessListener {
                showSuccessAnimation()
            }
            .addOnFailureListener { e ->
                binding.btnConfirmBooking.isEnabled = true
                binding.btnConfirmBooking.text = "Confirm Booking"

                Toast.makeText(
                    this,
                    "Failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showSuccessAnimation() {
        binding.layoutSuccess.successOverlay.visibility = View.VISIBLE

        val anim = AnimationUtils.loadAnimation(this, R.anim.scale_up)
        binding.layoutSuccess.successCard.startAnimation(anim)

        binding.root.postDelayed({
            finish()
        }, 2000)
    }
}
