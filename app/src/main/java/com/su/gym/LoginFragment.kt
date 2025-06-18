package com.su.gym

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.su.gym.base.BaseFragment
import com.su.gym.databinding.FragmentLoginBinding
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    
    companion object {
        private const val TAG = "LoginFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        binding.loginButton.setOnClickListener {
            login()
        }

        binding.textViewRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.textBecomeTrainer.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_trainer_registration)
        }
    }

    private fun handleUserLogin(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        // First check if user has a trainer application
                        db.collection("trainer_applications")
                            .document(userId)
                            .get()
                            .addOnSuccessListener { applicationDoc ->
                                if (applicationDoc.exists()) {
                                    val status = applicationDoc.getString("status")
                                    when (status) {
                                        "approved" -> {
                                            // Check if trainer document exists
                                            db.collection("trainer")
                                                .document(userId)
                                                .get()
                                                .addOnSuccessListener { trainerDoc ->
                                                    if (trainerDoc.exists()) {
                                                        // User is an approved trainer, launch TrainerMainActivity
                                                        val intent = Intent(requireContext(), TrainerMainActivity::class.java)
                                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        startActivity(intent)
                                                        requireActivity().finish()
                                                    } else {
                                                        // Something went wrong - trainer doc should exist
                                                        Toast.makeText(context, "Error: Trainer profile not found", Toast.LENGTH_SHORT).show()
                                                        auth.signOut()
                                                        findNavController().navigate(R.id.action_loginFragment_to_mainScreenFragment)
                                                    }
                                                }
                                        }
                                        "pending" -> {
                                            Toast.makeText(context, "Your trainer application is pending approval", Toast.LENGTH_LONG).show()
                                            auth.signOut()
                                            findNavController().navigate(R.id.action_loginFragment_to_mainScreenFragment)
                                        }
                                        "rejected" -> {
                                            Toast.makeText(context, "Your trainer application was not approved", Toast.LENGTH_LONG).show()
                                            auth.signOut()
                                            findNavController().navigate(R.id.action_loginFragment_to_mainScreenFragment)
                                        }
                                        else -> {
                                            // No trainer application, proceed as regular user
                                            findNavController().navigate(R.id.action_loginFragment_to_mainScreenFragment)
                                        }
                                    }
                                } else {
                                    // No trainer application, proceed as regular user
                                    findNavController().navigate(R.id.action_loginFragment_to_mainScreenFragment)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error checking trainer application status", e)
                                Toast.makeText(context, "Error verifying application status", Toast.LENGTH_SHORT).show()
                                auth.signOut()
                                findNavController().navigate(R.id.action_loginFragment_to_mainScreenFragment)
                            }
                    }
                } else {
                    Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleTrainerLogin(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    // Check if user is a trainer
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        db.collection("trainer").document(userId)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // User is a trainer, launch TrainerMainActivity
                                    val intent = Intent(requireContext(), TrainerMainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    requireActivity().finish()
                                } else {
                                    // User is not a trainer, show error
                                    Toast.makeText(context, "This account is not registered as a trainer", Toast.LENGTH_SHORT).show()
                                    auth.signOut()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error checking trainer status", e)
                                Toast.makeText(context, "Error verifying trainer status", Toast.LENGTH_SHORT).show()
                                auth.signOut()
                            }
                    }
                } else {
                    Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleTooManyAttempts(email: String) {
        // Send password reset email
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        context,
                        "Too many login attempts. A password reset email has been sent.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Failed to send password reset email. Please try again later.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun login() {
        val email = binding.editTextEmail.text.toString()
        val password = binding.editTextPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.loginButton.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                binding.loginButton.isEnabled = true

                if (task.isSuccessful) {
                    // Check if user is a trainer
                    val user = auth.currentUser
                    if (user != null) {
                        FirebaseFirestore.getInstance()
                            .collection("trainer")
                            .document(user.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // User is a trainer, launch TrainerMainActivity
                                    val intent = Intent(requireContext(), TrainerMainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    requireActivity().finish()
                                } else {
                                    // User is a regular user
                                    // Initialize drawer before navigation with a slight delay
                                    (requireActivity() as MainActivity).handlePostLogin()
                                    
                                    // Delay navigation slightly to allow drawer to initialize
                                    view?.postDelayed({
                                        if (isAdded) {  // Check if fragment is still attached
                                    findNavController().navigate(R.id.action_loginFragment_to_mainScreenFragment)
                                        }
                                    }, 150)  // Small delay to ensure drawer is ready
                                }
                            }
                            .addOnFailureListener {
                                // If there's an error checking trainer status, treat as regular user
                                // Initialize drawer before navigation with a slight delay
                                (requireActivity() as MainActivity).handlePostLogin()
                                
                                // Delay navigation slightly to allow drawer to initialize
                                view?.postDelayed({
                                    if (isAdded) {  // Check if fragment is still attached
                                findNavController().navigate(R.id.action_loginFragment_to_mainScreenFragment)
                                    }
                                }, 150)  // Small delay to ensure drawer is ready
                            }
                    }
                } else {
                    Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 