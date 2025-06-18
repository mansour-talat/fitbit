package com.su.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.su.gym.adapters.CustomExercisesAdapter
import com.su.gym.databinding.FragmentMainScreenBinding
import com.su.gym.utils.HomeProgressCard
import com.su.gym.models.CustomExercise

class MainScreenFragment : Fragment() {
    private var _binding: FragmentMainScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeProgressCard: HomeProgressCard
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userCalorieLimit = 2000 // Default value
    private val customExercisesAdapter = CustomExercisesAdapter()
    private var snapshotListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize HomeProgressCard
        homeProgressCard = HomeProgressCard(
            view = view,
            greetingText = binding.greetingText,
            progressIndicator = binding.calorieProgressIndicator,
            percentageText = binding.caloriePercentageText,
            calorieCountText = binding.calorieCountText
        )

        // Set up RecyclerView and load data
        setupCustomExercisesRecyclerView()
        setupAddExerciseButton()
        loadCustomExercises()

        // Load user data
        loadUserData()

        // Set up click listeners


        binding.bmiCalculatorButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainScreenFragment_to_bmiCalculator)
        }

        // Add navigation for feedback button
        binding.feedbackButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainScreenFragment_to_feedbackFragment)
        }

        // Add navigation for conflicts button
        binding.conflictsButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainScreenFragment_to_dialog_report_conflict)
        }
        binding.nutritionMainButton.setOnClickListener{
            findNavController().navigate(R.id.action_mainScreenFragment_to_navigation_food_history)

        }

        // Add navigation for nutrition button


    }

    private fun setupCustomExercisesRecyclerView() {
        binding.customExercisesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = customExercisesAdapter
        }
    }

    private fun setupAddExerciseButton() {
        binding.addExerciseButton.setOnClickListener {
            addSampleExercise()
        }
    }

    private fun addSampleExercise() {
        val userId = auth.currentUser?.uid ?: return
        
        val exercise = CustomExercise(
            id = db.collection("custom_exercises").document().id,
            userId = userId,
            name = "Push-ups",
            sets = 3,
            reps = 12
        )

        db.collection("custom_exercises")
            .document(exercise.id)
            .set(exercise)
            .addOnSuccessListener {
                Toast.makeText(context, "Exercise added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name") ?: "User"
                    userCalorieLimit = document.getLong("calorieLimit")?.toInt() ?: 2000

                    // Update progress card
                    homeProgressCard.setUserName(name)
                    homeProgressCard.setCalorieLimit(userCalorieLimit)

                    // Load today's nutrition data
                    loadTodayNutritionData()
                }
            }
    }

    private fun loadTodayNutritionData() {
        val userId = auth.currentUser?.uid ?: return
        val today = java.time.LocalDate.now().toString()

        db.collection("nutrition")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { documents ->
                var totalCalories = 0
                var totalProtein = 0
                var totalCarbs = 0
                var totalFat = 0

                for (document in documents) {
                    totalCalories += document.getLong("calories")?.toInt() ?: 0
                    totalProtein += document.getLong("protein")?.toInt() ?: 0
                    totalCarbs += document.getLong("carbs")?.toInt() ?: 0
                    totalFat += document.getLong("fat")?.toInt() ?: 0
                }

                // Update progress card with both current calories and limit
                homeProgressCard.updateProgress(totalCalories, userCalorieLimit)
            }
    }

    private fun loadCustomExercises() {
        val userId = auth.currentUser?.uid ?: return
        
        snapshotListener = db.collection("custom_exercises")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    return@addSnapshotListener
                }

                val exercises = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CustomExercise::class.java)
                } ?: listOf()

                if (isAdded && _binding != null) {
                    customExercisesAdapter.updateExercises(exercises)
                    updateEmptyState(exercises.isEmpty())
                }
            }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        _binding?.let { binding ->
            binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.customExercisesRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snapshotListener?.remove() // Remove the snapshot listener
        _binding = null
    }
} 