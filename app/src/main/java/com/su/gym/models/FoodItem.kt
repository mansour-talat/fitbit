package com.su.gym.models

data class FoodItem(
    val id: String = "",
    val name: String = "",
    val quantity: Double = 0.0,
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val date: String = "",
    val userId: String = ""
) 