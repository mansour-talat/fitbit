package com.su.gym.model

data class Exercise(
    val name: String,
    val type: String,
    val muscle: String,
    val equipment: String,
    val difficulty: String,
    val instructions: String,
    val force: String? = null,
    val level: String? = null,
    val mechanic: String? = null,
    val instructionsList: List<String>? = null,
    val category: String? = null,
    val primaryMuscles: List<String>? = null,
    val secondaryMuscles: List<String>? = null,
    val variations: List<String>? = null
) 