package com.su.gym.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.su.gym.databinding.ItemExerciseBinding
import com.su.gym.models.Exercise

class ExerciseAdapter(
    private val onExerciseClick: (Exercise) -> Unit
) : ListAdapter<Exercise, ExerciseAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemExerciseBinding.inflate(
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
        private val binding: ItemExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.viewDetailsButton.setOnClickListener {
                Log.d("ExerciseAdapter", "View Details button clicked")
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val exercise = getItem(position)
                    Log.d("ExerciseAdapter", "Calling onExerciseClick with exercise: ${exercise.id}")
                    onExerciseClick(exercise)
                } else {
                    Log.e("ExerciseAdapter", "Invalid adapter position")
                }
            }
        }

        fun bind(exercise: Exercise) {
            binding.apply {
                titleText.text = exercise.title.ifEmpty { "Untitled Exercise" }

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

                // Show category if available
                categoryChip.apply {
                    text = exercise.category.ifEmpty { "General" }
                    visibility = View.VISIBLE
                }

                // Show difficulty if available
                difficultyChip.apply {
                    text = exercise.difficulty.ifEmpty { "Not set" }
                    visibility = View.VISIBLE
                }

                // Show duration if available
                durationChip.apply {
                    text = if (exercise.duration.isNotEmpty()) exercise.duration else "Duration not set"
                    visibility = View.VISIBLE
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Exercise>() {
            override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
                return oldItem == newItem
            }
        }
    }
} 