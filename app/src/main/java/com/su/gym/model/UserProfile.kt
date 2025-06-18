package com.su.gym.model

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val weight: Double = 0.0,
    val height: Double = 0.0,
    val age: Int = 0,
    val profileImageUrl: String = ""
) 