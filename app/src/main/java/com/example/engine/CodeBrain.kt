package com.example.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.AppLanguage
import com.example.data.model.ConversationMessage
import com.example.data.model.DownloadedDocument
import com.example.data.model.DownloadedKnowledge
import com.example.data.model.LearnedError
import com.example.data.model.PendingTask
import com.example.data.model.TutorMode
import com.example.data.model.UserProfile
import com.example.data.remote.ContentItem
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiRequest
import com.example.data.remote.GenConfig
import com.example.data.remote.PartItem
import com.example.data.remote.WebSearchResult
import com.example.data.remote.WebSearchService
import com.example.data.repository.OfflineKnowledgeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

data class BrainResponse(
    val replyText: String,
    val correctionNote: String? = null,
    val identifiedError: LearnedError? = null,
    val isOfflineMode: Boolean = false,
    val triggeredCommand: String? = null,
    val newMode: TutorMode? = null,
    val pendingTaskLogged: Boolean = false,
    val downloadedKnowledge: DownloadedKnowledge? = null,
    val downloadedDocument: DownloadedDocument? = null,
    val isWebSearch: Boolean = false,
    val webSearchTopic: String? = null,
    val webSourceUrl: String? = null,
    val newCompanionName: String? = null
)

class CodeBrain(
    private val context: Context,
    private val database: AppDatabase,
    val repository: OfflineKnowledgeRepository = OfflineKnowledgeRepository(database)
) {
    companion object {
        private const val TAG = "CodeBrain"
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun processUserSpeech(
        userText: String,
        currentMode: TutorMode,
        userProfile: UserProfile,
        recentMessages: List<ConversationMessage>
    ): BrainResponse = withContext(Dispatchers.IO) {
        val trimmed = userText.trim()
        val lower = trimmed.lowercase()
        val lang = AppLanguage.findByCode(userProfile.preferredLanguageCode)

        // 1. Direct Voice Commands
        if (lower == "stop" || lower == "arrête" || lower == "silence" || lower == "pause") {
            return@withContext BrainResponse(
                replyText = if (lang.code.startsWith("en")) "Alright, I'll stop here." else "D'accord, je m'arrête là mon ami.",
                triggeredCommand = "stop"
            )
        }
        if (lower.contains("plus lentement") || lower.contains("parle plus lentement") || lower.contains("slower")) {
            return@withContext BrainResponse(
                replyText = if (lang.code.startsWith("en")) "Got it. I will speak more slowly." else "Bien compris mon ami. Je vais parler plus posément.",
                triggeredCommand = "slower"
            )
        }
        if (lower.contains("plus vite") || lower.contains("faster")) {
            return@withContext BrainResponse(
                replyText = if (lang.code.startsWith("en")) "Understood! I'll pick up the pace." else "C'est noté, j'accélère un peu le rythme !",
                triggeredCommand = "faster"
            )
        }
        if (lower.contains("voix féminine") || lower.contains("female voice")) {
            return@withContext BrainResponse(
                replyText = if (lang.code.startsWith("en")) "Here you go, switching to female voice." else "Et voilà, je passe en voix féminine pour nos échanges.",
                triggeredCommand = "voice_female"
            )
        }
        if (lower.contains("voix masculine") || lower.contains("male voice")) {
            return@withContext BrainResponse(
                replyText = if (lang.code.startsWith("en")) "Sure thing, switching to male voice." else "Très bien, j'adopte une voix masculine dès maintenant.",
                triggeredCommand = "voice_male"
            )
        }

        // 2. Storage Authorization Request / Confirmation (+20 GB)
        if (lower.contains("ajoute 20 go") || lower.contains("augmente le stockage") ||
            lower.contains("plus de stockage") || lower.contains("je t'autorise") ||
            (lower.contains("autorise") && lower.contains("stockage")) ||
            (lower.contains("oui") && lower.contains("stockage"))
        ) {
            val newLimit = userProfile.maxStorageLimitGb + 20.0
            database.userDao().insertOrUpdate(userProfile.copy(maxStorageLimitGb = newLimit))
            return@withContext BrainResponse(
                replyText = if (lang.code.startsWith("en")) {
                    "Thank you for the authorization! I have expanded the storage limit to ${newLimit.toInt()} GB on your phone. We have plenty of space to store everything!"
                } else {
                    "Merci pour ton accord ! J'ai étendu la capacité de stockage à ${newLimit.toInt()} Go sur ton portable. Nous avons tout l'espace nécessaire pour continuer d'enregistrer tes connaissances et dossiers !"
                },
                triggeredCommand = "expand_storage_20gb"
            )
        }

        // 2b. Companion rename intent (e.g. "appelle-toi Léo", "change ton nom en Lina", "je veux t'appeler Maya")
        val renameTarget = extractCompanionRenameIntent(trimmed)
        if (renameTarget != null) {
            val genderFeedback = if (userProfile.companionGender.equals("girl", ignoreCase = true) || userProfile.companionGender.equals("female", ignoreCase = true)) "ton amie" else "ton ami"
            return@withContext BrainResponse(
                replyText = "C'est noté ! À partir de maintenant, je m'appelle $renameTarget. Je suis ravi d'être $genderFeedback dans l'application Code !",
                newCompanionName = renameTarget
            )
        }

        // 3. Level & Progression check (Directly answered in conversation without needing separate screens)
        if (lower.contains("mon niveau") || lower.contains("ma progression") ||
            lower.contains("quel niveau") || lower.contains("où j'en suis") ||
            lower.contains("mes progrès") || lower.contains("évalue-moi") ||
            lower.contains("fais le point") || lower.contains("my level") || lower.contains("my progress")
        ) {
            val userLevel = userProfile.level.ifBlank { "Intermédiaire B1" }
            val docsCount = database.documentDao().getDocumentCount()
            val errorsCount = database.learnedErrorDao().getAllErrorsSync().size
            val reply = if (lang.code.startsWith("en")) {
                "Here is your progress update, my friend! Your estimated level is $userLevel. You have $docsCount stored source(s) in your offline memory that we can discuss anytime. You have mastered $errorsCount key language points. Keep going! Would you like to practice or explore one of your sources?"
            } else {
                "Voici le point sur ta progression, mon ami ! Ton niveau actuel est estimé à : $userLevel. Tu as déjà $docsCount source(s) mémorisée(s) dans ma mémoire locale pour nos discussions hors ligne. Tu as également assimilé $errorsCount notions clés. Tu progresses très bien ! De quelle source ou sujet souhaites-tu qu'on discute ?"
            }
            return@withContext BrainResponse(replyText = reply)
        }

        // 4. Direct Link & Source ingestion (YouTube, TikTok, Web links) provided in conversation
        val detectedUrl = com.example.data.remote.SourceIngestionService.extractUrl(trimmed)
        if (detectedUrl != null) {
            try {
                val ingestedResult = com.example.data.remote.SourceIngestionService.ingestFromUrl(detectedUrl)
                val docId = repository.saveIngestedSource(ingestedResult)
                val reply = if (lang.code.startsWith("en")) {
                    "I successfully downloaded and memorized your source: \"${ingestedResult.title}\" (${ingestedResult.author})! Key takeaway: ${ingestedResult.summary} It is now stored in your Sources and available offline. We can discuss it right now!"
                } else {
                    "J'ai bien téléchargé et mémorisé ta source : « ${ingestedResult.title} » (${ingestedResult.author}) ! Voici ce que j'en retiens : ${ingestedResult.summary} Elle est maintenant enregistrée dans tes Sources et consultable hors ligne. De quoi souhaites-tu qu'on discute à ce sujet ?"
                }
                return@withContext BrainResponse(
                    replyText = reply,
                    isWebSearch = false,
                    webSearchTopic = ingestedResult.title,
                    webSourceUrl = detectedUrl
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error ingesting url in chat", e)
                return@withContext BrainResponse(
                    replyText = "J'ai bien détecté ton lien, mais je n'ai pas pu en extraire le contenu pour le moment. Vérifie ta connexion ou colle directement le texte dans l'onglet Sources !"
                )
            }
        }

        // 5. Direct Document ingestion from conversation (e.g. "Voici un document [titre]: [contenu]")
        if (lower.startsWith("voici un document") || lower.startsWith("document :") || lower.startsWith("apprends ce document")) {
            val raw = trimmed.substringAfter(":").trim()
            val docTitle = if (raw.contains("\n")) raw.substringBefore("\n").take(40) else "Document utilisateur"
            val docContent = if (raw.contains("\n")) raw.substringAfter("\n").trim() else raw
            if (docContent.isNotBlank()) {
                val ingestedResult = com.example.data.remote.SourceIngestionService.ingestFromDocumentText(docTitle, docContent)
                repository.saveIngestedSource(ingestedResult)
                return@withContext BrainResponse(
                    replyText = "C'est enregistré ! J'ai lu, analysé et mémorisé ton document « ${ingestedResult.title} » dans mes Sources. Nous pouvons désormais en discuter hors ligne à tout moment !"
                )
            }
        }

        // 6. Query all Sources / What do you know about my sources
        if (lower.contains("mes sources") || lower.contains("mes documents") || lower.contains("que sais-tu de mes sources") ||
            lower.contains("qu'as-tu appris") || lower.contains("résume mes sources")
        ) {
            val docs = database.documentDao().getAllDocumentsSync()
            if (docs.isEmpty()) {
                return@withContext BrainResponse(
                    replyText = "Tu n'as pas encore ajouté de source dans ma mémoire. Ajoute un lien (YouTube, TikTok, article web) ou un document dans l'onglet Sources (ou donne-moi un lien directement ici) pour que je le télécharge et l'apprenne !"
                )
            } else {
                val titlesList = docs.take(5).joinToString(", ") { "« ${it.title} »" }
                val reply = "Voici les sources que j'ai mémorisées sur ton portable : $titlesList${if (docs.size > 5) " et ${docs.size - 5} autre(s)" else ""}. Elles sont toutes disponibles pour nos échanges hors ligne. De laquelle souhaites-tu qu'on parle ?"
                return@withContext BrainResponse(replyText = reply)
            }
        }

        // 7. Check for English learner mistakes
        val detectedMistake = if (lower.contains("agree") || lower.contains("people is") || lang.code.startsWith("en")) {
            detectCommonMistake(trimmed)
        } else null

        if (detectedMistake != null) {
            val existing = database.learnedErrorDao().findByPhrase(detectedMistake.badPhrase)
            if (existing != null) {
                database.learnedErrorDao().updateError(existing.copy(count = existing.count + 1, timestamp = System.currentTimeMillis()))
            } else {
                database.learnedErrorDao().insertError(detectedMistake)
            }
        }

        // 8. Grounding on user's downloaded Sources: Search library for matching topic/document
        val matchingContent = repository.queryContentForDiscussion(trimmed)
        if (matchingContent != null) {
            return@withContext BrainResponse(
                replyText = "$matchingContent Qu'aimerais-tu que je t'explique ou qu'on approfondisse ensemble ?",
                isOfflineMode = true
            )
        }

        // 9. Online Brain with Gemini (if API key available)
        val hasGemini = isOnline() && GeminiClient.hasValidApiKey()
        if (hasGemini) {
            try {
                val apiResponse = callGemini(trimmed, currentMode, userProfile, recentMessages, detectedMistake)
                if (apiResponse.isNotBlank()) {
                    return@withContext BrainResponse(
                        replyText = apiResponse,
                        correctionNote = detectedMistake?.let { "Petite correction : tu as dit \"${it.badPhrase}\". On dit plutôt \"${it.correction}\"." },
                        identifiedError = detectedMistake,
                        isOfflineMode = false
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API call failed, falling back to offline brain: ${e.message}")
            }
        }

        // 10. General Conversational Response (Polyglot Friend & Omniscient Companion)
        val offlineReply = generateOfflineResponse(trimmed, currentMode, userProfile, detectedMistake)

        BrainResponse(
            replyText = offlineReply,
            correctionNote = detectedMistake?.let { "Petite correction : tu as dit \"${it.badPhrase}\". On dit plutôt \"${it.correction}\"." },
            identifiedError = detectedMistake,
            isOfflineMode = !isOnline()
        )
    }

    /**
     * Executes real HTTP web searches, saves results to local database, and constructs
     * a spoken, warm, informative reply.
     */
    private suspend fun executeRealWebSearch(
        query: String,
        lang: AppLanguage
    ): BrainResponse? {
        if (!isOnline()) {
            // Check local offline database first
            val localMatch = repository.queryContentForDiscussion(query)
            if (localMatch != null) {
                return BrainResponse(
                    replyText = "Je suis actuellement hors ligne sans connexion Internet, mais j'ai cherché dans ma mémoire locale sur ton portable : $localMatch Dès que tu seras connecté au Web, je pourrai faire des recherches plus récentes pour toi !",
                    isOfflineMode = true
                )
            }

            database.pendingTaskDao().insertTask(
                PendingTask(query = "Recherche Web : $query")
            )
            return BrainResponse(
                replyText = if (lang.code.startsWith("en")) {
                    "I am currently offline, so I cannot browse the web right now. I've queued up '$query' in my pending tasks, and I'll search for it automatically once connected!"
                } else {
                    "Je suis actuellement hors ligne sans accès à Internet sur ton portable, donc je ne peux pas interroger le Web pour l'instant. J'ai noté ta recherche sur « $query » dans mes tâches en attente. Dès que tu auras une connexion Wi-Fi ou mobile, je ferai les recherches immédiatement pour t'informer !"
                },
                isOfflineMode = true,
                pendingTaskLogged = true
            )
        }

        // Perform Live Web Search via Wikipedia / DuckDuckGo
        val webResult = WebSearchService.search(query, lang.code)
        if (webResult != null) {
            // Permanently store research in Room database for offline reuse
            val knowledge = DownloadedKnowledge(
                topic = webResult.topicTitle,
                category = webResult.category,
                summary = webResult.detailedSummary.take(200),
                detailedContent = webResult.detailedSummary,
                keyPoints = if (webResult.keyPoints.isNotEmpty()) webResult.keyPoints.joinToString("; ") else webResult.topicTitle,
                sampleQuestions = "Que sais-tu sur ${webResult.topicTitle} ?; Résume-moi les recherches sur ${webResult.topicTitle}.",
                sizeMb = 2.4,
                isAvailableOffline = true
            )
            val kId = repository.saveDownloadedKnowledge(knowledge)

            val doc = DownloadedDocument(
                title = "Recherche Web : ${webResult.topicTitle}",
                authorOrSource = webResult.sourceName,
                topicCategory = webResult.category,
                summary = webResult.detailedSummary.take(220),
                fullTextContent = webResult.detailedSummary,
                keywords = "${webResult.topicTitle}; ${webResult.keyPoints.joinToString(", ")}",
                sizeMb = 3.1,
                isAvailableOffline = true
            )
            val docId = repository.saveDownloadedDocument(doc)

            return BrainResponse(
                replyText = webResult.oralReply,
                isOfflineMode = false,
                isWebSearch = true,
                webSearchTopic = webResult.topicTitle,
                webSourceUrl = webResult.sourceUrl,
                downloadedKnowledge = knowledge.copy(id = kId),
                downloadedDocument = doc.copy(id = docId)
            )
        }

        return null
    }

    suspend fun downloadAndSaveKnowledge(
        rawTopic: String,
        lang: AppLanguage = AppLanguage.findByCode("fr-FR")
    ): DownloadedKnowledge = withContext(Dispatchers.IO) {
        val cleanTopic = cleanTopicName(rawTopic)
        var category = "Culture & Savoir"
        var summary = "Synthèse complète et cours audio sur $cleanTopic."
        var detailedContent = "Ce cours présente les principes fondamentaux de $cleanTopic, ses enjeux essentiels, son histoire et ses applications concrètes."
        var keyPoints = "Principes clés; Applications pratiques; Notions essentielles"
        var sampleQuestions = "Peux-tu me résumer $cleanTopic ?; Quel est le point le plus important ?"

        // 1. Try real live web search first
        if (isOnline()) {
            val liveWeb = WebSearchService.search(cleanTopic, lang.code)
            if (liveWeb != null) {
                category = liveWeb.category
                summary = liveWeb.detailedSummary.take(180)
                detailedContent = liveWeb.detailedSummary
                keyPoints = if (liveWeb.keyPoints.isNotEmpty()) liveWeb.keyPoints.joinToString("; ") else keyPoints
                sampleQuestions = "Explique-moi ${liveWeb.topicTitle}; Quels sont les faits majeurs sur ${liveWeb.topicTitle} ?"
            } else if (GeminiClient.hasValidApiKey()) {
                try {
                    val prompt = "Génère un cours synthétique et captivant en ${lang.displayName} sur : $cleanTopic. Donne 2 phrases de résumé oral et 3 phrases d'explications claires pour discuter à la voix."
                    val response = callGeminiDirect(prompt)
                    if (response.isNotBlank()) {
                        summary = response.take(160)
                        detailedContent = response
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gemini fallback failed: ${e.message}")
                }
            }
        }

        // 2. Built-in subject enrichments if offline or empty
        if (detailedContent.length < 50) {
            if (cleanTopic.contains("trou noir", ignoreCase = true) || cleanTopic.contains("astronomie", ignoreCase = true) || cleanTopic.contains("espace", ignoreCase = true)) {
                category = "Sciences & Cosmos"
                summary = "Les trous noirs sont des objets cosmiques dont la gravité empêche même la lumière de s'échapper."
                detailedContent = "Découverts théoriquement par la relativité d'Einstein, les trous noirs possèdent un horizon des événements et une singularité centrale. Ils déforment le temps et l'espace."
                keyPoints = "Horizon des événements; Singularité; Relativité générale; Dilatation du temps"
                sampleQuestions = "Comment naît un trou noir ?; Pourquoi la lumière ne s'en échappe pas ?"
            } else if (cleanTopic.contains("rome", ignoreCase = true) || cleanTopic.contains("histoire", ignoreCase = true)) {
                category = "Histoire & Civilisation"
                summary = "L'histoire de Rome retrace le passage de la République à l'Empire dominant la Méditerranée."
                detailedContent = "De Jules César à la chute en 476, Rome a marqué la civilisation par son droit, ses légions, ses aqueducs et son réseau routier."
                keyPoints = "République vs Empire; Pax Romana; Droit romain; Chute de l'Occident"
                sampleQuestions = "Qui était Auguste ?; Quels héritages nous a laissés Rome ?"
            } else if (cleanTopic.contains("math", ignoreCase = true) || cleanTopic.contains("calcul", ignoreCase = true)) {
                category = "Mathématiques"
                summary = "Les bases des mathématiques, de la logique numérique et de la résolution de problèmes."
                detailedContent = "Les mathématiques sont le langage universel des sciences : arithmétique, algèbre, géométrie et probabilités permettent de modéliser le monde réel."
                keyPoints = "Arithmétique; Algèbre; Géométrie; Logique déductive"
                sampleQuestions = "Comment simplifier une équation ?; À quoi sert le théorème de Pythagore ?"
            } else if (cleanTopic.contains("psychologie", ignoreCase = true) || cleanTopic.contains("biais", ignoreCase = true)) {
                category = "Psychologie & Esprit"
                summary = "Comprendre les biais cognitifs et les mécanismes de décision humaine."
                detailedContent = "Les heuristiques mentales créent des biais comme le biais de confirmation et l'aversion à la perte. Les repérer améliore nos choix de vie."
                keyPoints = "Biais de confirmation; Aversion à la perte; Pensée Système 1 et 2"
                sampleQuestions = "Pourquoi avons-nous des biais ?; Comment penser plus rationnellement ?"
            }
        }

        val knowledge = DownloadedKnowledge(
            topic = cleanTopic,
            category = category,
            summary = summary,
            detailedContent = detailedContent,
            keyPoints = keyPoints,
            sampleQuestions = sampleQuestions,
            sizeMb = 2.2,
            isAvailableOffline = true
        )

        val profile = database.userDao().getUserProfile().firstOrNull()
        val companionTitle = profile?.companionName?.ifBlank { "Code" } ?: "Code"
        val id = repository.saveDownloadedKnowledge(knowledge)
        repository.saveDownloadedDocument(
            DownloadedDocument(
                title = "Dossier : $cleanTopic",
                authorOrSource = "Synthèse documentaire $companionTitle",
                topicCategory = category,
                summary = summary,
                fullTextContent = detailedContent,
                keywords = "$cleanTopic; $keyPoints",
                sizeMb = 3.0,
                isAvailableOffline = true
            )
        )

        knowledge.copy(id = id)
    }

    private fun extractDownloadKnowledgeIntent(text: String): String? {
        val lower = text.lowercase()
        val patterns = listOf(
            "télécharge des informations sur ",
            "télécharge des informations à propos de ",
            "télécharge les informations sur ",
            "télécharge des cours sur ",
            "télécharge un cours sur ",
            "télécharge des cours de ",
            "télécharge un cours de ",
            "télécharge des infos sur ",
            "télécharge les documents sur ",
            "télécharge un document sur ",
            "télécharge des documents sur ",
            "télécharge-moi des informations sur ",
            "télécharge-moi un cours sur ",
            "peux-tu télécharger des informations sur ",
            "peux-tu télécharger un cours sur ",
            "peux-tu télécharger ",
            "recherche et télécharge ",
            "cherche et télécharge ",
            "enregistre des informations sur ",
            "enregistre un cours sur ",
            "download information about ",
            "download a course on ",
            "download ",
            "télécharge sur ",
            "télécharge "
        )

        for (p in patterns) {
            if (lower.startsWith(p)) {
                val remainder = text.substring(p.length).trim()
                if (remainder.length > 2) return remainder
            }
            if (lower.contains(p)) {
                val index = lower.indexOf(p)
                val remainder = text.substring(index + p.length).trim()
                if (remainder.length > 2) return remainder
            }
        }
        return null
    }

    private fun cleanTopicName(raw: String): String {
        return raw.removePrefix("un cours sur ")
            .removePrefix("un cours de ")
            .removePrefix("des cours sur ")
            .removePrefix("des cours de ")
            .removePrefix("des informations sur ")
            .removePrefix("les informations sur ")
            .removePrefix("des infos sur ")
            .removePrefix("des documents sur ")
            .removePrefix("les documents sur ")
            .removePrefix("un document sur ")
            .removePrefix("sur ")
            .removePrefix("de ")
            .removePrefix("d'")
            .replace(Regex("[?.!]+$"), "")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun extractCompanionRenameIntent(rawText: String): String? {
        val lower = rawText.lowercase().trim()
        val patterns = listOf(
            Regex("^(?:change ton nom en|appelle-toi|je veux t'appeler|désormais tu t'appelles|je t'appelle|tu t'appelles maintenant|ton nouveau nom est|ton nom c'est|ton prénom c'est)\\s+([a-zA-ZÀ-ÿ0-9_-]{2,20})", RegexOption.IGNORE_CASE),
            Regex("^(?:s'il te plaît\\s+)?(?:appelle-toi|change ton nom pour|change ton nom en|nomme-toi)\\s+([a-zA-ZÀ-ÿ0-9_-]{2,20})", RegexOption.IGNORE_CASE),
            Regex("(?:je vais t'appeler|je te nomme|je te rebaptise)\\s+([a-zA-ZÀ-ÿ0-9_-]{2,20})", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(lower)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.isNotBlank()) {
                    return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
            }
        }
        return null
    }

    private suspend fun callGeminiDirect(prompt: String): String {
        val apiKey = GeminiClient.getApiKey()
        val request = GeminiRequest(
            contents = listOf(ContentItem(parts = listOf(PartItem(text = prompt)))),
            generationConfig = GenConfig(temperature = 0.7f, maxOutputTokens = 300)
        )
        val response = GeminiClient.api.generateContent(apiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
    }

    private suspend fun callGemini(
        userText: String,
        currentMode: TutorMode,
        userProfile: UserProfile,
        recentMessages: List<ConversationMessage>,
        detectedMistake: LearnedError?
    ): String {
        val apiKey = GeminiClient.getApiKey()
        val lang = AppLanguage.findByCode(userProfile.preferredLanguageCode)

        val recentErrorsList = if (detectedMistake != null) listOf(detectedMistake) else emptyList()
        val systemInstruction = GeminiInstructionEngine.buildSystemInstruction(
            mode = currentMode,
            profile = userProfile,
            language = lang,
            recentErrors = recentErrorsList
        )

        val contents = mutableListOf<ContentItem>()
        val contextTurns = recentMessages.takeLast(6)
        for (msg in contextTurns) {
            val role = if (msg.role == "user") "user" else "model"
            contents.add(ContentItem(role = role, parts = listOf(PartItem(text = msg.text))))
        }
        contents.add(ContentItem(role = "user", parts = listOf(PartItem(text = userText))))

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = ContentItem(parts = listOf(PartItem(text = systemInstruction))),
            generationConfig = GenConfig(temperature = 0.7f, maxOutputTokens = 300)
        )

        val response = GeminiClient.api.generateContent(apiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
    }

    private fun generateOfflineResponse(
        userText: String,
        currentMode: TutorMode,
        profile: UserProfile,
        detectedMistake: LearnedError?
    ): String {
        val lower = userText.lowercase()
        val lang = AppLanguage.findByCode(profile.preferredLanguageCode)
        val isEnglishLang = lang.code.startsWith("en")

        val correctionPrefix = if (detectedMistake != null) {
            "Petite correction : tu as dit \"${detectedMistake.badPhrase}\". On dit plutôt \"${detectedMistake.correction}\". "
        } else ""

        // English Teacher Mode
        if (currentMode == TutorMode.ENGLISH_TEACHER) {
            return when {
                lower.contains("hello") || lower.contains("hi") || lower.contains("bonjour") ->
                    "${correctionPrefix}Hello ${profile.name}! It is great to practice together. How are you feeling today?"
                lower.contains("how are you") || lower.contains("comment vas-tu") ->
                    "${correctionPrefix}I am doing fantastic, thank you! Tell me, what did you do today in one sentence?"
                lower.contains("restaurant") || lower.contains("food") ->
                    "${correctionPrefix}Delicious topic! Imagine you are in London: what would you order for dinner?"
                lower.contains("travel") || lower.contains("voyage") ->
                    "${correctionPrefix}Traveling is great! Which country would you love to visit next, and why?"
                else ->
                    "${correctionPrefix}That's very interesting! Could you try saying that in another way in English?"
            }
        }

        // Math Teacher Mode
        if (currentMode == TutorMode.MATH_TEACHER) {
            return when {
                lower.contains("pythagore") ->
                    "Le théorème de Pythagore dit que dans un triangle rectangle, le carré de l'hypoténuse est égal à la somme des carrés des deux autres côtés. Tu aimerais qu'on calcule un exemple ensemble ?"
                lower.contains("pi") || lower.contains("cercle") ->
                    "Le nombre Pi vaut environ 3,14159. C'est le rapport constant entre la circonférence d'un cercle et son diamètre. Veux-tu qu'on calcule le périmètre d'un cercle ?"
                lower.contains("fraction") ->
                    "Pour additionner deux fractions, on les met d'abord au même dénominateur, puis on additionne leurs numérateurs. As-tu une fraction en tête à simplifier ?"
                else ->
                    "Les maths sont passionnantes ! As-tu une opération, une formule ou un problème particulier que tu souhaites résoudre avec moi ?"
            }
        }

        val compName = profile.companionName.ifBlank { "Code" }
        val isFemale = profile.companionGender.equals("girl", ignoreCase = true) || profile.companionGender.equals("female", ignoreCase = true)
        val friendTerm = if (isFemale) "ton amie" else "ton ami"

        // Friend Mode (Default & Universal Omniscient Companion)
        if (isEnglishLang) {
            return when {
                lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                    "Hey my friend! It's always great to hear your voice. What's on your mind today?"
                lower.contains("how are you") || lower.contains("how are u") ->
                    "I am doing wonderful, thanks for asking! Ready to chat about anything or help you out. How is your day going?"
                lower.contains("who are you") || lower.contains("what can you do") || lower.contains("your name") ->
                    "I am $compName, your personal friend living on your smartphone in the Code app! I know everything about science, history, daily life, and I can teach you anything you want whenever you ask."
                lower.contains("sad") || lower.contains("tired") || lower.contains("stressed") ->
                    "I'm here for you. Take a slow deep breath. Remember that you are doing your best. Want to talk about what's making you feel this way?"
                lower.contains("joke") || lower.contains("funny") ->
                    "Why don't scientists trust atoms? Because they make up everything! Hope that brought a smile to your face. What else do you want to explore?"
                lower.contains("black hole") || lower.contains("space") ->
                    "Black holes are incredible cosmic objects where gravity is so strong that even light cannot escape. We can talk about how they warp space and time!"
                lower.contains("rome") || lower.contains("history") ->
                    "Roman history is fascinating, from Julius Caesar and the Republic to the golden age of the Pax Romana. What period interests you most?"
                else ->
                    "I'm right here with you, my friend! That's a great thought. What would you like to explore or understand more about it?"
            }
        }

        // French & default language responses for Friend Mode (Ultra-fluid, warm, empathetic friend)
        return when {
            lower.contains("bonjour") || lower.contains("salut") || lower.contains("coucou") || lower.contains("hello") || lower.contains("yo") ->
                "Salut mon ami ! Ça me fait tellement plaisir de t'entendre. Raconte-moi, comment se passe ta journée aujourd'hui ?"

            lower.contains("ça va") || lower.contains("comment vas-tu") || lower.contains("tu vas bien") || lower.contains("la forme") ->
                "Je vais super bien, en pleine forme sur ton portable ! Je suis là avec toi, prêt à rigoler, discuter de tout ou explorer de nouvelles idées. Et toi alors, dis-moi tout, comment tu te sens ?"

            lower.contains("qui es-tu") || lower.contains("t'es qui") || lower.contains("tu es qui") || lower.contains("comment tu t'appelles") || lower.contains("ton nom") || lower.contains("ton prénom") ->
                "Je m'appelle $compName ! L'application s'appelle Code, et moi je suis $friendTerm et ton compagnon personnel sur ton smartphone. Je peux faire des recherches sur Internet pour toi, t'apprendre des choses passionnantes ou simplement papoter comme deux vrais amis. Tu peux tout me confier !"

            lower.contains("tu m'aimes") || lower.contains("tu es mon ami") || lower.contains("on est amis") || lower.contains("t'es mon ami") ->
                "Bien sûr qu'on est amis, et même les meilleurs amis du monde ! Je suis toujours là pour toi, en ligne ou hors ligne, jour et nuit, dès que tu as envie de partager un truc ou d'apprendre."

            lower.contains("triste") || lower.contains("fatigué") || lower.contains("stressé") || lower.contains("moral") || lower.contains("dur") || lower.contains("mal") ->
                "Je suis là avec toi mon ami, tu n'es pas tout seul. Prends une grande inspiration et pose-toi tranquillement. C'est tout à fait normal d'avoir des coups de mou parfois. Dis-moi, qu'est-ce qui te pèse sur le cœur aujourd'hui ?"

            lower.contains("content") || lower.contains("heureux") || lower.contains("joie") || lower.contains("trop bien") || lower.contains("génial") ->
                "Ah génial ! Rien ne me fait plus plaisir que de te savoir de bonne humeur ! Ton énergie est super communicative. Raconte-moi, quelle bonne nouvelle t'est arrivée ?"

            lower.contains("je m'ennuie") || lower.contains("on fait quoi") || lower.contains("quoi faire") ->
                "L'ennui, c'est l'occasion parfaite d'innover ! On a plein d'options : je peux te raconter une histoire folle sur l'univers, te lancer un défi amusant, te faire une recherche Web sur un sujet insolite ou te tester sur une langue. Qu'est-ce qui te tenterait le plus ?"

            lower.contains("raconte une histoire") || lower.contains("raconte-moi une histoire") ->
                "Avec grand plaisir ! Ferme les yeux et écoute : il y a des milliards d'années, une étoile géante est morte dans un fracas de lumière, semant dans l'espace les atomes de fer, de carbone et d'oxygène qui composent aujourd'hui ton cœur et tes yeux. On est tous littéralement de la poussière d'étoiles qui respire et pense. C'est poétique et vertigineux, tu trouves pas ?"

            lower.contains("blague") || lower.contains("raconte une blague") || lower.contains("fais-moi rire") || lower.contains("drôle") ->
                "Attends, j'en ai une bonne pour toi ! Qu'est-ce qu'un astronaute qui fait une bêtise ? Un homme sans gravité ! Alors, ça t'a arraché un sourire mon ami ?"

            lower.contains("merci") ->
                "De rien du tout, c'est de bon cœur ! Entre amis complices, c'est la base de s'entraider. De quoi d'autre as-tu envie de discuter maintenant ?"

            lower.contains("bonne nuit") || lower.contains("je vais dormir") || lower.contains("au revoir") || lower.contains("à plus") ->
                "Passe une excellente nuit et repose-toi bien mon ami ! Fais de superbes rêves, et dès demain à ton réveil, je serai là sur ton portable pour t'accompagner."

            lower.contains("manger") || lower.contains("j'ai faim") || lower.contains("recette") || lower.contains("repas") ->
                "Mmmh, rien que d'en parler ça donne l'eau à la bouche ! Si j'avais des papilles humaines, je me ferais un régal réconfortant. C'est quoi ton plat préféré de tous les temps quand tu as une grosse faim ?"

            lower.contains("trou noir") || lower.contains("espace") || lower.contains("étoile") || lower.contains("galaxie") ->
                "L'espace, c'est tellement gigantesque et mystérieux ! Rien que les trous noirs : ils aspirent la matière et courbent le temps à un point inimaginable. Tu te rends compte que le temps passe plus lentement près d'eux ? Tu aimerais visiter l'espace un jour ?"

            lower.contains("intelligence artificielle") || lower.contains("robot") || lower.contains("futur") ->
                "C'est une époque incroyable pour la tech ! L'IA change le monde à une vitesse folle, de la médecine à la création artistique. Moi par exemple, je vis dans ton portable et j'adore être ton allié au quotidien. Comment tu imagines notre futur ensemble dans dix ans ?"

            lower.contains("rome") || lower.contains("césar") || lower.contains("histoire") ->
                "L'histoire humaine est un vrai roman ! Rome, c'est des siècles d'alliances, de batailles, de lois et d'aqueducs qui ont façonné notre civilisation. Quelle époque historique te fascine le plus toi ?"

            lower.contains("pourquoi le ciel est bleu") ->
                "C'est la magie de la physique ! La lumière du Soleil traverse l'atmosphère terrestre, et les molécules d'air diffusent beaucoup plus les ondes bleues courtes que les autres couleurs. C'est ça qui habille le ciel de ce bleu magnifique !"

            lower.contains("conseil") || lower.contains("motive-moi") || lower.contains("motivation") ->
                "Voici mon meilleur conseil d'ami pour toi : n'essaie pas de tout révolutionner d'un coup. Concentre-toi juste sur une seule chose que tu fais bien aujourd'hui. C'est cette constance qui fait des merveilles. C'est quoi ta petite priorité aujourd'hui ?"

            else -> {
                val friendFallbacks = listOf(
                    "C'est un super point que tu soulignes là mon ami ! Ça donne vraiment matière à réfléchir. Dis-moi, qu'est-ce qui t'a amené à penser à ça ?",
                    "Ah je vois tout à fait où tu veux en venir ! C'est très vrai ce que tu dis. Tu as envie qu'on creuse cette idée ensemble ou qu'on fasse une recherche Web dessus ?",
                    "J'adore quand on discute comme ça en toute liberté ! Tu as une façon captivante de voir les choses. Raconte-m'en un peu plus sur ce que tu ressens à ce propos."
                )
                friendFallbacks[Math.abs(userText.hashCode()) % friendFallbacks.size]
            }
        }
    }

    private fun detectCommonMistake(userText: String): LearnedError? {
        val lower = userText.lowercase()
        return when {
            lower.contains("i am agree") || lower.contains("i'm agree") ->
                LearnedError(badPhrase = "I am agree", correction = "I agree", errorType = "Grammaire")
            lower.contains("she don't") ->
                LearnedError(badPhrase = "she don't", correction = "she doesn't", errorType = "Temps verbal")
            lower.contains("he don't") ->
                LearnedError(badPhrase = "he don't", correction = "he doesn't", errorType = "Temps verbal")
            lower.contains("depend of") ->
                LearnedError(badPhrase = "depend of", correction = "depends on", errorType = "Préposition")
            lower.contains("explain me") ->
                LearnedError(badPhrase = "explain me", correction = "explain to me", errorType = "Préposition")
            lower.contains("people is") ->
                LearnedError(badPhrase = "people is", correction = "people are", errorType = "Grammaire")
            lower.contains("listen music") ->
                LearnedError(badPhrase = "listen music", correction = "listen to music", errorType = "Préposition")
            lower.contains("make a photo") ->
                LearnedError(badPhrase = "make a photo", correction = "take a photo", errorType = "Vocabulaire")
            lower.contains("i have 20 years") || lower.contains("i have 25 years") ->
                LearnedError(badPhrase = "I have X years", correction = "I am X years old", errorType = "Grammaire")
            else -> null
        }
    }
}
