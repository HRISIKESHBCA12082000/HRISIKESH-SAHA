package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.Expense
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = RetrofitSyncClient.getMoshi()
    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    suspend fun generateInsights(expenses: List<Expense>): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Gemini API key is not configured in Secrets."
        }

        if (expenses.isEmpty()) {
            return "Add some expenses first so I can analyze your spending habits!"
        }

        val expenseSummary = expenses.joinToString("\n") { 
            "- ${it.title}: $${String.format("%.2f", it.amount)} in ${it.category} on ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp))}"
        }

        val prompt = """
            You are an expert personal finance advisor.
            Analyze the following list of expenses:
            $expenseSummary
            
            Provide exactly 3 concise, friendly, and highly specific spending insights or optimization tips based on this data. Highlight categories where they spent the most or had sudden spending. Do not use markdown headers or complex schemas. Just direct, clean bullet points with a bold short title (e.g. "**Food Spikes**: ..."). Keep it extremely action-oriented and positive!
        """.trimIndent()

        return makeGeminiRequest(prompt, null) ?: "Unable to connect to financial coaching assistant. Please try again."
    }

    suspend fun parseNaturalLanguageExpense(input: String): ParsedExpense? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured.")
            return null
        }

        val prompt = """
            You are a transaction parser. Extract spending details from this note:
            "$input"
            
            Format your response STRICTLY as a single valid JSON object. Do not include any wrapping markdown block markers (like ```json ... ```) or extra words. Just print the JSON object itself.
            JSON Schema:
            {
              "title": "Clean, short name of the vendor or purchase, capitalized",
              "amount": Double value of the cost (must be positive),
              "category": "Must be one of: Food, Shopping, Bills, Entertainment, Travel, Others"
            }
            If you cannot identify the amount or what they spent on, return empty fields but construct the JSON.
        """.trimIndent()

        val jsonResponse = makeGeminiRequest(prompt, "application/json")
        return try {
            if (jsonResponse != null) {
                val cleanJson = jsonResponse.trim().removeSurrounding("```json", "```").trim()
                val jsonObject = JSONObject(cleanJson)
                val title = jsonObject.optString("title", "Unknown Purchase")
                val amount = jsonObject.optDouble("amount", 0.0)
                val category = jsonObject.optString("category", "Others")
                ParsedExpense(
                    title = title,
                    amount = if (amount.isNaN()) 0.0 else amount,
                    category = category
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse natural language response: ${e.message}", e)
            null
        }
    }

    private suspend fun makeGeminiRequest(prompt: String, mimeType: String?): String? = withContext(Dispatchers.IO) {
        try {
            val config = if (mimeType != null) GeminiGenerationConfig(responseMimeType = mimeType, temperature = 0.1f) else null
            val requestBodyObj = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                generationConfig = config
            )

            val jsonString = requestAdapter.toJson(requestBodyObj)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonString.toRequestBody(mediaType)

            val url = "$BASE_URL?key=${getApiKey()}"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Request failed: Code=${response.code}, Error=$errorBody")
                    return@withContext null
                }
                val responseString = response.body?.string() ?: return@withContext null
                val rootJson = JSONObject(responseString)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API request failed: ${e.message}", e)
        }
        return@withContext null
    }
}

data class ParsedExpense(
    val title: String,
    val amount: Double,
    val category: String
)
