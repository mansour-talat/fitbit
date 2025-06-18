package com.su.gym.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.su.gym.R
import com.su.gym.model.Trainer

class TrainerAdapter(
    private val onTrainerClick: (String) -> Unit,
    private val onRateTrainer: (Trainer) -> Unit,
    private val onReportConflict: (Trainer) -> Unit
) : RecyclerView.Adapter<TrainerAdapter.TrainerViewHolder>() {

    companion object {
        private const val TAG = "TrainerAdapter"
    }

    private val trainers = mutableListOf<Trainer>()

    fun submitList(newTrainers: List<Trainer>) {
        trainers.clear()
        trainers.addAll(newTrainers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainerViewHolder {
        Log.d(TAG, "Creating new ViewHolder")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trainer, parent, false)
        return TrainerViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainerViewHolder, position: Int) {
        Log.d(TAG, "Binding ViewHolder at position $position")
        val trainer = trainers[position]
        holder.bind(trainer)
    }

    override fun getItemCount(): Int = trainers.size

    inner class TrainerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewName)
        private val specializationTextView: TextView = itemView.findViewById(R.id.textViewSpecialization)
        private val ratingTextView: TextView = itemView.findViewById(R.id.textViewRating)
        private val chatButton: MaterialButton = itemView.findViewById(R.id.buttonChat)
        private val overflowButton: AppCompatImageButton = itemView.findViewById(R.id.buttonOverflow)

        init {
            Log.d(TAG, "Initializing ViewHolder")
            
            // Set up overflow button click listener
            overflowButton.setOnClickListener { view ->
                Log.d(TAG, "Overflow button clicked in ViewHolder")
                val position = adapterPosition
                Log.d(TAG, "Position: $position, Is position valid: ${position != RecyclerView.NO_POSITION}")
                
                if (position != RecyclerView.NO_POSITION) {
                    val trainer = trainers[position]
                    Log.d(TAG, "Showing popup menu for trainer: ${trainer.name}")
                    showPopupMenu(view, trainer)
                }
            }
        }

        fun bind(trainer: Trainer) {
            Log.d(TAG, "Binding trainer: ${trainer.name}")
            
            nameTextView.text = trainer.name
            specializationTextView.text = trainer.specialization
            ratingTextView.text = "Rating: ${String.format("%.1f", trainer.rating)}"
            
            chatButton.setOnClickListener {
                onTrainerClick(trainer.id)
            }
        }

        private fun showPopupMenu(view: View, trainer: Trainer) {
            try {
                Log.d(TAG, "Creating popup menu for trainer: ${trainer.name}")
                val popup = PopupMenu(view.context, view)
                
                // Inflate menu
                popup.menuInflater.inflate(R.menu.trainer_item_menu, popup.menu)
                
                // Set up click listener
                popup.setOnMenuItemClickListener { menuItem ->
                    Log.d(TAG, "Menu item clicked: ${menuItem.title}")
                    when (menuItem.itemId) {
                        R.id.action_rate_trainer -> {
                            Log.d(TAG, "Rate trainer selected for: ${trainer.name}")
                            onRateTrainer(trainer)
                            true
                        }
                        R.id.action_report_conflict -> {
                            Log.d(TAG, "Report conflict selected for: ${trainer.name}")
                            onReportConflict(trainer)
                            true
                        }
                        else -> false
                    }
                }

                // Show the popup menu
                Log.d(TAG, "Showing popup menu")
                popup.show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing popup menu", e)
                e.printStackTrace()
                Toast.makeText(view.context, "Error showing menu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 