package com.su.gym.services

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.models.FoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.text.SimpleDateFormat
import java.util.*

class NutritionService {
    private val apiKey = "q2HuaUvhly1zs2DyXSYpGQ==Toh5aM5ImbLrftEc"
    private val baseUrl = "https://api.calorieninjas.com/v1/nutrition"
    private val db = FirebaseFirestore.getInstance()

    suspend fun searchFood(query: String, quantity: Double = 100.0): List<FoodItem> = withContext(Dispatchers.IO) {
        try {
            // First get the nutrition data for 100g of the food
            val url = URL("$baseUrl?query=${query.replace(" ", "%20")}")
            val connection = url.openConnection() as HttpsURLConnection
            
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("X-Api-Key", apiKey)
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val items = parseNutritionResponse(response)
                
                // Scale the nutrition values based on the requested quantity
                if (items.isNotEmpty() && quantity != 100.0) {
                    val scaleFactor = quantity / 100.0
                    val originalItem = items[0]
                    val scaledItem = FoodItem(
                        id = originalItem.id,
                        name = originalItem.name,
                        quantity = quantity,
                        calories = originalItem.calories * scaleFactor,
                        protein = originalItem.protein * scaleFactor,
                        carbs = originalItem.carbs * scaleFactor,
                        fat = originalItem.fat * scaleFactor
                    )
                    return@withContext listOf(scaledItem)
                }
                items
            } else {
                Log.e("NutritionService", "API call failed with code: ${connection.responseCode}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("NutritionService", "Error fetching nutrition data", e)
            emptyList()
        }
    }

    private fun parseNutritionResponse(response: String): List<FoodItem> {
        val items = mutableListOf<FoodItem>()
        try {
            val jsonObject = JSONObject(response)
            val itemsArray = jsonObject.getJSONArray("items")
            
            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                items.add(FoodItem(
                    id = "", // Will be set when saved to Firestore
                    name = item.getString("name"),
                    quantity = item.getDouble("serving_size_g"),
                    calories = item.getDouble("calories"),
                    protein = item.getDouble("protein_g"),
                    carbs = item.getDouble("carbohydrates_total_g"),
                    fat = item.getDouble("fat_total_g")
                ))
            }
        } catch (e: Exception) {
            Log.e("NutritionService", "Error parsing nutrition response", e)
        }
        return items
    }

    suspend fun addFood(food: FoodItem) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        val foodData = hashMapOf(
            "userId" to userId,
            "date" to today,
            "name" to food.name,
            "quantity" to food.quantity,
            "calories" to food.calories,
            "protein" to food.protein,
            "carbs" to food.carbs,
            "fat" to food.fat
        )
        
        try {
            db.collection("nutrition")
                .add(foodData)
                .await()
        } catch (e: Exception) {
            Log.e("NutritionService", "Error adding food to Firestore", e)
            throw e
        }
    }

    suspend fun getFoodsForDate(userId: String, date: String): List<FoodItem> {
        return db.collection("nutrition")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", date)
            .get()
            .await()
            .toObjects(FoodItem::class.java)
    }

    suspend fun updateFood(foodId: String, food: FoodItem) {
        db.collection("nutrition")
            .document(foodId)
            .set(food)
            .await()
    }

    suspend fun deleteFood(foodId: String) {
        db.collection("nutrition")
            .document(foodId)
            .delete()
            .await()
    }
} 