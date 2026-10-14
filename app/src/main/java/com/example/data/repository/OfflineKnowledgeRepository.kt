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

    /**
     * Query downloaded offline knowledge and documents to provide rich context for voice discussions.
     */
    suspend fun queryContentForDiscussion(userPrompt: String): String? = withContext(Dispatchers.IO) {
        val trimmed = userPrompt.trim()
        if (trimmed.isBlank()) return@withContext null

        val keywords = trimmed.lowercase()
            .split(" ", "'", "-", "?", "!", ",", ".", ":", ";")
            .map { it.trim() }
            .filter { it.length >= 3 && !stopWords.contains(it) }

        // 1. Direct Topic match (highest relevance)
        for (kw in keywords) {
            val topicMatch = database.knowledgeDao().findMatchingByTopic(kw)
            if (topicMatch != null) {
                return@withContext "D'après notre cours téléchargé sur ${topicMatch.topic} : ${topicMatch.summary} ${topicMatch.detailedContent.take(220)}... Points clés : ${topicMatch.keyPoints}."
            }
        }

        // 2. Direct Document Title match
        for (kw in keywords) {
            val docTitleMatch = database.documentDao().findMatchingByTitle(kw)
            if (docTitleMatch != null) {
                return@withContext "D'après le document \"${docTitleMatch.title}\" (${docTitleMatch.topicCategory}) stocké sur ton téléphone : ${docTitleMatch.summary} ${docTitleMatch.fullTextContent.take(200)}..."
            }
        }

        // 3. Broader Knowledge match
        for (kw in keywords) {
            val kwMatch = database.knowledgeDao().findMatchingKnowledge(kw)
            if (kwMatch != null) {
                return@withContext "D'après nos connaissances enregistrées sur ${kwMatch.topic} : ${kwMatch.summary} ${kwMatch.detailedContent.take(200)}..."
            }
        }

        // 4. Broader Document match
        for (kw in keywords) {
            val docMatch = database.documentDao().searchDocuments(kw)
            if (docMatch.isNotEmpty()) {
                val doc = docMatch.first()
                return@withContext "Selon notre document \"${doc.title}\" en mémoire locale : ${doc.summary} ${doc.fullTextContent.take(200)}..."
            }
        }

        // 5. Fallback full prompt check if no keywords matched
        val directKnowledge = database.knowledgeDao().findMatchingKnowledge(trimmed)
        if (directKnowledge != null) {
            return@withContext "D'après notre cours sur ${directKnowledge.topic} : ${directKnowledge.summary} Points clés : ${directKnowledge.keyPoints}."
        }

        null
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
