package com.su.gym.models

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class NutritionPlan(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "",
    val duration: String = "",
    val targetCalories: String = "",
    val restrictions: List<String> = listOf(),
    val guidelines: List<String> = listOf(),
    val trainerId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val macros: Map<String, String> = mapOf(),
    val meals: List<Meal> = listOf()
) {
    data class Meal(
        val type: String = "",
        val calories: String = "",
        val protein: String = "",
        val carbs: String = "",
        val fats: String = "",
        val foods: List<String> = listOf()
    )

    data class Food(
        val name: String = "",
        val quantity: Int = 0,
        val unit: String = "g",
        val calories: Int = 0,
        val protein: Int = 0,
        val carbs: Int = 0,
        val fats: Int = 0
    )

    companion object {
        fun fromDocument(doc: DocumentSnapshot): NutritionPlan {
            val data = doc.data ?: return NutritionPlan(id = doc.id)
            
            return NutritionPlan(
                id = doc.id,
                name = data["name"]?.toString() ?: "",
                description = data["description"]?.toString() ?: "",
                type = data["type"]?.toString() ?: "",
                duration = data["duration"]?.toString() ?: "",
                targetCalories = data["targetCalories"]?.toString() ?: "",
                restrictions = when (val r = data["restrictions"]) {
                    is List<*> -> r.mapNotNull { it?.toString() }
                    is String -> r.split(Regex("\\d+\\.")).map { it.trim() }.filter { it.isNotEmpty() }
                    else -> listOf()
                },
                guidelines = when (val g = data["guidelines"]) {
                    is List<*> -> g.mapNotNull { it?.toString() }
                    is String -> g.split(Regex("\\d+\\.")).map { it.trim() }.filter { it.isNotEmpty() }
                    else -> listOf()
                },
                trainerId = data["trainerId"]?.toString() ?: "",
                createdAt = data["createdAt"] as? Timestamp,
                updatedAt = data["updatedAt"] as? Timestamp,
                macros = (data["macros"] as? Map<*, *>)?.mapNotNull { (key, value) ->
                    val strKey = key?.toString()
                    val strValue = value?.toString()
                    if (strKey != null) {
                        strKey to (strValue ?: "")
                    } else null
                }?.toMap() ?: mapOf(),
                meals = (data["meals"] as? List<*>)?.mapNotNull { mealData ->
                    if (mealData !is Map<*, *>) null
                    else {
                        Meal(
                            type = mealData["type"]?.toString() ?: "",
                            calories = mealData["calories"]?.toString() ?: "",
                            protein = mealData["protein"]?.toString() ?: "",
                            carbs = mealData["carbs"]?.toString() ?: "",
                            fats = mealData["fats"]?.toString() ?: "",
                            foods = (mealData["foods"] as? List<*>)?.mapNotNull { it?.toString() } ?: listOf()
                        )
                    }
                } ?: listOf()
            )
        }
    }
} 