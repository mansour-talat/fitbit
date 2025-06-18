package com.su.gym.base

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.R
import com.su.gym.TrainerMainActivity

abstract class BaseFragment<VB : ViewBinding> : Fragment() {
    private var _binding: VB? = null
    protected val binding get() = _binding!!
    protected val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    protected val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = createBinding(inflater, container)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    protected fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    protected fun checkAuthAndNavigate() {
        if (!isAdded || isDetached) return

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Check if user is in trainer collection
            db.collection("trainer")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { trainerDoc ->
                    if (!isAdded || isDetached) return@addOnSuccessListener

                    if (trainerDoc.exists()) {
                        // Trainer - launch TrainerMainActivity
                        val intent = Intent(requireContext(), TrainerMainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        // Regular user - navigate to home
                        findNavController().navigate(R.id.homeFragment)
                    }
                }
                .addOnFailureListener { e ->
                    if (!isAdded || isDetached) return@addOnFailureListener
                    showToast("Error checking user status: ${e.message}")
                    auth.signOut()
                    findNavController().navigate(R.id.nav_login)
                }
        } else {
            findNavController().navigate(R.id.nav_login)
        }
    }
} 