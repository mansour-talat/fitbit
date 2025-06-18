package com.su.gym.model

data class Client(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val height: Double = 0.0,
    val weight: Double = 0.0,
    val fitnessGoals: List<String> = emptyList(),
    val medicalConditions: List<String> = emptyList(),
    val emergencyContact: String = "",
    val membershipStartDate: String = "",
    val membershipEndDate: String = "",
    val membershipType: String = "",
    val membershipStatus: String = "",
    val lastVisitDate: String = "",
    val notes: String = ""
) 