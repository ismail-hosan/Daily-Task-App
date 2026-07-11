package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiClient {
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class TextPart(val text: String)
    data class Content(val parts: List<TextPart>)
    data class GenerateRequest(val contents: List<Content>)

    data class ResponsePart(val text: String?)
    data class ResponseContent(val parts: List<ResponsePart>?)
    data class Candidate(val content: ResponseContent?)
    data class GenerateResponse(val candidates: List<Candidate>?)

    suspend fun getAiSuggestion(tasks: List<Task>, habits: List<Habit>, energyLevel: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is not configured. Please add GEMINI_API_KEY to your Secrets panel."
        }

        val taskListStr = tasks.joinToString("\n") { "- [${if (it.isCompleted) "x" else " "}] ${it.title} (${it.priority}, Category: ${it.category})" }
        val habitListStr = habits.joinToString("\n") { "- ${it.emoji} ${it.name} (Streak: ${it.streak} days)" }

        val prompt = """
            You are a wise productivity advisor. Given the user's details, suggest ONE immediate actionable priority and a brief encouraging tip (max 2 sentences total).
            Keep it actionable and motivating!
            
            Current energy level: $energyLevel
            
            Today's Tasks:
            $taskListStr
            
            Habits being tracked:
            $habitListStr
            
            Respond with ONLY the actionable priority and the tip. Example: "Prioritize drafting the PRD now since your energy is High. Hydration habit is on an 8-day streak, keep it up!"
        """.trimIndent()

        val requestAdapter = moshi.adapter(GenerateRequest::class.java)
        val responseAdapter = moshi.adapter(GenerateResponse::class.java)

        try {
            val requestBodyJson = requestAdapter.toJson(
                GenerateRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(TextPart(prompt))
                        )
                    )
                )
            )

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Let's focus on your most important task today!"
                }
                val bodyString = response.body?.string() ?: ""
                val jsonResponse = responseAdapter.fromJson(bodyString)
                val text = jsonResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                text?.trim() ?: "Focus on completing your highest priority task next!"
            }
        } catch (e: Exception) {
            "Focus on your primary priority. (AI suggestions will activate when GEMINI_API_KEY is configured)"
        }
    }
}
