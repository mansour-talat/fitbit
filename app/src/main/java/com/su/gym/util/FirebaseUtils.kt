package com.su.gym.util

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.model.User

object FirebaseUtils {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    fun saveUserData(user: User, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.currentUser?.let { firebaseUser ->
            db.collection("users").document(firebaseUser.uid)
                .set(user)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.message ?: "Unknown error") }
        }
    }

    fun getUserData(onSuccess: (User?) -> Unit, onError: (String) -> Unit) {
        auth.currentUser?.let { firebaseUser ->
            db.collection("users").document(firebaseUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        onSuccess(document.toObject(User::class.java))
                    } else {
                        onSuccess(null)
                    }
                }
                .addOnFailureListener { e -> onError(e.message ?: "Unknown error") }
        }
    }
} 