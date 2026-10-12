package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class WebSearchResult(
    val query: String,
    val topicTitle: String,
    val oralReply: String,
    val detailedSummary: String,
    val sourceName: String = "Wikipédia / Web",
    val sourceUrl: String? = null,
    val keyPoints: List<String> = emptyList(),
    val category: String = "Culture & Sciences"
)

object WebSearchService {
    private const val TAG = "WebSearchService"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Extracts search query if the user's message is an explicit or implicit search request.
     */
    fun extractSearchQuery(userText: String): String? {
        val trimmed = userText.trim()
        val lower = trimmed.lowercase()

        val patterns = listOf(
            "fais des recherches sur internet à propos de ",
            "fais des recherches sur le net à propos de ",
            "fais des recherches sur internet sur ",
            "fais des recherches sur le web sur ",
            "fais des recherches sur le net sur ",
            "fais des recherches sur ",
            "fais une recherche sur internet sur ",
            "fais une recherche sur le web sur ",
            "fais une recherche sur ",
            "fais des recherches pour ",
            "fais des recherches ",
            "recherche sur internet à propos de ",
            "recherche sur le net à propos de ",
            "recherche sur internet sur ",
            "recherche sur le net sur ",
            "recherche sur le web sur ",
            "recherche sur internet ",
            "recherche sur le net ",
            "recherche sur le web ",
            "cherche sur internet à propos de ",
            "cherche sur le net à propos de ",
            "cherche sur internet sur ",
            "cherche sur le net sur ",
            "cherche sur le web sur ",
            "cherche sur internet ",
            "cherche sur le net ",
            "cherche sur le web ",
            "renseigne-toi sur internet sur ",
            "renseigne-toi sur ",
            "peux-tu faire des recherches sur ",
            "peux-tu rechercher sur internet ",
            "peux-tu rechercher ",
            "peux-tu chercher ",
            "peux-tu te renseigner sur ",
            "cherche des informations sur ",
            "cherche des infos sur ",
            "trouve des informations sur ",
            "trouve des infos sur ",
            "donne-moi des informations sur ",
            "donne-moi des infos sur ",
            "quelles sont les informations sur ",
            "quelles sont les infos sur ",
            "search the web for ",
            "search internet for ",
            "search on internet for ",
            "search for ",
            "look up on internet ",
            "look up ",
            "find information about ",
            "tell me about ",
            "parle-moi de ",
            "parle-moi du ",
            "parle-moi des ",
            "parle-moi d'",
            "raconte-moi l'histoire de ",
            "raconte-moi l'histoire du ",
            "raconte-moi l'histoire des ",
            "raconte-moi ",
            "c'est quoi ",
            "qu'est-ce que c'est que ",
            "qu'est-ce que ",
            "qu'est ce que ",
            "qui est ",
            "qui était ",
            "qui sont ",
            "recherche "
        )

        for (pattern in patterns) {
            if (lower.startsWith(pattern)) {
                val candidate = trimmed.substring(pattern.length).trim()
                    .replace(Regex("[?.!]+$"), "").trim()
                if (candidate.length >= 2) return candidate
            }
            if (lower.contains(pattern)) {
                val idx = lower.indexOf(pattern)
                val candidate = trimmed.substring(idx + pattern.length).trim()
                    .replace(Regex("[?.!]+$"), "").trim()
                if (candidate.length >= 2) return candidate
            }
        }

        // Generic question forms ending with ?
        if (trimmed.endsWith("?") && (lower.startsWith("pourquoi ") || lower.startsWith("comment ") || lower.startsWith("quand "))) {
            val candidate = trimmed.replace(Regex("[?.!]+$"), "").trim()
            if (candidate.length >= 4) return candidate
        }

        return null
    }

    /**
     * Executes real HTTP web searches using Wikipedia and DuckDuckGo.
     */
    suspend fun search(query: String, preferredLangCode: String = "fr-FR"): WebSearchResult? = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
            .removePrefix("un ")
            .removePrefix("une ")
            .removePrefix("le ")
            .removePrefix("la ")
            .removePrefix("les ")
            .removePrefix("l'")
            .removePrefix("d'")
            .removePrefix("du ")
            .removePrefix("des ")
            .trim()

        if (cleanQuery.isBlank()) return@withContext null

        val langPrefix = when {
            preferredLangCode.startsWith("en", ignoreCase = true) -> "en"
            preferredLangCode.startsWith("es", ignoreCase = true) -> "es"
            preferredLangCode.startsWith("de", ignoreCase = true) -> "de"
            preferredLangCode.startsWith("it", ignoreCase = true) -> "it"
            preferredLangCode.startsWith("zh", ignoreCase = true) -> "zh"
            else -> "fr"
        }

        // 1. Try Wikipedia in preferred language
        val wikiResult = queryWikipedia(cleanQuery, langPrefix)
        if (wikiResult != null) return@withContext wikiResult

        // 2. If not French or preferred had no hits, try English Wikipedia (largest database)
        if (langPrefix != "en") {
            val enWikiResult = queryWikipedia(cleanQuery, "en")
            if (enWikiResult != null) {
                // Adapt to user's language
                val localizedOral = if (langPrefix == "fr") {
                    "J'ai fait des recherches sur le Web international pour toi à propos de ${enWikiResult.topicTitle} ! Voici les faits principaux : ${enWikiResult.detailedSummary}. Veux-tu qu'on approfondisse ?"
                } else {
                    enWikiResult.oralReply
                }
                return@withContext enWikiResult.copy(oralReply = localizedOral)
            }
        }

        // 3. Fallback: DuckDuckGo Instant Answer
        val ddgResult = queryDuckDuckGo(cleanQuery, langPrefix)
        if (ddgResult != null) return@withContext ddgResult

        null
    }

    private fun queryWikipedia(query: String, lang: String): WebSearchResult? {
        try {
            // First: Search for top article title
            val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
            val searchUrl = "https://$lang.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&utf8=1&format=json&srlimit=3"

            val searchReq = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "LilianCompanionAndroid/1.0 (Mobile Assistant)")
                .build()

            val searchResp = httpClient.newCall(searchReq).execute()
            if (!searchResp.isSuccessful) return null

            val searchJson = JSONObject(searchResp.body?.string() ?: "")
            val queryObj = searchJson.optJSONObject("query") ?: return null
            val searchArr = queryObj.optJSONArray("search") ?: return null

            if (searchArr.length() == 0) return null

            val topHit = searchArr.getJSONObject(0)
            val articleTitle = topHit.optString("title", query)
            val encodedTitle = URLEncoder.encode(articleTitle.replace(" ", "_"), StandardCharsets.UTF_8.name())

            // Second: Fetch Page Summary
            val summaryUrl = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$encodedTitle"
            val summaryReq = Request.Builder()
                .url(summaryUrl)
                .header("User-Agent", "LilianCompanionAndroid/1.0 (Mobile Assistant)")
                .build()

            val summaryResp = httpClient.newCall(summaryReq).execute()
            if (!summaryResp.isSuccessful) return null

            val summaryJson = JSONObject(summaryResp.body?.string() ?: "")
            val extract = summaryJson.optString("extract", "").trim()
            val description = summaryJson.optString("description", "")
            val pageUrl = summaryJson.optJSONObject("content_urls")
                ?.optJSONObject("desktop")
                ?.optString("page", "https://$lang.wikipedia.org/wiki/$encodedTitle")

            if (extract.length < 25) return null

            val category = determineCategory(articleTitle, description, extract)
            val keyPoints = extractKeyPoints(extract)
            val oralReply = formatFriendOralSpeech(articleTitle, extract, lang)

            return WebSearchResult(
                query = query,
                topicTitle = articleTitle,
                oralReply = oralReply,
                detailedSummary = extract,
                sourceName = "Wikipédia / Web",
                sourceUrl = pageUrl,
                keyPoints = keyPoints,
                category = category
            )
        } catch (e: Exception) {
            Log.w(TAG, "Wikipedia search error for query '$query': ${e.message}")
            return null
        }
    }

    private fun queryDuckDuckGo(query: String, lang: String): WebSearchResult? {
        try {
            val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "LilianCompanionAndroid/1.0")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return null

            val json = JSONObject(resp.body?.string() ?: "")
            val abstractText = json.optString("AbstractText", "").trim()
            val heading = json.optString("Heading", query)
            val abstractUrl = json.optString("AbstractURL", "")

            if (abstractText.length < 25) return null

            val oralReply = formatFriendOralSpeech(heading, abstractText, lang)

            return WebSearchResult(
                query = query,
                topicTitle = heading,
                oralReply = oralReply,
                detailedSummary = abstractText,
                sourceName = "DuckDuckGo / Web",
                sourceUrl = if (abstractUrl.isNotBlank()) abstractUrl else null,
                keyPoints = extractKeyPoints(abstractText),
                category = "Culture & Savoir"
            )
        } catch (e: Exception) {
            Log.w(TAG, "DuckDuckGo search error: ${e.message}")
            return null
        }
    }

    /**
     * Formats web knowledge into a warm, fluent, conversational explanation
     * like a knowledgeable friend speaking on the phone.
     */
    fun formatFriendOralSpeech(title: String, rawExtract: String, lang: String): String {
        val clean = rawExtract
            .replace(Regex("\\s*\\([^)]*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val sentences = clean.split(Regex("(?<=[.!?])\\s+"))
            .filter { it.isNotBlank() && it.length > 10 }
        val condensed = if (sentences.size > 2) sentences.take(2).joinToString(" ") else clean

        if (lang == "fr") {
            val hash = Math.abs(title.hashCode())
            val opener = when (hash % 4) {
                0 -> "Alors, j'ai regardé ça pour toi ! En fait,"
                1 -> "Tu sais, c'est une super question ! En gros,"
                2 -> "J'ai cherché sur le Web pour toi, et voilà ce qu'il faut retenir :"
                else -> "Ah, c'est passionnant ça ! Pour faire simple,"
            }

            val closer = when ((hash / 3) % 4) {
                0 -> "Franchement, c'est captivant ! Tu en penses quoi mon ami ?"
                1 -> "C'est fou quand on y pense ! Tu aimerais qu'on approfondisse quel aspect ?"
                2 -> "Tu connaissais déjà un peu le sujet ou tu le découvres aujourd'hui ?"
                else -> "Dis-moi, qu'est-ce qui t'intrigue le plus là-dedans ?"
            }

            return "$opener $condensed $closer"
        } else {
            return "I looked that up for you! In short: $condensed Pretty fascinating, right? What do you think about it, my friend?"
        }
    }

    private fun determineCategory(title: String, desc: String, extract: String): String {
        val combined = "$title $desc $extract".lowercase()
        return when {
            combined.contains("astronomie") || combined.contains("espace") || combined.contains("physique") ||
            combined.contains("chimie") || combined.contains("biologie") || combined.contains("planète") ||
            combined.contains("trou noir") || combined.contains("cosmos") -> "Sciences & Univers"

            combined.contains("histoire") || combined.contains("guerre") || combined.contains("siècle") ||
            combined.contains("empereur") || combined.contains("roi") || combined.contains("bataille") ||
            combined.contains("civilisation") || combined.contains("révolution") -> "Histoire & Civilisation"

            combined.contains("informatique") || combined.contains("intelligence artificielle") ||
            combined.contains("logiciel") || combined.contains("technologie") || combined.contains("robot") ||
            combined.contains("internet") || combined.contains("ordinateur") -> "Technologies & IA"

            combined.contains("math") || combined.contains("théorème") || combined.contains("équation") ||
            combined.contains("géométrie") -> "Mathématiques & Logique"

            combined.contains("psychologie") || combined.contains("cerveau") || combined.contains("cognitif") ||
            combined.contains("émotion") || combined.contains("mental") -> "Psychologie & Esprit"

            combined.contains("pays") || combined.contains("ville") || combined.contains("capitale") ||
            combined.contains("géographie") || combined.contains("fleuve") -> "Géographie & Monde"

            else -> "Culture & Savoir"
        }
    }

    private fun extractKeyPoints(text: String): List<String> {
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length > 15 }
        return if (sentences.isNotEmpty()) sentences.take(3) else listOf(text.take(100))
    }
}
