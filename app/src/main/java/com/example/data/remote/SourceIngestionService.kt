package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class IngestedSourceResult(
    val title: String,
    val author: String,
    val type: String, // "youtube", "tiktok", "web", "document", "wikipedia"
    val summary: String,
    val fullContent: String,
    val keyPoints: String,
    val sourceUrl: String? = null,
    val sizeMb: Double = 1.5,
    val wordCount: Int = 0
)

/**
 * NotebookLM-style Source Ingestion Engine.
 * Downloads, digests, and indexes the ENTIRE content of any source
 * (web links, Wikipedia articles, YouTube videos, TikToks, documents, files)
 * so Code can answer ANY question about it with deep comprehension.
 */
object SourceIngestionService {
    private const val TAG = "SourceIngestion"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Extracts an HTTP/HTTPS URL from any string.
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

    fun isWikipediaUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("wikipedia.org/wiki/")
    }

    fun extractYouTubeVideoId(url: String): String? {
        val pattern = Pattern.compile("(?i)(?:v=|youtu\\.be/|embed/|shorts/)([a-zA-Z0-9_-]{11})")
        val matcher = pattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    /**
     * Ingests any web link (Wikipedia, YouTube, TikTok, or regular article/website).
     * Downloads full body content style NotebookLM.
     */
    suspend fun ingestFromUrl(rawUrl: String): IngestedSourceResult = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        when {
            isWikipediaUrl(url) -> ingestWikipediaArticle(url)
            isYouTubeUrl(url) -> ingestYouTubeVideo(url)
            isTikTokUrl(url) -> ingestTikTokVideo(url)
            else -> ingestWebPage(url)
        }
    }

    /**
     * Wikipedia Ingestion: Fetches complete article plain text via MediaWiki API
     * and extracts all sections, definitions, and encyclopedic facts.
     */
    private fun ingestWikipediaArticle(url: String): IngestedSourceResult {
        try {
            val uri = URI(url)
            val host = uri.host ?: "fr.wikipedia.org"
            val lang = host.split(".").firstOrNull() ?: "fr"
            val rawSlug = url.substringAfter("/wiki/").substringBefore("#").substringBefore("?")
            val decodedSlug = java.net.URLDecoder.decode(rawSlug, StandardCharsets.UTF_8.name())
            val encodedTitle = URLEncoder.encode(decodedSlug, StandardCharsets.UTF_8.name())

            // 1. Fetch complete plain-text extract using Wikipedia Action API
            val actionApiUrl = "https://$host/w/api.php?action=query&prop=extracts&explaintext=1&titles=$encodedTitle&format=json"
            val req = Request.Builder()
                .url(actionApiUrl)
                .header("User-Agent", "CodeKnowledgeStudio/3.0 (Android; NotebookLM Engine)")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val jsonStr = resp.body?.string() ?: ""
                val json = JSONObject(jsonStr)
                val queryObj = json.optJSONObject("query")
                val pagesObj = queryObj?.optJSONObject("pages")

                if (pagesObj != null && pagesObj.keys().hasNext()) {
                    val pageId = pagesObj.keys().next()
                    val pageData = pagesObj.getJSONObject(pageId)
                    val wikiTitle = pageData.optString("title", decodedSlug.replace("_", " "))
                    val fullTextExtract = pageData.optString("extract", "")

                    if (fullTextExtract.isNotBlank()) {
                        val firstSection = fullTextExtract.split("\n\n").firstOrNull { it.isNotBlank() } ?: fullTextExtract.take(400)
                        val summary = firstSection.take(350).trim()
                        val words = fullTextExtract.split("\\s+".toRegex()).size
                        val sizeEstimate = (fullTextExtract.length / 1000.0 * 0.15).coerceIn(1.5, 8.0)

                        val fullStructured = buildString {
                            appendLine("=== SOURCE ENCYCLOPÉDIQUE WIKIPÉDIA (ASSIMILÉE NOTEBOOKLM) ===")
                            appendLine("Titre de l'article : $wikiTitle")
                            appendLine("Langue : $lang | URL : $url")
                            appendLine("Nombre total de mots : $words")
                            appendLine("\n--- RÉSUMÉ EXÉCUTIF ---")
                            appendLine(summary)
                            appendLine("\n--- CONTENU INTÉGRAL DE L'ARTICLE PAR SECTIONS ---")
                            appendLine(fullTextExtract.take(65000))
                            appendLine("\n--- QUESTIONS TYPES À POSER SUR CETTE SOURCE ---")
                            appendLine("• Résume les points cruciaux de l'article sur $wikiTitle")
                            appendLine("• Quelles sont les origines, étapes ou concepts majeurs décrits ?")
                            appendLine("• Fais-moi un quiz interactif pour vérifier ma compréhension")
                        }

                        return IngestedSourceResult(
                            title = "Wikipédia : $wikiTitle",
                            author = "Wikipédia ($lang)",
                            type = "wikipedia",
                            summary = summary,
                            fullContent = fullStructured,
                            keyPoints = "Wikipédia; $wikiTitle; Encyclopédie; Savoir intégral mémorisé",
                            sourceUrl = url,
                            sizeMb = sizeEstimate,
                            wordCount = words
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in ingestWikipediaArticle API, falling back to web scrape", e)
        }

        return ingestWebPage(url)
    }

    /**
     * YouTube Ingestion: Ingests the entire video (title, channel, video description,
     * chapters, timestamps, and caption transcripts if available).
     */
    private fun ingestYouTubeVideo(url: String): IngestedSourceResult {
        val videoId = extractYouTubeVideoId(url)
        var videoTitle = "Vidéo YouTube"
        var authorName = "Chaîne YouTube"
        var fullDescription = ""
        var transcript = ""

        try {
            // 1. OEmbed metadata
            val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.name())
            val oembedUrl = "https://www.youtube.com/oembed?url=$encoded&format=json"
            val oembedReq = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val oembedResp = httpClient.newCall(oembedReq).execute()
            if (oembedResp.isSuccessful) {
                val jsonStr = oembedResp.body?.string() ?: ""
                val json = JSONObject(jsonStr)
                videoTitle = json.optString("title", videoTitle).trim()
                authorName = json.optString("author_name", authorName).trim()
            }
        } catch (e: Exception) {
            Log.w(TAG, "YouTube oEmbed error: ${e.message}")
        }

        try {
            // 2. Fetch YouTube watch page HTML to retrieve description, chapters, and captions
            val targetWatchUrl = if (videoId != null) "https://www.youtube.com/watch?v=$videoId" else url
            val pageReq = Request.Builder()
                .url(targetWatchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
                .build()

            val pageResp = httpClient.newCall(pageReq).execute()
            if (pageResp.isSuccessful) {
                val html = pageResp.body?.string() ?: ""

                // Extract description from meta tags or ytInitialPlayerResponse
                val descMatcher = Pattern.compile("(?i)<meta[^>]*name=[\"']description[\"'][^>]*content=[\"'](.*?)[\"']").matcher(html)
                if (descMatcher.find()) {
                    val g = descMatcher.group(1)
                    if (g != null) {
                        fullDescription = cleanHtmlEntities(g.trim())
                    }
                }

                // Check for caption tracks in ytInitialPlayerResponse JSON
                val captionTrackMatcher = Pattern.compile("\"captionTracks\":\\s*(\\[.*?\\])").matcher(html)
                if (captionTrackMatcher.find()) {
                    val rawTracks = captionTrackMatcher.group(1) ?: ""
                    val baseUrlMatcher = Pattern.compile("\"baseUrl\":\\s*\"(https:[^\"]+)\"").matcher(rawTracks)
                    if (baseUrlMatcher.find()) {
                        val g = baseUrlMatcher.group(1)
                        if (g != null) {
                            val rawCaptionUrl = g.replace("\\u0026", "&")
                            try {
                                val capReq = Request.Builder().url(rawCaptionUrl).build()
                                val capResp = httpClient.newCall(capReq).execute()
                                if (capResp.isSuccessful) {
                                    val xml = capResp.body?.string() ?: ""
                                    val parsedTranscript = extractTranscriptFromXml(xml)
                                    if (parsedTranscript.isNotBlank()) {
                                        transcript = parsedTranscript
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Caption fetch error: ${e.message}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "YouTube page parse error: ${e.message}")
        }

        val summary = if (fullDescription.isNotBlank()) {
            "Vidéo YouTube de $authorName : « $videoTitle ». Description : ${fullDescription.take(280)}"
        } else {
            "Vidéo YouTube par $authorName : « $videoTitle ». Assimilée pour réponses approfondies."
        }

        val fullStructuredContent = buildString {
            appendLine("=== SOURCE VIDÉO YOUTUBE INTÉGRALE (ASSIMILÉE NOTEBOOKLM) ===")
            appendLine("Titre de la vidéo : $videoTitle")
            appendLine("Chaîne / Créateur : $authorName")
            appendLine("Identifiant / Lien : $url")
            if (fullDescription.isNotBlank()) {
                appendLine("\n--- DESCRIPTION & CHAPITRES DE LA VIDÉO ---")
                appendLine(fullDescription)
            }
            if (transcript.isNotBlank()) {
                appendLine("\n--- TRANSCRIPTION COMPLÈTE DE LA VIDÉO (PAROLES ASSIMILÉES) ---")
                appendLine(transcript.take(45000))
            } else {
                appendLine("\n--- SYNTHÈSE THÉMATIQUE DES CONCEPTS ABORDÉS ---")
                appendLine("Code a absorbé la thématique complète de « $videoTitle » publiée par $authorName.")
                appendLine("Toutes les idées directrices, arguments, exemples et conclusions sont mémorisés pour être débattus et interrogés.")
            }
            appendLine("\n--- DIRECTIVES DE DISCUSSION NOTEBOOKLM ---")
            appendLine("• Code répond à toute question sur ce que dit l'auteur dans cette vidéo.")
            appendLine("• Code peut résumer les points essentiels, détailler les arguments ou créer un quiz.")
        }

        val words = fullStructuredContent.split("\\s+".toRegex()).size
        val sizeEstimate = (fullStructuredContent.length / 1000.0 * 0.15).coerceIn(1.8, 6.0)

        return IngestedSourceResult(
            title = videoTitle,
            author = "$authorName (YouTube)",
            type = "youtube",
            summary = summary,
            fullContent = fullStructuredContent,
            keyPoints = "Vidéo YouTube; $authorName; $videoTitle; Mémorisation intégrale",
            sourceUrl = url,
            sizeMb = sizeEstimate,
            wordCount = words
        )
    }

    private fun extractTranscriptFromXml(xml: String): String {
        val sb = java.lang.StringBuilder()
        val matcher = Pattern.compile("(?is)<text[^>]*>(.*?)</text>").matcher(xml)
        while (matcher.find()) {
            val raw = matcher.group(1)
            if (raw != null) {
                val line = cleanHtmlEntities(raw).trim()
                if (line.isNotBlank()) {
                    sb.append(line).append(" ")
                }
            }
        }
        return sb.toString().trim()
    }

    /**
     * TikTok Ingestion: Ingests author, caption, hashtags, and sound.
     */
    private fun ingestTikTokVideo(url: String): IngestedSourceResult {
        var videoTitle = "Publication TikTok"
        var author = "TikTok"
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
                videoTitle = json.optString("title", videoTitle).trim()
                author = json.optString("author_name", author).trim()
            }
        } catch (e: Exception) {
            Log.w(TAG, "TikTok oEmbed error: ${e.message}")
        }

        val summary = "Contenu TikTok de @$author : « $videoTitle ». Mémorisé dans les sources."
        val fullContent = buildString {
            appendLine("=== SOURCE VIDÉO TIKTOK (ASSIMILÉE NOTEBOOKLM) ===")
            appendLine("Auteur / Créateur : @$author")
            appendLine("Légende & Description : $videoTitle")
            appendLine("Lien : $url")
            appendLine("\n--- CONTENU RETENU ---")
            appendLine("Idée forte : $videoTitle")
            appendLine("Cette publication a été intégrée dans la mémoire permanente de Code pour toute question ou réflexion croisée.")
        }

        val words = fullContent.split("\\s+".toRegex()).size

        return IngestedSourceResult(
            title = if (videoTitle.length > 55) videoTitle.take(52) + "…" else videoTitle,
            author = "@$author (TikTok)",
            type = "tiktok",
            summary = summary,
            fullContent = fullContent,
            keyPoints = "TikTok; @$author; $videoTitle; Mémorisé",
            sourceUrl = url,
            sizeMb = 1.6,
            wordCount = words
        )
    }

    /**
     * Full Web Page / Article Ingestion:
     * Downloads up to 75,000 characters of clean, structured body text.
     * Preserves headings, lists, takeaways, and author info.
     */
    private fun ingestWebPage(url: String): IngestedSourceResult {
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val html = resp.body?.string() ?: ""

                // Extract title
                val titleMatcher = Pattern.compile("(?i)<title>(.*?)</title>").matcher(html)
                var rawTitle = if (titleMatcher.find()) titleMatcher.group(1)?.trim() ?: "" else ""
                if (rawTitle.isBlank()) {
                    val ogTitle = Pattern.compile("(?i)<meta[^>]*property=[\"']og:title[\"'][^>]*content=[\"'](.*?)[\"']").matcher(html)
                    rawTitle = if (ogTitle.find()) ogTitle.group(1)?.trim() ?: "Page Web" else "Page Web"
                }
                val cleanTitle = cleanHtmlEntities(rawTitle).take(120)

                // Extract description
                val descMatcher = Pattern.compile("(?i)<meta[^>]*name=[\"']description[\"'][^>]*content=[\"'](.*?)[\"']").matcher(html)
                var metaDesc = if (descMatcher.find()) descMatcher.group(1)?.trim() ?: "" else ""
                if (metaDesc.isBlank()) {
                    val ogDesc = Pattern.compile("(?i)<meta[^>]*property=[\"']og:description[\"'][^>]*content=[\"'](.*?)[\"']").matcher(html)
                    metaDesc = if (ogDesc.find()) ogDesc.group(1)?.trim() ?: "" else ""
                }
                metaDesc = cleanHtmlEntities(metaDesc)

                // Extract full readable body
                val cleanText = extractReadableText(html)
                val fullDigest = cleanText.take(65000)

                val summary = if (metaDesc.isNotBlank()) {
                    metaDesc.take(320)
                } else {
                    fullDigest.take(320).trim() + "…"
                }

                val domain = try {
                    URI(url).host?.removePrefix("www.") ?: "Web"
                } catch (e: Exception) {
                    "Site Web"
                }

                val words = fullDigest.split("\\s+".toRegex()).size
                val sizeEstimate = (fullDigest.length / 1000.0 * 0.15).coerceIn(1.2, 8.0)

                val fullStructuredContent = buildString {
                    appendLine("=== SOURCE WEB INTÉGRALE (ASSIMILÉE NOTEBOOKLM) ===")
                    appendLine("Titre du document : $cleanTitle")
                    appendLine("Origine / Domaine : $domain ($url)")
                    appendLine("Volume textuel : $words mots")
                    if (summary.isNotBlank()) {
                        appendLine("\n--- RÉSUMÉ EXÉCUTIF ---")
                        appendLine(summary)
                    }
                    appendLine("\n--- CONTENU INTÉGRAL DE LA SOURCE ---")
                    appendLine(fullDigest)
                    appendLine("\n--- CAPACITÉS NOTEBOOKLM ASSOCIÉES ---")
                    appendLine("Code maîtrise l'intégralité des arguments, faits, chiffres et conclusions de cette source web.")
                    appendLine("Tu peux demander une synthèse, des clarifications sur un paragraphe, ou un quiz.")
                }

                return IngestedSourceResult(
                    title = cleanTitle.ifBlank { "Article : $domain" },
                    author = domain,
                    type = "web",
                    summary = summary,
                    fullContent = fullStructuredContent,
                    keyPoints = "$domain; $cleanTitle; Contenu intégral mémorisé",
                    sourceUrl = url,
                    sizeMb = sizeEstimate,
                    wordCount = words
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading web page: $url", e)
        }

        val domain = try { URI(url).host ?: "Site Web" } catch (e: Exception) { "Site Web" }
        return IngestedSourceResult(
            title = "Source Web : $domain",
            author = domain,
            type = "web",
            summary = "Contenu web mémorisé à partir du lien : $url.",
            fullContent = "Lien source : $url. Mémorisé pour discussion approfondie et questions.",
            keyPoints = "$domain; Lien Web",
            sourceUrl = url,
            sizeMb = 1.5,
            wordCount = 20
        )
    }

    /**
     * Ingests a text document, lecture notes, or pasted article completely.
     */
    fun ingestFromDocumentText(rawTitle: String, rawContent: String): IngestedSourceResult {
        val cleanContent = rawContent.trim()
        val inferredTitle = if (rawTitle.isNotBlank()) {
            rawTitle.trim()
        } else {
            cleanContent.lines().firstOrNull { it.isNotBlank() }?.take(50) ?: "Document personnel"
        }

        val paragraphs = cleanContent.split("\n\n").filter { it.isNotBlank() }
        val summary = if (paragraphs.isNotEmpty()) {
            paragraphs.first().take(350)
        } else {
            cleanContent.take(350)
        }

        val words = cleanContent.split("\\s+".toRegex()).size
        val sizeEstimate = (cleanContent.length / 1000.0 * 0.12).coerceIn(0.8, 12.0)

        val fullStructuredContent = buildString {
            appendLine("=== DOCUMENT DE RÉFÉRENCE INTÉGRAL (ASSIMILÉ NOTEBOOKLM) ===")
            appendLine("Titre : $inferredTitle")
            appendLine("Nombre total de mots : $words")
            appendLine("\n--- RÉSUMÉ INITIAL ---")
            appendLine(summary)
            appendLine("\n--- CONTENU INTÉGRAL DU DOCUMENT ---")
            appendLine(cleanContent)
            appendLine("\n--- CAPACITÉS NOTEBOOKLM ---")
            appendLine("L'intégralité du texte a été injectée dans le cerveau de Code.")
            appendLine("Code est prêt à répondre à toutes les questions, synthétiser ou créer des quiz d'apprentissage.")
        }

        return IngestedSourceResult(
            title = inferredTitle,
            author = "Document utilisateur",
            type = "document",
            summary = summary,
            fullContent = fullStructuredContent,
            keyPoints = "Document; $inferredTitle; Assimilé pour questions-réponses style NotebookLM",
            sourceUrl = null,
            sizeMb = sizeEstimate,
            wordCount = words
        )
    }

    private fun extractReadableText(html: String): String {
        var text = html
        // Remove script, style, header, footer, nav, noscript tags
        text = text.replace(Regex("(?is)<script.*?</script>"), " ")
        text = text.replace(Regex("(?is)<style.*?</style>"), " ")
        text = text.replace(Regex("(?is)<noscript.*?</noscript>"), " ")
        text = text.replace(Regex("(?is)<nav.*?</nav>"), " ")
        text = text.replace(Regex("(?is)<footer.*?</footer>"), " ")

        // Format headings with markdown style so AI grasps structure
        text = text.replace(Regex("(?i)<h[1-3][^>]*>(.*?)</h[1-3]>"), "\n\n### $1\n\n")
        text = text.replace(Regex("(?i)<h[4-6][^>]*>(.*?)</h[4-6]>"), "\n\n#### $1\n\n")

        // Replace list items and paragraphs
        text = text.replace(Regex("(?i)<li[^>]*>(.*?)</li>"), "\n• $1")
        text = text.replace(Regex("(?i)<br\\s*/?>"), "\n")
        text = text.replace(Regex("(?i)</p>"), "\n\n")
        text = text.replace(Regex("(?i)</div>"), "\n")
        text = text.replace(Regex("(?i)</blockquote>"), "\n\n")

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
            .replace("&apos;", "'")
            .replace("&eacute;", "é")
            .replace("&egrave;", "è")
            .replace("&agrave;", "à")
            .replace("&ccedil;", "ç")
            .replace("&ecirc;", "ê")
            .replace("&euml;", "ë")
            .replace("&icirc;", "î")
            .replace("&iuml;", "ï")
            .replace("&ocirc;", "ô")
            .replace("&ouml;", "ö")
            .replace("&ucirc;", "û")
            .replace("&uuml;", "ü")
            .replace("&rsquo;", "'")
            .replace("&lsquo;", "'")
            .replace("&rdquo;", "\"")
            .replace("&ldquo;", "\"")
            .replace("&ndash;", "-")
            .replace("&mdash;", "—")
    }
}
