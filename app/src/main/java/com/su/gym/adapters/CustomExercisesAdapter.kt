package com.su.gym.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.su.gym.databinding.ItemCustomExerciseBinding
import com.su.gym.models.CustomExercise

class CustomExercisesAdapter : RecyclerView.Adapter<CustomExercisesAdapter.CustomExerciseViewHolder>() {
    private var exercises = listOf<CustomExercise>()

    fun updateExercises(newExercises: List<CustomExercise>) {
        exercises = newExercises
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomExerciseViewHolder {
        val binding = ItemCustomExerciseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CustomExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomExerciseViewHolder, position: Int) {
        holder.bind(exercises[position])
    }

    override fun getItemCount() = exercises.size

    inner class CustomExerciseViewHolder(
        private val binding: ItemCustomExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(exercise: CustomExercise) {
            binding.apply {
                exerciseNameText.text = exercise.name
                setsText.text = "${exercise.sets ?: 0} sets"
                repsText.text = "${exercise.reps ?: 0} reps"
            }
        }
    }
} 