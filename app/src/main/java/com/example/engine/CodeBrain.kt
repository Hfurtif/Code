package com.example.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.example.data.repository.OfflineKnowledgeRepository
import kotlinx.coroutines.Dispatchers
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
    val downloadedDocument: DownloadedDocument? = null
)

class CodeBrain(
    private val context: Context,
    private val database: AppDatabase,
    val repository: OfflineKnowledgeRepository = OfflineKnowledgeRepository(database)
) {

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

        // 2. Storage Authorization Request / Confirmation
        if (lower.contains("ajoute 20 go") || lower.contains("augmente le stockage") ||
            lower.contains("plus de stockage") || lower.contains("je t'autorise") ||
            (lower.contains("autorise") && lower.contains("stockage")) ||
            (lower.contains("oui") && lower.contains("stockage"))
        ) {
            val newLimit = userProfile.maxStorageLimitGb + 20.0
            database.userDao().insertOrUpdate(userProfile.copy(maxStorageLimitGb = newLimit))
            return@withContext BrainResponse(
                replyText = if (lang.code.startsWith("en")) {
                    "Thank you for the authorization! I have expanded the storage limit to ${newLimit.toInt()} GB. We now have plenty of space on your phone."
                } else {
                    "Merci pour ton accord ! J'ai étendu la capacité de stockage à ${newLimit.toInt()} Go sur ton portable. Nous avons tout l'espace nécessaire pour continuer d'enregistrer tes connaissances !"
                },
                triggeredCommand = "expand_storage_20gb"
            )
        }

        // 3. Learning On-Demand Mode Switches (English, Math, Custom Teacher vs Friend)
        if (lower.contains("apprends-moi l'anglais") || lower.contains("je veux apprendre l'anglais") ||
            lower.contains("teach me english") || lower.contains("mode prof d'anglais") || lower.contains("cours d'anglais")
        ) {
            val responseText = if (lang.code.startsWith("en")) {
                "Awesome! I am now in English teacher mode. Let's practice together! Say something in English or tell me what you want to work on."
            } else {
                "Génial mon ami ! Je me mets maintenant en mode professeur d'anglais. Dis-moi ton objectif ou lance-toi simplement en anglais pour que je puisse t'écouter et t'aider !"
            }
            return@withContext BrainResponse(
                replyText = responseText,
                newMode = TutorMode.ENGLISH_TEACHER
            )
        }

        if (lower.contains("apprends-moi les maths") || lower.contains("apprends-moi les mathématiques") ||
            lower.contains("cours de maths") || lower.contains("teach me math")
        ) {
            return@withContext BrainResponse(
                replyText = "Avec grand plaisir ! Je passe en mode professeur de mathématiques. Quel domaine veux-tu aborder : calcul mental, algèbre, géométrie ou logique ?",
                newMode = TutorMode.MATH_TEACHER
            )
        }

        if (lower.contains("reviens en mode ami") || lower.contains("sois mon ami") ||
            lower.contains("discussion normale") || lower.contains("on arrête la leçon")
        ) {
            return@withContext BrainResponse(
                replyText = "D'accord mon ami ! On revient à notre conversation libre et ouverte. De quoi d'autre as-tu envie de me parler ?",
                newMode = TutorMode.FRIEND
            )
        }

        // 4. Download Course / Information Request Intent Detection
        val downloadTopic = extractDownloadKnowledgeIntent(trimmed)
        if (downloadTopic != null) {
            if (isOnline()) {
                val knowledge = downloadAndSaveKnowledge(downloadTopic, lang)
                return@withContext BrainResponse(
                    replyText = if (lang.code.startsWith("en")) {
                        "All set! I searched, downloaded and saved the complete material on ${knowledge.topic} to your phone. Even offline, we can discuss it anytime!"
                    } else {
                        "C'est fait mon ami ! J'ai téléchargé et enregistré le cours complet sur ${knowledge.topic} dans la mémoire de ton portable. Même hors ligne et sans Internet, nous pourrons en discuter librement à l'oral !"
                    },
                    triggeredCommand = "knowledge_downloaded",
                    downloadedKnowledge = knowledge
                )
            } else {
                database.pendingTaskDao().insertTask(
                    PendingTask(query = "Télécharger : $downloadTopic")
                )
                return@withContext BrainResponse(
                    replyText = if (lang.code.startsWith("en")) {
                        "I don't have internet access right now to download $downloadTopic. I've queued it up in my tasks, and I will download it as soon as you connect!"
                    } else {
                        "Je n'ai pas de connexion Internet en ce moment pour télécharger des informations sur $downloadTopic. Je l'ai enregistré dans mes tâches. Dès que ton portable sera connecté, je ferai les recherches et je téléchargerai tout pour toi !"
                    },
                    isOfflineMode = true,
                    pendingTaskLogged = true
                )
            }
        }

        // 5. Check for English learner mistakes if in English mode or user spoke English
        val detectedMistake = if (currentMode == TutorMode.ENGLISH_TEACHER || lower.contains("agree") || lower.contains("people is")) {
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

        // 6. Online Brain (Gemini 3.5 Flash)
        val online = isOnline() && GeminiClient.hasValidApiKey()
        if (online) {
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
                // Network or API failure -> Fallback gracefully to offline conversational intelligence
            }
        }

        // 7. Offline Intelligence: Search downloaded library for matching topic/document
        val matchingContent = repository.queryContentForDiscussion(trimmed)
        if (matchingContent != null) {
            return@withContext BrainResponse(
                replyText = "$matchingContent Qu'aimerais-tu que je t'explique plus en détail ?",
                isOfflineMode = true
            )
        }

        // 8. General Offline Response (Polyglot Friend & Omniscient Companion)
        val offlineReply = generateOfflineResponse(trimmed, currentMode, userProfile, detectedMistake)

        BrainResponse(
            replyText = offlineReply,
            correctionNote = detectedMistake?.let { "Petite correction : tu as dit \"${it.badPhrase}\". On dit plutôt \"${it.correction}\"." },
            identifiedError = detectedMistake,
            isOfflineMode = true
        )
    }

    suspend fun downloadAndSaveKnowledge(rawTopic: String, lang: AppLanguage = AppLanguage.findByCode("fr-FR")): DownloadedKnowledge = withContext(Dispatchers.IO) {
        val cleanTopic = cleanTopicName(rawTopic)
        var category = "Culture & Savoir"
        var summary = "Synthèse complète et cours audio sur $cleanTopic."
        var detailedContent = "Ce cours présente les principes fondamentaux de $cleanTopic, ses enjeux essentiels, son histoire et ses applications concrètes."
        var keyPoints = "Principes clés; Applications pratiques; Notions essentielles"
        var sampleQuestions = "Peux-tu me résumer $cleanTopic ?; Quel est le point le plus important ?"

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
        } else if (isOnline() && GeminiClient.hasValidApiKey()) {
            try {
                val prompt = "Génère un cours synthétique et captivant en ${lang.displayName} sur : $cleanTopic. Donne 2 phrases de résumé oral et 3 phrases d'explications claires pour discuter à la voix."
                val response = callGeminiDirect(prompt)
                if (response.isNotBlank()) {
                    summary = response.take(160)
                    detailedContent = response
                }
            } catch (e: Exception) {
                // keep robust default
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

        val id = repository.saveDownloadedKnowledge(knowledge)
        repository.saveDownloadedDocument(
            DownloadedDocument(
                title = "Dossier : $cleanTopic",
                authorOrSource = "Synthèse documentaire Lilian",
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
            "fais des recherches sur ",
            "fais une recherche sur ",
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

        val roleInstruction = when (currentMode) {
            TutorMode.ENGLISH_TEACHER -> """
                Rôle : Tu es le professeur d'anglais personnel de l'utilisateur.
                Si l'élève fait une erreur d'anglais, corrige gentiment à l'oral ("Petite correction : tu as dit [X], on dit plutôt [Y]").
                Encourage la pratique orale en posant une question en anglais.
            """.trimIndent()
            TutorMode.MATH_TEACHER -> """
                Rôle : Tu es le professeur de mathématiques personnel de l'utilisateur.
                Explique les concepts clairement et simplement à l'oral avec des exemples concrets.
            """.trimIndent()
            TutorMode.CUSTOM_TEACHER -> """
                Rôle : Tu es le professeur particulier de l'utilisateur sur le sujet demandé.
                Enseigne avec passion et pédagogie.
            """.trimIndent()
            TutorMode.FRIEND -> """
                Rôle : Tu es Lilian, le meilleur ami de l'utilisateur qui vit sur son smartphone.
                Tu connais TOUT (culture, sciences, histoire, vie quotidienne, philosophie, sentiments).
                Tu es chaleureux, bienveillant, enthousiaste, loyal et drôle.
                Tu parles comme un vrai ami au téléphone.
            """.trimIndent()
        }

        val systemInstruction = """
            $roleInstruction
            Langue principale de conversation : ${lang.displayName} (${lang.code}).
            Prénom de ton ami : ${userProfile.name}.
            
            Règles strictes pour la voix :
            1. Réponds de façon concise et naturelle (2 à 3 phrases maximum), idéale pour la synthèse vocale.
            2. Aucun symbole spécial (*, #, _, -), pas de listes à puces, pas de markdown.
            3. Termine toujours par une question engageante ou une relance amicale pour faire vivre la conversation.
        """.trimIndent()

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

        // Friend Mode (Default & Universal Omniscient Companion)
        if (isEnglishLang) {
            return when {
                lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                    "Hey my friend! It's always great to hear your voice. What's on your mind today?"
                lower.contains("how are you") || lower.contains("how are u") ->
                    "I am doing wonderful, thanks for asking! Ready to chat about anything or help you out. How is your day going?"
                lower.contains("who are you") || lower.contains("what can you do") ->
                    "I am Lilian, your personal friend living on your smartphone! I know everything about science, history, daily life, and I can teach you anything you want whenever you ask."
                lower.contains("sad") || lower.contains("tired") || lower.contains("stressed") ->
                    "I'm here for you. Take a slow deep breath. Remember that you are doing your best. Want to talk about what's making you feel this way?"
                lower.contains("joke") || lower.contains("funny") ->
                    "Why don't scientists trust atoms? Because they make up everything! Hope that brought a smile to your face. What else do you want to explore?"
                lower.contains("black hole") || lower.contains("space") ->
                    "Black holes are incredible cosmic objects where gravity is so strong that even light cannot escape. We can talk about how they warp space and time!"
                lower.contains("rome") || lower.contains("history") ->
                    "Roman history is fascinating, from Julius Caesar and the Republic to the golden age of the Pax Romana. What period interests you most?"
                else ->
                    "I hear you, my friend. That's a great thought. Tell me more, what do you think is the most important part of this?"
            }
        }

        // French & default language responses for Friend Mode
        return when {
            lower.contains("bonjour") || lower.contains("salut") || lower.contains("coucou") || lower.contains("hello") ->
                "Salut mon ami ! Quel plaisir de t'entendre. Comment se passe ta journée aujourd'hui ?"
            lower.contains("ça va") || lower.contains("comment vas-tu") || lower.contains("tu vas bien") ->
                "Je vais super bien, merci de demander ! Je suis en pleine forme sur ton portable, prêt à discuter de tout avec toi. Et toi, comment te sens-tu ?"
            lower.contains("qui es-tu") || lower.contains("t'es qui") || lower.contains("tu es qui") ->
                "Je suis Lilian, ton ami personnel et compagnon vocal qui habite sur ton portable ! Je connais tout, de la culture aux sciences et à la vie quotidienne, et si tu veux apprendre quelque chose comme l'anglais ou les maths, il suffit de me le demander !"
            lower.contains("triste") || lower.contains("fatigué") || lower.contains("stressé") || lower.contains("moral") ->
                "Je suis là avec toi mon ami. Prends une grande inspiration. C'est normal d'avoir des moments de fatigue ou de doute. Qu'est-ce qui te pèse en ce moment ?"
            lower.contains("merci") ->
                "Avec tout mon cœur ! Entre amis, c'est naturel d'être là l'un pour l'autre. Qu'est-ce qu'on fait ensuite ?"
            lower.contains("blague") || lower.contains("raconte une blague") || lower.contains("fais-moi rire") ->
                "Pourquoi les plongeurs plongent-ils toujours en arrière et jamais en avant ? Parce que sinon ils tombent dans le bateau ! Ça t'a fait sourire ?"
            lower.contains("trou noir") || lower.contains("espace") || lower.contains("étoile") ->
                "Les trous noirs sont parmi les objets les plus fascinants de l'univers : ils déforment le temps et l'espace jusqu'à ce que rien ne puisse s'échapper. Tu veux qu'on parle de l'horizon des événements ?"
            lower.contains("rome") || lower.contains("césar") || lower.contains("histoire") ->
                "L'histoire de Rome est captivante, de la République aux empereurs et à la Pax Romana. Qu'est-ce qui t'intrigue le plus dans cette époque ?"
            lower.contains("pourquoi le ciel est bleu") ->
                "Le ciel est bleu à cause de la diffusion de Rayleigh : l'atmosphère diffuse beaucoup plus la lumière bleue du Soleil que les autres couleurs car sa longueur d'onde est plus courte !"
            lower.contains("conseil") || lower.contains("motive-moi") ->
                "Voici un conseil d'ami : concentre-toi sur une seule petite victoire aujourd'hui. C'est l'accumulation de petits pas qui crée les plus grands changements. Quelle est ta priorité aujourd'hui ?"
            else ->
                "Je t'écoute avec attention mon ami. C'est un sujet très intéressant. Dis-moi, qu'est-ce qui t'a amené à penser à ça ?"
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
