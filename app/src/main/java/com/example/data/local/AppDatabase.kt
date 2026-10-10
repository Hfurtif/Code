package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ConversationMessage
import com.example.data.model.LearnedError
import com.example.data.model.Lesson
import com.example.data.model.PendingTask
import com.example.data.model.ResourcePack
import com.example.data.model.UserProfile
import com.example.data.model.Vocabulary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        ConversationMessage::class,
        Lesson::class,
        LearnedError::class,
        Vocabulary::class,
        ResourcePack::class,
        PendingTask::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun lessonDao(): LessonDao
    abstract fun learnedErrorDao(): LearnedErrorDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun resourceDao(): ResourceDao
    abstract fun pendingTaskDao(): PendingTaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "code_teacher_db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial data
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            seedInitialData(database)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            // Seed profile
            database.userDao().insertOrUpdate(
                UserProfile(
                    id = 1,
                    name = "Alex",
                    level = "B1",
                    objective = "Conversation fluide",
                    streakDays = 5,
                    xpScore = 580,
                    voiceGender = "female",
                    voiceAccent = "en-US",
                    speechRate = 1.0f,
                    speechPitch = 1.0f,
                    continuousMode = true,
                    correctionFrequency = "normal",
                    explanationLang = "fr",
                    hasCompletedOnboarding = true
                )
            )

            // Seed initial lessons
            database.lessonDao().insertLesson(
                Lesson(
                    title = "Bilan & Présentation",
                    type = "vocab",
                    subject = "Faire connaissance en anglais",
                    durationMinutes = 10,
                    summary = "Introduction orale, présentation des objectifs et repères de prononciation.",
                    wordsLearned = 5,
                    errorsCount = 1,
                    score = 95,
                    isCompleted = true
                )
            )
            database.lessonDao().insertLesson(
                Lesson(
                    title = "Le Present Perfect en situation",
                    type = "grammar",
                    subject = "Parler de ses expériences",
                    durationMinutes = 15,
                    summary = "Comment exprimer ce qu'on a déjà fait sans confondre avec le prétérit.",
                    wordsLearned = 7,
                    errorsCount = 2,
                    score = 90,
                    isCompleted = true
                )
            )
            database.lessonDao().insertLesson(
                Lesson(
                    title = "Commander au Restaurant",
                    type = "roleplay",
                    subject = "Jeu de rôle à Londres",
                    durationMinutes = 12,
                    summary = "Passer commande, poser des questions sur le menu et demander poliment l'addition.",
                    wordsLearned = 6,
                    errorsCount = 0,
                    score = 98,
                    isCompleted = false
                )
            )

            // Seed vocabulary
            val now = System.currentTimeMillis()
            database.vocabularyDao().insertWord(
                Vocabulary(
                    word = "Fluency",
                    translation = "Aisance / fluidité",
                    example = "She speaks English with natural fluency.",
                    masteryLevel = 3,
                    dateLearned = now - 86400000L * 2
                )
            )
            database.vocabularyDao().insertWord(
                Vocabulary(
                    word = "Resilience",
                    translation = "Résilience",
                    example = "Resilience is essential to build lasting habits.",
                    masteryLevel = 4,
                    dateLearned = now - 86400000L * 3
                )
            )
            database.vocabularyDao().insertWord(
                Vocabulary(
                    word = "Mindset",
                    translation = "État d'esprit",
                    example = "A growth mindset helps you embrace mistakes as learning opportunities.",
                    masteryLevel = 4,
                    dateLearned = now - 86400000L * 4
                )
            )
            database.vocabularyDao().insertWord(
                Vocabulary(
                    word = "Milestone",
                    translation = "Étape clé",
                    example = "Reaching the B2 level is an important milestone.",
                    masteryLevel = 2,
                    dateLearned = now - 86400000L
                )
            )
            database.vocabularyDao().insertWord(
                Vocabulary(
                    word = "Overcome",
                    translation = "Surmonter",
                    example = "With daily practice, you will overcome your fear of public speaking.",
                    masteryLevel = 3,
                    dateLearned = now
                )
            )

            // Seed sample errors for progression analysis
            database.learnedErrorDao().insertError(
                LearnedError(
                    badPhrase = "I am agree with you",
                    correction = "I agree with you",
                    errorType = "Grammaire",
                    count = 3
                )
            )
            database.learnedErrorDao().insertError(
                LearnedError(
                    badPhrase = "She don't like it",
                    correction = "She doesn't like it",
                    errorType = "Temps verbal",
                    count = 2
                )
            )
            database.learnedErrorDao().insertError(
                LearnedError(
                    badPhrase = "Depend of the weather",
                    correction = "Depends on the weather",
                    errorType = "Préposition",
                    count = 2
                )
            )

            // Seed downloadable resource packs
            database.resourceDao().insertResource(
                ResourcePack(
                    id = "model_offline_nano",
                    name = "Modèle IA Léger (On-Device Nano)",
                    type = "Modèle IA Embarqué",
                    sizeMb = 48.0,
                    downloadDate = now - 86400000L * 5,
                    lastUsedDate = now,
                    isDownloaded = true,
                    isProtected = false
                )
            )
            database.resourceDao().insertResource(
                ResourcePack(
                    id = "dict_fr_en",
                    name = "Dictionnaire Anglais-Français Essentiel",
                    type = "Dictionnaire & Traduction",
                    sizeMb = 18.5,
                    downloadDate = now - 86400000L * 5,
                    lastUsedDate = now,
                    isDownloaded = true,
                    isProtected = true // Protected: core progression
                )
            )
            database.resourceDao().insertResource(
                ResourcePack(
                    id = "pack_travel_vocab",
                    name = "Pack Vocabulaire Voyage & Situations réelles",
                    type = "Vocabulaire thématique",
                    sizeMb = 14.2,
                    downloadDate = now - 86400000L * 2,
                    lastUsedDate = now - 86400000L,
                    isDownloaded = true,
                    isProtected = false
                )
            )
            database.resourceDao().insertResource(
                ResourcePack(
                    id = "pack_grammar_rules",
                    name = "Règles de Grammaire & Modaux avancés",
                    type = "Guide pédagogique",
                    sizeMb = 6.0,
                    downloadDate = now - 86400000L * 4,
                    lastUsedDate = now,
                    isDownloaded = true,
                    isProtected = true
                )
            )
            database.resourceDao().insertResource(
                ResourcePack(
                    id = "pack_hd_audio_voices",
                    name = "Pack Voix HD Synthétiseur Audio",
                    type = "Audio & Prononciation",
                    sizeMb = 32.0,
                    downloadDate = 0L,
                    lastUsedDate = 0L,
                    isDownloaded = false,
                    isProtected = false
                )
            )

            // Seed initial greeting message
            database.conversationDao().insertMessage(
                ConversationMessage(
                    role = "code",
                    text = "Bonjour ! Je suis Code, ton professeur personnel. Je suis prêt à t'accompagner. De quoi aimerais-tu parler aujourd'hui ?",
                    mode = "ENGLISH"
                )
            )
        }
    }
}
