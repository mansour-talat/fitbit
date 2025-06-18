package com.su.gym

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.su.gym.databinding.FragmentRegistrationBinding
import com.su.gym.model.User

class RegistrationFragment : Fragment() {

    private var _binding: FragmentRegistrationBinding? = null
    private val binding get() = _binding!!
    private var imageUri: Uri? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val storage = FirebaseStorage.getInstance()

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUri = uri
                binding.imageViewProfile.setImageURI(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onResume() {
        super.onResume()
        // Ensure the drawer is hidden when on the registration screen
        try {
            (requireActivity() as? MainActivity)?.setDrawerVisibility(false)
        } catch (e: Exception) {
            // Ignore errors
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.imageViewProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            getContent.launch(intent)
        }

        binding.buttonRegister.setOnClickListener {
            registerUser()
        }

        binding.textViewLogin.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
    }

    private fun registerUser() {
        val email = binding.editTextEmail.text.toString()
        val password = binding.editTextPassword.text.toString()
        val name = binding.editTextName.text.toString()
        
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        // Create user document in Firestore
                        val userData = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "role" to "user"
                        )
                        
                        db.collection("users")
                            .document(it.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                // Sign out the user since they need to login first
                                auth.signOut()
                                Toast.makeText(context, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                                // Navigate to login page
                                findNavController().navigate(R.id.loginFragment)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 