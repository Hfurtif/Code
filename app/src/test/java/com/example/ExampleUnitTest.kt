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
        assertEquals("Professeur d'anglais", TutorMode.ENGLISH.title)
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
