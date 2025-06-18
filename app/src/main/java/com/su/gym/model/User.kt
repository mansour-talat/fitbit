package com.su.gym.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val age: Int = 0,
    val height: Double = 0.0,
    val weight: Double = 0.0,
    val photoUrl: String = ""
) 