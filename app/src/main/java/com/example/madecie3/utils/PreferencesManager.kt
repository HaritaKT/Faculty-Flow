package com.example.madecie3.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
    
    // User Type
    var userType: String
        get() = prefs.getString(Constants.KEY_USER_TYPE, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_USER_TYPE, value).apply()
    
    // User Name
    var userName: String
        get() = prefs.getString(Constants.KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_USER_NAME, value).apply()
    
    // User Email
    var userEmail: String
        get() = prefs.getString(Constants.KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_USER_EMAIL, value).apply()
    
    // Login Status
    var isLoggedIn: Boolean
        get() = prefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_IS_LOGGED_IN, value).apply()
    
    // Clear all preferences
    fun clearAll() {
        prefs.edit().clear().apply()
    }
    
    // Check if user is logged in
    fun isUserLoggedIn(): Boolean {
        return isLoggedIn && userName.isNotEmpty() && userEmail.isNotEmpty()
    }
}
