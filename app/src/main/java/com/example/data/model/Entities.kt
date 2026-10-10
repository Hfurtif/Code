package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Élève",
    val level: String = "B1", // A1, A2, B1, B2, C1, C2
    val objective: String = "Conversation fluide", // voyage, travail, examen, conversation
    val streakDays: Int = 3,
    val xpScore: Int = 420,
    val voiceId: String = "",
    val voiceGender: String = "female", // female, male
    val voiceAccent: String = "en-US", // en-US, en-GB, fr-FR
    val speechRate: Float = 1.0f, // 0.6f to 1.4f
    val speechPitch: Float = 1.0f,
    val continuousMode: Boolean = true,
    val correctionFrequency: String = "normal", // often, normal, rare
    val explanationLang: String = "fr", // fr, en
    val hasCompletedOnboarding: Boolean = true,
    val dailyReminderEnabled: Boolean = false
)

@Entity(tableName = "conversations")
data class ConversationMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" or "code"
    val text: String,
    val correctionNote: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String = "ENGLISH"
)

@Entity(tableName = "lessons")
data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // "vocab", "grammar", "pronunciation", "roleplay", "spaced_repetition"
    val subject: String,
    val durationMinutes: Int = 15,
    val summary: String,
    val wordsLearned: Int = 0,
    val errorsCount: Int = 0,
    val score: Int = 100,
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val resourcePackId: String? = null
)

@Entity(tableName = "learned_errors")
data class LearnedError(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val badPhrase: String,
    val correction: String,
    val errorType: String, // "Grammaire", "Vocabulaire", "Prononciation", "Temps verbal", "Préposition"
    val count: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vocabulary")
data class Vocabulary(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val translation: String,
    val example: String,
    val masteryLevel: Int = 1, // 1 to 5
    val dateLearned: Long = System.currentTimeMillis(),
    val nextReviewDate: Long = System.currentTimeMillis() + 86400000L // J+1
)

@Entity(tableName = "resources")
data class ResourcePack(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // "Modèle IA Léger", "Dictionnaire En-Fr", "Vocabulaire Voyage", "Audio Haute Fidélité"
    val sizeMb: Double,
    val downloadDate: Long,
    val lastUsedDate: Long,
    val isDownloaded: Boolean = false,
    val isProtected: Boolean = false
)

@Entity(tableName = "pending_tasks")
data class PendingTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val resolvedAnswer: String? = null
)
