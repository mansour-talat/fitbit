package com.su.gym.models

data class CustomExercise(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val sets: Int? = null,
    val reps: Int? = null,
    val time: Int? = null
) 