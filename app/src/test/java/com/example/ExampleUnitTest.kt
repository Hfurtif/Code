package com.example

import com.example.data.model.TutorMode
import com.example.engine.CurriculumRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testTutorModesConfigured() {
        assertEquals(4, TutorMode.values().size)
        assertEquals("Professeur d'anglais", TutorMode.ENGLISH_TEACHER.title)
    }

    @Test
    fun testWebSearchIntentExtraction() {
        val q1 = com.example.data.remote.WebSearchService.extractSearchQuery("Fais des recherches sur les trous noirs")
        assertEquals("les trous noirs", q1)

        val q2 = com.example.data.remote.WebSearchService.extractSearchQuery("Recherche sur internet l'intelligence artificielle")
        assertEquals("l'intelligence artificielle", q2)

        val q3 = com.example.data.remote.WebSearchService.extractSearchQuery("C'est quoi un trou noir ?")
        assertEquals("un trou noir", q3)
    }

    @Test
    fun testCurriculumContentLoaded() {
        assertTrue(CurriculumRepository.levelQuestions.isNotEmpty())
        assertTrue(CurriculumRepository.roleplays.isNotEmpty())
        assertTrue(CurriculumRepository.pronunciationDrills.isNotEmpty())
    }

    @Test
    fun testRoleplayScenariosValid() {
        val cafeScenario = CurriculumRepository.roleplays.firstOrNull { it.id == "london_cafe" }
        assertNotNull(cafeScenario)
        assertEquals("Commander dans un café à Londres", cafeScenario?.title)
    }

    @Test
    fun testGeminiInstructionEngine() {
        val profile = com.example.data.model.UserProfile(name = "Alex", level = "B2", companionName = "Léo", companionGender = "boy")
        val lang = com.example.data.model.AppLanguage.findByCode("fr-FR")
        val instruction = com.example.engine.GeminiInstructionEngine.buildSystemInstruction(
            mode = com.example.data.model.TutorMode.ENGLISH_TEACHER,
            profile = profile,
            language = lang
        )
        assertTrue(instruction.contains("Alex"))
        assertTrue(instruction.contains("Léo"))
        assertTrue(instruction.contains("figure masculine"))
        assertTrue(instruction.contains("Code"))
        assertTrue(instruction.contains("Franchement"))
    }

    @Test
    fun testCompanionGirlPersona() {
        val profile = com.example.data.model.UserProfile(name = "Max", level = "B1", companionName = "Lina", companionGender = "girl")
        val lang = com.example.data.model.AppLanguage.findByCode("fr-FR")
        val instruction = com.example.engine.GeminiInstructionEngine.buildSystemInstruction(
            mode = com.example.data.model.TutorMode.FRIEND,
            profile = profile,
            language = lang
        )
        assertTrue(instruction.contains("Lina"))
        assertTrue(instruction.contains("figure féminine"))
    }

    @Test
    fun testNotebookLMSourceIngestion() {
        val result = com.example.data.remote.SourceIngestionService.ingestFromDocumentText(
            rawTitle = "Cours d'Astrophysique",
            rawContent = "Les étoiles à neutrons sont des résidus d'étoiles massives qui ont explosé en supernova. Leur densité est phénoménale."
        )
        assertEquals("Cours d'Astrophysique", result.title)
        assertTrue(result.fullContent.contains("supernova"))
        assertTrue(result.wordCount > 10)
    }

    @Test
    fun testTtsOralTextCleaning() {
        val markdownText = "**Bonjour !** Voici un lien [site](https://example.com) et un code ```val x = 1```. C.-à-d. 25€ et 30°C à 120km/h 😊."
        val cleaned = com.example.voice.TtsManager.cleanOralText(markdownText)
        
        // Assert markdown stripped
        assertTrue(!cleaned.contains("**"))
        assertTrue(!cleaned.contains("https://"))
        assertTrue(!cleaned.contains("```"))
        
        // Assert expansions
        assertTrue(cleaned.contains("euros"))
        assertTrue(cleaned.contains("degrés Celsius"))
        assertTrue(cleaned.contains("kilomètres par heure"))
        assertTrue(cleaned.contains("c'est-à-dire"))
        
        // Assert emojis stripped
        assertTrue(!cleaned.contains("😊"))
    }

    @Test
    fun testTtsUtteranceChunking() {
        val longText = (1..50).joinToString(" ") { "Phrase numéro $it pour tester la synthèse vocale." }
        val chunks = com.example.voice.TtsManager.splitIntoUtteranceChunks(longText, maxCharsPerChunk = 200)
        
        assertTrue(chunks.size > 1)
        for (chunk in chunks) {
            assertTrue(chunk.length <= 250) // reasonable margin after sentence trim
            assertTrue(chunk.isNotBlank())
        }
    }

    @Test
    fun testVoiceCommandsTtsControls() {
        val cmdUnmute = com.example.voice.VoiceCommandManager.parse("Active la voix")
        assertTrue(cmdUnmute is com.example.voice.VoiceCommandResult.ToggleAutoSpeech)
        assertTrue((cmdUnmute as com.example.voice.VoiceCommandResult.ToggleAutoSpeech).enable)

        val cmdMute = com.example.voice.VoiceCommandManager.parse("Coupe la voix")
        assertTrue(cmdMute is com.example.voice.VoiceCommandResult.ToggleAutoSpeech)
        assertTrue(!(cmdMute as com.example.voice.VoiceCommandResult.ToggleAutoSpeech).enable)

        val cmdFaster = com.example.voice.VoiceCommandManager.parse("Parle plus vite")
        assertTrue(cmdFaster is com.example.voice.VoiceCommandResult.AdjustSpeechRate)
        assertTrue((cmdFaster as com.example.voice.VoiceCommandResult.AdjustSpeechRate).delta > 0)

        val cmdSlower = com.example.voice.VoiceCommandManager.parse("Parle moins vite")
        assertTrue(cmdSlower is com.example.voice.VoiceCommandResult.AdjustSpeechRate)
        assertTrue((cmdSlower as com.example.voice.VoiceCommandResult.AdjustSpeechRate).delta < 0)
    }

    @Test
    fun testTtsEngineInfoModel() {
        val engineInfo = com.example.voice.TtsEngineInfo(
            packageName = "com.google.android.tts",
            label = "Services vocaux de Google",
            isDefault = true
        )
        assertEquals("com.google.android.tts", engineInfo.packageName)
        assertTrue(engineInfo.isDefault)
    }

    @Test
    fun testCalameNfdNormalizationAndStopwords() {
        val norm1 = com.example.engine.CalameAnalysisEngine.normalize("Élève ingénieur à Genève & forêt")
        assertEquals("eleve ingenieur a geneve & foret", norm1)

        val tokens = com.example.engine.CalameAnalysisEngine.tokenize("La photosynthèse est le processus biologique des plantes.")
        assertTrue(tokens.contains("photosynthese"))
        assertTrue(tokens.contains("processus"))
        assertTrue(tokens.contains("biologique"))
        assertTrue(tokens.contains("plantes"))
        // Stopwords filtered
        assertTrue(!tokens.contains("est"))
        assertTrue(!tokens.contains("des"))
    }

    @Test
    fun testCalameChunkingOffsets() {
        val text = "Premier paragraphe important sur la biologie cellulaire. Les mitochondries sont les centrales énergétiques des cellules vivantes. Elles produisent de l'ATP indispensable pour toutes les activités métaboliques de l'organisme humain et animal. Deuxième paragraphe détaillant la membrane plasmique et son rôle dans les échanges sélectifs."
        val source = com.example.engine.CalameAnalysisEngine.SourceDoc(
            id = 1L,
            title = "Biologie Cellulaire",
            text = text,
            isSelected = true
        )
        val chunks = com.example.engine.CalameAnalysisEngine.createChunks(listOf(source))
        assertTrue(chunks.isNotEmpty())

        for (chunk in chunks) {
            assertTrue(chunk.start >= 0)
            assertTrue(chunk.end <= text.length)
            assertTrue(chunk.start < chunk.end)
            assertEquals(text.substring(chunk.start, chunk.end), chunk.text)
        }
    }

    @Test
    fun testCalameTfIdfRetrieval() {
        val s1 = com.example.engine.CalameAnalysisEngine.SourceDoc(
            id = 1L,
            title = "Astronomie",
            text = "Les trous noirs sont des objets célestes gravitationnels extrêmes formés par l'effondrement d'étoiles massives.",
            isSelected = true
        )
        val s2 = com.example.engine.CalameAnalysisEngine.SourceDoc(
            id = 2L,
            title = "Histoire romaine",
            text = "Jules César a conquis la Gaule avant de franchir le Rubicon en 49 avant notre ère.",
            isSelected = true
        )

        val retrieved = com.example.engine.CalameAnalysisEngine.retrieveChunks(listOf(s1, s2), "trou noir gravitationnel", topN = 2)
        assertTrue(retrieved.isNotEmpty())
        assertEquals(1L, retrieved.first().sourceId)
        assertEquals("Astronomie", retrieved.first().sourceTitle)
        assertTrue(retrieved.first().score > 0.0)
    }

    @Test
    fun testCalameStudioGenerators() {
        val text = "L'intelligence artificielle repose sur des modèles neuronaux profonds. Les réseaux de neurones apprennent à partir de grandes quantités de données textuelles et visuelles. L'apprentissage supervisé et par renforcement permet d'optimiser les prédictions et la précision algorithmique."
        val source = com.example.engine.CalameAnalysisEngine.SourceDoc(
            id = 1L,
            title = "Cours IA",
            text = text,
            isSelected = true
        )
        val sources = listOf(source)

        // Test Summary
        val summary = com.example.engine.CalameAnalysisEngine.generateStudioContent(sources, "Mon Carnet", com.example.engine.CalameAnalysisEngine.StudioKind.SUMMARY)
        assertTrue(summary.isNotBlank())

        // Test Guide
        val guide = com.example.engine.CalameAnalysisEngine.generateStudioContent(sources, "Mon Carnet", com.example.engine.CalameAnalysisEngine.StudioKind.GUIDE)
        assertTrue(guide.contains("Notions clés"))
        assertTrue(guide.contains("À retenir"))

        // Test FAQ
        val faq = com.example.engine.CalameAnalysisEngine.generateStudioContent(sources, "Mon Carnet", com.example.engine.CalameAnalysisEngine.StudioKind.FAQ)
        assertTrue(faq.contains("Que disent les sources"))

        // Test Audio
        val audio = com.example.engine.CalameAnalysisEngine.generateStudioContent(sources, "Mon Carnet", com.example.engine.CalameAnalysisEngine.StudioKind.AUDIO)
        assertTrue(audio.contains("Mon Carnet"))

        // Test Slides
        val slides = com.example.engine.CalameAnalysisEngine.generateStudioContent(sources, "Mon Carnet", com.example.engine.CalameAnalysisEngine.StudioKind.SLIDES)
        assertTrue(slides.contains("Diapositive 1"))

        // Test Mindmap
        val mindmap = com.example.engine.CalameAnalysisEngine.generateStudioContent(sources, "Mon Carnet", com.example.engine.CalameAnalysisEngine.StudioKind.MINDMAP)
        assertTrue(mindmap.contains("◆ Mon Carnet"))

        // Test Table
        val table = com.example.engine.CalameAnalysisEngine.generateStudioContent(sources, "Mon Carnet", com.example.engine.CalameAnalysisEngine.StudioKind.TABLE)
        assertTrue(table.contains("Source | Mots | Mots-clés"))
        assertTrue(table.contains("Cours IA"))

        // Test Report
        val report = com.example.engine.CalameAnalysisEngine.generateStudioContent(sources, "Mon Carnet", com.example.engine.CalameAnalysisEngine.StudioKind.REPORT)
        assertTrue(report.contains("Rapport de Synthèse"))
        assertTrue(report.contains("1. Introduction"))
    }

    @Test
    fun testPronunciationMatchingSimilarity() {
        // Perfect match
        val match100 = com.example.voice.TtsManager.calculatePronunciationMatch("comfortable", "comfortable")
        assertEquals(100, match100)

        // Close match (single typo or speech variation)
        val matchClose = com.example.voice.TtsManager.calculatePronunciationMatch("schedule", "shedule")
        assertTrue(matchClose >= 80)

        // Completely different word
        val matchDifferent = com.example.voice.TtsManager.calculatePronunciationMatch("wednesday", "banana")
        assertTrue(matchDifferent < 40)
    }

    @Test
    fun testCurriculumPronunciationBankContent() {
        val bank = com.example.engine.CurriculumRepository.pronunciationBank
        assertTrue(bank.size >= 15)

        val comfortable = bank.firstOrNull { it.word == "Comfortable" }
        assertNotNull(comfortable)
        assertEquals("/ˈkʌm.fə.tə.bəl/", comfortable?.phoneticIpa)
        assertEquals("comf-ter-buhl", comfortable?.syllables)
        assertEquals("Mots pièges", comfortable?.category)

        val categories = bank.map { it.category }.distinct()
        assertTrue(categories.contains("Mots pièges"))
        assertTrue(categories.contains("Lettres muettes"))
        assertTrue(categories.contains("Sons TH & R"))
        assertTrue(categories.contains("Rythme & Accent"))
    }

    @Test
    fun testPronunciationTargetExtraction() {
        // French phrasing
        val target1 = com.example.engine.CodeBrain.extractPronunciationTarget("Comment se prononce Comfortable ?")
        assertEquals("Comfortable", target1)

        val target2 = com.example.engine.CodeBrain.extractPronunciationTarget("Comment prononcer Thoroughly s'il te plaît ?")
        assertEquals("Thoroughly", target2)

        // English phrasing
        val target3 = com.example.engine.CodeBrain.extractPronunciationTarget("How do you pronounce Schedule?")
        assertEquals("Schedule", target3)

        val target4 = com.example.engine.CodeBrain.extractPronunciationTarget("How to pronounce \"Entrepreneur\"")
        assertEquals("Entrepreneur", target4)
    }
}

