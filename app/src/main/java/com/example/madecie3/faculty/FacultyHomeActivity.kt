package com.example.madecie3.faculty

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.madecie3.R
import com.example.madecie3.databinding.ActivityFacultyHomeBinding
import com.example.madecie3.faculty.adapters.ScheduleAdapter
import com.example.madecie3.faculty.models.ScheduleItem
import com.example.madecie3.utils.PreferencesManager
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class FacultyHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFacultyHomeBinding
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var preferencesManager: PreferencesManager

    private val allDaySlots: MutableMap<String, List<String>> = mutableMapOf()
    private var selectedDay: String = defaultChipDay()
    private var isUpdatingFromDb    = false

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshTask = object : Runnable {
        override fun run() {
            updateDayProgress(allDaySlots[selectedDay] ?: emptyList())
            refreshHandler.postDelayed(this, 60_000L)
        }
    }

    companion object {
        private const val ACTIVE_DAY_START_MINS = 8 * 60
        private const val ACTIVE_DAY_END_MINS   = 20 * 60

        val DAY_KEYS = listOf("monday", "tuesday", "wednesday", "thursday", "friday")

        fun todayDayKey(): String = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY    -> "monday"
            Calendar.TUESDAY   -> "tuesday"
            Calendar.WEDNESDAY -> "wednesday"
            Calendar.THURSDAY  -> "thursday"
            Calendar.FRIDAY    -> "friday"
            Calendar.SATURDAY  -> "saturday"
            Calendar.SUNDAY    -> "sunday"
            else               -> "monday"
        }

        /** Chip to pre-select. Weekends fall back to Monday (no Sat/Sun chips). */
        fun defaultChipDay(): String {
            val today = todayDayKey()
            return if (today in DAY_KEYS) today else "monday"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityFacultyHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()

        db                 = FirebaseFirestore.getInstance()
        auth               = FirebaseAuth.getInstance()
        preferencesManager = PreferencesManager(this)

        setupUI()
        setupScheduleRecyclerView()
        setupDayChips()
        setupNavigation()
        setupClickListeners()
        fetchFacultyData()
        listenForTimetable()
        listenForPendingBookings()
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.post(refreshTask)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshTask)
    }

    // ── Window insets ──────────────────────────────────────────────────────────

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Top: push content below status bar
            // Bottom: 96 dp (app nav card height + margins) + system nav bar height
            //         so the last schedule card is never hidden behind either bar
            val baseBottomPx = (96 * resources.displayMetrics.density).toInt()
            binding.nestedScrollView.updatePadding(
                top    = systemBars.top,
                bottom = baseBottomPx + systemBars.bottom
            )

            // Float the nav card above the system nav bar
            val navCard = binding.bottomNavCard
            val params  = navCard.layoutParams as CoordinatorLayout.LayoutParams
            val baseMarginPx = (16 * resources.displayMetrics.density).toInt()
            params.bottomMargin = baseMarginPx + systemBars.bottom
            navCard.layoutParams = params

            WindowInsetsCompat.CONSUMED
        }
    }

    // ── UI setup ───────────────────────────────────────────────────────────────

    private fun setupUI() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else      -> "Good Evening"
        }
        binding.tvFacultyName.text = preferencesManager.userName
    }

    private fun setupScheduleRecyclerView() {
        scheduleAdapter = ScheduleAdapter()
        binding.rvSchedule.layoutManager = LinearLayoutManager(this)
        binding.rvSchedule.adapter = scheduleAdapter
    }

    // ── Day chip strip ────────────────────────────────────────────────────────

    private fun setupDayChips() {
        val chipIdToDay = mapOf(
            R.id.chipMon to "monday",
            R.id.chipTue to "tuesday",
            R.id.chipWed to "wednesday",
            R.id.chipThu to "thursday",
            R.id.chipFri to "friday"
        )

        val defaultDay  = defaultChipDay()
        val defaultChip = chipIdToDay.entries
            .firstOrNull { it.value == defaultDay }?.key ?: R.id.chipMon
        binding.chipGroupDays.findViewById<Chip>(defaultChip)?.isChecked = true
        updateTimelineTitle(selectedDay)

        binding.chipGroupDays.setOnCheckedStateChangeListener { _, checkedIds ->
            val dayKey = chipIdToDay[checkedIds.firstOrNull()
                ?: return@setOnCheckedStateChangeListener]
                ?: return@setOnCheckedStateChangeListener
            selectedDay = dayKey
            updateTimelineTitle(dayKey)
            renderScheduleForDay(dayKey)
            updateDayProgress(allDaySlots[dayKey] ?: emptyList())
        }
    }

    private fun updateTimelineTitle(dayKey: String) {
        binding.tvTimelineTitle.text = if (dayKey == todayDayKey()) {
            "Today's Timeline"
        } else {
            "${dayKey.replaceFirstChar { it.uppercase() }}'s Timeline"
        }
    }

    // ── Firestore timetable listener ──────────────────────────────────────────

    private fun listenForTimetable() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("timetables").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val data = snapshot.data ?: return@addSnapshotListener

                val hasPerDay = DAY_KEYS.any { data.containsKey(it) }
                if (hasPerDay) {
                    for (day in DAY_KEYS) {
                        @Suppress("UNCHECKED_CAST")
                        allDaySlots[day] = data[day] as? List<String> ?: emptyList()
                    }
                } else {
                    // Legacy flat structure — broadcast to all days until re-upload
                    @Suppress("UNCHECKED_CAST")
                    val flat = data["slots"] as? List<String> ?: emptyList()
                    for (day in DAY_KEYS) allDaySlots[day] = flat
                }

                renderScheduleForDay(selectedDay)
                updateDayProgress(allDaySlots[selectedDay] ?: emptyList())
            }
    }

    // ── Schedule rendering ────────────────────────────────────────────────────

    private fun renderScheduleForDay(dayKey: String) {
        val items = (allDaySlots[dayKey] ?: emptyList())
            .mapNotNull { parseSlotString(it) }
            .sortedBy { parseTimeToMinutes(it.startTime) }
        scheduleAdapter.submitList(items)
        binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun parseSlotString(raw: String): ScheduleItem? {
        val parts     = raw.split("|").map { it.trim() }
        val timeRange = parts.getOrNull(0)?.trim() ?: return null
        val times     = timeRange.split(Regex("\\s*-\\s*"))
        val subject   = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() } ?: "Busy Period"
        val room      = parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() } ?: "N/A"
        val isBusy    = subject.equals("Busy Period", ignoreCase = true)
                || subject.equals("Busy", ignoreCase = true)
        return ScheduleItem(
            startTime   = times.getOrNull(0)?.trim() ?: "",
            endTime     = times.getOrNull(1)?.trim() ?: "",
            className   = subject,
            classRoom   = room,
            status      = if (isBusy) "Busy" else "In Class",
            statusColor = if (isBusy) "amber" else "blue"
        )
    }

    // ── Day progress ──────────────────────────────────────────────────────────

    private fun updateDayProgress(slots: List<String>) {
        if (selectedDay != todayDayKey()) {
            binding.tvProgress.text         = "--"
            binding.tvDayProgressLabel.text = "Other day"
            return
        }

        val now         = Calendar.getInstance()
        val currentMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        when {
            currentMins < ACTIVE_DAY_START_MINS -> {
                binding.tvProgress.text         = "0%"
                binding.tvDayProgressLabel.text = "Day Not Started"
                return
            }
            currentMins >= ACTIVE_DAY_END_MINS -> {
                binding.tvProgress.text         = "100%"
                binding.tvDayProgressLabel.text = "Day Ended"
                return
            }
        }

        binding.tvDayProgressLabel.text = "Day Progress"

        if (slots.isEmpty()) {
            val wall = ((currentMins - ACTIVE_DAY_START_MINS).toFloat() /
                    (ACTIVE_DAY_END_MINS - ACTIVE_DAY_START_MINS) * 100).toInt()
            binding.tvProgress.text = "$wall%"
            return
        }

        try {
            val startMins = mutableListOf<Int>()
            val endMins   = mutableListOf<Int>()
            slots.forEach { slot ->
                val times = slot.split("|")[0].trim().split(Regex("\\s*-\\s*"))
                if (times.size >= 2) {
                    startMins.add(parseTimeToMinutes(times[0].trim()))
                    endMins.add(parseTimeToMinutes(times[1].trim()))
                }
            }
            if (startMins.isEmpty()) { binding.tvProgress.text = "0%"; return }

            val schedStart = startMins.min().coerceAtLeast(ACTIVE_DAY_START_MINS)
            val schedEnd   = endMins.max().coerceAtMost(ACTIVE_DAY_END_MINS)

            val progress = when {
                currentMins <= schedStart -> 0
                currentMins >= schedEnd   -> 100
                else -> (((currentMins - schedStart).toFloat() /
                        (schedEnd - schedStart).toFloat()) * 100).toInt()
            }
            binding.tvProgress.text = "$progress%"
        } catch (e: Exception) {
            binding.tvProgress.text = "0%"
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val upper = timeStr.uppercase().trim()
            val isPM  = upper.contains("PM")
            val isAM  = upper.contains("AM")
            val clean = upper.replace("AM", "").replace("PM", "").trim()
            val parts = clean.split(":")
            var h     = parts[0].trim().toInt()
            val m     = if (parts.size > 1) parts[1].trim().take(2).toInt() else 0
            when {
                isPM && h != 12             -> h += 12
                isAM && h == 12             -> h = 0
                !isPM && !isAM && h in 1..7 -> h += 12
            }
            h * 60 + m
        } catch (e: Exception) { 0 }
    }

    // ── Faculty profile ────────────────────────────────────────────────────────

    private fun fetchFacultyData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val name     = snapshot.getString("name") ?: ""
                    val imageUrl = snapshot.getString("profileImageUrl")
                    binding.tvFacultyName.text = name
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this).load(imageUrl)
                            .placeholder(R.drawable.ic_menu_home)
                            .circleCrop().into(binding.ivProfile)
                    }
                }
            }
    }

    // ── Pending bookings ───────────────────────────────────────────────────────

    private fun listenForPendingBookings() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("bookings")
            .whereEqualTo("facultyId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                val count = snapshot?.size() ?: 0
                binding.tvPendingCount.text = count.toString()
                binding.cardPendingBookings.visibility =
                    if (count > 0) View.VISIBLE else View.GONE
            }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun setupNavigation() {
        binding.navTimetable.setOnClickListener {
            startActivity(Intent(this, TimetableUploadActivity::class.java))
        }
        binding.navBookings.setOnClickListener {
            startActivity(Intent(this, BookingInboxActivity::class.java))
        }
        binding.navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileEditorActivity::class.java))
        }
    }

    private fun setupClickListeners() {
        binding.cardPendingBookings.setOnClickListener {
            startActivity(Intent(this, BookingInboxActivity::class.java))
        }
        binding.switchBusy.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromDb) {
                val userId = auth.currentUser?.uid ?: return@setOnCheckedChangeListener
                db.collection("users").document(userId)
                    .update("availability", if (isChecked) "amber" else "green")
                updateStatusUI(isChecked)
            }
        }
        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileEditorActivity::class.java))
        }
    }

    private fun updateStatusUI(isBusy: Boolean) {
        val color = ContextCompat.getColor(
            this, if (isBusy) R.color.ios_red else R.color.ios_green
        )
        binding.statusIndicator.backgroundTintList = ColorStateList.valueOf(color)
        binding.switchBusy.trackTintList           = ColorStateList.valueOf(color)
    }
}
