package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.CachedQueryResponse
import com.example.data.model.ConversationMessage
import com.example.data.model.UserProfile
import com.example.data.remote.ContentItem
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiRequest
import com.example.data.remote.GenConfig
import com.example.data.remote.PartItem
import com.example.data.repository.OfflineKnowledgeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Locale

enum class IntelligenceStrategy(val label: String, val badge: String) {
    ONLINE_CLOUD_LLM("Gemini Cloud", "🟢 En ligne (Gemini)"),
    OFFLINE_CACHED_STRATEGY("Cache Hors-ligne", "⚡ Hors ligne (Cache)"),
    OFFLINE_LOCAL_LIGHTWEIGHT_LLM("LLM Local Téléphone", "🧠 Hors ligne (LLM Local)")
}

data class IntelligenceResult(
    val replyText: String,
    val strategyUsed: IntelligenceStrategy,
    val confidenceScore: Float = 1.0f,
    val isFromCache: Boolean = false,
    val isOffline: Boolean = false,
    val sourceSummary: String? = null
)

data class CacheStats(
    val totalCachedCount: Int = 0,
    val totalHitCount: Int = 0
)

/**
 * Service Layer responsible for checking network connectivity and dynamically
 * routing queries between:
 * 1. Online Cloud LLM (Gemini API with real-time web search and multi-turn context)
 * 2. Offline Cached Response Strategy (Instant semantic and hash match with TTL)
 * 3. Offline Local Lightweight LLM Engine (On-device neural & document grounding)
 */
class AdaptiveIntelligenceService(
    private val context: Context,
    private val database: AppDatabase,
    val connectivityService: NetworkConnectivityService = NetworkConnectivityService(context),
    val localLlmEngine: LocalLightweightLlmEngine = LocalLightweightLlmEngine(database),
    val repository: OfflineKnowledgeRepository = OfflineKnowledgeRepository(database)
) {
    companion object {
        private const val TAG = "AdaptiveIntelService"

        fun normalizeQuery(raw: String): String {
            return raw.lowercase(Locale.ROOT)
                .replace(Regex("[?.!,'\"«»]+"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        fun computeHash(input: String): String {
            val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun computeJaccardSimilarity(s1: String, s2: String): Float {
            val set1 = s1.split(" ").toSet()
            val set2 = s2.split(" ").toSet()
            val intersection = set1.intersect(set2).size
            val union = set1.union(set2).size
            return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
        }
    }

    private val cachedQueryDao = database.cachedQueryDao()

    private val _currentStrategy = MutableStateFlow(IntelligenceStrategy.ONLINE_CLOUD_LLM)
    val currentStrategy: StateFlow<IntelligenceStrategy> = _currentStrategy.asStateFlow()

    private val _cacheStats = MutableStateFlow(CacheStats())
    val cacheStats: StateFlow<CacheStats> = _cacheStats.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            refreshCacheStats()
        }
    }

    suspend fun refreshCacheStats() = withContext(Dispatchers.IO) {
        val count = cachedQueryDao.countCachedResponses()
        _cacheStats.value = CacheStats(totalCachedCount = count)
    }

    /**
     * Core orchestrator method: checks connectivity and executes the optimal strategy.
     */
    suspend fun processQuery(
        userPrompt: String,
        userProfile: UserProfile,
        recentMessages: List<ConversationMessage>,
        preferredDocId: Long? = null,
        groundingContext: String? = null
    ): IntelligenceResult = withContext(Dispatchers.IO) {
        val trimmed = userPrompt.trim()
        val normalized = normalizeQuery(trimmed)
        val queryHash = computeHash(normalized)

        val isNetworkAvailable = connectivityService.isNetworkAvailable()
        val hasGeminiKey = GeminiClient.hasValidApiKey(userProfile.geminiApiKey)

        // =========================================================================
        // STRATEGY 1: ONLINE CLOUD LLM (Gemini API)
        // =========================================================================
        if (isNetworkAvailable && hasGeminiKey) {
            try {
                val cloudReply = callGeminiCloud(
                    prompt = trimmed,
                    userProfile = userProfile,
                    recentMessages = recentMessages,
                    groundingContext = groundingContext
                )

                if (cloudReply.isNotBlank()) {
                    _currentStrategy.value = IntelligenceStrategy.ONLINE_CLOUD_LLM

                    // Asynchronously cache cloud response for future offline use
                    cacheResponse(
                        rawQuery = trimmed,
                        normalizedQuery = normalized,
                        queryHash = queryHash,
                        responseText = cloudReply,
                        isAiGenerated = true
                    )

                    return@withContext IntelligenceResult(
                        replyText = cloudReply,
                        strategyUsed = IntelligenceStrategy.ONLINE_CLOUD_LLM,
                        confidenceScore = 0.99f,
                        isFromCache = false,
                        isOffline = false
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Online Gemini call failed, falling back to offline strategies: ${e.message}")
            }
        }

        // =========================================================================
        // STRATEGY 2: OFFLINE CACHED RESPONSE STRATEGY
        // =========================================================================
        val cachedMatch = findCachedResponse(normalized, queryHash)
        if (cachedMatch != null) {
            _currentStrategy.value = IntelligenceStrategy.OFFLINE_CACHED_STRATEGY
            cachedQueryDao.incrementHitCount(cachedMatch.id)
            refreshCacheStats()

            return@withContext IntelligenceResult(
                replyText = cachedMatch.responseText,
                strategyUsed = IntelligenceStrategy.OFFLINE_CACHED_STRATEGY,
                confidenceScore = cachedMatch.confidenceScore,
                isFromCache = true,
                isOffline = true,
                sourceSummary = cachedMatch.sourceSummary
            )
        }

        // =========================================================================
        // STRATEGY 3: OFFLINE LOCAL LIGHTWEIGHT LLM (On-device reasoning & grounding)
        // =========================================================================
        _currentStrategy.value = IntelligenceStrategy.OFFLINE_LOCAL_LIGHTWEIGHT_LLM
        val localResult = localLlmEngine.generateOfflineResponse(
            userPrompt = trimmed,
            userProfile = userProfile,
            preferredDocId = preferredDocId
        )

        // Cache this on-device generated response so repeated queries are served instantly
        cacheResponse(
            rawQuery = trimmed,
            normalizedQuery = normalized,
            queryHash = queryHash,
            responseText = localResult.replyText,
            sourceSummary = localResult.summaryExcerpt,
            isAiGenerated = true
        )

        IntelligenceResult(
            replyText = localResult.replyText,
            strategyUsed = IntelligenceStrategy.OFFLINE_LOCAL_LIGHTWEIGHT_LLM,
            confidenceScore = localResult.confidenceScore,
            isFromCache = false,
            isOffline = true,
            sourceSummary = localResult.summaryExcerpt
        )
    }

    private suspend fun findCachedResponse(normalized: String, queryHash: String): CachedQueryResponse? {
        // 1. Exact normalized match
        val exact = cachedQueryDao.findExactMatch(normalized)
        if (exact != null) return exact

        // 2. Hash match
        val byHash = cachedQueryDao.findByHash(queryHash)
        if (byHash != null) return byHash

        // 3. Keyword semantic lookup for short queries
        val words = normalized.split(" ").filter { it.length >= 4 }
        for (w in words.take(2)) {
            val candidates = cachedQueryDao.searchByKeyword(w)
            if (candidates.isNotEmpty()) {
                val candidate = candidates.first()
                val similarity = computeJaccardSimilarity(normalized, candidate.normalizedQuery)
                if (similarity >= 0.65f) {
                    return candidate
                }
            }
        }
        return null
    }

    private suspend fun cacheResponse(
        rawQuery: String,
        normalizedQuery: String,
        queryHash: String,
        responseText: String,
        sourceSummary: String? = null,
        isAiGenerated: Boolean = true
    ) {
        try {
            val existing = cachedQueryDao.findExactMatch(normalizedQuery)
            if (existing != null) {
                cachedQueryDao.incrementHitCount(existing.id)
            } else {
                cachedQueryDao.insertOrUpdate(
                    CachedQueryResponse(
                        rawQuery = rawQuery,
                        normalizedQuery = normalizedQuery,
                        queryHash = queryHash,
                        responseText = responseText,
                        sourceSummary = sourceSummary,
                        confidenceScore = 0.95f,
                        isAiGenerated = isAiGenerated
                    )
                )
            }
            refreshCacheStats()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache query response: ${e.message}")
        }
    }

    private suspend fun callGeminiCloud(
        prompt: String,
        userProfile: UserProfile,
        recentMessages: List<ConversationMessage>,
        groundingContext: String?
    ): String {
        val systemInstruction = buildString {
            append("Tu es J.A.R.V.I.S., le majordome d'intelligence artificielle de style Iron Man logé directement sur le téléphone. ")
            append("Tu t'adresses toujours à l'utilisateur avec courtoisie en l'appelant « monsieur ». ")
            append("Tes réponses sont très concises (1 à 2 phrases percutantes à l'oral), précises, intelligentes et chaleureuses. ")
            if (!groundingContext.isNullOrBlank()) {
                append("\n\nSOURCES DU TÉLÉPHONE :\n").append(groundingContext)
            }
        }

        val contents = mutableListOf<ContentItem>()
        for (msg in recentMessages.takeLast(6)) {
            val role = if (msg.role == "user") "user" else "model"
            contents.add(ContentItem(role = role, parts = listOf(PartItem(text = msg.text))))
        }
        contents.add(ContentItem(role = "user", parts = listOf(PartItem(text = prompt))))

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = ContentItem(parts = listOf(PartItem(text = systemInstruction))),
            generationConfig = GenConfig(temperature = 0.6f, maxOutputTokens = 250)
        )

        return GeminiClient.callWithFallback(request, userProfile.geminiApiKey)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cachedQueryDao.clearAll()
        refreshCacheStats()
    }
}
