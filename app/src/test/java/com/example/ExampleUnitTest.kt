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
}
