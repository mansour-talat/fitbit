package com.su.gym.models

data class ActivityStats(
    val steps: Long = 0,
    val calories: Long = 0,
    val distance: Float = 0f,
    val activeMinutes: Int = 0
) 