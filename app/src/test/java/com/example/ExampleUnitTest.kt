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
}
