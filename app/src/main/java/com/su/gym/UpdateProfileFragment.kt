package com.su.gym

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.su.gym.databinding.FragmentUpdateProfileBinding
import com.su.gym.model.UserProfile
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UpdateProfileFragment : Fragment() {
    private var _binding: FragmentUpdateProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.imageViewProfile.setImageURI(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupClickListeners()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.editTextName.setText(document.getString("name"))
                    binding.editTextEmail.setText(document.getString("email"))
                    binding.editTextAge.setText(document.getLong("age")?.toString())
                    binding.editTextHeight.setText(document.getDouble("height")?.toString())
                    binding.editTextWeight.setText(document.getDouble("weight")?.toString())

                    // Load profile image
                    document.getString("profileImageUrl")?.let { imageUrl ->
                        Glide.with(this)
                            .load(imageUrl)
                            .circleCrop()
                            .into(binding.imageViewProfile)
                    }
                }
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupClickListeners() {
        binding.buttonChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            getContent.launch(intent)
        }

        binding.buttonChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.buttonUpdate.setOnClickListener {
            updateProfile()
        }
    }

    private fun updateProfile() {
        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val age = binding.editTextAge.text.toString().toIntOrNull()
        val height = binding.editTextHeight.text.toString().toDoubleOrNull()
        val weight = binding.editTextWeight.text.toString().toDoubleOrNull()

        if (name.isEmpty() || email.isEmpty() || age == null || height == null || weight == null) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)

        // Check if email is being changed
        if (email != auth.currentUser?.email) {
            // Reauthenticate and update email
            auth.currentUser?.let { user ->
                // First update the profile data
                updateProfileDataWithEmail(userRef, name, email, age, height, weight, selectedImageUri)
            }
        } else {
            // No email change, just update profile data
            if (selectedImageUri != null) {
                uploadImageAndUpdateProfile(userRef, name, email, age, height, weight)
            } else {
                updateProfileData(userRef, name, email, age, height, weight, null)
            }
        }
    }

    private fun updateProfileDataWithEmail(
        userRef: com.google.firebase.firestore.DocumentReference,
        name: String,
        newEmail: String,
        age: Int,
        height: Double,
        weight: Double,
        imageUri: Uri?
    ) {
        // First update non-email profile data
        val updates = hashMapOf<String, Any>(
            "name" to name,
            "age" to age,
            "height" to height,
            "weight" to weight
        )

        userRef.update(updates)
            .addOnSuccessListener {
                // After basic profile is updated, handle email update
                if (newEmail != auth.currentUser?.email) {
                    showReauthenticationDialog(newEmail)
                } else if (imageUri != null) {
                    uploadImageAndUpdateProfile(userRef, name, newEmail, age, height, weight)
                } else {
                    binding.progressBar.visibility = View.GONE
                    showSuccessDialog()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Profile Updated")
            .setMessage("Your profile has been updated successfully.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                findNavController().navigateUp()
            }
            .setCancelable(false)
            .show()
    }

    private fun showReauthenticationDialog(newEmail: String) {
        if (!isAdded) return  // Check if fragment is attached
        
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reauthenticate, null)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.editTextPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("Authentication Required")
            .setMessage("Please enter your current password to verify your new email address.")
            .setView(dialogView)
            .setPositiveButton("Verify") { dialog, _ ->
                val password = passwordInput.text.toString()
                if (password.isNotEmpty()) {
                    reauthenticateAndUpdateEmail(password, newEmail)
                } else {
                    if (isAdded) {  // Check if fragment is still attached
                        Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun reauthenticateAndUpdateEmail(password: String, newEmail: String) {
        if (!isAdded) return  // Check if fragment is attached
        
        binding.progressBar.visibility = View.VISIBLE
        val user = auth.currentUser
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user?.email ?: "", password)

        user?.reauthenticate(credential)
            ?.addOnSuccessListener {
                // Send verification email to the new email address
                auth.currentUser?.verifyBeforeUpdateEmail(newEmail)
                    ?.addOnSuccessListener {
                        if (isAdded) {  // Check if fragment is still attached
                            binding.progressBar.visibility = View.GONE
                            showVerificationEmailSentDialog(newEmail)
                        }
                    }
                    ?.addOnFailureListener { e ->
                        if (isAdded) {  // Check if fragment is still attached
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(context, "Failed to send verification email: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            ?.addOnFailureListener { e ->
                if (isAdded) {  // Check if fragment is still attached
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showVerificationEmailSentDialog(newEmail: String) {
        if (!isAdded) return  // Check if fragment is attached
        
        AlertDialog.Builder(requireContext())
            .setTitle("Verify Your New Email")
            .setMessage("A verification email has been sent to $newEmail. Please check your email and click the verification link. After verification, please log out and log back in to complete the email update.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                // Safely handle navigation
                try {
                    activity?.let { activity ->
                        if (activity is MainActivity) {
                            activity.onBackPressed()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating back", e)
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun uploadImageAndUpdateProfile(
        userRef: com.google.firebase.firestore.DocumentReference,
        name: String,
        email: String,
        age: Int,
        height: Double,
        weight: Double
    ) {
        val userId = auth.currentUser?.uid ?: return
        val imageRef = storage.reference.child("profile_images/$userId.jpg")
        selectedImageUri?.let { uri ->
            uploadImage(uri) { downloadUrl ->
                if (downloadUrl != null) {
                    updateProfileData(userRef, name, email, age, height, weight, downloadUrl)
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to get image URL", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadImage(uri: Uri, onComplete: (String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val imageRef = storage.reference.child("profile_images/$userId/${UUID.randomUUID()}")
        
        binding.progressBar.visibility = View.VISIBLE
        
        imageRef.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUrl ->
                        binding.progressBar.visibility = View.GONE
                        onComplete(downloadUrl.toString())
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        handleUploadError(e)
                        onComplete(null)
                    }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                handleUploadError(e)
                onComplete(null)
            }
    }

    private fun handleUploadError(e: Exception) {
        val errorMessage = when {
            e.message?.contains("Permission denied") == true -> 
                "Permission denied. Please check your internet connection and try again."
            e.message?.contains("App Check") == true ->
                "App verification failed. Please try again later."
            else -> "Failed to upload image: ${e.message}"
        }
        
        activity?.runOnUiThread {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
        Log.e(TAG, "Upload error: ${e.message}", e)
    }

    private fun updateProfileData(
        userRef: com.google.firebase.firestore.DocumentReference,
        name: String,
        email: String,
        age: Int,
        height: Double,
        weight: Double,
        profileImageUrl: String?
    ) {
        val updates = hashMapOf<String, Any>(
            "name" to name,
            "email" to email,
            "age" to age,
            "height" to height,
            "weight" to weight
        )

        if (profileImageUrl != null) {
            updates["profileImageUrl"] = profileImageUrl
        }

        userRef.update(updates)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                // Update the drawer header
                (requireActivity() as MainActivity).updateDrawerHeader()
                showRestartDialog()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showRestartDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Profile Updated")
            .setMessage("Please restart the app to see all changes.")
            .setPositiveButton("Restart") { _, _ ->
                // Restart the app
                val intent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (intent != null) {
                    startActivity(intent)
                }
                requireActivity().finish()
            }
            .setNegativeButton("Later") { dialog, _ ->
                dialog.dismiss()
                findNavController().navigateUp()
            }
            .setCancelable(false)
            .show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val currentPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.editTextCurrentPassword)
        val newPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.editTextNewPassword)
        val confirmPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.editTextConfirmPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change") { dialog, _ ->
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 6) {
                    Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                changePassword(currentPassword, newPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        binding.progressBar.visibility = View.VISIBLE
        val user = auth.currentUser
        val email = user?.email

        if (user == null || email == null) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Create credentials for reauthentication
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)

        // Reauthenticate
        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Update password in Firebase Auth
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "Password updated successfully. Please login again.", Toast.LENGTH_LONG).show()
                        // Sign out user
                        auth.signOut()
                        // Navigate safely
                        try {
                            val mainActivity = requireActivity() as MainActivity
                            mainActivity.navigateToLogin()
                        } catch (e: Exception) {
                            // Fallback navigation
                            activity?.let { act ->
                                act.finish()
                                val intent = Intent(act, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                act.startActivity(intent)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "Failed to update password: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "UpdateProfileFragment"
    }
} 