package com.example.voice

import com.example.data.model.TutorMode
import java.text.Normalizer
import java.util.Locale

/**
 * Result of parsing spoken text for executive voice commands.
 */
sealed class VoiceCommandResult {
    data class ModeChange(val mode: TutorMode, val feedback: String) : VoiceCommandResult()
    data class SearchWeb(val query: String, val feedback: String) : VoiceCommandResult()
    data class ClearHistory(val feedback: String) : VoiceCommandResult()
    data class RepeatReply(val feedback: String) : VoiceCommandResult()
    data class StopSpeaking(val feedback: String) : VoiceCommandResult()
    data class OpenVoiceStudio(val feedback: String) : VoiceCommandResult()
    data class OpenAgenticHarness(val feedback: String) : VoiceCommandResult()
    data class StartQuiz(val feedback: String) : VoiceCommandResult()
    data class CreateFlashcard(val prompt: String, val feedback: String) : VoiceCommandResult()
    data class ToggleContinuousMode(val enable: Boolean, val feedback: String) : VoiceCommandResult()
    data class ToggleAutoSpeech(val enable: Boolean, val feedback: String) : VoiceCommandResult()
    data class AdjustSpeechRate(val delta: Float, val feedback: String) : VoiceCommandResult()
    data class StartHandsFree(val feedback: String) : VoiceCommandResult()
    data class StopHandsFree(val feedback: String) : VoiceCommandResult()
    data class Help(val feedback: String) : VoiceCommandResult()
    data class ConversationPrompt(val prompt: String) : VoiceCommandResult()
}

data class VoiceCommandHelpItem(
    val title: String,
    val examples: List<String>,
    val description: String,
    val category: String
)

/**
 * Intelligent voice command parser that detects user intent from spoken speech-to-text.
 * Recognizes commands in French and English with fuzzy accent stripping.
 */
object VoiceCommandManager {

    private fun normalize(input: String): String {
        val trimmed = input.lowercase(Locale.ROOT).trim()
        val nfd = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
        return nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    val availableCommands: List<VoiceCommandHelpItem> = listOf(
        VoiceCommandHelpItem(
            title = "Contrôle de la voix & silence",
            examples = listOf("\"Stop\"", "\"Silence\"", "\"Tais-toi\"", "\"Pause\""),
            description = "Interrompt instantanément la synthèse vocale en cours.",
            category = "Contrôle"
        ),
        VoiceCommandHelpItem(
            title = "Répéter la réponse",
            examples = listOf("\"Répète\"", "\"Répète ce que tu as dit\"", "\"Repeat please\""),
            description = "Réécoute la dernière réponse vocale de l'assistant.",
            category = "Contrôle"
        ),
        VoiceCommandHelpItem(
            title = "Effacer la discussion",
            examples = listOf("\"Efface la conversation\"", "\"Nouvelle conversation\"", "\"Clear chat\""),
            description = "Réinitialise l'historique et démarre un nouvel échange.",
            category = "Contrôle"
        ),
        VoiceCommandHelpItem(
            title = "Recherche sur le Web",
            examples = listOf("\"Recherche sur internet les actualités IA\"", "\"Cherche sur le web la météo à Paris\"", "\"Google qui a gagné la coupe du monde\""),
            description = "Active le moteur de recherche web temps réel via l'agent.",
            category = "Recherche & Agent"
        ),
        VoiceCommandHelpItem(
            title = "Changer de rôle / mode",
            examples = listOf("\"Mode prof d'anglais\"", "\"Passe en mode maths\"", "\"Mode ami\"", "\"Mode professeur particulier\""),
            description = "Bascule instantanément de tuteur (Anglais, Maths, Spécialiste ou Ami omniscient).",
            category = "Pédagogie"
        ),
        VoiceCommandHelpItem(
            title = "Révision & Quiz",
            examples = listOf("\"Lance un quiz\"", "\"Fais-moi un quiz\"", "\"Révise mes cartes mémoire\""),
            description = "Ouvre l'atelier de flashcards et démarre une session de révision interactive.",
            category = "Pédagogie"
        ),
        VoiceCommandHelpItem(
            title = "Créer une flashcard",
            examples = listOf("\"Crée une flashcard sur la photosynthèse\"", "\"Ajoute une carte mémoire vocabulaire espagnol\""),
            description = "Prépare automatiquement une fiche de révision à partir du sujet dicté.",
            category = "Pédagogie"
        ),
        VoiceCommandHelpItem(
            title = "Studio des voix",
            examples = listOf("\"Change de voix\"", "\"Ouvre le studio de voix\"", "\"Paramètres de voix\""),
            description = "Ouvre l'interface de sélection des voix (Google HD, ElevenLabs, Hume).",
            category = "Configuration"
        ),
        VoiceCommandHelpItem(
            title = "Harnais Agentique",
            examples = listOf("\"Ouvre le harnais\"", "\"Harnais agentique\"", "\"Mode agent autonome\""),
            description = "Affiche la console de supervision des outils autonomes (ReAct).",
            category = "Configuration"
        ),
        VoiceCommandHelpItem(
            title = "Mode conversation continue",
            examples = listOf("\"Active le mode continu\"", "\"Désactive le mode continu\""),
            description = "Le micro se réactive automatiquement après chaque réponse vocale.",
            category = "Contrôle"
        ),
        VoiceCommandHelpItem(
            title = "Synthèse vocale automatique",
            examples = listOf("\"Active la voix\"", "\"Coupe la voix\"", "\"Mets en sourdine\""),
            description = "Active ou coupe la lecture orale automatique des réponses de l'IA.",
            category = "Contrôle"
        ),
        VoiceCommandHelpItem(
            title = "Vitesse d'élocution TTS",
            examples = listOf("\"Parle plus vite\"", "\"Parle moins vite\"", "\"Accélère la voix\""),
            description = "Ajuste la vitesse d'élocution du moteur TextToSpeech Android.",
            category = "Contrôle"
        ),
        VoiceCommandHelpItem(
            title = "Aide vocale",
            examples = listOf("\"Aide\"", "\"Quelles sont les commandes vocales ?\"", "\"Que peux-tu faire ?\""),
            description = "Énonce ou affiche la liste complète des commandes vocales supportées.",
            category = "Aide"
        )
    )

    fun parse(spokenText: String, companionName: String = "Code"): VoiceCommandResult {
        val clean = spokenText.trim()
        if (clean.isBlank()) {
            return VoiceCommandResult.ConversationPrompt("")
        }

        val norm = normalize(clean)

        // 1. Stop / Silence command
        if (norm == "stop" || norm == "silence" || norm == "tais toi" || norm == "tais-toi" ||
            norm == "arrete" || norm == "pause" || norm == "chut" || norm == "arrete de parler" ||
            norm == "shut up" || norm == "stop speaking"
        ) {
            return VoiceCommandResult.StopSpeaking("Silence demandé.")
        }

        // 2. Repeat command
        if (norm == "repete" || norm == "repete s'il te plait" || norm == "repete stp" ||
            norm == "repete ce que tu as dit" || norm == "qu'est ce que tu as dit" ||
            norm == "repeat" || norm == "say that again"
        ) {
            return VoiceCommandResult.RepeatReply("Je te répète ma dernière réponse.")
        }

        // 3. Clear conversation
        if (norm == "efface la conversation" || norm == "efface l'historique" ||
            norm == "supprime la conversation" || norm == "nouvelle conversation" ||
            norm == "reset la conversation" || norm == "clear chat" || norm == "clear conversation"
        ) {
            return VoiceCommandResult.ClearHistory("Conversation réinitialisée.")
        }

        // 4. Web Search command
        val webSearchPrefixes = listOf(
            "cherche sur le web ",
            "cherche sur internet ",
            "cherche sur google ",
            "recherche sur le web ",
            "recherche sur internet ",
            "fais une recherche sur ",
            "recherche ",
            "cherche ",
            "google "
        )
        for (prefix in webSearchPrefixes) {
            val normPrefix = normalize(prefix)
            if (norm.startsWith(normPrefix) && norm.length > normPrefix.length + 2) {
                val query = clean.substring(prefix.length).trim()
                if (query.isNotBlank()) {
                    return VoiceCommandResult.SearchWeb(
                        query = query,
                        feedback = "Recherche web lancée pour : $query"
                    )
                }
            }
        }

        // 5. English Teacher mode
        if (norm.contains("mode prof d'anglais") || norm.contains("mode anglais") ||
            norm.contains("professeur d'anglais") || norm.contains("passe en anglais") ||
            norm.contains("parle anglais") || norm.contains("english teacher") || norm == "english mode"
        ) {
            return VoiceCommandResult.ModeChange(
                TutorMode.ENGLISH_TEACHER,
                "Mode Professeur d'anglais activé ! Let's practice English together!"
            )
        }

        // 6. Math Teacher mode
        if (norm.contains("mode math") || norm.contains("mode prof de math") ||
            norm.contains("professeur de mathematiques") || norm.contains("passe en maths") ||
            norm == "mode math" || norm == "math teacher"
        ) {
            return VoiceCommandResult.ModeChange(
                TutorMode.MATH_TEACHER,
                "Mode Professeur de mathématiques activé ! Prêt pour résoudre des calculs et problèmes."
            )
        }

        // 7. Custom Teacher mode
        if (norm.contains("mode professeur particulier") || norm.contains("mode tuteur") ||
            norm.contains("professeur particulier")
        ) {
            return VoiceCommandResult.ModeChange(
                TutorMode.CUSTOM_TEACHER,
                "Mode Professeur particulier activé. Quel sujet souhaites-tu explorer en profondeur ?"
            )
        }

        // 8. Friend / General conversation mode
        if (norm.contains("mode ami") || norm.contains("mode normal") ||
            norm.contains("mode conversation") || norm.contains("ami omniscient") ||
            norm == "mode ami"
        ) {
            return VoiceCommandResult.ModeChange(
                TutorMode.FRIEND,
                "Mode Ami omniscient réactivé. Je t'écoute !"
            )
        }

        // 9. Quiz / Review command
        if (norm.contains("lance un quiz") || norm.contains("fais moi un quiz") ||
            norm.contains("commence un quiz") || norm.contains("fais un quiz") ||
            norm.contains("revise mes flashcards") || norm.contains("revise les cartes") ||
            norm.contains("start quiz") || norm == "quiz"
        ) {
            return VoiceCommandResult.StartQuiz("Ouverture de l'atelier de révision et quiz.")
        }

        // 10. Flashcard creation
        val flashcardPrefixes = listOf(
            "cree une flashcard sur ",
            "cree une flashcard pour ",
            "cree une carte memoire sur ",
            "ajoute une flashcard sur ",
            "nouvelle flashcard "
        )
        for (prefix in flashcardPrefixes) {
            val normPrefix = normalize(prefix)
            if (norm.startsWith(normPrefix) && norm.length > normPrefix.length + 2) {
                val topic = clean.substring(prefix.length).trim()
                if (topic.isNotBlank()) {
                    return VoiceCommandResult.CreateFlashcard(
                        prompt = topic,
                        feedback = "Création d'une flashcard pour : $topic"
                    )
                }
            }
        }

        // 11. Voice Studio command
        if (norm.contains("change de voix") || norm.contains("studio de voix") ||
            norm.contains("parametres de voix") || norm.contains("ouvre la voix") ||
            norm.contains("voice studio") || norm == "voix"
        ) {
            return VoiceCommandResult.OpenVoiceStudio("Ouverture du studio des voix (Google HD, ElevenLabs, Hume).")
        }

        // 12. Agentic Harness command
        if (norm.contains("harnais agentique") || norm.contains("ouvre le harnais") ||
            norm.contains("mode agent") || norm.contains("console agent") ||
            norm.contains("agentic harness")
        ) {
            return VoiceCommandResult.OpenAgenticHarness("Ouverture de la console du Harnais Agentique.")
        }

        // 13. Continuous conversation toggle
        if (norm.contains("active le mode continu") || norm.contains("mode continu active") ||
            norm.contains("enable continuous mode")
        ) {
            return VoiceCommandResult.ToggleContinuousMode(
                enable = true,
                feedback = "Mode conversation continue activé. Le micro se réactivera automatiquement après mes réponses."
            )
        }
        if (norm.contains("desactive le mode continu") || norm.contains("arrete le mode continu") ||
            norm.contains("disable continuous mode")
        ) {
            return VoiceCommandResult.ToggleContinuousMode(
                enable = false,
                feedback = "Mode conversation continue désactivé."
            )
        }

        // 13b. Hands-Free English Conversation Mode
        if (norm.contains("quitter mains libres") || norm.contains("arrete mains libres") ||
            norm.contains("stop hands free") || norm.contains("ferme mains libres") ||
            norm.contains("desactive mains libres") || norm.contains("quitte mains libres")
        ) {
            return VoiceCommandResult.StopHandsFree("Session mains libres terminée.")
        }
        if (norm.contains("mains libres") || norm.contains("hands free") ||
            norm.contains("conversation mains libres") || norm.contains("pratique danglais mains libres") ||
            norm.contains("english practice") || norm == "hands-free" || norm == "handsfree"
        ) {
            return VoiceCommandResult.StartHandsFree("Ouverture de l'atelier de conversation en anglais mains libres.")
        }

        // 14. Auto-Speech / Voice synthesis toggle
        if (norm == "active la voix" || norm == "allume la voix" || norm == "parle a voix haute" ||
            norm == "active la parole" || norm == "unmute" || norm == "enable voice"
        ) {
            return VoiceCommandResult.ToggleAutoSpeech(
                enable = true,
                feedback = "Synthèse vocale automatique activée. Je lirai mes réponses à voix haute."
            )
        }
        if (norm == "coupe la voix" || norm == "mets en sourdine" || norm == "mute la voix" ||
            norm == "silence radio" || norm == "ne parle plus" || norm == "desactive la voix" ||
            norm == "mute" || norm == "disable voice"
        ) {
            return VoiceCommandResult.ToggleAutoSpeech(
                enable = false,
                feedback = "Synthèse vocale en sourdine. Mes réponses seront affichées sans audio automatique."
            )
        }

        // 15. Speech speed adjustment commands
        if (norm.contains("parle plus vite") || norm.contains("accelere la voix") || norm.contains("speak faster")) {
            return VoiceCommandResult.AdjustSpeechRate(
                delta = 0.15f,
                feedback = "J'accélère un peu mon débit de parole."
            )
        }
        if (norm.contains("parle moins vite") || norm.contains("ralentis la voix") || norm.contains("speak slower")) {
            return VoiceCommandResult.AdjustSpeechRate(
                delta = -0.15f,
                feedback = "Je ralentis mon débit de parole pour plus de clarté."
            )
        }

        // 16. Help command
        if (norm == "aide" || norm == "help" || norm.contains("commandes vocales") ||
            norm.contains("quelles sont tes commandes") || norm.contains("que peux tu faire")
        ) {
            return VoiceCommandResult.Help(
                "Voici les commandes vocales : tu peux dire 'Stop', 'Répète', 'Active la voix', 'Coupe la voix', 'Parle plus vite', 'Recherche sur le web...', 'Mode prof d'anglais', 'Lance un quiz', 'Change de voix' ou me poser n'importe quelle question !"
            )
        }

        // Default: normal conversational message for the AI
        return VoiceCommandResult.ConversationPrompt(clean)
    }
}
