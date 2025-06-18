package com.su.gym

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.su.gym.databinding.FragmentTrainerRegistrationBinding
import java.util.UUID

class TrainerRegistrationFragment : Fragment() {
    private var _binding: FragmentTrainerRegistrationBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore
    private var profilePictureUri: Uri? = null
    private var cvUri: Uri? = null
    private val PICK_PROFILE_IMAGE = 1
    private val PICK_CV_FILE = 2

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false) -> {
                openImagePicker()
            }
            permissions.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false) -> {
                openImagePicker()
            }
            else -> {
                Toast.makeText(requireContext(), "Permission denied. Cannot upload profile picture.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                cvUri = uri
                binding.textViewCVFileName.text = getFileNameFromUri(uri)
            }
        }
    }

    private val getProfilePicture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                profilePictureUri = uri
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(binding.imageViewProfile)
            }
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            profilePictureUri = it
            binding.imageViewProfile.setImageURI(it)
        }
    }

    private val pickCv = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
                try {
                // Take persistent URI permission
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                    cvUri = uri
                    binding.textViewCVFileName.text = getFileNameFromUri(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error processing selected file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainerRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Restore state if available
        savedInstanceState?.let {
            cvUri = it.getParcelable("cvUri")
            profilePictureUri = it.getParcelable("profilePictureUri")
            cvUri?.let { uri -> binding.textViewCVFileName.text = getFileNameFromUri(uri) }
            profilePictureUri?.let { uri -> binding.imageViewProfile.setImageURI(uri) }
        }

        // Set up gender dropdown
        val genders = arrayOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genders)
        binding.autoCompleteGender.setAdapter(adapter)

        binding.buttonUploadProfile.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.buttonUploadCV.setOnClickListener {
            try {
                // Use the system's document picker directly
                pickCv.launch(arrayOf("application/pdf"))
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error opening file picker", Toast.LENGTH_SHORT).show()
            }
        }

        binding.submitButton.setOnClickListener {
            submitApplication()
        }

        // Prevent back navigation
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Navigate to login screen instead of welcome screen
                    findNavController().navigate(R.id.action_trainer_registration_to_loginFragment)
                }
            }
        )
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // For Android 13 and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // For older Android versions
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, PICK_PROFILE_IMAGE)
    }

    private fun showFilePickerDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select CV File")
            .setMessage("Please select your CV file (PDF format)")
            .setPositiveButton("Select File") { dialog, _ ->
                dialog.dismiss()
                launchFilePicker()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun launchFilePicker() {
        try {
            pickCv.launch(arrayOf("application/pdf"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error opening file picker", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                PICK_PROFILE_IMAGE -> {
                    profilePictureUri = data.data
                    binding.imageViewProfile.setImageURI(profilePictureUri)
                }
                PICK_CV_FILE -> {
                    cvUri = data.data
                    val fileName = getFileNameFromUri(data.data)
                    binding.textViewCVFileName.text = fileName
                }
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri?): String {
        if (uri == null) return ""
        return try {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                it.moveToFirst()
                it.getString(nameIndex) ?: "Selected CV"
            } ?: "Selected CV"
        } catch (e: Exception) {
            e.printStackTrace()
            "Selected CV"
        }
    }

    private fun getFileSize(uri: Uri?): Long {
        if (uri == null) return 0
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            it.moveToFirst()
            it.getLong(sizeIndex)
        } ?: 0
    }

    private fun submitApplication() {
        val fullName = binding.editTextName.text.toString()
        val email = binding.editTextEmail.text.toString()
        val phone = binding.editTextPhone.text.toString()
        val age = binding.editTextAge.text.toString()
        val specialization = binding.editTextSpecialization.text.toString()
        val gender = binding.autoCompleteGender.text.toString()
        val password = binding.editTextPassword.text.toString()
        val confirmPassword = binding.editTextConfirmPassword.text.toString()

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || age.isEmpty() ||
            specialization.isEmpty() || gender.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (profilePictureUri == null) {
            Toast.makeText(requireContext(), "Please upload a profile picture", Toast.LENGTH_SHORT).show()
            return
        }

        if (cvUri == null) {
            Toast.makeText(requireContext(), "Please upload your CV", Toast.LENGTH_SHORT).show()
            return
        }

        // Check file sizes
        val profilePictureSize = getFileSize(profilePictureUri)
        val cvSize = getFileSize(cvUri)

        if (profilePictureSize > 5 * 1024 * 1024) { // 5MB limit
            Toast.makeText(requireContext(), "Profile picture size should be less than 5MB", Toast.LENGTH_SHORT).show()
            return
        }

        if (cvSize > 10 * 1024 * 1024) { // 10MB limit
            Toast.makeText(requireContext(), "CV size should be less than 10MB", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.submitButton.isEnabled = false

        // Create user account
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Upload profile picture
                        val profilePictureRef = storage.reference.child("profile_pictures/${user.uid}")
                        profilePictureRef.putFile(profilePictureUri!!)
                            .addOnSuccessListener {
                                // Get profile picture URL
                                profilePictureRef.downloadUrl.addOnSuccessListener { profilePictureUrl ->
                                    // Upload CV
                                    val cvRef = storage.reference.child("cvs/${user.uid}")
                                    cvRef.putFile(cvUri!!)
                                        .addOnSuccessListener {
                                            // Get CV URL
                                            cvRef.downloadUrl.addOnSuccessListener { cvUrl ->
                                                // Save trainer data to Firestore
                                                val trainerData = hashMapOf(
                                                    "fullName" to fullName,
                                                    "email" to email,
                                                    "phone" to phone,
                                                    "age" to age.toInt(),
                                                    "specialization" to specialization,
                                                    "gender" to gender,
                                                    "profilePictureUrl" to profilePictureUrl.toString(),
                                                    "cvUrl" to cvUrl.toString(),
                                                    "isApproved" to false
                                                )

                                                firestore.collection("trainers")
                                                    .document(user.uid)
                                                    .set(trainerData)
                                                    .addOnSuccessListener {
                                                        binding.progressBar.visibility = View.GONE
                                                        binding.submitButton.isEnabled = true
                                                        Toast.makeText(requireContext(), "Registration successful! Please wait for approval.", Toast.LENGTH_LONG).show()
                                                        findNavController().navigate(R.id.action_trainer_registration_to_loginFragment)
                                                    }
                                                    .addOnFailureListener { e ->
                                                        binding.progressBar.visibility = View.GONE
                                                        binding.submitButton.isEnabled = true
                                                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            binding.progressBar.visibility = View.GONE
                                            binding.submitButton.isEnabled = true
                                            Toast.makeText(requireContext(), "Error uploading CV: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                binding.progressBar.visibility = View.GONE
                                binding.submitButton.isEnabled = true
                                Toast.makeText(requireContext(), "Error uploading profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.submitButton.isEnabled = true
                    Toast.makeText(requireContext(), "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        cvUri?.let { outState.putParcelable("cvUri", it) }
        profilePictureUri?.let { outState.putParcelable("profilePictureUri", it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 