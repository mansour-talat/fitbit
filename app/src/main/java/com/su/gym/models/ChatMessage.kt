package com.su.gym.models

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val isFromUser: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) 