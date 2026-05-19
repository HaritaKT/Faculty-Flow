package com.example.madecie3

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// FIX: Missing import for ViewBinding class. Without this, the compiler
// cannot resolve ActivityLoginBinding on a clean build / after package rename.
import com.example.madecie3.databinding.ActivityLoginBinding
import com.example.madecie3.faculty.FacultyHomeActivity
import com.example.madecie3.student.FacultyDirectoryActivity
import com.example.madecie3.utils.Constants
import com.example.madecie3.utils.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)
        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        setupButtons()
    }

    private fun setupButtons() {
        binding.btnLogin.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            when {
                email.isEmpty() -> binding.tilEmail.error = "Email is required"
                !email.endsWith(Constants.EMAIL_DOMAIN) ->
                    binding.tilEmail.error = "Please use your @rvu.edu.in email"
                password.isEmpty() -> binding.tilPassword.error = "Password is required"
                else -> {
                    binding.tilEmail.error    = null
                    binding.tilPassword.error = null
                    performFirebaseLogin(email, password)
                }
            }
        }

        binding.tvSignupToggle.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
    }

    private fun performFirebaseLogin(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text      = "Signing in…"

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                preferencesManager.userName  = document.getString("name") ?: ""
                                preferencesManager.userEmail = email
                                preferencesManager.userType  = document.getString("userType") ?: ""
                                preferencesManager.isLoggedIn = true
                                navigateToDashboard()
                            } else {
                                resetLoginButton()
                                Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            resetLoginButton()
                            Toast.makeText(this, "Error fetching profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    resetLoginButton()
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun resetLoginButton() {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.text      = "Sign In"
    }

    private fun navigateToDashboard() {
        val intent = if (preferencesManager.userType == Constants.USER_TYPE_STUDENT) {
            Intent(this, FacultyDirectoryActivity::class.java)
        } else {
            Intent(this, FacultyHomeActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}