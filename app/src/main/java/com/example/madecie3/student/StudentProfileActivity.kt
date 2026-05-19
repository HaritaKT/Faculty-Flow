package com.example.madecie3.student

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.madecie3.LoginActivity
import com.example.madecie3.databinding.ActivityStudentProfileBinding
import com.example.madecie3.utils.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentProfileBinding
    private lateinit var preferencesManager: PreferencesManager
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)

        setupListeners()
        loadProfile()
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }
        binding.btnMyBookings.setOnClickListener {
            startActivity(Intent(this, MyBookingsActivity::class.java))
        }
        binding.btnSignOut.setOnClickListener { signOut() }
    }

    private fun loadProfile() {
        binding.tvStudentName.text = preferencesManager.userName.ifEmpty { "Student" }
        binding.tvStudentEmail.text = preferencesManager.userEmail.ifEmpty { "Not available" }

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val name = doc.getString("name").orEmpty()
                val email = doc.getString("email").orEmpty()

                binding.tvStudentName.text = name.ifEmpty { preferencesManager.userName }
                binding.tvStudentEmail.text = email.ifEmpty { preferencesManager.userEmail }
                binding.tvDegree.text = doc.getString("degree").orEmpty().ifEmpty { "Not added" }
                binding.tvSemester.text = doc.getString("semester").orEmpty().ifEmpty { "Not added" }

                if (name.isNotEmpty()) preferencesManager.userName = name
                if (email.isNotEmpty()) preferencesManager.userEmail = email
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Could not load profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun signOut() {
        auth.signOut()
        preferencesManager.clearAll()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
