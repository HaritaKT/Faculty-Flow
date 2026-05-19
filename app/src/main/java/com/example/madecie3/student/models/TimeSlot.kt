package com.example.madecie3.student.models

data class TimeSlot(
    val time: String,
    val duration: String,
    val isAvailable: Boolean = true
)