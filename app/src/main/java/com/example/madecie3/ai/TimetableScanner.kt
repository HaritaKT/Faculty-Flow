package com.example.madecie3.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import com.example.madecie3.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object TimetableScanner {

    private const val TAG = "TimetableScanner"
    private const val GEMINI_MODEL = "gemini-flash-lite-latest"

    fun extractBusySlots(
        context: Context,
        uri: Uri,
        onResult: (List<TimetableSlot>) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmaps  = loadBitmaps(context, uri)
                val allSlots = mutableListOf<TimetableSlot>()

                for (bitmap in bitmaps) {
                    val slots = callGeminiVision(context, bitmap)
                    allSlots.addAll(slots)
                }

                val final = allSlots.distinctBy { "${it.time}|${it.day}" }
                Log.d(TAG, "Final slots: ${final.size}")
                withContext(Dispatchers.Main) { onResult(final) }

            } catch (e: Exception) {
                Log.e(TAG, "Scanning failed", e)
                withContext(Dispatchers.Main) { 
                    android.widget.Toast.makeText(context, "Scanning failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    onResult(emptyList()) 
                }
            }
        }
    }

    private fun callGeminiVision(context: Context, bitmap: Bitmap): List<TimetableSlot> {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank()) {
            CoroutineScope(Dispatchers.Main).launch {
                android.widget.Toast.makeText(context, "Gemini API key missing in .env", android.widget.Toast.LENGTH_LONG).show()
            }
            return emptyList()
        }

        val base64Image = bitmapToBase64(bitmap)

        val prompt = """
You are a timetable parser. This image shows a weekly class schedule table with days as rows and time slots as columns.

Your job: Extract EVERY class slot that is NOT empty, NOT "FREE", and NOT "LUNCH BREAK".

Return ONLY a valid JSON array. No explanation, no markdown, no code fences. Just the raw JSON array.

Each object must have exactly these 4 keys:
- "day": lowercase full day name ("monday", "tuesday", "wednesday", "thursday", "friday")
- "time": the time range exactly as shown in the column header (e.g. "09:10 - 10:10")  
- "subject": the subject name in that cell (e.g. "MADE", "Dynamics and Controls", "Probability and Statistics")
- "room": room code if visible, otherwise "N/A"

Critical rules:
1. Process ALL 5 days: monday, tuesday, wednesday, thursday, friday
2. Process ALL time columns including the LAST column (e.g. 03:50 - 04:50)
3. Skip cells that say FREE or LUNCH BREAK - do not include them at all
4. Each day×time combination is one object in the array
5. If the same subject appears in multiple slots for the same day, include each separately

Example of correct output:
[{"day":"monday","time":"10:10 - 11:10","subject":"MADE","room":"N/A"},{"day":"monday","time":"11:10 - 12:10","subject":"Dynamics and Controls","room":"N/A"}]
""".trimIndent()

        val url = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/${'$'}GEMINI_MODEL:generateContent?key=${'$'}apiKey"
        )

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0)
                put("maxOutputTokens", 8192)
                put("responseMimeType", "application/json")
            })
        }

        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod  = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput       = true
        conn.connectTimeout = 60_000
        conn.readTimeout    = 60_000

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(requestBody.toString())
            writer.flush()
        }

        val responseCode = conn.responseCode
        val responseText = if (responseCode == 200) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "unknown error"
            Log.e(TAG, "Gemini API error ${'$'}responseCode: ${'$'}err")
            CoroutineScope(Dispatchers.Main).launch {
                android.widget.Toast.makeText(context, "API Error ${'$'}responseCode: ${'$'}err", android.widget.Toast.LENGTH_LONG).show()
            }
            return emptyList()
        }

        return parseGeminiResponse(responseText)
    }

    private fun parseGeminiResponse(responseText: String): List<TimetableSlot> {
        return try {
            val root = JSONObject(responseText)
            val text = root
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()

            val startIndex = text.indexOf('[')
            val endIndex = text.lastIndexOf(']')
            val jsonStr = if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                text.substring(startIndex, endIndex + 1)
            } else {
                text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            }

            val array  = JSONArray(jsonStr)
            val slots  = mutableListOf<TimetableSlot>()

            for (i in 0 until array.length()) {
                val obj     = array.getJSONObject(i)
                val day     = obj.optString("day", "").lowercase().trim()
                val time    = obj.optString("time", "").trim()
                val subject = obj.optString("subject", "").trim()
                val room    = obj.optString("room", "N/A").trim()

                if (day.isEmpty() || time.isEmpty() || subject.isEmpty()) continue

                val subjectLower = subject.lowercase()
                if (subjectLower == "free" || subjectLower.contains("lunch")) continue

                slots.add(TimetableSlot(time = time, subject = subject, room = room, day = day))
            }
            slots
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini response: ${'$'}{e.message}")
            emptyList()
        }
    }

    private fun loadBitmaps(context: Context, uri: Uri): List<Bitmap> {
        val cr       = context.contentResolver
        val mimeType = cr.getType(uri)
        return if (mimeType == "application/pdf") {
            renderPdfToBitmaps(context, uri)
        } else {
            val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(cr, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(cr, uri)
            }
            listOf(bmp.copy(Bitmap.Config.ARGB_8888, false))
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxSide = 1568
        val scaled  = if (bitmap.width > maxSide || bitmap.height > maxSide) {
            val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else bitmap

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 92, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun renderPdfToBitmaps(context: Context, uri: Uri): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val renderer = PdfRenderer(pfd)
            for (i in 0 until renderer.pageCount) {
                renderer.openPage(i).use { page ->
                    val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    Canvas(bmp).drawColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bmp)
                }
            }
            renderer.close()
        }
        return bitmaps
    }
}
