package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<ContentItem>,
    val systemInstruction: ContentItem? = null,
    val generationConfig: GenConfig? = null
)

@JsonClass(generateAdapter = true)
data class ContentItem(
    val role: String? = null,
    val parts: List<PartItem>
)

@JsonClass(generateAdapter = true)
data class PartItem(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenConfig(
    val temperature: Float? = 0.7f,
    val maxOutputTokens: Int? = 350
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<CandidateItem>? = null,
    val error: ErrorItem? = null
)

@JsonClass(generateAdapter = true)
data class CandidateItem(
    val content: ContentItem? = null
)

@JsonClass(generateAdapter = true)
data class ErrorItem(
    val message: String? = null,
    val code: Int? = null
)

interface GeminiApi {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContentLite(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContentV2(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: GeminiApi = retrofit.create(GeminiApi::class.java)

    fun hasValidApiKey(customKey: String = ""): Boolean {
        if (customKey.isNotBlank()) return true
        val key = BuildConfig.GEMINI_API_KEY
        return !key.isNullOrBlank() && key != "MY_GEMINI_API_KEY"
    }

    fun getApiKey(customKey: String = ""): String {
        if (customKey.isNotBlank()) return customKey
        val key = BuildConfig.GEMINI_API_KEY ?: ""
        return if (key == "MY_GEMINI_API_KEY") "" else key
    }

    suspend fun callWithFallback(request: GeminiRequest, customKey: String = ""): String {
        val key = getApiKey(customKey)
        if (key.isBlank()) return ""

        return try {
            val resp = api.generateContent(key, request)
            resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
        } catch (e: Exception) {
            android.util.Log.w("GeminiClient", "gemini-2.5-flash failed: ${e.message}, trying gemini-1.5-flash...")
            try {
                val respLite = api.generateContentLite(key, request)
                respLite.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
            } catch (e2: Exception) {
                android.util.Log.w("GeminiClient", "gemini-1.5-flash failed: ${e2.message}, trying gemini-2.0-flash...")
                try {
                    val respV2 = api.generateContentV2(key, request)
                    respV2.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
                } catch (e3: Exception) {
                    android.util.Log.e("GeminiClient", "All Gemini models failed: ${e3.message}")
                    ""
                }
            }
        }
    }
}
