package com.example.madecie3.student.models

data class Faculty(
    val id: String,
    val name: String,
    val designation: String,
    val department: String,
    val availability: String // "green", "amber", "grey"
)
