package com.example.madecie3.ai

import android.content.Context
import android.net.Uri
import com.example.madecie3.faculty.models.BookingRequest
import com.example.madecie3.student.models.Booking

object SmartEngine {

    // ✅ Faculty booking sorting
    fun sortRequests(requests: List<BookingRequest>): List<BookingRequest> {
        return AIRequestSorter.sortRequests(requests)
    }

    // ✅ Student booking sorting
    fun sortStudentBookings(bookings: List<Booking>): List<Booking> {
        return bookings.sortedByDescending { it.timestamp }
    }

    /**
     * ✅ NEW PRODUCTION-READY SCANNER
     * Handles PDF and Images, returns structured TimetableSlot objects.
     */
    fun scanTimetable(
        context: Context,
        uri: Uri,
        onResult: (List<TimetableSlot>) -> Unit
    ) {
        TimetableScanner.extractBusySlots(context, uri, onResult)
    }
}
