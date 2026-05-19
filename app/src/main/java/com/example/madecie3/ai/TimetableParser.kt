package com.example.madecie3.ai

data class TimetableSlot(
    val time: String,
    val subject: String = "Busy Period",
    val room: String = "N/A",
    val day: String = ""
)

/**
 * Kept as a fallback for any code that still references TimetableParser.
 * Primary parsing is now done by TimetableScanner via Gemini Vision API.
 */
object TimetableParser {

    fun parse(rawText: String): List<TimetableSlot> = emptyList()

    fun extractTimeRange(text: String): String? {
        val m = Regex("""(\d{1,2}:\d{2})\s*[-–]\s*(\d{1,2}:\d{2})""").find(text)
        return m?.let { "${it.groupValues[1]} - ${it.groupValues[2]}" }
    }

    fun normaliseTime(t: String) = t.uppercase().replace(Regex("\\s+"), "")
}