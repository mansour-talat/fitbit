package com.su.gym

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.su.gym.databinding.FragmentDashboardBinding
import com.su.gym.services.StepCounterService
import com.su.gym.utils.formatDuration
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    // Activity tracking variables
    private var initialStepCount: Int? = null
    private var steps = 0
    private var distance = 0.0 // meters
    private var caloriesBurned = 0.0
    private var startTime = 0L
    private var isTracking = false
    private var activityDuration: Long = 0L // ms
    private var timerRunning = false
    private var timerHandler: Handler? = null
    private var timerRunnable: Runnable? = null

    // Nutrition progress values
    private var currentCalories = 0
    private var currentProtein = 0
    private var currentCarbs = 0
    private var currentFat = 0
    private var caloriesLimit = 2000
    private var proteinLimit = 150
    private var carbsLimit = 250
    private var fatLimit = 65

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACTIVITY_RECOGNITION, false) -> {
                // Permission granted, start tracking
                setupActivityTracking()
            }
            else -> {
                // Permission denied
                Toast.makeText(context, "Activity recognition permission is required for step counting", Toast.LENGTH_LONG).show()
                binding.startActivityButton.isEnabled = false
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestPermissions()
        loadUserData()
        binding.setLimitsButton.setOnClickListener {
            showNutritionLimitsDialog()
        }
    }

    private fun checkAndRequestPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted, setup tracking
                setupActivityTracking()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION) -> {
                // Show rationale dialog
                showPermissionRationaleDialog()
            }
            else -> {
                // Request permissions
                requestPermissions()
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permission Required")
            .setMessage("The activity recognition permission is required to track your steps and physical activity.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                binding.startActivityButton.isEnabled = false
            }
            .show()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.BODY_SENSORS)
            permissions.add(Manifest.permission.BODY_SENSORS_BACKGROUND)
        }
        
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun setupActivityTracking() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Toast.makeText(context, "No step counter sensor found on this device", Toast.LENGTH_LONG).show()
            binding.startActivityButton.isEnabled = false
            return
        }

        binding.startActivityButton.setOnClickListener { startActivityTracking() }
        binding.stopActivityButton.setOnClickListener { stopActivityTracking() }
        binding.saveActivityButton.setOnClickListener { saveActivityStats() }

        binding.stopActivityButton.isEnabled = false
        binding.saveActivityButton.isEnabled = false
    }

    private fun startActivityTracking() {
        if (isTracking) return
        
        // Start the StepCounterService
        val serviceIntent = Intent(requireContext(), StepCounterService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent)
        } else {
            requireContext().startService(serviceIntent)
        }

        isTracking = true
        steps = 0
        distance = 0.0
        caloriesBurned = 0.0
        startTime = SystemClock.elapsedRealtime()
        activityDuration = 0L
        initialStepCount = null
        timerRunning = true
        
        binding.startActivityButton.isEnabled = false
        binding.stopActivityButton.isEnabled = true
        binding.saveActivityButton.isEnabled = false

        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
        startTimer()
    }

    private fun stopActivityTracking() {
        if (!isTracking) return
        
        // Stop the StepCounterService
        val serviceIntent = Intent(requireContext(), StepCounterService::class.java)
        requireContext().stopService(serviceIntent)

        isTracking = false
        timerRunning = false
        activityDuration = SystemClock.elapsedRealtime() - startTime
        
        binding.startActivityButton.isEnabled = true
        binding.stopActivityButton.isEnabled = false
        binding.saveActivityButton.isEnabled = true
        
        sensorManager.unregisterListener(this)
        stopTimer()
    }

    private fun saveActivityStats() {
        val userId = auth.currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val stats = hashMapOf(
            "userId" to userId,
            "date" to today,
            "steps" to steps,
            "distance" to distance,
            "caloriesBurned" to caloriesBurned,
            "activityTime" to activityDuration / 1000 // seconds
        )
        db.collection("activity")
            .add(stats)
            .addOnSuccessListener {
                Toast.makeText(context, "Activity stats saved!", Toast.LENGTH_SHORT).show()
                binding.saveActivityButton.isEnabled = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save activity stats: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startTimer() {
        timerHandler = Handler()
        timerRunnable = object : Runnable {
            override fun run() {
                if (timerRunning) {
                    activityDuration = SystemClock.elapsedRealtime() - startTime
                    binding.timerText.text = "Timer: ${formatDuration(activityDuration)}"
                    timerHandler?.postDelayed(this, 1000)
                }
            }
        }
        timerHandler?.post(timerRunnable!!)
    }

    private fun stopTimer() {
        timerHandler?.removeCallbacks(timerRunnable!!)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTracking || event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        val currentStepCount = event.values[0].toInt()
        if (initialStepCount == null) {
            initialStepCount = currentStepCount
        }
        steps = currentStepCount - (initialStepCount ?: currentStepCount)
        distance = steps * 0.78 // meters
        caloriesBurned = steps * 0.04 // kcal
        updateActivityStatsUI()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateActivityStatsUI() {
        binding.stepsText.text = steps.toString()
        binding.distanceText.text = String.format("%.1f m", distance)
        binding.caloriesBurnedText.text = String.format("%.1f", caloriesBurned)
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name") ?: "User"
                    val email = document.getString("email") ?: "user@example.com"
                    binding.textViewName.text = name
                    binding.textViewEmail.text = email

                    // Load nutrition limits
                    caloriesLimit = document.getLong("caloriesLimit")?.toInt() ?: 2000
                    proteinLimit = document.getLong("proteinLimit")?.toInt() ?: 150
                    carbsLimit = document.getLong("carbsLimit")?.toInt() ?: 250
                    fatLimit = document.getLong("fatLimit")?.toInt() ?: 65

                    loadTodayNutritionData()
                }
            }
    }

    private fun loadTodayNutritionData() {
        val userId = auth.currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        db.collection("nutrition")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { documents ->
                var totalCalories = 0
                var totalProtein = 0
                var totalCarbs = 0
                var totalFat = 0
                for (document in documents) {
                    totalCalories += document.getLong("calories")?.toInt() ?: 0
                    totalProtein += document.getLong("protein")?.toInt() ?: 0
                    totalCarbs += document.getLong("carbs")?.toInt() ?: 0
                    totalFat += document.getLong("fat")?.toInt() ?: 0
                }
                currentCalories = totalCalories
                currentProtein = totalProtein
                currentCarbs = totalCarbs
                currentFat = totalFat
                updateNutritionProgress()
            }
    }

    private fun updateNutritionProgress() {
        updateProgressBar(binding.caloriesProgress, currentCalories, caloriesLimit)
        updateProgressBar(binding.proteinProgress, currentProtein, proteinLimit)
        updateProgressBar(binding.carbsProgress, currentCarbs, carbsLimit)
        updateProgressBar(binding.fatProgress, currentFat, fatLimit)
        binding.caloriesProgressText.text = "$currentCalories / $caloriesLimit kcal"
        binding.proteinProgressText.text = "$currentProtein / $proteinLimit g"
        binding.carbsProgressText.text = "$currentCarbs / $carbsLimit g"
        binding.fatProgressText.text = "$currentFat / $fatLimit g"
    }

    private fun updateProgressBar(progressBar: com.google.android.material.progressindicator.LinearProgressIndicator, current: Int, max: Int) {
        val progress = (current.toFloat() / max * 100).toInt().coerceIn(0, 100)
        progressBar.progress = progress
    }

    private fun showNutritionLimitsDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_nutrition_limits, null)
        
        val caloriesInput = dialogView.findViewById<TextInputEditText>(R.id.caloriesLimitInput)
        val proteinInput = dialogView.findViewById<TextInputEditText>(R.id.proteinLimitInput)
        val carbsInput = dialogView.findViewById<TextInputEditText>(R.id.carbsLimitInput)
        val fatInput = dialogView.findViewById<TextInputEditText>(R.id.fatLimitInput)

        // Set current values
        caloriesInput.setText(caloriesLimit.toString())
        proteinInput.setText(proteinLimit.toString())
        carbsInput.setText(carbsLimit.toString())
        fatInput.setText(fatLimit.toString())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Daily Limits")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                // Get values from inputs
                val newCaloriesLimit = caloriesInput.text.toString().toIntOrNull() ?: caloriesLimit
                val newProteinLimit = proteinInput.text.toString().toIntOrNull() ?: proteinLimit
                val newCarbsLimit = carbsInput.text.toString().toIntOrNull() ?: carbsLimit
                val newFatLimit = fatInput.text.toString().toIntOrNull() ?: fatLimit

                // Save to Firestore
                saveNutritionLimits(newCaloriesLimit, newProteinLimit, newCarbsLimit, newFatLimit)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun saveNutritionLimits(
        newCaloriesLimit: Int,
        newProteinLimit: Int,
        newCarbsLimit: Int,
        newFatLimit: Int
    ) {
        val userId = auth.currentUser?.uid ?: return

        // Create limits object
        val nutritionLimits = hashMapOf(
            "caloriesLimit" to newCaloriesLimit,
            "proteinLimit" to newProteinLimit,
            "carbsLimit" to newCarbsLimit,
            "fatLimit" to newFatLimit,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        // Save to Firestore
        db.collection("users").document(userId)
            .update(nutritionLimits as Map<String, Any>)
            .addOnSuccessListener {
                // Update local values after successful save
                caloriesLimit = newCaloriesLimit
                proteinLimit = newProteinLimit
                carbsLimit = newCarbsLimit
                fatLimit = newFatLimit
                
                Toast.makeText(context, "Nutrition limits saved successfully!", Toast.LENGTH_SHORT).show()
                updateNutritionProgress()
            }
            .addOnFailureListener { e ->
                Log.e("DashboardFragment", "Error saving nutrition limits", e)
                Toast.makeText(context, "Failed to save nutrition limits: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isTracking) {
            sensorManager.unregisterListener(this)
            stopTimer()
        }
        _binding = null
    }
}
