package com.example.madecie3.utils

object Constants {
    // User Types
    const val USER_TYPE_STUDENT = "student"
    const val USER_TYPE_FACULTY = "faculty"
    
    // Booking Status
    const val BOOKING_STATUS_PENDING = "pending"
    const val BOOKING_STATUS_CONFIRMED = "confirmed"
    const val BOOKING_STATUS_DECLINED = "declined"
    const val BOOKING_STATUS_DONE = "done"
    
    // Availability Status
    const val AVAILABILITY_AVAILABLE = "green"
    const val AVAILABILITY_BUSY = "amber"
    const val AVAILABILITY_IN_CLASS = "grey"
    
    // Email Domain
    const val EMAIL_DOMAIN = "@rvu.edu.in"
    
    // Time Slot Duration
    const val DEFAULT_SLOT_DURATION = 30 // minutes
    
    // Max Characters
    const val MAX_NOTE_CHARACTERS = 150
    
    // SharedPreferences Keys
    const val PREF_NAME = "FacultyFlowPrefs"
    const val KEY_USER_TYPE = "user_type"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    
    // Intent Extras
    const val EXTRA_FACULTY_ID = "faculty_id"
    const val EXTRA_SELECTED_TIME = "selected_time"
    const val EXTRA_USER_TYPE = "user_type"
    
    // Departments
    val DEPARTMENTS = listOf(
        "Computer Science",
        "Electronics",
        "Mechanical",
        "Civil",
        "Electrical"
    )
    
    // Degrees
    val DEGREES = listOf(
        "B.Tech Computer Science",
        "B.Tech Electronics",
        "B.Tech Mechanical",
        "B.Tech Civil",
        "M.Tech Computer Science",
        "M.Tech Electronics"
    )
    
    // Semesters
    val SEMESTERS = listOf(
        "1st Semester",
        "2nd Semester",
        "3rd Semester",
        "4th Semester",
        "5th Semester",
        "6th Semester",
        "7th Semester",
        "8th Semester"
    )
}
