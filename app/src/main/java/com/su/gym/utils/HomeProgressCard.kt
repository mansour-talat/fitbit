package com.su.gym.utils

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.su.gym.R
import java.text.NumberFormat
import java.util.Locale

class HomeProgressCard(
    private val view: View,
    private val greetingText: TextView,
    private val progressIndicator: CircularProgressIndicator,
    private val percentageText: TextView,
    private val calorieCountText: TextView
) {
    private var currentCalories = 0
    private var calorieLimit = 2000 // Default limit

    init {
        // Set initial progress
        updateProgress(0, calorieLimit)
    }

    fun setUserName(name: String) {
        greetingText.text = "Hello $name"
    }

    fun setCalorieLimit(limit: Int) {
        calorieLimit = limit
        updateProgress(currentCalories, limit)
    }

    fun updateProgress(current: Int, limit: Int) {
        currentCalories = current
        calorieLimit = limit

        // Calculate percentage
        val percentage = if (limit > 0) {
            (current.toFloat() / limit * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        // Update progress indicator with animation
        progressIndicator.setProgress(percentage, true)

        // Update percentage text
        percentageText.text = "$percentage%"

        // Format calorie count with thousands separator
        val formattedCalories = NumberFormat.getNumberInstance(Locale.US).format(current)
        calorieCountText.text = "$formattedCalories kcal"

        // Update colors based on progress
        val color = when {
            percentage >= 100 -> R.color.error
            percentage >= 80 -> R.color.warning
            else -> R.color.success
        }
        progressIndicator.setIndicatorColor(ContextCompat.getColor(view.context, color))
    }
} 