package com.example.madecie3.student.models

data class Booking(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val facultyId: String = "",
    val facultyName: String = "",
    val facultyDesignation: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val status: String = "",
    val studentNote: String = "",
    val facultyReply: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)