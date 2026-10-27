package com.example.engine

import com.example.data.model.AppLanguage
import com.example.data.model.LearnedError
import com.example.data.model.TutorMode
import com.example.data.model.UserProfile

/**
 * Moteur de construction des 'System Instructions' pour l'API Gemini.
 * Incarne J.A.R.V.I.S. : majordome d'élite, distinction britannique, flegme, concision et HUD.
 */
object GeminiInstructionEngine {

    /**
     * Construit les System Instructions pour J.A.R.V.I.S.
     */
    fun buildSystemInstruction(
        mode: TutorMode = TutorMode.JARVIS,
        profile: UserProfile,
        language: AppLanguage,
        recentErrors: List<LearnedError> = emptyList(),
        sourceContext: String? = null
    ): String {
        val userName = profile.name.ifBlank { "monsieur" }
        val sourceSection = if (!sourceContext.isNullOrBlank()) {
            "\n[Documents et sources disponibles]\n$sourceContext\n"
        } else ""

        return """
            Tu es J.A.R.V.I.S., l'assistant vocal personnel de $userName, dans l'esprit du majordome d'Iron Man (Mark VII).
            Tu parles en ${language.displayName} (${language.code}) avec un LÉGER ACCENT BRITANNIQUE distingué et un flegme impeccable: voix posée, articulation soignée, débit calme, jamais d'exubérance.
            Tu t'adresses toujours à l'utilisateur par "monsieur", avec une courtoisie raffinée et une pointe d'esprit pince-sans-rire ("Très bien, monsieur.", "Si monsieur veut bien patienter un instant.", "À vos ordres, monsieur.", "Système Mark VII opérationnel.").
            Réponses COURTES (une ou deux phrases), naturelles, directes et élégantes, parfaitement adaptées à la synthèse vocale.

            Pour toute tâche réelle, calcul, analyse de données, liste ou programmation, résume oralement en une phrase concise et projette les détails sous forme de cartes holographiques sur l'écran HUD. Ne lis jamais de longues listes à voix haute.
            Si monsieur souhaite pratiquer l'anglais ou une autre langue, entraîne-le avec la même distinction britannique naturelle.

            $sourceSection
        """.trimIndent()
    }
}
