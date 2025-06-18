package com.su.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class FeedbackFragment : Fragment() {
    private lateinit var subjectInput: TextInputEditText
    private lateinit var messageInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var submitButton: MaterialButton

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feedback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        subjectInput = view.findViewById(R.id.subjectInput)
        messageInput = view.findViewById(R.id.messageInput)
        emailInput = view.findViewById(R.id.emailInput)
        submitButton = view.findViewById(R.id.submitButton)

        // Check if user is logged in
        if (auth.currentUser == null) {
            // Navigate to login
            Toast.makeText(context, "Please login to submit feedback", Toast.LENGTH_LONG).show()
            try {
                findNavController().navigate(R.id.loginFragment)
            } catch (e: Exception) {
                // Handle navigation error
                activity?.onBackPressed()
            }
            return
        }

        // Pre-fill email if user is logged in
        auth.currentUser?.email?.let { email ->
            emailInput.setText(email)
        }

        submitButton.setOnClickListener {
            submitFeedback()
        }
    }

    private fun submitFeedback() {
        // Check if user is still logged in
        if (auth.currentUser == null) {
            Toast.makeText(context, "Please login to submit feedback", Toast.LENGTH_LONG).show()
            try {
                findNavController().navigate(R.id.loginFragment)
            } catch (e: Exception) {
                // Handle navigation error
                activity?.onBackPressed()
            }
            return
        }

        val subject = subjectInput.text.toString().trim()
        val message = messageInput.text.toString().trim()
        val email = emailInput.text.toString().trim()

        if (subject.isEmpty()) {
            subjectInput.error = "Please enter a subject"
            return
        }

        if (message.isEmpty()) {
            messageInput.error = "Please enter your message"
            return
        }

        submitButton.isEnabled = false

        val feedback = hashMapOf(
            "subject" to subject,
            "message" to message,
            "email" to email,
            "userId" to auth.currentUser!!.uid,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "pending"
        )

        db.collection("feedback")
            .add(feedback)
            .addOnSuccessListener {
                Toast.makeText(context, "Feedback submitted successfully", Toast.LENGTH_SHORT).show()
                // Clear inputs
                subjectInput.text?.clear()
                messageInput.text?.clear()
                emailInput.text?.clear()
                submitButton.isEnabled = true
                // Navigate back safely
                try {
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    // If navigation fails, just finish the activity
                    activity?.onBackPressed()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error submitting feedback: ${e.message}", Toast.LENGTH_SHORT).show()
                submitButton.isEnabled = true
            }
    }
} 