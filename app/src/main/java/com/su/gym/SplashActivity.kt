package com.su.gym

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private var hasNavigated = false

    companion object {
        private const val TAG = "SplashActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        
        // Delay for 2 seconds to show splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserAndNavigate()
        }, 2000)
    }
    
    private fun checkUserAndNavigate() {
        // Prevent multiple navigations
        if (hasNavigated) return
        
        try {
            val currentUser = auth.currentUser
            Log.d(TAG, "Current user: ${currentUser?.uid}")

            if (currentUser == null) {
                // No user is signed in, go to MainActivity and show WelcomeFragment
                hasNavigated = true
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("destination", "welcome")
                startActivity(intent)
                finish()
            } else {
                // Check if the user is a trainer
                db.collection("trainer")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (!hasNavigated) {
                            hasNavigated = true
                            if (document.exists()) {
                                // User is a trainer
                                Log.d(TAG, "User is a trainer, navigating to trainer activity")
                                startActivity(Intent(this, TrainerMainActivity::class.java))
                            } else {
                                // User is a regular user
                                Log.d(TAG, "User is a regular user, navigating to main activity")
                                startActivity(Intent(this, MainActivity::class.java))
                            }
                            finish()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error checking trainer status", exception)
                        if (!hasNavigated) {
                            hasNavigated = true
                            // In case of error, default to MainActivity
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkUserAndNavigate", e)
            if (!hasNavigated) {
                hasNavigated = true
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
} 