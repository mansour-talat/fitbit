package com.su.gym.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Exercise(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val difficulty: String = "",
    val description: String = "",
    val duration: String = "",
    val sets: String = "",
    val reps: String = "",
    val restTime: String = "",
    val equipment: String = "",
    val muscleGroups: String = "",
    val instructions: List<String> = listOf(),
    val gifUrl: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): Exercise {
            val data = doc.data ?: return Exercise(id = doc.id)
            
            return Exercise(
                id = doc.id,
                title = data["title"]?.toString() ?: "",
                category = data["category"]?.toString() ?: "",
                difficulty = data["difficulty"]?.toString() ?: "",
                description = data["description"]?.toString() ?: "",
                duration = data["duration"]?.toString() ?: "",
                sets = data["sets"]?.toString() ?: "",
                reps = data["reps"]?.toString() ?: "",
                restTime = data["restTime"]?.toString() ?: "",
                equipment = data["equipment"]?.toString() ?: "",
                muscleGroups = data["muscleGroups"]?.toString() ?: "",
                instructions = when (val instr = data["instructions"]) {
                    is List<*> -> instr.mapNotNull { it?.toString() }
                    is String -> instr.split(Regex("\\d+\\.")).map { it.trim() }.filter { it.isNotEmpty() }
                    else -> listOf()
                },
                gifUrl = data["gifUrl"]?.toString() ?: "",
                createdAt = data["createdAt"] as? Timestamp,
                updatedAt = data["updatedAt"] as? Timestamp
            )
        }
    }
} 