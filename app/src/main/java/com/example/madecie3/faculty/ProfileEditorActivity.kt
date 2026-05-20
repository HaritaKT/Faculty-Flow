package com.example.madecie3.faculty

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.example.madecie3.BuildConfig
import com.example.madecie3.LoginActivity
import com.example.madecie3.R
import com.example.madecie3.databinding.ActivityProfileEditorBinding
import com.example.madecie3.utils.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileEditorActivity : androidx.appcompat.app.AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditorBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var preferencesManager: PreferencesManager
    private var selectedImageUri: Uri? = null

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }
                selectedImageUri = it
                binding.ivProfile.setImageURI(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityProfileEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        db                 = FirebaseFirestore.getInstance()
        auth               = FirebaseAuth.getInstance()
        storage            = FirebaseStorage.getInstance()
        preferencesManager = PreferencesManager(this)

        setupUI()
        setupFormFields()
        setupClickListeners()
        loadProfileData()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener { finish() }
        binding.tvChangePhoto.setOnClickListener { getContent.launch(arrayOf("image/*")) }
        binding.ivProfile.setOnClickListener    { getContent.launch(arrayOf("image/*")) }
    }

    private fun setupFormFields() {
        val departments = arrayOf(
            "Computer Science", "Electronics", "Mechanical", "Civil", "Electrical"
        )
        binding.etDepartment.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, departments)
        )
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            if (selectedImageUri != null) uploadImageAndSaveProfile() else saveProfile(null)
        }
        binding.btnSaveSticky.setOnClickListener {
            if (selectedImageUri != null) uploadImageAndSaveProfile() else saveProfile(null)
        }
        binding.btnDeleteAccount.setOnClickListener { signOut() }
    }

    private fun loadProfileData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    binding.etName.setText(doc.getString("name"))
                    binding.etDepartment.setText(doc.getString("department"), false)
                    binding.etRoomBlock.setText(doc.getString("roomBlock"))
                    binding.etOfficeHours.setText(doc.getString("officeHours"))
                    binding.etDesignation.setText(doc.getString("designation"))
                    val imageUrl = doc.getString("profileImageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this).load(imageUrl)
                            .placeholder(R.drawable.ic_menu_home)
                            .circleCrop().into(binding.ivProfile)
                    }
                }
            }
    }

    private fun uploadImageAndSaveProfile() {
        val uri = selectedImageUri ?: return
        val apiKey = BuildConfig.IMGBB_API_KEY

        if (apiKey.isBlank()) {
            Toast.makeText(this, "Image upload API key missing in .env", Toast.LENGTH_LONG).show()
            return
        }

        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Uploading Image..."

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 1. Convert URI to Base64 (Standard format for web uploads)
                val base64Image = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    } ?: throw Exception("Failed to open image")
                }

                // 2. Upload to ImgBB using OkHttp
                val imageUrl = withContext(Dispatchers.IO) {
                    val client = okhttp3.OkHttpClient()
                    val formBody = okhttp3.MultipartBody.Builder()
                        .setType(okhttp3.MultipartBody.FORM)
                        .addFormDataPart("key", apiKey)
                        .addFormDataPart("image", base64Image)
                        .build()

                    val request = okhttp3.Request.Builder()
                        .url("https://api.imgbb.com/1/upload")
                        .post(formBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val jsonResponse = org.json.JSONObject(response.body?.string() ?: "")

                    if (jsonResponse.has("data")) {
                        jsonResponse.getJSONObject("data").getString("url")
                    } else {
                        throw Exception("Upload failed: ${jsonResponse.optJSONObject("error")?.optString("message")}")
                    }
                }

                // 3. Save the resulting link to Firestore
                saveProfile(imageUrl)

            } catch (e: Exception) {
                resetSaveButton()
                Toast.makeText(this@ProfileEditorActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resetSaveButton() {
        binding.btnSave.isEnabled = true
        binding.btnSave.text      = "Update Profile"
    }

    private fun saveProfile(imageUrl: String?) {
        val userId = auth.currentUser?.uid ?: return
        val name   = binding.etName.text.toString().trim()

        val updates = hashMapOf<String, Any>(
            "name"        to name,
            "department"  to binding.etDepartment.text.toString().trim(),
            "roomBlock"   to binding.etRoomBlock.text.toString().trim(),
            "officeHours" to binding.etOfficeHours.text.toString().trim(),
            "designation" to binding.etDesignation.text.toString().trim()
        )
        imageUrl?.let { updates["profileImageUrl"] = it }

        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                preferencesManager.userName = name
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                resetSaveButton()
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun signOut() {
        auth.signOut()
        preferencesManager.clearAll()
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
