package com.su.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.databinding.FragmentHomeBinding
import com.su.gym.utils.HomeProgressCard

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeProgressCard: HomeProgressCard
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userCalorieLimit = 2000 // Default value

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
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

        // Set up RecyclerView
        binding.recentActivityRecyclerView.layoutManager = LinearLayoutManager(context)

        // Load user data
        loadUserData()

        // Set up click listeners
        binding.startWorkoutCard.setOnClickListener {
            // Navigate to workout screen
        }

        binding.addFoodCard.setOnClickListener {
            // Navigate to add food screen
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 