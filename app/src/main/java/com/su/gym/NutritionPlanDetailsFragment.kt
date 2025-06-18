package com.su.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.adapters.MealAdapter
import com.su.gym.databinding.FragmentNutritionPlanDetailsBinding
import com.su.gym.models.NutritionPlan
import androidx.recyclerview.widget.LinearLayoutManager

class NutritionPlanDetailsFragment : Fragment() {
    private var _binding: FragmentNutritionPlanDetailsBinding? = null
    private val binding get() = _binding!!
    private var planId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        planId = arguments?.getString("planId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNutritionPlanDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (planId == null) {
            showError("Plan not found")
            findNavController().navigateUp()
            return
        }

        loadPlanDetails()
    }

    private fun loadPlanDetails() {
        showLoading(true)
        FirebaseFirestore.getInstance()
            .collection("nutrition-plans")
            .document(planId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val plan = NutritionPlan.fromDocument(document)
                        displayPlanDetails(plan)
                    } catch (e: Exception) {
                        showError("Error loading plan details")
                    }
                } else {
                    showError("Plan not found")
                    findNavController().navigateUp()
                }
            }
            .addOnFailureListener {
                showError("Failed to load plan details")
                findNavController().navigateUp()
            }
            .addOnCompleteListener {
                showLoading(false)
            }
    }

    private fun displayPlanDetails(plan: NutritionPlan) {
        binding.apply {
            // Plan header
            planNameText.text = plan.name.ifEmpty { "Untitled Plan" }
            planDescriptionText.text = plan.description.ifEmpty { "No description available" }
            planDurationText.text = if (plan.duration.isNotEmpty()) "Duration: ${plan.duration}" else ""
            planCreatedAtText.text = plan.createdAt?.toDate()?.let { "Created: ${android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", it)}" } ?: ""
            planUpdatedAtText.text = plan.updatedAt?.toDate()?.let { "Updated: ${android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", it)}" } ?: ""

            // Macros
            proteinText.text = "Protein: ${plan.macros["protein"] ?: "0"}g"
            carbsText.text = "Carbs: ${plan.macros["carbs"] ?: "0"}g"
            fatsText.text = "Fats: ${plan.macros["fats"] ?: "0"}g"

            // Guidelines
            guidelinesText.text = if (plan.guidelines.isNotEmpty()) plan.guidelines.joinToString("\n") { "• $it" } else "No guidelines."

            // Restrictions
            restrictionsText.text = if (plan.restrictions.isNotEmpty()) plan.restrictions.joinToString("\n") { "• $it" } else "No restrictions."

            // Target calories
            targetCaloriesText.text = if (plan.targetCalories.isNotEmpty()) {
                "${plan.targetCalories} calories per day"
            } else {
                "Target calories not set"
            }
            
            // Meals
            if (plan.meals.isEmpty()) {
                mealsEmptyText.visibility = View.VISIBLE
                mealsRecyclerView.visibility = View.GONE
            } else {
                mealsEmptyText.visibility = View.GONE
                mealsRecyclerView.visibility = View.VISIBLE
                
                // Set up meals adapter
                mealsRecyclerView.layoutManager = LinearLayoutManager(context)

                val mealsAdapter = MealAdapter()
                mealsRecyclerView.adapter = mealsAdapter
                mealsAdapter.submitList(plan.meals)
            }
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