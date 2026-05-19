package com.example.madecie3

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.madecie3.databinding.ActivityMainBinding
import com.example.madecie3.faculty.FacultyHomeActivity
import com.example.madecie3.student.FacultyDirectoryActivity
import com.example.madecie3.utils.Constants
import com.example.madecie3.utils.PreferencesManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val preferencesManager = PreferencesManager(this)

        // FIX 4: Do NOT reset isLoggedIn = false here — that forced the user back
        // to LoginActivity every time the app launched. Now we read the flag and
        // route to the correct dashboard if the session is still valid.
        val destination: Intent = if (preferencesManager.isUserLoggedIn()) {
            if (preferencesManager.userType == Constants.USER_TYPE_STUDENT) {
                Intent(this, FacultyDirectoryActivity::class.java)
            } else {
                Intent(this, FacultyHomeActivity::class.java)
            }
        } else {
            Intent(this, LoginActivity::class.java)
        }

        val fadeIn  = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)

        binding.mainContent.startAnimation(fadeIn)

        binding.root.postDelayed({
            binding.mainContent.startAnimation(fadeOut)
            binding.root.postDelayed({
                startActivity(destination)
                finish()
            }, 1000)
        }, 2000)
    }
}