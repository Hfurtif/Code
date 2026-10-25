package com.example.engine

import java.text.Normalizer
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Calame 100% Local NLP & Source Analysis Engine.
 * Faithfully ports the exact extraction, TF-IDF / BM25 term weighting,
 * sentence density scoring, cloze question generation, and 10 Studio generators
 * from the Calame source analyzer architecture.
 */
object CalameAnalysisEngine {

    val STOPWORDS = setOf(
        "le", "la", "les", "un", "une", "des", "du", "de", "et", "en", "au", "aux",
        "ce", "ces", "cet", "cette", "que", "qui", "quoi", "dans", "pour", "par",
        "sur", "avec", "sans", "est", "sont", "etre", "etait", "ont", "avoir", "fait",
        "ainsi", "comme", "mais", "donc", "car", "son", "sa", "ses", "leur", "leurs",
        "nous", "vous", "ils", "elles", "plus", "tout", "tous", "toute", "peut",
        "aussi", "entre", "sous", "vers", "chez", "dont", "alors", "lors", "faire",
        "faut", "autre", "autres", "meme", "tres", "bien", "cela", "ceci", "apres",
        "avant", "encore", "toujours", "souvent", "chaque", "quand", "comment",
        "pourquoi", "doit", "doivent", "peuvent", "permet", "permettent", "etes",
        "faites", "the", "and", "for", "that", "with", "this", "from", "are", "was",
        "were", "have", "has", "not", "but", "you", "your", "can", "will"
    )

    data class SourceDoc(
        val id: Long,
        val title: String,
        val text: String,
        val isSelected: Boolean = true
    )

    data class SourceChunk(
        val sourceId: Long,
        val sourceTitle: String,
        val text: String,
        val start: Int,
        val end: Int,
        var score: Double = 0.0
    )

    enum class StudioKind(val key: String, val title: String, val icon: String, val description: String) {
        SUMMARY("summary", "Résumé structuré", "📑", "Synthèse concise et fidèle par points clés"),
        GUIDE("guide", "Guide d'étude", "📖", "Notions clés, définitions et points à retenir"),
        FAQ("faq", "Questions fréquentes", "❓", "Questions directes et réponses extraites des sources"),
        AUDIO("audio", "Résumé audio oral", "🎙️", "Script oralisé de 2 minutes pour écoute TTS"),
        SLIDES("slides", "Présentation", "📊", "Diapositives structurées avec titre et puces"),
        MINDMAP("mindmap", "Carte mentale", "🧠", "Arborescence textuelle hiérarchisée"),
        CARDS("cards", "Fiches d'apprentissage", "🗂️", "Paires Question / Réponse"),
        QUIZ("quiz", "Quiz QCM", "🎯", "5 questions à choix multiples avec corrigé"),
        TABLE("table", "Tableau comparatif", "📋", "Tableau de fréquence des concepts et mots-clés"),
        REPORT("report", "Rapport de synthèse", "📄", "Rapport complet avec introduction et thèmes")
    }

    /**
     * Accent-insensitive NFD normalization: strips accents and transforms to lowercase.
     */
    fun normalize(s: String): String {
        val nfd = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        return nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    /**
     * Tokenizes text into normalized word stems of at least 3 characters,
     * filtering out the bilingual stopword set.
     */
    fun tokenize(s: String): List<String> {
        val normStr = normalize(s)
        val regex = Regex("[a-z0-9]{3,}")
        return regex.findAll(normStr)
            .map { it.value }
            .filter { !STOPWORDS.contains(it) }
            .toList()
    }

    /**
     * Chunks text along sentence/paragraph boundaries (~380 chars buffer)
     * keeping precise start and end character offsets into the original source text.
     */
    fun createChunks(sources: List<SourceDoc>): List<SourceChunk> {
        val out = mutableListOf<SourceChunk>()
        val boundaryRegex = Regex("(?<=[.!?…])\\s+|\\n+")

        for (s in sources) {
            if (!s.isSelected || s.text.isBlank()) continue
            var from = 0
            var buf = ""
            var start = 0

            val parts = s.text.split(boundaryRegex)
            for (p in parts) {
                val trimmedP = p.trim()
                if (trimmedP.isEmpty()) continue
                val i = s.text.indexOf(p, from)
                if (i < 0) continue

                from = i + p.length
                if (buf.isEmpty()) {
                    start = i
                }
                buf = s.text.substring(start, from)

                if (buf.length > 380) {
                    out.add(
                        SourceChunk(
                            sourceId = s.id,
                            sourceTitle = s.title,
                            text = buf,
                            start = start,
                            end = from
                        )
                    )
                    buf = ""
                }
            }

            if (buf.isNotEmpty()) {
                out.add(
                    SourceChunk(
                        sourceId = s.id,
                        sourceTitle = s.title,
                        text = buf,
                        start = start,
                        end = from
                    )
                )
            }
        }
        return out
    }

    /**
     * Retrieves top-N chunks matching a search query using Calame's TF-IDF term weighting:
     * score = sum_w (1 + ln(TF(w))) * ln(1 + total_chunks / DF(w))
     */
    fun retrieveChunks(sources: List<SourceDoc>, query: String, topN: Int = 5): List<SourceChunk> {
        val queryTokens = tokenize(query).toSet()
        if (queryTokens.isEmpty()) return emptyList()

        val chunks = createChunks(sources)
        if (chunks.isEmpty()) return emptyList()

        // Calculate Document Frequency (DF) across all chunks
        val chunkTokensList = chunks.map { tokenize(it.text) }
        val df = mutableMapOf<String, Int>()

        for (tokens in chunkTokensList) {
            val uniqueTokens = tokens.toSet()
            for (w in uniqueTokens) {
                df[w] = (df[w] ?: 0) + 1
            }
        }

        // Calculate Term Frequency (TF) per chunk and compute TF-IDF score
        val totalChunks = chunks.size.toDouble()
        for (i in chunks.indices) {
            val chunk = chunks[i]
            val tokens = chunkTokensList[i]
            val tf = mutableMapOf<String, Int>()
            for (w in tokens) {
                tf[w] = (tf[w] ?: 0) + 1
            }

            var score = 0.0
            for (w in queryTokens) {
                val directCount = tf[w] ?: 0
                val stemMatch = if (directCount == 0) {
                    tf.entries.firstOrNull { (k, _) ->
                        k.removeSuffix("s") == w.removeSuffix("s") ||
                        (k.length >= 4 && w.length >= 4 && (k.startsWith(w) || w.startsWith(k)))
                    }
                } else null

                val count = if (directCount > 0) directCount else (stemMatch?.value ?: 0)
                if (count > 0) {
                    val matchKey = if (directCount > 0) w else (stemMatch?.key ?: w)
                    val docFreq = df[matchKey] ?: 1
                    score += (1.0 + ln(count.toDouble())) * ln(1.0 + totalChunks / docFreq.toDouble())
                }
            }
            chunk.score = score

        }

        return chunks.filter { it.score > 0.0 }
            .sortedByDescending { it.score }
            .take(topN)
    }

    /**
     * Extracts top keywords by frequency (words of 4+ characters, non-stopwords).
     */
    fun extractKeywords(sources: List<SourceDoc>, topN: Int = 10): List<String> {
        val freq = mutableMapOf<String, Int>()
        val display = mutableMapOf<String, String>()
        val wordRegex = Regex("[A-Za-zÀ-ÿ0-9]{4,}")

        for (s in sources) {
            if (!s.isSelected || s.text.isBlank()) continue
            val matches = wordRegex.findAll(s.text)
            for (m in matches) {
                val w = m.value
                val k = normalize(w)
                if (STOPWORDS.contains(k)) continue
                freq[k] = (freq[k] ?: 0) + 1
                if (!display.containsKey(k)) {
                    display[k] = w.lowercase()
                }
            }
        }

        return freq.entries
            .sortedByDescending { it.value }
            .take(topN)
            .mapNotNull { display[it.key] }
    }

    /**
     * Extracts top key sentences using keyword density scoring:
     * score = (number of top-15 keywords in sentence) / sqrt(sentence length)
     */
    fun extractTopSentences(sources: List<SourceDoc>, topN: Int = 6): List<String> {
        val kw = extractKeywords(sources, 15).map { normalize(it) }.toSet()
        if (kw.isEmpty()) return emptyList()

        val chunks = createChunks(sources)
        val sentenceRegex = Regex("(?<=[.!?…])\\s+")
        val candidates = mutableListOf<Pair<String, Double>>()

        for (chunk in chunks) {
            val sentences = chunk.text.split(sentenceRegex)
            for (raw in sentences) {
                val s = raw.trim()
                if (s.length > 30) {
                    val sentenceTokens = tokenize(s)
                    val matchingCount = sentenceTokens.count { kw.contains(it) }
                    val score = matchingCount.toDouble() / sqrt(s.length.toDouble())
                    candidates.add(Pair(s, score))
                }
            }
        }

        return candidates
            .sortedByDescending { it.second }
            .take(topN)
            .map { it.first }
    }

    /**
     * Finds the best sentence matching a specific keyword.
     */
    fun findBestSentence(sources: List<SourceDoc>, keyword: String): String {
        val topChunk = retrieveChunks(sources, keyword, 1).firstOrNull() ?: return ""
        val sentenceRegex = Regex("(?<=[.!?…])\\s+")
        val sentences = topChunk.text.split(sentenceRegex)
        val normKeyword = normalize(keyword)

        val match = sentences.find {
            val normSent = normalize(it)
            normSent.contains(normKeyword) || normSent.contains(normKeyword.removeSuffix("s"))
        }
        return (match ?: sentences.firstOrNull() ?: "").trim()
    }

    /**
     * Generates Atelier Studio content completely locally and offline.
     */
    fun generateStudioContent(
        sources: List<SourceDoc>,
        notebookTitle: String,
        kind: StudioKind
    ): String {
        val activeSources = sources.filter { it.isSelected && it.text.isNotBlank() }
        if (activeSources.isEmpty()) {
            return "Aucune source active disponible. Activez au moins une source pour générer ce contenu."
        }

        val kw = extractKeywords(activeSources, 10)
        val ts = extractTopSentences(activeSources, 6)
        val bul: (List<String>) -> String = { list -> list.joinToString("\n") { "• $it" } }

        return when (kind) {
            StudioKind.SUMMARY -> {
                if (ts.isEmpty()) "Contenu trop court pour extraire un résumé."
                else bul(ts)
            }

            StudioKind.GUIDE -> {
                val notions = if (kw.isNotEmpty()) "Notions clés : ${kw.joinToString(", ")}" else "Notions clés : Généralités"
                val retain = if (ts.isNotEmpty()) bul(ts) else "• Consultez les sources détaillées."
                "$notions\n\nÀ retenir :\n$retain"
            }

            StudioKind.FAQ -> {
                val topK = kw.take(5)
                if (topK.isEmpty()) {
                    "Aucune question fréquente n'a pu être formulée."
                } else {
                    topK.joinToString("\n\n") { k ->
                        val ans = findBestSentence(activeSources, k).ifBlank { "Informations détaillées dans la source." }
                        "Que disent les sources sur « $k » ?\n$ans"
                    }
                }
            }

            StudioKind.AUDIO -> {
                val sentences = ts.take(4)
                if (sentences.isEmpty()) {
                    "Voici un résumé oral de vos sources : le texte analysé présente les notions principales de $notebookTitle."
                } else {
                    "Voici un résumé de « $notebookTitle ». ${sentences.joinToString(" ")}"
                }
            }

            StudioKind.SLIDES -> {
                val slide1 = "Diapositive 1 — $notebookTitle\n• ${kw.take(3).joinToString(", ")}"
                val otherSlides = kw.take(5).mapIndexed { idx, k ->
                    val best = findBestSentence(activeSources, k).ifBlank { "Point clé analysé." }
                    "Diapositive ${idx + 2} — ${k.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}\n• $best"
                }
                (listOf(slide1) + otherSlides).joinToString("\n\n")
            }

            StudioKind.MINDMAP -> {
                val top6 = kw.take(6)
                val lines = mutableListOf("◆ $notebookTitle")
                top6.forEachIndexed { i, k ->
                    val isLast = i == top6.size - 1
                    val prefix = if (!isLast) "├ " else "└ "
                    val subPrefix = if (!isLast) "│  └ " else "   └ "
                    val best = findBestSentence(activeSources, k).take(110).ifBlank { "Concepts associés" }
                    lines.add("$prefix$k")
                    lines.add("$subPrefix$best")
                }
                lines.joinToString("\n")
            }

            StudioKind.CARDS -> {
                val top8 = kw.take(8)
                if (top8.isEmpty()) {
                    "Pas assez de vocabulaire pour générer des fiches."
                } else {
                    top8.mapIndexed { idx, k ->
                        val best = findBestSentence(activeSources, k).ifBlank { "Concept extrait des sources." }
                        "Fiche ${idx + 1}\nQuestion : Que faut-il retenir de « $k » ?\nRéponse : $best"
                    }.joinToString("\n\n")
                }
            }

            StudioKind.QUIZ -> {
                generateClozeQuiz(activeSources, kw)
            }

            StudioKind.TABLE -> {
                val allTextNorm = normalize(activeSources.joinToString(" ") { it.text })
                val sourcesTable = buildString {
                    appendLine("Source | Mots | Mots-clés")
                    for (src in activeSources) {
                        val wordCount = Regex("\\S+").findAll(src.text).count()
                        val srcKw = extractKeywords(listOf(src), 3).joinToString(", ")
                        appendLine("${src.title} | $wordCount | $srcKw")
                    }
                }

                val kwTable = buildString {
                    appendLine("\nMot-clé | Occurrences")
                    for (k in kw) {
                        val occurrences = allTextNorm.split(normalize(k)).size - 1
                        appendLine("$k | $occurrences")
                    }
                }

                sourcesTable + kwTable
            }

            StudioKind.REPORT -> {
                val intro = ts.take(2).joinToString(" ")
                val themes = kw.take(4).joinToString("\n") { k ->
                    val best = findBestSentence(activeSources, k).ifBlank { "Détails à retrouver dans les sources." }
                    "• ${k.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} : $best"
                }
                val keyPoints = bul(ts.drop(2).take(4).ifEmpty { ts.take(2) })
                val sourcesList = activeSources.joinToString("\n") { "• ${it.title}" }

                """
                Rapport de Synthèse — $notebookTitle

                1. Introduction
                $intro

                2. Thèmes principaux
                $themes

                3. Points clés
                $keyPoints

                4. Sources analysées
                $sourcesList
                """.trimIndent()
            }
        }
    }

    /**
     * Generates a 5-question multiple-choice cloze quiz where key terms are masked with '_____'
     * and distractors are selected from other extracted keywords.
     */
    private fun generateClozeQuiz(sources: List<SourceDoc>, keywords: List<String>): String {
        data class Question(val question: String, val options: List<String>, val answerIndex: Int)
        val questions = mutableListOf<Question>()
        val usedSentences = mutableSetOf<String>()

        for (k in keywords) {
            if (questions.size >= 5) break
            val sentence = findBestSentence(sources, k)
            if (sentence.isBlank() || usedSentences.contains(sentence)) continue

            val regex = Regex(Regex.escape(k), RegexOption.IGNORE_CASE)
            if (!regex.containsMatchIn(sentence)) continue

            usedSentences.add(sentence)
            val distractors = keywords.filter { it != k }.shuffled().take(3)
            val allOptions = (distractors + k).shuffled()
            val answerIndex = allOptions.indexOf(k)
            val clozeText = sentence.replace(regex, "_____")

            questions.add(Question(clozeText, allOptions, answerIndex))
        }

        if (questions.isEmpty()) {
            return "Pas assez de matière pour un quiz à choix multiples."
        }

        val letters = listOf("A", "B", "C", "D")
        val questionsText = questions.mapIndexed { i, q ->
            val opts = q.options.mapIndexed { j, opt -> "   ${letters.getOrElse(j) { "?" }}) $opt" }.joinToString("\n")
            "${i + 1}. ${q.question}\n$opts"
        }.joinToString("\n\n")

        val answersText = questions.mapIndexed { i, q ->
            "${i + 1}-${letters.getOrElse(q.answerIndex) { "A" }}"
        }.joinToString("  ")

        return "$questionsText\n\nRéponses : $answersText"
    }
}
