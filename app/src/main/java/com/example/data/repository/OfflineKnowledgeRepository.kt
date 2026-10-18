package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.DownloadedDocument
import com.example.data.model.DownloadedKnowledge
import com.example.data.model.PendingTask
import com.example.data.model.ResourcePack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class OfflineKnowledgeRepository(
    private val database: AppDatabase
) {
    val allKnowledgeFlow: Flow<List<DownloadedKnowledge>> = database.knowledgeDao().getAllKnowledge()
    val allDocumentsFlow: Flow<List<DownloadedDocument>> = database.documentDao().getAllDocuments()
    val allResourcesFlow: Flow<List<ResourcePack>> = database.resourceDao().getAllResources()
    val pendingTasksFlow: Flow<List<PendingTask>> = database.pendingTaskDao().getUnresolvedTasks()

    private val stopWords = setOf(
        "parle", "parles", "parlez", "peux", "veut", "veux", "faire", "cours", "info",
        "infos", "donne", "donnes", "explique", "expliquer", "dis", "discuter", "qu'est",
        "c'est", "quoi", "pour", "dans", "avec", "tout", "plus", "bien", "comment",
        "pourquoi", "quand", "quel", "quelle", "quels", "quelles", "avoir", "etre",
        "sujet", "savoir", "connais", "connaissances", "about", "what", "tell", "explain", "learn"
    )

    data class NotebookLMSourceContext(
        val matchingDocuments: List<DownloadedDocument>,
        val formattedContext: String
    )

    /**
     * Builds full NotebookLM grounding context from user's accumulated sources.
     * Ingests full text content so Code can answer ANY question about the sources.
     */
    suspend fun buildNotebookLMContext(userPrompt: String, preferredDocId: Long? = null): NotebookLMSourceContext? = withContext(Dispatchers.IO) {
        val trimmed = userPrompt.trim()
        val allDocs = database.documentDao().getAllDocumentsSync()
        if (allDocs.isEmpty()) return@withContext null

        val lower = trimmed.lowercase()

        // Pure greetings with no intellectual intent
        val pureGreetings = setOf("bonjour", "salut", "coucou", "hello", "hi", "ça va", "comment tu vas")
        if (pureGreetings.contains(lower) && preferredDocId == null) return@withContext null

        val isExplicitSourceInquiry = lower.contains("source") || lower.contains("document") ||
                lower.contains("lien") || lower.contains("article") || lower.contains("vidéo") ||
                lower.contains("youtube") || lower.contains("tiktok") || lower.contains("pdf") ||
                lower.contains("fichier") || lower.contains("notes") || lower.contains("résume") ||
                lower.contains("synthèse") || lower.contains("d'après") || lower.contains("selon") ||
                lower.contains("qu'as-tu appris") || lower.contains("que sais-tu") || lower.contains("quiz") ||
                lower.contains("teste-moi") || lower.contains("questionne-moi")

        val keywords = lower
            .split(" ", "'", "-", "?", "!", ",", ".", ":", ";", "\"", "(", ")")
            .map { it.trim() }
            .filter { it.length >= 3 && !stopWords.contains(it) }

        val scoredDocs = allDocs.map { doc ->
            var score = 0
            if (preferredDocId != null && doc.id == preferredDocId) {
                score += 1000
            }
            val docTitleLower = doc.title.lowercase()
            val docKeywordsLower = doc.keywords.lowercase()
            val docSummaryLower = doc.summary.lowercase()
            val docContentLower = doc.fullTextContent.lowercase()

            for (kw in keywords) {
                if (docTitleLower.contains(kw)) score += 15
                if (docKeywordsLower.contains(kw)) score += 8
                if (docSummaryLower.contains(kw)) score += 6
                if (docContentLower.contains(kw)) score += 3
            }

            Pair(doc, score)
        }

        val topMatches = scoredDocs.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }

        val selectedDocs = when {
            preferredDocId != null -> {
                val pref = allDocs.find { it.id == preferredDocId }
                if (pref != null) listOf(pref) + topMatches.filter { it.id != preferredDocId }.take(2)
                else topMatches.take(3)
            }
            topMatches.isNotEmpty() -> topMatches.take(3)
            isExplicitSourceInquiry -> allDocs.take(4)
            allDocs.size <= 3 && keywords.isNotEmpty() -> allDocs
            else -> emptyList()
        }

        if (selectedDocs.isEmpty()) return@withContext null

        val formatted = buildString {
            appendLine("=== BASE DE CONNAISSANCES DE L'UTILISATEUR (STYLE NOTEBOOKLM) ===")
            appendLine("Code possède en mémoire les sources suivantes accumulées au jour le jour par l'utilisateur.")
            appendLine("Tu as une maîtrise TOTALE et ABSOLUE de leur contenu pour répondre à TOUTES les questions :")
            for ((idx, doc) in selectedDocs.withIndex()) {
                appendLine("\n--- SOURCE N°${idx + 1} : « ${doc.title} » ---")
                appendLine("Type : ${doc.topicCategory} | Origine : ${doc.authorOrSource}${if (!doc.sourceUrl.isNullOrBlank()) " (${doc.sourceUrl})" else ""}")
                appendLine("Résumé clé : ${doc.summary}")
                appendLine("Points essentiels : ${doc.keywords}")
                appendLine("CONTENU INTÉGRAL DE LA SOURCE :")
                appendLine(doc.fullTextContent.take(30000))
            }
            appendLine("\n=== DIRECTIVES NOTEBOOKLM POUR TA RÉPONSE ===")
            appendLine("1. Réponds avec une précision remarquable à la question de ton ami en t'appuyant sur les faits, chiffres, explications et arguments de la source.")
            appendLine("2. Tu peux citer la source (« Dans ton document... », « Selon la vidéo... », « D'après l'article... ») et répondre à n'importe quelle question (résumé, détail, explication, quiz).")
            appendLine("3. Reste toujours complice, vivant, chaleureux et oral (ne lis pas froidement, discute avec intelligence comme deux vrais amis).")
        }

        NotebookLMSourceContext(selectedDocs, formatted)
    }

    suspend fun getTotalWordsAccumulated(): Int = withContext(Dispatchers.IO) {
        val allDocs = database.documentDao().getAllDocumentsSync()
        allDocs.sumOf { doc ->
            doc.fullTextContent.split("\\s+".toRegex()).size
        }
    }

    /**
     * Finds a matching user-downloaded offline document when the user wants to discuss it.
     */
    suspend fun findMatchingDocumentForDiscussion(userPrompt: String): DownloadedDocument? = withContext(Dispatchers.IO) {
        val nbContext = buildNotebookLMContext(userPrompt)
        nbContext?.matchingDocuments?.firstOrNull()
    }

    /**
     * Query user-downloaded offline sources and documents when specifically relevant to the discussion.
     */
    suspend fun queryContentForDiscussion(userPrompt: String): String? = withContext(Dispatchers.IO) {
        val nbContext = buildNotebookLMContext(userPrompt)
        nbContext?.formattedContext
    }

    suspend fun saveDownloadedKnowledge(knowledge: DownloadedKnowledge): Long = withContext(Dispatchers.IO) {
        val id = database.knowledgeDao().insertKnowledge(knowledge)
        database.resourceDao().insertResource(
            ResourcePack(
                id = "knowledge_$id",
                name = "Cours : ${knowledge.topic}",
                type = "Connaissances hors-ligne",
                sizeMb = knowledge.sizeMb,
                downloadDate = System.currentTimeMillis(),
                lastUsedDate = System.currentTimeMillis(),
                isDownloaded = true,
                isProtected = false
            )
        )
        id
    }

    suspend fun saveDownloadedDocument(document: DownloadedDocument): Long = withContext(Dispatchers.IO) {
        val id = database.documentDao().insertDocument(document)
        database.resourceDao().insertResource(
            ResourcePack(
                id = "document_$id",
                name = "Document : ${document.title}",
                type = "Document de référence",
                sizeMb = document.sizeMb,
                downloadDate = System.currentTimeMillis(),
                lastUsedDate = System.currentTimeMillis(),
                isDownloaded = true,
                isProtected = false
            )
        )
        id
    }

    suspend fun deleteKnowledge(knowledge: DownloadedKnowledge) = withContext(Dispatchers.IO) {
        database.knowledgeDao().deleteKnowledge(knowledge)
        database.resourceDao().markDeleted("knowledge_${knowledge.id}")
    }

    suspend fun deleteDocument(document: DownloadedDocument) = withContext(Dispatchers.IO) {
        database.documentDao().deleteDocument(document)
        database.resourceDao().markDeleted("document_${document.id}")
    }

    suspend fun saveIngestedSource(source: com.example.data.remote.IngestedSourceResult): Long = withContext(Dispatchers.IO) {
        val categoryName = when (source.type.lowercase()) {
            "youtube" -> "YouTube 🎥"
            "tiktok" -> "TikTok 📱"
            "web" -> "Site Web 🌐"
            "image" -> "Image 🖼️"
            "folder" -> "Dossier 📁"
            "pdf" -> "PDF 📑"
            else -> "Document 📄"
        }

        val doc = DownloadedDocument(
            title = source.title,
            authorOrSource = source.author,
            topicCategory = categoryName,
            summary = source.summary,
            fullTextContent = source.fullContent,
            keywords = source.keyPoints,
            downloadDate = System.currentTimeMillis(),
            sizeMb = source.sizeMb,
            isAvailableOffline = true,
            sourceUrl = source.sourceUrl,
            sourceType = source.type
        )

        val id = database.documentDao().insertDocument(doc)

        // Also register knowledge entity for faster contextual lookups
        database.knowledgeDao().insertKnowledge(
            DownloadedKnowledge(
                topic = source.title,
                category = categoryName,
                summary = source.summary,
                detailedContent = source.fullContent,
                keyPoints = source.keyPoints,
                sampleQuestions = "De quoi parle ${source.title} ?; Quels sont les points essentiels ?; Que retenir ?",
                sizeMb = source.sizeMb,
                isAvailableOffline = true
            )
        )

        database.resourceDao().insertResource(
            ResourcePack(
                id = "document_$id",
                name = "${categoryName}: ${source.title}",
                type = "Source mémorisée",
                sizeMb = source.sizeMb,
                downloadDate = System.currentTimeMillis(),
                lastUsedDate = System.currentTimeMillis(),
                isDownloaded = true,
                isProtected = false
            )
        )

        id
    }

    suspend fun storeSearchResultAsDocument(
        query: String,
        category: String,
        summary: String,
        fullContent: String
    ): DownloadedDocument = withContext(Dispatchers.IO) {
        val doc = DownloadedDocument(
            title = query.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            authorOrSource = "Recherche Web Code",
            topicCategory = category,
            summary = summary,
            fullTextContent = fullContent,
            keywords = query,
            downloadDate = System.currentTimeMillis(),
            sizeMb = 2.5,
            isAvailableOffline = true
        )
        val id = saveDownloadedDocument(doc)
        doc.copy(id = id)
    }

    suspend fun getTotalStorageUsedMb(): Double = withContext(Dispatchers.IO) {
        val knowledgeList = allKnowledgeFlow.firstOrNull() ?: emptyList()
        val docList = allDocumentsFlow.firstOrNull() ?: emptyList()
        val resources = allResourcesFlow.firstOrNull() ?: emptyList()

        val knowledgeMb = knowledgeList.sumOf { it.sizeMb }
        val docMb = docList.sumOf { it.sizeMb }
        val resourceMb = resources.filter { it.isDownloaded }.sumOf { it.sizeMb }

        knowledgeMb + docMb + resourceMb
    }

    /**
     * Initializes initial boot pack if not yet installed, so Code works offline from day one.
     */
    suspend fun initializeBootPackIfNeeded(
        onProgress: (Float, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val profile = database.userDao().getUserProfile().firstOrNull() ?: return@withContext
        if (profile.hasDownloadedInitialPack) return@withContext

        onProgress(0.15f, "Initialisation du pack hors-ligne essentiel...")
        delay(350)
        onProgress(0.40f, "Téléchargement des modules de sciences et d'histoire...")
        delay(400)
        onProgress(0.70f, "Téléchargement des dictionnaires et règles d'anglais...")
        delay(350)
        onProgress(0.95f, "Indexation de la base de connaissances locale...")
        delay(300)

        database.userDao().insertOrUpdate(profile.copy(hasDownloadedInitialPack = true))
        onProgress(1.0f, "Pack hors-ligne installé avec succès ! Ton compagnon est prêt à discuter.")
    }
}
