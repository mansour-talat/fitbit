package com.su.gym.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object TrainerUtils {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun isApprovedTrainer(): Boolean {
        val currentUser = auth.currentUser ?: return false
        
        return try {
            // Check trainer application status
            val applicationSnapshot = db.collection("trainer_applications")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            if (applicationSnapshot.isEmpty) return false

            val application = applicationSnapshot.documents[0]
            val status = application.getString("status")

            if (status != "approved") return false

            // Verify trainer document exists
            val trainerDoc = db.collection("trainer")
                .document(currentUser.uid)
                .get()
                .await()

            trainerDoc.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getTrainerStatus(): String {
        val currentUser = auth.currentUser ?: return "not_logged_in"
        
        return try {
            val applicationSnapshot = db.collection("trainer_applications")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            if (applicationSnapshot.isEmpty) return "not_trainer"

            val application = applicationSnapshot.documents[0]
            application.getString("status") ?: "unknown"
        } catch (e: Exception) {
            "error"
        }
    }

    fun checkTrainerStatus(onComplete: (isTrainer: Boolean, status: String) -> Unit) {
        val currentUser = auth.currentUser ?: run {
            onComplete(false, "not_logged_in")
            return
        }

        db.collection("trainer_applications")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onComplete(false, "not_trainer")
                    return@addOnSuccessListener
                }

                val application = documents.documents[0]
                val status = application.getString("status") ?: "unknown"

                if (status == "approved") {
                    // Verify trainer document exists
                    db.collection("trainer")
                        .document(currentUser.uid)
                        .get()
                        .addOnSuccessListener { trainerDoc ->
                            onComplete(trainerDoc.exists(), status)
                        }
                        .addOnFailureListener {
                            onComplete(false, "error")
                        }
                } else {
                    onComplete(false, status)
                }
            }
            .addOnFailureListener {
                onComplete(false, "error")
            }
    }
} 