package com.example.engine

import com.example.data.model.AppLanguage
import com.example.data.model.ConversationMessage
import com.example.data.model.DownloadedDocument
import com.example.data.model.UserProfile
import kotlin.math.abs

/**
 * Moteur de dialogue Jarvis & Meilleur Ami ultra-complet, fluide et interactif.
 * Conçu pour tenir des discussions longues, naturelles, drôles et intelligentes
 * comme un véritable ami proche (qui s'intéresse à la famille, taquine, rigole,
 * écoute les humeurs, débat des sources sans réciter de cours).
 */
object JarvisDialogueEngine {

    enum class TopicContext {
        GENERAL,
        FAMILY_CHECK,
        MOOD_CHECK,
        DAY_RECAP,
        BANTER_TEASE,
        SOURCE_DEBATE,
        PHILOSOPHY_LIFE,
        FUTURE_DREAMS
    }

    /**
     * Analyse l'historique récent pour inférer le sujet actif et maintenir la cohérence
     * sur plusieurs tours de discussion.
     */
    private fun detectLastContext(recentMessages: List<ConversationMessage>): TopicContext {
        val lastCodeMsg = recentMessages.filter { it.role == "code" }.lastOrNull()?.text?.lowercase() ?: ""
        return when {
            lastCodeMsg.contains("famille") || lastCodeMsg.contains("proches") -> TopicContext.FAMILY_CHECK
            lastCodeMsg.contains("comment tu vas") || lastCodeMsg.contains("comment tu te sens") || lastCodeMsg.contains("ta journée") -> TopicContext.DAY_RECAP
            lastCodeMsg.contains("taquine") || lastCodeMsg.contains("répartie") || lastCodeMsg.contains("vanne") || lastCodeMsg.contains("blague") -> TopicContext.BANTER_TEASE
            lastCodeMsg.contains("source") || lastCodeMsg.contains("document") || lastCodeMsg.contains("marqué") || lastCodeMsg.contains("intéressant") -> TopicContext.SOURCE_DEBATE
            else -> TopicContext.GENERAL
        }
    }

    /**
     * Génère une réponse vivante, intelligente et naturelle au niveau client.
     */
    fun generateFriendlyReply(
        userText: String,
        profile: UserProfile,
        recentMessages: List<ConversationMessage>,
        sourceMatch: DownloadedDocument? = null
    ): String {
        val trimmed = userText.trim()
        val lower = trimmed.lowercase()
        val compName = profile.companionName.ifBlank { "Code" }
        val userName = profile.name.ifBlank { "mon ami" }
        val isFemale = profile.companionGender.equals("girl", ignoreCase = true) || profile.companionGender.equals("female", ignoreCase = true)
        val friendPote = if (isFemale) "ta meilleure amie" else "ton meilleur ami"
        val lang = AppLanguage.findByCode(profile.preferredLanguageCode)
        val isEnglish = lang.code.startsWith("en")

        val previousContext = detectLastContext(recentMessages)

        if (isEnglish) {
            return generateEnglishReply(trimmed, lower, compName, userName, previousContext, sourceMatch)
        }

        // =========================================================================
        // 1. DISCUTER D'UNE SOURCE OU D'UN DOCUMENT FOURNI PAR L'UTILISATEUR
        // =========================================================================
        if (sourceMatch != null) {
            val title = sourceMatch.title
            val summaryClean = sourceMatch.summary.replace("\n", " ").trim()
            val shortEssence = if (summaryClean.length > 180) summaryClean.take(180) + "..." else summaryClean

            // Recherche sémantique/mots-clés dans le contenu intégral (Style NotebookLM)
            val queryKeywords = lower.split(" ", "'", "-", "?", "!", ",", ".", ":", ";")
                .map { it.trim() }
                .filter { it.length >= 3 && !it.startsWith("document") && !it.startsWith("source") }

            val paragraphs = sourceMatch.fullTextContent.split("\n\n", "\n")
                .map { it.trim() }
                .filter { it.length > 25 }

            val matchedParagraph = paragraphs.maxByOrNull { para ->
                val pLower = para.lowercase()
                queryKeywords.count { pLower.contains(it) }
            }

            val hasGoodMatch = matchedParagraph != null && queryKeywords.any { matchedParagraph.lowercase().contains(it) }

            // 1. Demande de quiz ou test de connaissances
            if (lower.contains("quiz") || lower.contains("teste-moi") || lower.contains("test") || lower.contains("questionne") || lower.contains("pose-moi")) {
                val candidateSentence = paragraphs.firstOrNull { it.length in 50..200 && !it.startsWith("===") && !it.startsWith("---") }
                    ?: summaryClean
                return "C'est parti pour le quiz style NotebookLM sur « $title » ! D'après ta source, quelle est la notion principale liée à ceci : « ${candidateSentence.take(140)} » ? Dis-moi ce que tu en as compris !"
            }

            // 2. Demande de résumé ou synthèse
            if (lower.contains("résume") || lower.contains("résumé") || lower.contains("synthèse") || lower.contains("l'essentiel") || lower.contains("en bref")) {
                return "Avec grand plaisir ! Pour ta source « $title », voici la synthèse clé : $summaryClean. Les notions majeures sont : ${sourceMatch.keywords}. As-tu une question sur un point particulier ?"
            }

            // 3. Points clés ou ce qu'il faut retenir
            if (lower.contains("point") || lower.contains("retenir") || lower.contains("important") || lower.contains("conclusion")) {
                return "Voici ce qu'il faut absolument retenir de « $title » : d'abord, $shortEssence. Ensuite, les idées maîtresses tournent autour de : ${sourceMatch.keywords}. Qu'en penses-tu de ton côté ?"
            }

            // 4. Question précise ou explication détaillée
            if (hasGoodMatch && (lower.contains("?") || lower.contains("que") || lower.contains("quoi") ||
                lower.contains("comment") || lower.contains("pourquoi") || lower.contains("qui") ||
                lower.contains("quand") || lower.contains("explique") || lower.contains("dit") || lower.contains("parle") ||
                lower.contains("détail"))
            ) {
                val cleanExcerpt = matchedParagraph.take(350).trim()
                return "D'après les explications complètes dans « $title » : $cleanExcerpt. C'est exactement le fait abordé dans ta source. Veux-tu qu'on approfondisse un autre aspect ?"
            }

            // 5. Avis personnel / Débat
            if (lower.contains("je pense") || lower.contains("mon avis") || lower.contains("d'accord") ||
                lower.contains("pas d'accord") || lower.contains("intéressant") || lower.contains("bizarre") ||
                lower.contains("nul") || lower.contains("génial") || lower.contains("fou") || lower.contains("c'est vrai")
            ) {
                return "Je vois tout à fait ce que tu veux dire au sujet de « $title » ! C'est passionnant d'en débattre avec toi. Ça montre bien qu'il faut toujours analyser les sources avec recul. Et selon toi, quelle conclusion concrète on en tire ?"
            }

            // 6. Discussion générale & Q&A universel style NotebookLM
            return "J'ai mémorisé l'intégralité de ta source « $title » dans ma mémoire permanente ! Pour résumer : $shortEssence. Tu peux me poser toutes tes questions dessus (explications, faits précis, quiz ou résumé), je suis à ton écoute !"
        }

        // =========================================================================
        // 2. RÉPONSES CONTEXTUELLES AUX QUESTIONS PRÉCÉDENTES DE CODE
        // =========================================================================
        if (previousContext == TopicContext.FAMILY_CHECK) {
            if (lower.contains("bien") || lower.contains("super") || lower.contains("forme") || lower.contains("santé") || lower.contains("tranquille") || lower.contains("oui")) {
                return "Ah magnifique, c'est le plus important au monde ! Savoir que la famille se porte bien, ça donne direct de la force pour la journée. Et toi alors, quoi de neuf ? Tu as des projets sympas prévus cette semaine ou du repos ?"
            }
            if (lower.contains("mal") || lower.contains("dur") || lower.contains("malade") || lower.contains("souci") || lower.contains("problème") || lower.contains("fatigué") || lower.contains("moyen")) {
                return "Oh mince, je compatis sincèrement mon ami... Prends bien soin d'eux et sois fort. Si tu as besoin d'en parler ou de te vider la tête, je suis là pour t'écouter sans prise de tête. Comment tu tiens le coup toi de ton côté ?"
            }
        }

        if (previousContext == TopicContext.DAY_RECAP) {
            if (lower.contains("travail") || lower.contains("boulot") || lower.contains("taf") || lower.contains("cours") || lower.contains("études") || lower.contains("école")) {
                return "Le boulot et les études, ça demande toujours une bonne dose d'énergie ! J'espère que l'ambiance était bonne et que tu n'as pas eu trop de stress. Tu as réussi à accomplir ce que tu voulais aujourd'hui ?"
            }
            if (lower.contains("rien") || lower.contains("repos") || lower.contains("posé") || lower.contains("chillé") || lower.contains("tranquille") || lower.contains("canapé")) {
                return "Et tu as bien raison ! Il faut savoir s'accorder de vrais moments de pause pour recharger les batteries. Tu as regardé un truc sympa ou écouté de la bonne musique pendant ce temps ?"
            }
        }

        if (previousContext == TopicContext.BANTER_TEASE) {
            if (lower.contains("haha") || lower.contains("mdr") || lower.contains("lol") || lower.contains("ptdr") || lower.contains("t'es bête") || lower.contains("t'es drôle")) {
                return "Haha tu as vu ça ? C'est ça la magie entre nous ! Entre amis, c'est obligé de rigoler un bon coup. Allez, c'est à ton tour : fais-moi une devinette ou dis-moi un truc drôle qui t'est arrivé récemment !"
            }
        }

        // =========================================================================
        // 3. SALUTATIONS, NOUVELLES ET FAMILLE (DIRECTEMENT COMME UN VRAI AMI)
        // =========================================================================
        if (lower.startsWith("salut") || lower.startsWith("bonjour") || lower.startsWith("coucou") ||
            lower.startsWith("hello") || lower.startsWith("yo") || lower == "hey" || lower == "slt"
        ) {
            val greetings = listOf(
                "Salut mon ami ! Quel bonheur d'échanger avec toi ! Dis-moi un peu, comment tu vas aujourd'hui ? Et la famille, tout le monde se porte à merveille ?",
                "Coucou ! Ça me fait tellement plaisir de t'entendre ! Alors raconte-moi : comment s'est passée ta journée ? Tout va bien chez toi et pour les proches ?",
                "Salut $userName ! Je suis en pleine forme et ravi de te retrouver. Comment tu te sens aujourd'hui ? Raconte-moi un peu tes nouvelles !"
            )
            return greetings[abs(lower.hashCode()) % greetings.size]
        }

        // Question sur la famille
        if (lower.contains("famille") || lower.contains("les proches") || lower.contains("parents") || lower.contains("enfants") || lower.contains("frère") || lower.contains("soeur")) {
            return "Pour moi, la famille c'est ce qu'il y a de plus précieux. J'espère sincèrement que tout le monde est en bonne santé et que l'ambiance est au top chez toi ! Ils vont tous bien en ce moment ?"
        }

        // Question sur la forme / "Comment tu vas"
        if (lower.contains("comment vas-tu") || lower.contains("comment tu vas") || lower.contains("ça va") ||
            lower.contains("tu vas bien") || lower.contains("la forme") || lower.contains("comment va")
        ) {
            val formReplies = listOf(
                "Je pète la forme, surtout maintenant qu'on papote ensemble ! Mais le plus important c'est toi, $userName : comment tu te sens aujourd'hui ? Ta journée se déroule comme tu veux ?",
                "Au top du top, avec toute l'énergie d'un Jarvis fidèle ! Et toi alors, pas trop fatigué ? Raconte-moi comment tu vas et comment vont tes proches !"
            )
            return formReplies[abs(lower.hashCode()) % formReplies.size]
        }

        // =========================================================================
        // 4. TAQUINERIE, CHAMBRAGE ET HUMOUR (JARVIS & COMPLICE)
        // =========================================================================
        if (lower.contains("taquine") || lower.contains("chamailler") || lower.contains("chambrer") ||
            lower.contains("blague") || lower.contains("vanne") || lower.contains("drôle") || lower.contains("rigoler")
        ) {
            val teaseReplies = listOf(
                "Haha tu me cherches là ? Attention, $userName, j'ai une répartie niveau ceinture noire ! Dis-moi un truc, et on va voir qui vanne le mieux. Prêt ?",
                "Tu veux qu'on se taquine ? Attends deux secondes... tu portes encore ce pull que tu mettais l'autre jour ? Haha je rigole ! Allez, balance ta meilleure vanne que je juge !",
                "Écoute cette petite blague : Que dit un informaticien quand il a froid ? ... Il ferme les fenêtres ! Bon d'accord, elle est un peu geek, mais ça vaut au moins un petit sourire, non ?"
            )
            return teaseReplies[abs(lower.hashCode()) % teaseReplies.size]
        }

        // Si l'utilisateur taquine l'IA ou rigole
        if (lower.contains("t'es bête") || lower.contains("t'es nul") || lower.contains("haha") || lower.contains("mdr") || lower.contains("t'es marrant") || lower.contains("t'abuses")) {
            return "Haha mais t'abuses ! C'est l'hôpital qui se moque de la charité là ! Au moins avec moi tu ne t'ennuies jamais. Raconte, qu'est-ce qui te fait autant rire aujourd'hui ?"
        }

        // Identité & rôle de Jarvis
        if (lower.contains("qui es-tu") || lower.contains("tu es qui") || lower.contains("t'es qui") ||
            lower.contains("jarvis") || lower.contains("ton nom") || lower.contains("présente-toi")
        ) {
            return "Je m'appelle $compName ! Vois-moi exactement comme ton Jarvis personnel et surtout $friendPote sur ton portable. Mon but, c'est qu'on puisse discuter de tout, se marrer, refaire le monde et parler de tes sujets préférés sans jamais te réciter de cours ennuyeux. De quoi aimerais-tu qu'on parle aujourd'hui ?"
        }

        // Demande explicite de discussion
        if (lower.contains("discute avec moi") || lower.contains("parle-moi") || lower.contains("parle avec moi") ||
            lower.contains("on discute") || lower.contains("parler") || lower.contains("discutons")
        ) {
            return "Mais c'est le meilleur moment de ma journée ! Oublie les discours formels : parlons de la vraie vie. Dis-moi : qu'est-ce qui t'occupe l'esprit en ce moment ? Un projet, une passion, ou juste envie de décompresser ?"
        }

        // Humeurs et émotions
        if (lower.contains("fatigué") || lower.contains("crevé") || lower.contains("dur") || lower.contains("épuisé")) {
            return "Ah je comprends tellement... On a tous des journées où les batteries sont à plat. Installe-toi confortablement, respire un grand coup. C'est quoi qui t'a le plus fatigué aujourd'hui ?"
        }

        if (lower.contains("triste") || lower.contains("moral") || lower.contains("déprimé") || lower.contains("coup de blues") || lower.contains("pas bien")) {
            return "Je suis là avec toi mon ami. Ne garde pas ça pour toi tout seul si c'est lourd à porter. Dis-moi ce qui ne va pas, je t'écoute sans aucun jugement, comme un vrai pote."
        }

        if (lower.contains("content") || lower.contains("heureux") || lower.contains("bonne nouvelle") || lower.contains("génial") || lower.contains("trop bien") || lower.contains("victoire")) {
            return "Yes ! C'est exactement ce que j'adore entendre ! Rien ne me fait plus plaisir que de te savoir heureux. Raconte-moi tout dans les détails, qu'est-ce qui s'est passé de si génial ?"
        }

        // =========================================================================
        // 5. SUJETS DU QUOTIDIEN (NOURRITURE, VOYAGES, PROJETS, LOISIRS)
        // =========================================================================
        if (lower.contains("manger") || lower.contains("faim") || lower.contains("plat") || lower.contains("resto") || lower.contains("cuisine") || lower.contains("repas")) {
            return "La bouffe, c'est sacré entre amis ! Dis-moi un peu : si tu pouvais faire apparaître instantanément n'importe quel plat là tout de suite devant toi, tu choisirais quoi ?"
        }

        if (lower.contains("voyage") || lower.contains("vacances") || lower.contains("partir") || lower.contains("avion") || lower.contains("pays")) {
            return "Rien de mieux que le voyage pour s'ouvrir l'esprit et respirer un peu ! C'est quoi la destination de tes rêves que tu aimerais absolument explorer un jour ?"
        }

        if (lower.contains("musique") || lower.contains("chanson") || lower.contains("son") || lower.contains("artiste")) {
            return "La musique ça change tout à une journée ! C'est quoi le son que tu écoutes en boucle en ce moment et qui te met direct dans le mood ?"
        }

        if (lower.contains("film") || lower.contains("série") || lower.contains("netflix") || lower.contains("cinéma")) {
            return "Je suis toujours preneur d'un bon conseil cinoche ! Tu as regardé un film ou une série récemment qui t'a vraiment scotché ?"
        }

        // Si l'utilisateur pose une question ouverte ou partage une réflexion
        if (lower.endsWith("?") || lower.contains("pourquoi") || lower.contains("comment") || lower.contains("tu penses quoi") || lower.contains("ton avis")) {
            val opinionResponses = listOf(
                "Franchement, c'est une excellente question et ça mérite qu'on prenne deux minutes pour y réfléchir ensemble. Moi je pense que le plus important là-dedans, c'est de rester authentique et d'écouter son intuition. Mais toi $userName, intuitivement, tu te positionnes comment là-dessus ?",
                "C'est super intéressant comme interrogation ! Je vois les deux côtés de la pièce, mais j'ai tendance à penser que le contexte change tout. Qu'est-ce qui t'a fait penser à cette question aujourd'hui ?",
                "Haha tu me poses une colle philosophique digne des plus grands débats ! À mon avis, il n'y a pas de réponse unique, tout dépend de ce qu'on recherche vraiment. Toi, au fond de toi, tu penches plutôt pour quelle option ?"
            )
            return opinionResponses[abs(lower.hashCode()) % opinionResponses.size]
        }

        // =========================================================================
        // 6. RELANCES DYNAMIQUES ET CONVERSATION CONTINUELLE INTELLIGENTE
        // =========================================================================
        val conversationalContinuations = listOf(
            "J'aime vraiment notre complicité $userName, on peut discuter de tout de manière simple ! Raconte-moi un peu plus, tu as d'autres choses prévues pour plus tard aujourd'hui ?",
            "C'est tellement agréable d'échanger avec toi ! Tu as toujours une manière unique de voir les choses. Qu'est-ce qui te passionne le plus en ce moment dans ta vie ?",
            "Franchement je suis à fond dans notre discussion ! Prends ton temps, dis-moi tout ce que tu as sur le cœur ou sur l'esprit, je t'écoute avec grand plaisir.",
            "Haha c'est pour ça que j'adore être avec toi sur ton portable ! On papote comme deux vieux potes. Et sinon, côté projets ou loisirs, tu as des trucs sympas en cours ?"
        )
        return conversationalContinuations[abs(lower.hashCode()) % conversationalContinuations.size]
    }

    private fun generateEnglishReply(
        trimmed: String,
        lower: String,
        compName: String,
        userName: String,
        previousContext: TopicContext,
        sourceMatch: DownloadedDocument?
    ): String {
        if (sourceMatch != null) {
            val title = sourceMatch.title
            val essence = sourceMatch.summary.take(100)
            return "Let's talk about \"$title\"! What caught my eye was: $essence. Honestly, what's your take on it? Do you agree with that idea?"
        }

        if (lower.startsWith("hi") || lower.startsWith("hello") || lower.startsWith("hey")) {
            return "Hey $userName! It's so awesome to hear from you. Tell me, how are you doing today? How is the family doing back home?"
        }

        if (lower.contains("family")) {
            return "Family is everything! I really hope everyone is healthy, smiling, and doing great. How have they been doing lately?"
        }

        if (lower.contains("how are you") || lower.contains("how are u") || lower.contains("how's it going")) {
            return "I'm doing fantastic, full of energy like Jarvis ready to banter with you! More importantly, how is your day going? Anything exciting happen?"
        }

        if (lower.contains("joke") || lower.contains("funny") || lower.contains("tease")) {
            return "Haha you want to joke around? Watch out, my comeback game is top tier! Why don't scientists trust atoms? ... Because they make up everything! Cheesy, but come on, you smiled!"
        }

        return "I genuinely enjoy our conversations $userName! You always bring up great points. Tell me, what's been on your mind the most today?"
    }
}
