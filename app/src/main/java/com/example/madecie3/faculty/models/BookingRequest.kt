package com.example.madecie3.faculty.models

import com.google.firebase.firestore.PropertyName

data class BookingRequest(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val date: String = "",
    val timeSlot: String = "",
    @get:PropertyName("studentNote") @set:PropertyName("studentNote") var note: String = "",
    val status: String = "pending"
) {
    // Computed property to match the UI expectation
    val requestedTime: String
        get() = if (date.isNotEmpty() && timeSlot.isNotEmpty()) "$date, $timeSlot" else ""
}
