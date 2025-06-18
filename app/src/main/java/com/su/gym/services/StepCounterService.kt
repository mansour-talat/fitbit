package com.su.gym.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.su.gym.MainActivity
import com.su.gym.R
import com.su.gym.models.ActivityStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StepCounterService : Service(), SensorEventListener {
    private val binder = LocalBinder()
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var stepCount = 0
    private var lastStepCount = 0
    private var lastStepTime = 0L
    private var isMoving = false
    private var lastStepTimestamp = 0L
    private var stepInterval = 0L
    private var stepLength = 0.7f // Default step length in meters
    private val caloriesPerStep = 0.04f // calories per step
    private var isSensorRegistered = false

    private val _activityStats = MutableStateFlow(ActivityStats())
    val activityStats: StateFlow<ActivityStats> = _activityStats.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): StepCounterService = this@StepCounterService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        if (stepSensor == null) {
            Log.e(TAG, "No step counter sensor found")
            stopSelf()
        } else {
            Log.d(TAG, "Step counter sensor found: ${stepSensor?.name}")
            registerStepSensor()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        if (!isSensorRegistered) {
            registerStepSensor()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Counter Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your steps and activity"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Activity Tracking Active")
        .setContentText("Tracking your steps and activity")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(createPendingIntent())
        .build()

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun registerStepSensor() {
        stepSensor?.let { sensor ->
            if (!isSensorRegistered) {
                Log.d(TAG, "Attempting to register sensor: ${sensor.name}")
                val registered = sensorManager.registerListener(
                    this,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                if (registered) {
                    isSensorRegistered = true
                    Log.d(TAG, "Step sensor listener registered successfully")
                } else {
                    Log.e(TAG, "Failed to register step sensor listener")
                    stopSelf()
                }
            }
        } ?: run {
            Log.e(TAG, "Cannot register sensor - sensor is null")
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        unregisterStepSensor()
        return super.onUnbind(intent)
    }

    private fun unregisterStepSensor() {
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this)
            isSensorRegistered = false
            Log.d(TAG, "Step sensor listener unregistered")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: $accuracy")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val currentSteps = event.values[0].toInt()
            val currentTime = System.currentTimeMillis()

            // Calculate steps since last update
            val stepsSinceLastUpdate = if (lastStepCount == 0) 0 else currentSteps - lastStepCount
            
            Log.d(TAG, "Steps since last update: $stepsSinceLastUpdate")
            
            if (stepsSinceLastUpdate > 0) {
                stepCount += stepsSinceLastUpdate
                lastStepCount = currentSteps
                lastStepTimestamp = currentTime

                // Update movement status
                isMoving = true
                lastStepTime = currentTime

                // Calculate distance and calories
                val distance = stepCount * stepLength
                val calories = stepCount * caloriesPerStep

                // Calculate active minutes (time spent moving)
                val activeMinutes = if (isMoving) {
                    ((currentTime - lastStepTimestamp) / 60000).toInt()
                } else {
                    0
                }

                // Update stats
                val newStats = ActivityStats(
                    steps = stepCount.toLong(),
                    calories = calories.toLong(),
                    distance = distance,
                    activeMinutes = activeMinutes
                )
                
                Log.d(TAG, "Updating stats: $newStats")
                _activityStats.value = newStats

                // Update notification
                updateNotification(newStats)
            } else if (currentTime - lastStepTime > INACTIVE_THRESHOLD) {
                isMoving = false
                _activityStats.value = _activityStats.value.copy(activeMinutes = 0)
            }
        }
    }

    private fun updateNotification(stats: ActivityStats) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Activity Tracking Active")
            .setContentText("Steps: ${stats.steps} | Distance: ${String.format("%.2f", stats.distance)}m")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createPendingIntent())
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterStepSensor()
        Log.d(TAG, "Service destroyed")
    }

    fun resetStats() {
        stepCount = 0
        lastStepCount = 0
        lastStepTime = 0
        lastStepTimestamp = 0
        stepInterval = 0
        isMoving = false
        _activityStats.value = ActivityStats()
        Log.d(TAG, "Stats reset")
    }

    fun isUserMoving(): Boolean = isMoving

    companion object {
        private const val TAG = "StepCounterService"
        private const val CHANNEL_ID = "StepCounterServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val INACTIVE_THRESHOLD = 3000L // 3 seconds without steps
    }
} 