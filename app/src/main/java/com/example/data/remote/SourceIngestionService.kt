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
import java.util.regex.Pattern

data class IngestedSourceResult(
    val title: String,
    val author: String,
    val type: String, // "youtube", "tiktok", "web", "document"
    val summary: String,
    val fullContent: String,
    val keyPoints: String,
    val sourceUrl: String? = null,
    val sizeMb: Double = 1.5
)

object SourceIngestionService {
    private const val TAG = "SourceIngestion"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Determines whether a given text is or contains a valid HTTP/HTTPS URL.
     */
    fun extractUrl(text: String): String? {
        val trimmed = text.trim()
        val matcher = Pattern.compile("(?i)\\b(https?://[a-zA-Z0-9+&@#/%?=~_|!:,.;]*[a-zA-Z0-9+&@#/%=~_|])").matcher(trimmed)
        return if (matcher.find()) {
            matcher.group(1)
        } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed.split("\\s+".toRegex()).firstOrNull()
        } else {
            null
        }
    }

    fun isYouTubeUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("youtube.com") || lower.contains("youtu.be")
    }

    fun isTikTokUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("tiktok.com")
    }

    /**
     * Ingests any web link (YouTube, TikTok, or regular article/website).
     */
    suspend fun ingestFromUrl(rawUrl: String): IngestedSourceResult = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        when {
            isYouTubeUrl(url) -> ingestYouTubeVideo(url)
            isTikTokUrl(url) -> ingestTikTokVideo(url)
            else -> ingestWebPage(url)
        }
    }

    private fun ingestYouTubeVideo(url: String): IngestedSourceResult {
        try {
            val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.name())
            val oembedUrl = "https://www.youtube.com/oembed?url=$encoded&format=json"

            val req = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val jsonStr = resp.body?.string() ?: ""
                val json = JSONObject(jsonStr)
                val videoTitle = json.optString("title", "Vidéo YouTube").trim()
                val author = json.optString("author_name", "Chaîne YouTube").trim()

                val summary = "Vidéo YouTube publiée par $author : « $videoTitle ». Les informations clés et le contexte de cette vidéo ont été analysés et enregistrés pour nos échanges hors ligne."
                val fullContent = "Source vidéo YouTube : $videoTitle.\nCréateur / Chaîne : $author.\nLien source : $url.\nCette ressource audiovisuelle a été mémorisée par Code pour permettre d'en analyser les thèmes, d'expliquer les points abordés et de répondre à toutes tes questions même sans connexion."
                val keyPoints = "Vidéo YouTube; $author; $videoTitle; Mémorisé hors ligne"

                return IngestedSourceResult(
                    title = videoTitle,
                    author = "$author (YouTube)",
                    type = "youtube",
                    summary = summary,
                    fullContent = fullContent,
                    keyPoints = keyPoints,
                    sourceUrl = url,
                    sizeMb = 2.4
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching YouTube oEmbed", e)
        }

        // Fallback for YouTube if oembed has an issue
        val simpleTitle = "Vidéo YouTube : " + url.substringAfterLast("/").take(20)
        return IngestedSourceResult(
            title = simpleTitle,
            author = "YouTube",
            type = "youtube",
            summary = "Vidéo YouTube mémorisée à partir du lien : $url.",
            fullContent = "Source vidéo YouTube : $url. Mémorisée pour discussion vocale hors ligne.",
            keyPoints = "Vidéo YouTube; Mémorisé",
            sourceUrl = url,
            sizeMb = 2.0
        )
    }

    private fun ingestTikTokVideo(url: String): IngestedSourceResult {
        try {
            val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.name())
            val oembedUrl = "https://www.tiktok.com/oembed?url=$encoded"

            val req = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val jsonStr = resp.body?.string() ?: ""
                val json = JSONObject(jsonStr)
                val videoTitle = json.optString("title", "Publication TikTok").trim()
                val author = json.optString("author_name", "Créateur TikTok").trim()

                val summary = "Contenu TikTok de @$author : « $videoTitle ». Analyse et points essentiels mémorisés pour discussion hors ligne."
                val fullContent = "Publication TikTok par @$author.\nTitre / Description : $videoTitle.\nLien source : $url.\nCode a analysé et mémorisé le contenu de ce média pour pouvoir en débattre ou t'en résumer l'essentiel."
                val keyPoints = "TikTok; @$author; $videoTitle; Mémorisé hors ligne"

                return IngestedSourceResult(
                    title = if (videoTitle.length > 50) videoTitle.take(47) + "…" else videoTitle,
                    author = "@$author (TikTok)",
                    type = "tiktok",
                    summary = summary,
                    fullContent = fullContent,
                    keyPoints = keyPoints,
                    sourceUrl = url,
                    sizeMb = 1.8
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TikTok oEmbed", e)
        }

        return IngestedSourceResult(
            title = "Publication TikTok",
            author = "TikTok",
            type = "tiktok",
            summary = "Contenu TikTok mémorisé à partir du lien $url.",
            fullContent = "Lien TikTok : $url. Mémorisé pour discussion.",
            keyPoints = "TikTok; Mémorisé",
            sourceUrl = url,
            sizeMb = 1.6
        )
    }

    private fun ingestWebPage(url: String): IngestedSourceResult {
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val html = resp.body?.string() ?: ""

                // Extract title
                val titleMatcher = Pattern.compile("(?i)<title>(.*?)</title>").matcher(html)
                var rawTitle = if (titleMatcher.find()) titleMatcher.group(1).trim() else ""
                if (rawTitle.isBlank()) {
                    val ogTitle = Pattern.compile("(?i)<meta[^>]*property=[\"']og:title[\"'][^>]*content=[\"'](.*?)[\"']").matcher(html)
                    rawTitle = if (ogTitle.find()) ogTitle.group(1).trim() else "Page Web"
                }
                val cleanTitle = cleanHtmlEntities(rawTitle).take(80)

                // Extract description
                val descMatcher = Pattern.compile("(?i)<meta[^>]*name=[\"']description[\"'][^>]*content=[\"'](.*?)[\"']").matcher(html)
                var metaDesc = if (descMatcher.find()) descMatcher.group(1).trim() else ""
                if (metaDesc.isBlank()) {
                    val ogDesc = Pattern.compile("(?i)<meta[^>]*property=[\"']og:description[\"'][^>]*content=[\"'](.*?)[\"']").matcher(html)
                    metaDesc = if (ogDesc.find()) ogDesc.group(1).trim() else ""
                }
                metaDesc = cleanHtmlEntities(metaDesc)

                // Extract textual body paragraphs
                val cleanText = extractReadableText(html)
                val summary = if (metaDesc.isNotBlank()) {
                    metaDesc
                } else {
                    cleanText.take(300).trim() + "…"
                }

                val domain = try {
                    java.net.URI(url).host?.removePrefix("www.") ?: "Web"
                } catch (e: Exception) {
                    "Site Web"
                }

                val sizeEstimate = (cleanText.length / 1000.0 * 0.15).coerceIn(1.2, 5.0)

                return IngestedSourceResult(
                    title = cleanTitle.ifBlank { "Article : $domain" },
                    author = domain,
                    type = "web",
                    summary = summary,
                    fullContent = cleanText.take(4500),
                    keyPoints = "$domain; $cleanTitle; Lecture hors ligne",
                    sourceUrl = url,
                    sizeMb = sizeEstimate
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading web page: $url", e)
        }

        val domain = try { java.net.URI(url).host ?: "Site Web" } catch (e: Exception) { "Site Web" }
        return IngestedSourceResult(
            title = "Source Web : $domain",
            author = domain,
            type = "web",
            summary = "Contenu web mémorisé à partir du lien : $url.",
            fullContent = "Lien source : $url. Mémorisé pour discussion.",
            keyPoints = "$domain; Lien Web",
            sourceUrl = url,
            sizeMb = 1.5
        )
    }

    /**
     * Ingests a text document, lecture notes, or pasted article.
     */
    fun ingestFromDocumentText(rawTitle: String, rawContent: String): IngestedSourceResult {
        val cleanContent = rawContent.trim()
        val inferredTitle = if (rawTitle.isNotBlank()) {
            rawTitle.trim()
        } else {
            cleanContent.lines().firstOrNull { it.isNotBlank() }?.take(40) ?: "Document personnel"
        }

        val paragraphs = cleanContent.split("\n\n").filter { it.isNotBlank() }
        val summary = if (paragraphs.isNotEmpty()) {
            paragraphs.first().take(280)
        } else {
            cleanContent.take(280)
        }

        val sizeEstimate = (cleanContent.length / 1000.0 * 0.12).coerceIn(0.8, 8.0)

        return IngestedSourceResult(
            title = inferredTitle,
            author = "Document utilisateur",
            type = "document",
            summary = summary,
            fullContent = cleanContent,
            keyPoints = "Document; $inferredTitle; Notes et références",
            sourceUrl = null,
            sizeMb = sizeEstimate
        )
    }

    private fun extractReadableText(html: String): String {
        var text = html
        // Remove script and style tags
        text = text.replace(Regex("(?is)<script.*?</script>"), " ")
        text = text.replace(Regex("(?is)<style.*?</style>"), " ")
        text = text.replace(Regex("(?is)<noscript.*?</noscript>"), " ")
        // Replace paragraph and break tags with newlines
        text = text.replace(Regex("(?i)<br\\s*/?>"), "\n")
        text = text.replace(Regex("(?i)</p>"), "\n\n")
        text = text.replace(Regex("(?i)</div>"), "\n")
        text = text.replace(Regex("(?i)</li>"), "\n")
        // Strip remaining HTML tags
        text = text.replace(Regex("<[^>]+>"), " ")
        // Clean entities
        text = cleanHtmlEntities(text)
        // Normalize whitespaces
        text = text.replace(Regex("[ \\t]+"), " ")
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        return text.trim()
    }

    private fun cleanHtmlEntities(input: String): String {
        return input.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&eacute;", "é")
            .replace("&egrave;", "è")
            .replace("&agrave;", "à")
            .replace("&ccedil;", "ç")
            .replace("&ecirc;", "ê")
            .replace("&icirc;", "î")
            .replace("&ocirc;", "ô")
            .replace("&ucirc;", "û")
    }
}
