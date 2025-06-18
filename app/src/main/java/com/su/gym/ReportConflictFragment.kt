package com.su.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.databinding.FragmentReportConflictBinding
import java.util.*

class ReportConflictFragment : Fragment() {
    private var _binding: FragmentReportConflictBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val priorities = listOf(
        "low",
        "medium",
        "high",
        "urgent"
    )

    private data class TrainerInfo(
        val id: String,
        val name: String
    )
    private val trainersList = mutableListOf<TrainerInfo>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportConflictBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupDropdowns()
        loadTrainers()
        setupSubmitButton()
    }

    private fun loadTrainers() {
        binding.progressBar.visibility = View.VISIBLE
        
        db.collection("trainer")
            .get()
            .addOnSuccessListener { documents ->
                trainersList.clear()
                for (document in documents) {
                    val name = document.getString("name") ?: continue
                    trainersList.add(TrainerInfo(document.id, name))
                }
                
                // Setup trainer dropdown
                val trainerNames = trainersList.map { it.name }
                val trainerAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    trainerNames
                )
                (binding.dropdownTrainer as? AutoCompleteTextView)?.setAdapter(trainerAdapter)
                
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Error loading trainers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupDropdowns() {
        // Setup Priority Dropdown
        val priorityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            priorities
        )
        (binding.dropdownPriority as? AutoCompleteTextView)?.setAdapter(priorityAdapter)
    }

    private fun setupSubmitButton() {
        binding.buttonSubmit.setOnClickListener {
            submitReport()
        }
    }

    private fun submitReport() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Please log in to submit a report", Toast.LENGTH_SHORT).show()
            return
        }

        val description = binding.editTextDescription.text.toString().trim()
        val priority = binding.dropdownPriority.text.toString().toLowerCase(Locale.ROOT)
        val selectedTrainerName = binding.dropdownTrainer.text.toString()

        if (description.isEmpty() || priority.isEmpty() || selectedTrainerName.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Find trainer ID from selected name
        val selectedTrainer = trainersList.find { it.name == selectedTrainerName }
        if (selectedTrainer == null) {
            Toast.makeText(context, "Please select a valid trainer", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.buttonSubmit.isEnabled = false

        val timestamp = com.google.firebase.Timestamp.now()
        
        val conflictData = hashMapOf(
            "userId" to currentUser.uid,
            "trainerId" to selectedTrainer.id,
            "description" to description,
            "priority" to priority,
            "status" to "pending",
            "createdAt" to timestamp,
            "updatedAt" to timestamp,
            "createdBy" to currentUser.uid
        )

        db.collection("conflicts")
            .add(conflictData)
            .addOnSuccessListener { documentReference ->
                binding.progressBar.visibility = View.GONE
                binding.buttonSubmit.isEnabled = true
                Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show()
                clearForm()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.buttonSubmit.isEnabled = true
                Toast.makeText(context, "Error submitting report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearForm() {
        binding.editTextDescription.text?.clear()
        binding.dropdownPriority.text?.clear()
        binding.dropdownTrainer.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 