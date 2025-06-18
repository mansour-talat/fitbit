package com.su.gym

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.adapters.ExerciseAdapter
import com.su.gym.databinding.FragmentWorkoutPlansBinding
import com.su.gym.models.Exercise

class WorkoutPlansFragment : Fragment() {
    private var _binding: FragmentWorkoutPlansBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: ExerciseAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAdapter()
        setupRecyclerView()
        loadExercises()
    }

    private fun setupAdapter() {
        adapter = ExerciseAdapter { exercise ->
            Log.d("WorkoutPlansFragment", "Exercise clicked: ${exercise.id}")
            try {
                Log.d("WorkoutPlansFragment", "Attempting to find NavController")
                // Get the NavController from the activity
                val navController = (requireActivity() as MainActivity).navController
                Log.d("WorkoutPlansFragment", "Found NavController, navigating to exercise details")
                navController.navigate(
                    R.id.exerciseDetailsFragment,
                    Bundle().apply { putString("exerciseId", exercise.id) }
                )
            } catch (e: Exception) {
                Log.e("WorkoutPlansFragment", "Navigation failed", e)
                showError("Failed to navigate: ${e.message}")
            }
        }
    }

    private fun setupRecyclerView() {
        binding.exercisesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@WorkoutPlansFragment.adapter
            visibility = View.VISIBLE
        }
    }

    private fun loadExercises() {
        showLoading(true)
        FirebaseFirestore.getInstance()
            .collection("workout-plans")
            .get()
            .addOnSuccessListener { documents ->
                val exercises = documents.mapNotNull { doc ->
                    try {
                        Exercise.fromDocument(doc)
                    } catch (e: Exception) {
                        Log.e("WorkoutPlansFragment", "Error parsing exercise document", e)
                        null
                    }
                }
                
                if (exercises.isEmpty()) {
                    showEmptyState()
                } else {
                    Log.d("WorkoutPlansFragment", "Loaded ${exercises.size} exercises")
                    adapter.submitList(exercises)
                    showContent()
                }
            }
            .addOnFailureListener { e ->
                Log.e("WorkoutPlansFragment", "Failed to load exercises", e)
                showError("Failed to load exercises")
            }
            .addOnCompleteListener {
                showLoading(false)
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyState() {
        binding.apply {
            exercisesRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        }
    }

    private fun showContent() {
        binding.apply {
            exercisesRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 