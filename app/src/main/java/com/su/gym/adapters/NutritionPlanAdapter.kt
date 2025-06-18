package com.su.gym.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.su.gym.databinding.ItemNutritionPlanBinding
import com.su.gym.models.NutritionPlan

class NutritionPlanAdapter(
    private val onPlanClick: (NutritionPlan) -> Unit
) : ListAdapter<NutritionPlan, NutritionPlanAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemNutritionPlanBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemNutritionPlanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.viewDetailsButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPlanClick(getItem(position))
                }
            }
        }

        fun bind(plan: NutritionPlan) {
            binding.apply {
                planNameText.text = plan.name.ifEmpty { "Untitled Plan" }
                planDescriptionText.text = plan.description.ifEmpty { "No description available" }
                
                // Show type if available
                typeChip.apply {
                    text = plan.type.ifEmpty { "General" }
                    visibility = View.VISIBLE
                }

                // Show duration if available
                durationChip.apply {
                    text = if (plan.duration.isNotEmpty()) "${plan.duration} weeks" else "Duration not set"
                    visibility = View.VISIBLE
                }

                // Show target calories if available
                caloriesChip.apply {
                    text = if (plan.targetCalories.isNotEmpty()) "${plan.targetCalories} calories" else "Calories not set"
                    visibility = View.VISIBLE
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NutritionPlan>() {
            override fun areItemsTheSame(oldItem: NutritionPlan, newItem: NutritionPlan): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: NutritionPlan, newItem: NutritionPlan): Boolean {
                return oldItem == newItem
            }
        }
    }
} 