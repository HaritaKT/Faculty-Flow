package com.example.madecie3

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.madecie3.databinding.ActivitySignupBinding
import com.example.madecie3.faculty.FacultyHomeActivity
import com.example.madecie3.student.FacultyDirectoryActivity
import com.example.madecie3.utils.Constants
import com.example.madecie3.utils.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isStudent = true

    // Hardcoded Faculty Access Code
    private val FACULTY_ACCESS_CODE = "FACULTY2024"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)
        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        updateUserTypeUI()
        setupUserTypeSelection()
        setupFormFields()
        setupSignupButton()
    }

    private fun setupUserTypeSelection() {
        binding.btnStudent.setOnClickListener {
            if (!isStudent) { isStudent = true; updateUserTypeUI() }
        }
        binding.btnFaculty.setOnClickListener {
            if (isStudent) { isStudent = false; updateUserTypeUI() }
        }
    }

    private fun updateUserTypeUI() {
        if (isStudent) {
            binding.btnStudent.apply { setBackgroundResource(R.drawable.segmented_control_selected); setTypeface(null, Typeface.BOLD) }
            binding.btnFaculty.apply { background = null; setTypeface(null, Typeface.NORMAL) }
            binding.studentFieldsContainer.visibility = View.VISIBLE
            binding.facultyFieldsContainer.visibility = View.GONE
        } else {
            binding.btnFaculty.apply { setBackgroundResource(R.drawable.segmented_control_selected); setTypeface(null, Typeface.BOLD) }
            binding.btnStudent.apply { background = null; setTypeface(null, Typeface.NORMAL) }
            binding.studentFieldsContainer.visibility = View.GONE
            binding.facultyFieldsContainer.visibility = View.VISIBLE
        }
    }

    private fun setupFormFields() {
        val degrees = arrayOf("B.Tech Computer Science","B.Tech Electronics","B.Tech Mechanical","B.Tech Civil","M.Tech Computer Science","M.Tech Electronics")
        binding.etDegree.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, degrees))

        val semesters = arrayOf("1st Semester","2nd Semester","3rd Semester","4th Semester","5th Semester","6th Semester","7th Semester","8th Semester")
        binding.etSemester.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, semesters))
    }

    private fun setupSignupButton() {
        binding.btnSignup.setOnClickListener { if (validateForm()) performFirebaseSignup() }
        binding.tvLoginToggle.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true
        val name     = binding.etName.text.toString().trim()
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (name.isEmpty())     { binding.tilName.error = "Name is required";     isValid = false } else binding.tilName.error = null
        if (email.isEmpty())    { binding.tilEmail.error = "Email is required";   isValid = false }
        else if (!isValidEmail(email)) { binding.tilEmail.error = "Please use your @rvu.edu.in email"; isValid = false }
        else binding.tilEmail.error = null

        if (password.length < 6) { binding.tilPassword.error = "Password must be at least 6 characters"; isValid = false } else binding.tilPassword.error = null

        if (isStudent) {
            if (binding.etDegree.text.isNullOrBlank())   { binding.tilDegree.error = "Degree is required";     isValid = false } else binding.tilDegree.error = null
            if (binding.etSemester.text.isNullOrBlank()) { binding.tilSemester.error = "Semester is required"; isValid = false } else binding.tilSemester.error = null
        } else {
            val accessCode = binding.etAccessCode.text.toString().trim()
            if (accessCode.isEmpty()) {
                binding.tilAccessCode.error = "Access code is required"
                isValid = false
            } else if (accessCode != FACULTY_ACCESS_CODE) {
                binding.tilAccessCode.error = "Invalid Faculty Access Code"
                isValid = false
            } else {
                binding.tilAccessCode.error = null
            }
        }
        return isValid
    }

    private fun isValidEmail(email: String) =
        email.endsWith("@rvu.edu.in") && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun performFirebaseSignup() {
        val name     = binding.etName.text.toString().trim()
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val userType = if (isStudent) Constants.USER_TYPE_STUDENT else Constants.USER_TYPE_FACULTY

        binding.btnSignup.isEnabled = false
        binding.btnSignup.text      = "Creating account…"

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    firebaseUser?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                val userId   = firebaseUser.uid
                                val userData = mutableMapOf<String, Any>("uid" to userId, "name" to name, "email" to email, "userType" to userType)
                                if (isStudent) {
                                    userData["degree"]   = binding.etDegree.text.toString()
                                    userData["semester"] = binding.etSemester.text.toString()
                                }
                                db.collection("users").document(userId).set(userData)
                                    .addOnSuccessListener {
                                        auth.signOut() // Force user to log in after verification
                                        Toast.makeText(this, "Verification email sent. Please verify and login.", Toast.LENGTH_LONG).show()
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e -> resetButton(); Toast.makeText(this, "Error saving user: ${e.message}", Toast.LENGTH_SHORT).show() }
                            } else {
                                resetButton()
                                Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    resetButton()
                    val msg = when {
                        task.exception?.message?.contains("already in use", true) == true -> "Email already registered"
                        task.exception?.message?.contains("badly formatted", true) == true -> "Invalid email format"
                        task.exception?.message?.contains("password", true) == true -> "Weak password"
                        else -> "Signup failed: ${task.exception?.message}"
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun resetButton() { binding.btnSignup.isEnabled = true; binding.btnSignup.text = "Create Account" }

    private fun navigateToDashboard() {
        startActivity(Intent(this, if (isStudent) FacultyDirectoryActivity::class.java else FacultyHomeActivity::class.java))
        finish()
    }
}
