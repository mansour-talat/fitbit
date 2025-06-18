package com.su.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.adapter.TrainerAdapter
import com.su.gym.databinding.FragmentTrainerListBinding
import com.su.gym.model.Trainer

class TrainerListFragment : Fragment() {

    private var _binding: FragmentTrainerListBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var trainerAdapter: TrainerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainerListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        setupRecyclerView()
        loadTrainers()
    }

    private fun setupRecyclerView() {
        trainerAdapter = TrainerAdapter(
            onTrainerClick = { trainerId ->
                navigateToChat(trainerId)
            },
            onRateTrainer = { trainer ->
                showRatingDialog(trainer)
            },
            onReportConflict = { trainer ->
                showReportDialog(trainer)
            }
        )
        
        binding.recyclerViewTrainers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = trainerAdapter
        }
    }

    private fun loadTrainers() {
        _binding?.progressBar?.visibility = View.VISIBLE
        _binding?.textViewNoTrainers?.visibility = View.GONE

        // Check if user is authenticated
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _binding?.progressBar?.visibility = View.GONE
            Toast.makeText(context, "Please log in to view trainers", Toast.LENGTH_SHORT).show()
            // Navigate to login screen
            findNavController().navigate(R.id.nav_login)
            return
        }

        // Try to access the trainers collection
        db.collection("trainer")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                
                _binding?.progressBar?.visibility = View.GONE
                if (documents.isEmpty) {
                    _binding?.textViewNoTrainers?.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                val trainers = documents.mapNotNull { doc ->
                    try {
                        Trainer(
                            id = doc.id,
                            name = doc.getString("name") ?: "Unknown",
                            specialization = doc.getString("specialization") ?: "Not specified",
                            rating = doc.getDouble("rating") ?: 0.0,
                            profilePictureUrl = doc.getString("profilePictureUrl")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                if (isAdded) {
                    trainerAdapter.submitList(trainers)
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener
                
                _binding?.progressBar?.visibility = View.GONE
                context?.let { ctx ->
                    Toast.makeText(ctx, "Error loading trainers: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                
                // Check if it's a permission error
                if (e.message?.contains("permission-denied") == true) {
                    // Try to refresh the auth token
                    currentUser.getIdToken(true)
                        .addOnSuccessListener {
                            if (isAdded) {
                            // Retry loading trainers after refreshing the token
                            loadTrainers()
                        }
                        }
                        .addOnFailureListener { tokenError ->
                            if (!isAdded) return@addOnFailureListener
                            
                            context?.let { ctx ->
                                Toast.makeText(ctx, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
                            }
                            // Navigate to login screen
                            findNavController().navigate(R.id.nav_login)
                        }
                }
        }
    }

    private fun navigateToChat(trainerId: String) {
        try {
            // First check if the trainer exists
            db.collection("trainer")
                .document(trainerId)
                .get()
                .addOnSuccessListener { document ->
                    if (!isAdded) return@addOnSuccessListener
                    
                    if (document.exists()) {
                        try {
                            // Try to use Navigation Component
                            findNavController().navigate(R.id.chatFragment, Bundle().apply {
                                putString("trainerId", trainerId)
                            })
                        } catch (e: Exception) {
                            if (!isAdded) return@addOnSuccessListener
                            
                            // Fallback to direct fragment transaction
                            val fragment = ChatFragment().apply {
                                arguments = Bundle().apply {
                                    putString("trainerId", trainerId)
                                }
                            }
                            
                            // Get the main activity
                            val mainActivity = activity as? MainActivity
                            mainActivity?.supportFragmentManager?.beginTransaction()
                                ?.replace(R.id.nav_host_fragment, fragment)
                                ?.addToBackStack(null)
                                ?.commit()
                        }
                    } else {
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Trainer not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    if (!isAdded) return@addOnFailureListener
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Error checking trainer: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            context?.let { ctx ->
                Toast.makeText(ctx, "Error navigating to chat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRatingDialog(trainer: Trainer) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rate_trainer, null)
        val ratingSlider = dialogView.findViewById<Slider>(R.id.ratingSlider)
        val commentInput = dialogView.findViewById<TextInputEditText>(R.id.commentInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rate ${trainer.name}")
            .setView(dialogView)
            .setPositiveButton("Submit") { dialog, _ ->
                val rating = ratingSlider.value
                val comment = commentInput.text?.toString() ?: ""
                submitRating(trainer.id, rating, comment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitRating(trainerId: String, rating: Float, comment: String) {
        val userId = auth.currentUser?.uid ?: return
        
        val ratingData = hashMapOf(
            "userId" to userId,
            "rating" to rating,
            "comment" to comment,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection("trainer")
            .document(trainerId)
            .collection("ratings")
            .add(ratingData)
            .addOnSuccessListener {
                // Update average rating
                updateTrainerAverageRating(trainerId)
                Toast.makeText(context, "Rating submitted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to submit rating: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTrainerAverageRating(trainerId: String) {
        db.collection("trainer")
            .document(trainerId)
            .collection("ratings")
            .get()
            .addOnSuccessListener { ratings ->
                if (ratings.isEmpty) return@addOnSuccessListener
                
                val averageRating = ratings.documents
                    .mapNotNull { it.getDouble("rating")?.toDouble() }
                    .average()

                db.collection("trainer")
                    .document(trainerId)
                    .update("rating", averageRating)
                    .addOnSuccessListener {
                        // Refresh the trainer list to show updated rating
                        loadTrainers()
                    }
            }
    }

    private fun showReportDialog(trainer: Trainer) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_report_conflict, null)
        val reportInput = dialogView.findViewById<TextInputEditText>(R.id.reportInput)
        val reportInputLayout = dialogView.findViewById<TextInputLayout>(R.id.reportInputLayout)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Report Conflict with ${trainer.name}")
            .setView(dialogView)
            .setPositiveButton("Submit") { dialog, _ ->
                val report = reportInput.text?.toString() ?: ""
                if (report.isBlank()) {
                    reportInputLayout.error = "Please describe the conflict"
                    return@setPositiveButton
                }
                submitReport(trainer.id, report)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitReport(trainerId: String, report: String) {
        val userId = auth.currentUser?.uid ?: return
        
        val reportData = hashMapOf(
            "userId" to userId,
            "trainerId" to trainerId,
            "report" to report,
            "status" to "pending",
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection("reports")
            .add(reportData)
            .addOnSuccessListener {
                Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to submit report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class Trainer(
        val id: String,
        val name: String,
        val specialization: String,
        val rating: Double,
        val profilePictureUrl: String? = null
    )

    inner class TrainerAdapter(
        private val onTrainerClick: (String) -> Unit,
        private val onRateTrainer: (Trainer) -> Unit,
        private val onReportConflict: (Trainer) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<TrainerAdapter.TrainerViewHolder>() {

        private val trainers = mutableListOf<Trainer>()

        fun submitList(newTrainers: List<Trainer>) {
            trainers.clear()
            trainers.addAll(newTrainers)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_trainer, parent, false)
            return TrainerViewHolder(view)
        }

        override fun onBindViewHolder(holder: TrainerViewHolder, position: Int) {
            holder.bind(trainers[position])
        }

        override fun getItemCount(): Int = trainers.size

        inner class TrainerViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            private val nameTextView: android.widget.TextView = itemView.findViewById(R.id.textViewName)
            private val specializationTextView: android.widget.TextView = itemView.findViewById(R.id.textViewSpecialization)
            private val ratingTextView: android.widget.TextView = itemView.findViewById(R.id.textViewRating)
            private val profileImageView: android.widget.ImageView = itemView.findViewById(R.id.imageViewTrainer)

            fun bind(trainer: Trainer) {
                nameTextView.text = trainer.name
                specializationTextView.text = trainer.specialization
                ratingTextView.text = "Rating: ${String.format("%.1f", trainer.rating)}"
                
                // Load profile image using Glide
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(trainer.profilePictureUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(profileImageView)
                
                itemView.setOnClickListener {
                    onTrainerClick(trainer.id)
                }
            }
        }
    }
} 