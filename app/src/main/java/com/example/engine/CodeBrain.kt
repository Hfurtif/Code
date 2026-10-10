package com.example.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.AppDatabase
import com.example.data.model.ConversationMessage
import com.example.data.model.LearnedError
import com.example.data.model.PendingTask
import com.example.data.model.TutorMode
import com.example.data.model.UserProfile
import com.example.data.remote.ContentItem
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiRequest
import com.example.data.remote.GenConfig
import com.example.data.remote.PartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BrainResponse(
    val replyText: String,
    val correctionNote: String? = null,
    val identifiedError: LearnedError? = null,
    val isOfflineMode: Boolean = false,
    val triggeredCommand: String? = null,
    val pendingTaskLogged: Boolean = false
)

class CodeBrain(
    private val context: Context,
    private val database: AppDatabase
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

        // 1. Voice Command Detection
        if (lower == "stop" || lower == "arrête" || lower == "silence") {
            return@withContext BrainResponse(
                replyText = "D'accord, je m'arrête.",
                triggeredCommand = "stop"
            )
        }
        if (lower.contains("plus lentement") || lower.contains("parle plus lentement") || lower.contains("slower")) {
            return@withContext BrainResponse(
                replyText = "Bien compris. Je vais parler plus posément pour que tu puisses bien tout saisir.",
                triggeredCommand = "slower"
            )
        }
        if (lower.contains("plus simple") || lower.contains("explique plus simplement") || lower.contains("simpler")) {
            return@withContext BrainResponse(
                replyText = "Pas de souci. On simplifie ! Dis-moi ce qui te semble difficile et on le reprend ensemble pas à pas.",
                triggeredCommand = "simpler"
            )
        }
        if (lower.contains("change de sujet") || lower.contains("changeons de sujet") || lower.contains("switch topic")) {
            return@withContext BrainResponse(
                replyText = "Avec plaisir ! De quoi as-tu envie de discuter maintenant ? Tu as carte blanche.",
                triggeredCommand = "change_topic"
            )
        }
        if (lower.contains("voix féminine") || lower.contains("passe à une voix féminine")) {
            return@withContext BrainResponse(
                replyText = "Voilà, je passe en voix féminine pour nos conversations.",
                triggeredCommand = "voice_female"
            )
        }
        if (lower.contains("voix masculine") || lower.contains("passe à une voix masculine")) {
            return@withContext BrainResponse(
                replyText = "Très bien, j'adopte une voix masculine dès maintenant.",
                triggeredCommand = "voice_male"
            )
        }
        if (lower.contains("supprime le dictionnaire") || lower.contains("libère de l'espace")) {
            return@withContext BrainResponse(
                replyText = "Je peux libérer l'espace des ressources temporaires. Tu souhaites que je supprime le pack maintenant ?",
                triggeredCommand = "storage_cleanup_prompt"
            )
        }

        // 2. Check for offline common English learner mistakes
        val detectedMistake = detectCommonMistake(trimmed)
        if (detectedMistake != null) {
            val existing = database.learnedErrorDao().findByPhrase(detectedMistake.badPhrase)
            if (existing != null) {
                database.learnedErrorDao().updateError(existing.copy(count = existing.count + 1, timestamp = System.currentTimeMillis()))
            } else {
                database.learnedErrorDao().insertError(detectedMistake)
            }
        }

        // 3. Dual Engine: Online Gemini vs Offline Brain
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
                // Network failure during request - fallback smoothly to offline mode
            }
        }

        // 4. Offline Fallback Brain
        val offlineReply = generateOfflineResponse(trimmed, currentMode, userProfile, detectedMistake)

        // Check if user asked for internet/current news when offline
        var loggedPending = false
        if (isQueryRequiringWeb(trimmed)) {
            database.pendingTaskDao().insertTask(
                PendingTask(query = trimmed)
            )
            loggedPending = true
        }

        BrainResponse(
            replyText = offlineReply,
            correctionNote = detectedMistake?.let { "Petite correction : tu as dit \"${it.badPhrase}\". On dit plutôt \"${it.correction}\"." },
            identifiedError = detectedMistake,
            isOfflineMode = true,
            pendingTaskLogged = loggedPending
        )
    }

    private suspend fun callGemini(
        userText: String,
        currentMode: TutorMode,
        userProfile: UserProfile,
        recentMessages: List<ConversationMessage>,
        detectedMistake: LearnedError?
    ): String {
        val apiKey = GeminiClient.getApiKey()

        val systemInstruction = """
            Tu es Code, un professeur personnel chaleureux, patient et encourageant, qui parle à voix haute.
            Mode actuel : ${currentMode.title}.
            Profil de l'élève : Prénom ${userProfile.name}, Niveau CECRL ${userProfile.level}, Objectif : ${userProfile.objective}.
            Fréquence des corrections : ${userProfile.correctionFrequency}.
            Langue des explications : ${if (userProfile.explanationLang == "fr") "Français pour les corrections et encouragements" else "Simple English"}.
            Règles strictes pour la voix :
            1. Tes réponses doivent être courtes et naturelles (2 à 4 phrases maximum), parfaites pour être prononcées par synthèse vocale.
            2. Jamais de listes à puces, pas de symboles (*, #, _, -), pas de markdown.
            3. Si l'élève parle en anglais et commet une erreur, réponds d'abord naturellement au fond de sa phrase, puis ajoute oralement avec douceur :
            "Petite correction : tu as dit [erreur]. On dit plutôt [bonne tournure]." (maximum 1 correction par réponse).
            4. Pose toujours une seule question engageante à la fois pour maintenir l'échange oral.
        """.trimIndent()

        val contents = mutableListOf<ContentItem>()

        // Add last 6 turns for context
        val contextTurns = recentMessages.takeLast(6)
        for (msg in contextTurns) {
            val role = if (msg.role == "user") "user" else "model"
            contents.add(ContentItem(role = role, parts = listOf(PartItem(text = msg.text))))
        }

        // Add current user message
        contents.add(ContentItem(role = "user", parts = listOf(PartItem(text = userText))))

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = ContentItem(parts = listOf(PartItem(text = systemInstruction))),
            generationConfig = GenConfig(temperature = 0.7f, maxOutputTokens = 300)
        )

        val response = GeminiClient.api.generateContent(apiKey, request)
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        return text.trim()
    }

    private fun generateOfflineResponse(
        userText: String,
        currentMode: TutorMode,
        profile: UserProfile,
        detectedMistake: LearnedError?
    ): String {
        val lower = userText.lowercase()

        if (isQueryRequiringWeb(userText)) {
            return "Je n'ai pas de connexion en ce moment, je ne peux pas vérifier cette information récente. On peut continuer notre leçon d'anglais, et je note ta demande pour quand je serai reconnecté."
        }

        val correctionPrefix = if (detectedMistake != null) {
            "Petite correction : tu as dit \"${detectedMistake.badPhrase}\". On dit plutôt \"${detectedMistake.correction}\". "
        } else ""

        return when (currentMode) {
            TutorMode.ENGLISH -> {
                when {
                    lower.contains("hello") || lower.contains("hi") || lower.contains("bonjour") ->
                        "${correctionPrefix}Hello ${profile.name}! It is great to hear you. How is your day going so far?"
                    lower.contains("how are you") || lower.contains("comment vas-tu") ->
                        "${correctionPrefix}I am doing fantastic, thank you! Ready to practice some English together today?"
                    lower.contains("restaurant") || lower.contains("coffee") || lower.contains("food") ->
                        "${correctionPrefix}Great topic! Imagine you are in London right now. What would you like to order for lunch?"
                    lower.contains("travel") || lower.contains("trip") || lower.contains("voyage") ->
                        "${correctionPrefix}Traveling is such an exciting experience. Where is the next destination you would love to visit?"
                    lower.contains("work") || lower.contains("job") || lower.contains("travail") ->
                        "${correctionPrefix}Work discussions are very useful for your fluency. What is the most exciting project you are working on?"
                    lower.contains("yes") || lower.contains("oui") || lower.contains("sure") ->
                        "${correctionPrefix}Awesome! Tell me, what did you do earlier today?"
                    else ->
                        "${correctionPrefix}I understand what you mean. That is very interesting! Can you tell me a little more about that in one sentence?"
                }
            }
            TutorMode.PERSONAL_GROWTH -> {
                when {
                    lower.contains("habitude") || lower.contains("habit") ->
                        "Les petites habitudes répétées chaque jour font de grandes transformations. Quelle est la petite habitude clé que tu aimerais ancrer cette semaine ?"
                    lower.contains("motivation") || lower.contains("fatigué") ->
                        "La motivation fluctue, mais la clarté reste. Ne cherche pas à tout accomplir d'un coup : quelle est une seule petite victoire possible aujourd'hui ?"
                    else ->
                        "C'est une excellente réflexion. Prendre un instant pour y penser montre ta volonté de progresser. Qu'est-ce qui t'aiderait le plus à passer à l'action ?"
                }
            }
            TutorMode.PSYCHOLOGY -> {
                when {
                    lower.contains("stress") || lower.contains("anxiété") || lower.contains("peur") ->
                        "Le stress est une réaction normale face à une situation importante. Prends une grande inspiration lente. Qu'est-ce qui est sous ton contrôle direct en cet instant ?"
                    lower.contains("conflit") || lower.contains("dispute") || lower.contains("colère") ->
                        "Dans un échange difficile, commencer par écouter sans interrompre apaise souvent les tensions. Comment pourrais-tu exprimer ton ressenti sans accuser l'autre ?"
                    else ->
                        "La communication bienveillante commence par une écoute attentive de soi-même. Qu'est-ce que tu ressens au fond par rapport à cette situation ?"
                }
            }
            TutorMode.FREE_DISCUSSION -> {
                "C'est passionnant comme sujet ! J'aime échanger sur cela avec toi. Quel aspect de cette question t'interpelle le plus ?"
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
            lower.contains("make a picture") || lower.contains("make a photo") ->
                LearnedError(badPhrase = "make a photo", correction = "take a photo", errorType = "Vocabulaire")
            lower.contains("i have 20 years") || lower.contains("i have 25 years") || lower.contains("i have 30 years") ->
                LearnedError(badPhrase = "I have X years", correction = "I am X years old", errorType = "Grammaire")
            lower.contains("since two days") || lower.contains("since 2 days") || lower.contains("since two years") ->
                LearnedError(badPhrase = "since two years", correction = "for two years", errorType = "Temps verbal")
            else -> null
        }
    }

    private fun isQueryRequiringWeb(userText: String): Boolean {
        val lower = userText.lowercase()
        return lower.contains("météo") || lower.contains("actualité") || lower.contains("qui a gagné") ||
                lower.contains("dernière nouvelle") || lower.contains("aujourd'hui à la télé") ||
                lower.contains("score du match") || lower.contains("président actuel")
    }
}
