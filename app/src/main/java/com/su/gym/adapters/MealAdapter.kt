package com.su.gym.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.su.gym.databinding.ItemMealBinding
import com.su.gym.models.NutritionPlan

class MealAdapter : ListAdapter<NutritionPlan.Meal, MealAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemMealBinding.inflate(
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
        private val binding: ItemMealBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(meal: NutritionPlan.Meal) {
            binding.apply {
                mealNameText.text = meal.type.ifEmpty { "Untitled Meal" }
                mealTimeText.visibility = View.GONE
                
                // Show calories if available
                caloriesText.text = if (meal.calories.isNotEmpty()) {
                    "${meal.calories} calories"
                } else {
                    "Calories not set"
                }

                // Calculate macros
                val protein = meal.protein.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
                val carbs = meal.carbs.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
                val fats = meal.fats.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0

                // Display macros
                if (protein == 0 && carbs == 0 && fats == 0) {
                    macrosText.text = "Macros not set"
                } else {
                    macrosText.text = "P: ${protein}g  C: ${carbs}g  F: ${fats}g"
                }

                // Show foods
                if (meal.foods.isEmpty()) {
                    foodsText.text = "No foods added"
                } else {
                    foodsText.text = meal.foods.joinToString("\n") { "• $it" }
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NutritionPlan.Meal>() {
            override fun areItemsTheSame(oldItem: NutritionPlan.Meal, newItem: NutritionPlan.Meal): Boolean {
                return oldItem.type == newItem.type
            }

            override fun areContentsTheSame(oldItem: NutritionPlan.Meal, newItem: NutritionPlan.Meal): Boolean {
                return oldItem == newItem
            }
        }
    }
} 