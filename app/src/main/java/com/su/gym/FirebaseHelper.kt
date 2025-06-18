package com.su.gym

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

object FirebaseHelper {
    
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }
    
    fun getCurrentUserDisplayName(): String? {
        return auth.currentUser?.displayName
    }
    
    fun signOut(context: Context, callback: () -> Unit) {
        try {
            auth.signOut()
            callback()
        } catch (e: Exception) {
            Toast.makeText(context, "Error signing out: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun deleteAccount(context: Context, callback: () -> Unit) {
        val user = auth.currentUser
        user?.delete()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                    callback()
                } else {
                    Toast.makeText(context, "Failed to delete account: ${task.exception?.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
    }
} 