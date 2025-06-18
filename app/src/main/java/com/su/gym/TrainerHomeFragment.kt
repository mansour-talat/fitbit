package com.su.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.databinding.FragmentTrainerHomeBinding
import java.text.SimpleDateFormat
import java.util.*

class TrainerHomeFragment : Fragment() {

    private var _binding: FragmentTrainerHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Load trainer data
        loadTrainerData()

        // Set up click listeners
        setupClickListeners()
    }

    private fun loadTrainerData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Get trainer data
            db.collection("trainer").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "Trainer"
                        binding.textViewWelcome.text = "Welcome, $name"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error loading trainer data: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            // Get client count
            db.collection("trainer").document(user.uid)
                .collection("clients")
                .get()
                .addOnSuccessListener { clients ->
                    binding.textViewClientCount.text = "${clients.size()} Active Clients"
                }

            // Get today's sessions
            val today = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = dateFormat.format(today.time)

            db.collection("trainer").document(user.uid)
                .collection("sessions")
                .whereEqualTo("date", todayStr)
                .get()
                .addOnSuccessListener { sessions ->
                    binding.textViewSessionCount.text = "${sessions.size()} Sessions Today"
                }

            // Get total sessions and rating
            db.collection("trainer").document(user.uid)
                .collection("sessions")
                .get()
                .addOnSuccessListener { sessions ->
                    binding.textViewTotalSessions.text = "Total Sessions: ${sessions.size()}"
                    
                    var totalRating = 0.0
                    var ratedSessions = 0
                    for (session in sessions) {
                        val rating = session.getDouble("rating")
                        if (rating != null) {
                            totalRating += rating
                            ratedSessions++
                        }
                    }
                    val averageRating = if (ratedSessions > 0) {
                        String.format("%.1f", totalRating / ratedSessions)
                    } else {
                        "0.0"
                    }
                    binding.textViewRating.text = "Average Rating: $averageRating"
                }
        }
    }

    private fun setupClickListeners() {
        binding.cardViewClients.setOnClickListener {
            // Navigate to clients fragment
            (activity as? TrainerMainActivity)?.let { activity ->
                activity.navController.navigate(R.id.nav_trainer_clients)
            }
        }

        binding.cardViewSchedule.setOnClickListener {
            // Navigate to schedule fragment
            (activity as? TrainerMainActivity)?.let { activity ->
                activity.navController.navigate(R.id.nav_trainer_schedule)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 