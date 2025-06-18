package com.su.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.adapters.NutritionPlanAdapter
import com.su.gym.databinding.FragmentNutritionPlansBinding
import com.su.gym.models.NutritionPlan

class NutritionPlansFragment : Fragment() {
    private var _binding: FragmentNutritionPlansBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: NutritionPlanAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNutritionPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = NutritionPlanAdapter { plan ->
            val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
            val bundle = Bundle().apply { putString("planId", plan.id) }
            navController.navigate(R.id.nutritionPlanDetailsFragment, bundle)
        }
        
        setupRecyclerView()
        loadNutritionPlans()
    }

    private fun setupRecyclerView() {
        binding.nutritionPlansRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@NutritionPlansFragment.adapter
        }
    }

    private fun loadNutritionPlans() {
        showLoading(true)
        FirebaseFirestore.getInstance()
            .collection("nutrition-plans")
            .get()
            .addOnSuccessListener { documents ->
                val plans = documents.mapNotNull { doc ->
                    try {
                        NutritionPlan.fromDocument(doc)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (plans.isEmpty()) {
                    showEmptyState()
                } else {
                    adapter.submitList(plans)
                    showContent()
                }
            }
            .addOnFailureListener {
                showError("Failed to load nutrition plans")
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
            nutritionPlansRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        }
    }

    private fun showContent() {
        binding.apply {
            nutritionPlansRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 