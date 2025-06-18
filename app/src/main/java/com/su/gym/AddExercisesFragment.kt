package com.su.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.databinding.FragmentAddExercisesBinding

class AddExercisesFragment : Fragment() {
    private var _binding: FragmentAddExercisesBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExercisesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addExerciseButton.setOnClickListener {
            val exerciseName = binding.exerciseNameInput.text.toString().trim()
            
            if (exerciseName.isEmpty()) {
                binding.exerciseNameInput.error = "Exercise name is required"
                return@setOnClickListener
            }

            // Get optional fields
            val time = binding.exerciseTimeInput.text.toString().toIntOrNull()
            val sets = binding.setsInput.text.toString().toIntOrNull()
            val reps = binding.repsInput.text.toString().toIntOrNull()

            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(context, "Please sign in to add exercises", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val exercise = hashMapOf(
                "name" to exerciseName,
                "userId" to userId,
                "createdAt" to System.currentTimeMillis()
            ).apply {
                time?.let { put("time", it) }
                sets?.let { put("sets", it) }
                reps?.let { put("reps", it) }
            }

            db.collection("custom_exercises")
                .add(exercise)
                .addOnSuccessListener {
                    Toast.makeText(context, "Exercise added successfully", Toast.LENGTH_SHORT).show()
                    clearInputs()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error adding exercise: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun clearInputs() {
        binding.exerciseNameInput.text?.clear()
        binding.exerciseTimeInput.text?.clear()
        binding.setsInput.text?.clear()
        binding.repsInput.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 