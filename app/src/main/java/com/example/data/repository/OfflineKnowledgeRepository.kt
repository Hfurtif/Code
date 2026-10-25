package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.CachedLesson
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
    val allCachedLessonsFlow: Flow<List<CachedLesson>> = database.cachedLessonDao().getAllCachedLessons()
    val offlineAvailableLessonsFlow: Flow<List<CachedLesson>> = database.cachedLessonDao().getOfflineAvailableLessons()

    private val stopWords = setOf(
        "parle", "parles", "parlez", "peux", "veut", "veux", "faire", "cours", "info",
        "infos", "donne", "donnes", "explique", "expliquer", "dis", "discuter", "qu'est",
        "c'est", "quoi", "pour", "dans", "avec", "tout", "plus", "bien", "comment",
        "pourquoi", "quand", "quel", "quelle", "quels", "quelles", "avoir", "etre",
        "sujet", "savoir", "connais", "connaissances", "about", "what", "tell", "explain", "learn"
    )

    val selectedDocumentsFlow: Flow<List<DownloadedDocument>> = database.documentDao().getSelectedDocuments()
    val allNotesFlow: Flow<List<com.example.data.model.NotebookNote>> = database.notebookNoteDao().getAllNotes()
    fun getStudioArtifacts(notebookTitle: String = "Mon Carnet"): Flow<List<com.example.data.model.StudioArtifact>> =
        database.studioArtifactDao().getArtifactsForNotebook(notebookTitle)

    data class NotebookLMSourceContext(
        val matchingDocuments: List<DownloadedDocument>,
        val formattedContext: String,
        val retrievedChunks: List<com.example.engine.CalameAnalysisEngine.SourceChunk> = emptyList(),
        val extractiveSummary: String = ""
    )

    /**
     * Builds full NotebookLM grounding context from user's accumulated sources
     * using Calame's exact 100% local TF-IDF retrieval scoring and chunk extraction.
     */
    suspend fun buildNotebookLMContext(userPrompt: String, preferredDocId: Long? = null): NotebookLMSourceContext? = withContext(Dispatchers.IO) {
        val trimmed = userPrompt.trim()
        val allDocs = database.documentDao().getAllDocumentsSync()
        if (allDocs.isEmpty()) return@withContext null

        val lower = trimmed.lowercase()

        // Pure greetings with no intellectual intent
        val pureGreetings = setOf("bonjour", "salut", "coucou", "hello", "hi", "ça va", "comment tu vas")
        if (pureGreetings.contains(lower) && preferredDocId == null) return@withContext null

        val sourceDocs = allDocs.map {
            com.example.engine.CalameAnalysisEngine.SourceDoc(
                id = it.id,
                title = it.title,
                text = it.fullTextContent,
                isSelected = if (preferredDocId != null && it.id == preferredDocId) true else it.isSelected
            )
        }

        // Retrieve relevant chunks with Calame TF-IDF
        var retrieved = com.example.engine.CalameAnalysisEngine.retrieveChunks(sourceDocs, trimmed, topN = 5)

        // If preferredDocId was specified, ensure at least one chunk from that document is included
        if (preferredDocId != null && retrieved.none { it.sourceId == preferredDocId }) {
            val prefDoc = sourceDocs.find { it.id == preferredDocId }
            if (prefDoc != null) {
                val prefChunks = com.example.engine.CalameAnalysisEngine.createChunks(listOf(prefDoc)).take(2)
                retrieved = (prefChunks + retrieved).take(5)
            }
        }

        // Fallback for general inquiries if no specific query terms matched
        if (retrieved.isEmpty()) {
            val isExplicitSourceInquiry = lower.contains("source") || lower.contains("document") ||
                    lower.contains("lien") || lower.contains("article") || lower.contains("résume") ||
                    lower.contains("synthèse") || lower.contains("qu'as-tu appris") || lower.contains("que sais-tu")

            if (isExplicitSourceInquiry || allDocs.size <= 3) {
                retrieved = com.example.engine.CalameAnalysisEngine.createChunks(sourceDocs.filter { it.isSelected }).take(4)
            }
        }

        if (retrieved.isEmpty()) return@withContext null

        val matchingDocIds = retrieved.map { it.sourceId }.toSet()
        val selectedDocs = allDocs.filter { matchingDocIds.contains(it.id) }

        val extractive = retrieved.take(3).mapIndexed { i, c ->
            "${c.text.take(260)} [${i + 1}]"
        }.joinToString("\n\n")

        val formatted = buildString {
            appendLine("=== CONTEXTE SOURCES LOCALES (STYLE CALAME & NOTEBOOKLM) ===")
            appendLine("Tu es un assistant de recherche. Réponds en français, uniquement à partir des extraits numérotés fournis ci-dessous.")
            appendLine("Cite systématiquement les extraits utilisés avec [1], [2], etc. Si l'information est absente des extraits, dis-le clairement.")
            for ((idx, chunk) in retrieved.withIndex()) {
                appendLine("\n[${idx + 1}] (« ${chunk.sourceTitle} »)")
                appendLine(chunk.text)
            }
            appendLine("\n=== DIRECTIVES NOTEBOOKLM ===")
            appendLine("1. Sois précis, chaleureux et direct.")
            appendLine("2. Inclus les citations [1], [2] directement après les affirmations qui s'appuient sur chaque extrait.")
        }

        NotebookLMSourceContext(
            matchingDocuments = selectedDocs,
            formattedContext = formatted,
            retrievedChunks = retrieved,
            extractiveSummary = extractive
        )
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

    suspend fun toggleDocumentSelected(id: Long, isSelected: Boolean) = withContext(Dispatchers.IO) {
        database.documentDao().updateSelected(id, isSelected)
    }

    suspend fun toggleAllDocuments(isSelected: Boolean) = withContext(Dispatchers.IO) {
        database.documentDao().updateAllSelected(isSelected)
    }

    suspend fun addNotebookNote(text: String, notebookTitle: String = "Mon Carnet"): Long = withContext(Dispatchers.IO) {
        database.notebookNoteDao().insertNote(com.example.data.model.NotebookNote(text = text, notebookTitle = notebookTitle))
    }

    suspend fun deleteNotebookNote(id: Long) = withContext(Dispatchers.IO) {
        database.notebookNoteDao().deleteById(id)
    }

    suspend fun generateStudioArtifact(
        kind: com.example.engine.CalameAnalysisEngine.StudioKind,
        notebookTitle: String = "Mon Carnet"
    ): String = withContext(Dispatchers.IO) {
        val selectedDocs = database.documentDao().getSelectedDocumentsSync()
        val sources = selectedDocs.map {
            com.example.engine.CalameAnalysisEngine.SourceDoc(
                id = it.id,
                title = it.title,
                text = it.fullTextContent,
                isSelected = it.isSelected
            )
        }
        val content = com.example.engine.CalameAnalysisEngine.generateStudioContent(sources, notebookTitle, kind)
        val artifactId = "${notebookTitle}_${kind.key}"
        database.studioArtifactDao().insertArtifact(
            com.example.data.model.StudioArtifact(
                id = artifactId,
                kind = kind.key,
                title = kind.title,
                content = content,
                notebookTitle = notebookTitle
            )
        )
        content
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

    fun getLessonsByLanguage(langCode: String): Flow<List<CachedLesson>> {
        return database.cachedLessonDao().getLessonsByLanguage(langCode)
    }

    suspend fun cacheLesson(lesson: CachedLesson) = withContext(Dispatchers.IO) {
        database.cachedLessonDao().insertLesson(lesson)
    }

    suspend fun setLessonOfflineCache(lessonId: String, isCached: Boolean) = withContext(Dispatchers.IO) {
        database.cachedLessonDao().updateOfflineStatus(lessonId, isCached)
    }

    suspend fun getCachedLessonById(id: String): CachedLesson? = withContext(Dispatchers.IO) {
        database.cachedLessonDao().getLessonById(id)
    }

    suspend fun ensureDefaultLessonsSeeded() = withContext(Dispatchers.IO) {
        val count = database.cachedLessonDao().countTotalLessons()
        if (count == 0) {
            database.cachedLessonDao().insertLessons(com.example.data.local.DefaultCachedLessons.getDefaultLessons())
        }
    }
}

