package com.example.gym.model

data class Client(
    val id: String,
    val name: String,
    val email: String,
    val imageUrl: String? = null,
    val status: String = "Active"
) 