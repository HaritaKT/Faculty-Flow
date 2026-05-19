package com.example.madecie3.utils

import android.content.Context
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*
import com.example.madecie3.R // Add this line

// View Extensions
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.enable() {
    isEnabled = true
}

fun View.disable() {
    isEnabled = false
}

// Context Extensions
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Fragment.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().showToast(message, duration)
}

// Keyboard Extensions
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun TextInputEditText.getString(): String {
    return text.toString().trim()
}

// Date Extensions
fun Date.toFormattedString(format: String = "EEE, MMM d"): String {
    return SimpleDateFormat(format, Locale.getDefault()).format(this)
}

fun Date.toTimeString(format: String = "h:mm a"): String {
    return SimpleDateFormat(format, Locale.getDefault()).format(this)
}

// Validation Extensions
fun String.isValidEmail(): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidRVUEmail(): Boolean {
    return isValidEmail() && this.endsWith("@rvu.edu.in")
}

// Color Extensions
fun Context.getStatusColor(status: String): Int {
    return when (status.lowercase()) {
        "green", "available" -> getColor(R.color.green_500)
        "amber", "busy" -> getColor(R.color.amber_500)
        "grey", "in_class", "unavailable" -> getColor(R.color.grey_500)
        else -> getColor(R.color.green_500)
    }
}

fun Context.getStatusBackground(status: String): Int {
    return when (status.lowercase()) {
        "green", "available" -> R.drawable.availability_green
        "amber", "busy" -> R.drawable.availability_amber
        "grey", "in_class", "unavailable" -> R.drawable.availability_grey
        else -> R.drawable.availability_green
    }
}
