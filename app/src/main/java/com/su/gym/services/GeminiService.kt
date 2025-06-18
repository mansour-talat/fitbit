package com.su.gym.services

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.BlockThreshold
import com.su.gym.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService {
    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE)
        )
    )

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val response: GenerateContentResponse = model.generateContent(prompt)
            return@withContext response.text ?: "Sorry, I couldn't generate a response."
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext when {
                e.message?.contains("not found") == true -> 
                    "Sorry, there was an error connecting to the AI service. Please check your internet connection and try again."
                e.message?.contains("invalid api key") == true ->
                    "There seems to be an issue with the API key. Please contact support."
                e.message?.contains("quota") == true ->
                    "The AI service quota has been exceeded. Please try again later."
                else -> "Sorry, there was an error: ${e.message}"
            }
        }
    }

    companion object {
        private var instance: GeminiService? = null

        fun getInstance(): GeminiService {
            return instance ?: synchronized(this) {
                instance ?: GeminiService().also { instance = it }
            }
        }
    }
} 