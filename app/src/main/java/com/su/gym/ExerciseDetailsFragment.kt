package com.su.gym

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.databinding.FragmentExerciseDetailsBinding
import com.su.gym.models.Exercise

class ExerciseDetailsFragment : Fragment() {
    private var _binding: FragmentExerciseDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val exerciseId = arguments?.getString("exerciseId")
        if (exerciseId == null) {
            showError("Exercise not found")
            return
        }

        loadExerciseDetails(exerciseId)
    }

    private fun loadExerciseDetails(exerciseId: String) {
        showLoading(true)
        FirebaseFirestore.getInstance()
            .collection("workout-plans")
            .document(exerciseId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val exercise = Exercise.fromDocument(document)
                    displayExercise(exercise)
                } else {
                    showError("Exercise not found")
                }
            }
            .addOnFailureListener {
                showError("Failed to load exercise details")
            }
            .addOnCompleteListener {
                showLoading(false)
            }
    }

    private fun displayExercise(exercise: Exercise) {
        binding.apply {
            // Load GIF if available
            if (exercise.gifUrl.isNotEmpty()) {
                exerciseGif.visibility = View.VISIBLE
                Glide.with(exerciseGif)
                    .asGif()
                    .load(exercise.gifUrl)
                    .centerCrop()
                    .into(exerciseGif)
            } else {
                exerciseGif.visibility = View.GONE
            }

            // Display basic information
            titleText.text = exercise.title
            categoryChip.text = exercise.category
            difficultyChip.text = exercise.difficulty
            descriptionText.text = exercise.description

            // Display exercise parameters
            durationText.text = "Duration: ${exercise.duration}"
            setsText.text = "Sets: ${exercise.sets}"
            repsText.text = "Reps: ${exercise.reps}"
            restTimeText.text = "Rest Time: ${exercise.restTime}"
            equipmentText.text = "Equipment: ${exercise.equipment}"

            // Display target areas
            muscleGroupsChip.text = exercise.muscleGroups

            // Display instructions
            Log.d("ExerciseDetails", "Instructions: ${exercise.instructions}")
            val instructionsText = if (exercise.instructions.isEmpty()) {
                "No instructions available"
            } else {
                exercise.instructions.joinToString("\n") { "• $it" }
            }
            instructionsTextView.text = instructionsText
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.contentLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 