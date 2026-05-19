package com.example.madecie3.faculty.models

data class ScheduleItem(
    val startTime: String,
    val endTime: String,
    val className: String,
    val classRoom: String,
    val status: String,
    val statusColor: String,  // "blue", "amber", "green", "grey"
    val day: String = ""      // "monday"…"friday", empty = unknown
)