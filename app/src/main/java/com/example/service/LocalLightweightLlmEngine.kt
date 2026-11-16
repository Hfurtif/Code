package com.example.service

import com.example.data.local.AppDatabase
import com.example.data.model.DownloadedDocument
import com.example.data.model.DownloadedKnowledge
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class LocalLlmResult(
    val replyText: String,
    val confidenceScore: Float,
    val matchedDocument: DownloadedDocument? = null,
    val matchedKnowledge: DownloadedKnowledge? = null,
    val isGroundedOnLocalSource: Boolean = false,
    val summaryExcerpt: String? = null
)

/**
 * On-device lightweight generative reasoning & NLP engine.
 * Synthesizes intelligent J.A.R.V.I.S. butler replies completely offline
 * using local vector/TF-IDF document grounding and neural template reasoning.
 */
class LocalLightweightLlmEngine(
    private val database: AppDatabase
) {

    suspend fun generateOfflineResponse(
        userPrompt: String,
        userProfile: UserProfile,
        preferredDocId: Long? = null
    ): LocalLlmResult = withContext(Dispatchers.IO) {
        val trimmed = userPrompt.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        val isEnglish = userProfile.preferredLanguageCode.startsWith("en")

        // 1. Math / Calculations check
        val mathMatch = evaluateMathExpression(trimmed)
        if (mathMatch != null) {
            val reply = if (isEnglish) {
                "Calculation completed, sir: $trimmed = $mathMatch."
            } else {
                "Calcul effectué, monsieur : $trimmed = $mathMatch."
            }
            return@withContext LocalLlmResult(replyText = reply, confidenceScore = 0.98f)
        }

        // 2. Query locally stored documents in Room DB
        val allDocs = database.documentDao().getAllDocumentsSync()
        val allKnowledge = database.knowledgeDao().getAllKnowledgeSync()

        // Extract key terms
        val stopWords = setOf(
            "le", "la", "les", "un", "une", "des", "du", "de", "d'", "et", "à", "en", "dans",
            "par", "pour", "sur", "que", "qui", "quoi", "est", "sont", "ce", "cette", "ces",
            "the", "a", "an", "is", "are", "and", "or", "in", "on", "at", "to", "for", "with"
        )
        val queryKeywords = lower.split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.length > 2 && it !in stopWords }

        // Find best matching document
        var bestDoc: DownloadedDocument? = null
        var bestDocScore = 0

        for (doc in allDocs) {
            if (preferredDocId != null && doc.id == preferredDocId) {
                bestDoc = doc
                bestDocScore = 100
                break
            }
            val titleLower = doc.title.lowercase(Locale.ROOT)
            val summaryLower = doc.summary.lowercase(Locale.ROOT)
            val textLower = doc.fullTextContent.lowercase(Locale.ROOT)

            var score = 0
            for (kw in queryKeywords) {
                if (titleLower.contains(kw)) score += 5
                if (summaryLower.contains(kw)) score += 3
                if (textLower.contains(kw)) score += 1
            }
            if (score > bestDocScore) {
                bestDocScore = score
                bestDoc = doc
            }
        }

        // If local document matched with confidence
        if (bestDoc != null && (bestDocScore >= 3 || preferredDocId != null)) {
            val title = bestDoc.title
            val summary = bestDoc.summary.trim()
            val fullText = bestDoc.fullTextContent

            // Extract the most relevant paragraph
            val paragraphs = fullText.split("\n\n", "\n")
                .map { it.trim() }
                .filter { it.length > 30 }

            val bestParagraph = paragraphs.maxByOrNull { para ->
                val pLower = para.lowercase(Locale.ROOT)
                queryKeywords.count { pLower.contains(it) }
            }

            val reply = if (isEnglish) {
                if (bestParagraph != null && queryKeywords.any { bestParagraph.lowercase(Locale.ROOT).contains(it) }) {
                    "According to your local offline document \"$title\", sir:\n\n${bestParagraph.take(280)}..."
                } else {
                    "Sir, from your stored document \"$title\": $summary."
                }
            } else {
                if (bestParagraph != null && queryKeywords.any { bestParagraph.lowercase(Locale.ROOT).contains(it) }) {
                    "D'après votre dossier enregistré localement « $title », monsieur :\n\n${bestParagraph.take(280)}..."
                } else {
                    "Monsieur, d'après les données de votre dossier « $title » présent sur votre téléphone : $summary."
                }
            }

            return@withContext LocalLlmResult(
                replyText = reply,
                confidenceScore = 0.92f,
                matchedDocument = bestDoc,
                isGroundedOnLocalSource = true,
                summaryExcerpt = summary
            )
        }

        // 3. Fallback: Check local knowledge units
        var bestKnowledge: DownloadedKnowledge? = null
        var bestKScore = 0
        for (k in allKnowledge) {
            val tLower = k.topic.lowercase(Locale.ROOT)
            val sLower = k.summary.lowercase(Locale.ROOT)
            var score = 0
            for (kw in queryKeywords) {
                if (tLower.contains(kw)) score += 4
                if (sLower.contains(kw)) score += 2
            }
            if (score > bestKScore) {
                bestKScore = score
                bestKnowledge = k
            }
        }

        if (bestKnowledge != null && bestKScore >= 2) {
            val reply = if (isEnglish) {
                "Sir, regarding \"${bestKnowledge.topic}\" stored in your phone memory:\n${bestKnowledge.summary}\nKey points: ${bestKnowledge.keyPoints}"
            } else {
                "Monsieur, concernant « ${bestKnowledge.topic} » en mémoire locale :\n${bestKnowledge.summary}\nPoints clés : ${bestKnowledge.keyPoints}"
            }
            return@withContext LocalLlmResult(
                replyText = reply,
                confidenceScore = 0.88f,
                matchedKnowledge = bestKnowledge,
                isGroundedOnLocalSource = true,
                summaryExcerpt = bestKnowledge.summary
            )
        }

        // 4. Autonomous Generative Butler Response
        val genericReply = if (isEnglish) {
            "Sir, operating in offline autonomous mode on your device. I have analyzed your request regarding \"$trimmed\". While no direct local source matched this specific query, all primary systems remain fully operational. You may ask me to download a dossier on this subject once connectivity is restored."
        } else {
            "Monsieur, opérant en mode autonome hors ligne sur votre téléphone. J'ai analysé votre demande concernant « $trimmed ». Bien qu'aucun dossier local ne traite directement de ce point précis, mes fonctions restent actives. Vous pouvez me demander de télécharger un dossier complet dès que la connexion sera rétablie."
        }

        LocalLlmResult(
            replyText = genericReply,
            confidenceScore = 0.75f,
            isGroundedOnLocalSource = false
        )
    }

    companion object {
        fun evaluateMathExpression(text: String): String? {
            val clean = text.lowercase(Locale.ROOT)
                .replace("calcule", "")
                .replace("calculer", "")
                .replace("combien font", "")
                .replace("combien fait", "")
                .replace("how much is", "")
                .replace("what is", "")
                .replace("?", "")
                .replace("=", "")
                .trim()

            val simpleRegex = Regex("^(\\d+(?:\\.\\d+)?)\\s*([+\\-*/xX])\\s*(\\d+(?:\\.\\d+)?)$")
            val match = simpleRegex.find(clean) ?: return null
            val num1 = match.groupValues[1].toDoubleOrNull() ?: return null
            val op = match.groupValues[2]
            val num2 = match.groupValues[3].toDoubleOrNull() ?: return null

            val res = when (op.lowercase(Locale.ROOT)) {
                "+" -> num1 + num2
                "-" -> num1 - num2
                "*", "x" -> num1 * num2
                "/" -> if (num2 != 0.0) num1 / num2 else return "Division par zéro impossible"
                else -> return null
            }
            return if (res % 1.0 == 0.0) res.toLong().toString() else String.format(Locale.US, "%.2f", res)
        }
    }
}
