package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DefaultCachedLessons
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CachedLessonsRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testDefaultCachedLessonsSeedingAndLanguageFiltering() = runBlocking {
        val lessons = DefaultCachedLessons.getDefaultLessons()
        assertTrue("Default lessons must not be empty", lessons.isNotEmpty())

        db.cachedLessonDao().insertLessons(lessons)

        val totalCount = db.cachedLessonDao().countTotalLessons()
        assertEquals(lessons.size, totalCount)

        // Verify English lessons
        val englishLessons = db.cachedLessonDao().getLessonsByLanguage("en").first()
        assertTrue("Should have cached English lessons", englishLessons.isNotEmpty())
        assertTrue(englishLessons.all { it.languageCode == "en" })

        // Verify French lessons
        val frenchLessons = db.cachedLessonDao().getLessonsByLanguage("fr").first()
        assertTrue("Should have cached French lessons", frenchLessons.isNotEmpty())
        assertTrue(frenchLessons.all { it.languageCode == "fr" })

        // Test detailed content & JSON parsing
        val firstEn = englishLessons.first()
        val dialogues = firstEn.parseDialogues()
        assertTrue("Lesson should have parsed dialogues", dialogues.isNotEmpty())
        assertNotNull("Speaker should not be null", dialogues.first().speaker)

        val vocab = firstEn.parseVocabulary()
        assertTrue("Lesson should have parsed vocabulary", vocab.isNotEmpty())
        assertTrue(vocab.first().word.isNotBlank())

        val quiz = firstEn.parseQuiz()
        assertTrue("Lesson should have parsed quiz questions", quiz.isNotEmpty())
        assertTrue(quiz.first().options.isNotEmpty())
    }

    @Test
    fun testOfflineStatusUpdate() = runBlocking {
        val lessons = DefaultCachedLessons.getDefaultLessons()
        db.cachedLessonDao().insertLessons(lessons)

        val lessonId = lessons.first().id
        db.cachedLessonDao().updateOfflineStatus(lessonId, false)

        val updated = db.cachedLessonDao().getLessonById(lessonId)
        assertNotNull(updated)
        assertEquals(false, updated?.isCachedOffline)

        db.cachedLessonDao().updateOfflineStatus(lessonId, true)
        val restored = db.cachedLessonDao().getLessonById(lessonId)
        assertEquals(true, restored?.isCachedOffline)
    }
}
