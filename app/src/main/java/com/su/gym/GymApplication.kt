package com.su.gym

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.ktx.initialize

class GymApplication : Application() {
    
    companion object {
        private const val TAG = "GymApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Firebase App Check with error handling
            val firebaseAppCheck = Firebase.appCheck
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            
            // Enable Firebase Database offline persistence
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            
            Log.d(TAG, "Firebase initialization completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }
    }
} 