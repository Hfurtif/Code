package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ConversationMessage
import com.example.data.model.LearnedError
import com.example.data.model.Lesson
import com.example.data.model.PendingTask
import com.example.data.model.ResourcePack
import com.example.data.model.UserProfile
import com.example.data.model.Vocabulary
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfile)

    @Update
    suspend fun update(profile: UserProfile)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ConversationMessage>>

    @Query("SELECT * FROM conversations WHERE mode = :mode ORDER BY timestamp ASC")
    fun getMessagesByMode(mode: String): Flow<List<ConversationMessage>>

    @Insert
    suspend fun insertMessage(message: ConversationMessage): Long

    @Query("DELETE FROM conversations")
    suspend fun clearHistory()
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons ORDER BY timestamp DESC")
    fun getAllLessons(): Flow<List<Lesson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: Lesson): Long

    @Update
    suspend fun updateLesson(lesson: Lesson)

    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    suspend fun getLessonById(id: Long): Lesson?
}

@Dao
interface LearnedErrorDao {
    @Query("SELECT * FROM learned_errors ORDER BY count DESC, timestamp DESC")
    fun getAllErrors(): Flow<List<LearnedError>>

    @Query("SELECT * FROM learned_errors WHERE badPhrase = :phrase LIMIT 1")
    suspend fun findByPhrase(phrase: String): LearnedError?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertError(error: LearnedError)

    @Update
    suspend fun updateError(error: LearnedError)
}

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary ORDER BY dateLearned DESC")
    fun getAllVocabulary(): Flow<List<Vocabulary>>

    @Query("SELECT * FROM vocabulary WHERE nextReviewDate <= :currentTime ORDER BY nextReviewDate ASC")
    fun getDueForReview(currentTime: Long): Flow<List<Vocabulary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(vocabulary: Vocabulary)

    @Update
    suspend fun updateWord(vocabulary: Vocabulary)
}

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resources")
    fun getAllResources(): Flow<List<ResourcePack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourcePack)

    @Update
    suspend fun updateResource(resource: ResourcePack)

    @Query("UPDATE resources SET isDownloaded = 0 WHERE id = :id")
    suspend fun markDeleted(id: String)
}

@Dao
interface PendingTaskDao {
    @Query("SELECT * FROM pending_tasks WHERE isResolved = 0 ORDER BY timestamp ASC")
    fun getUnresolvedTasks(): Flow<List<PendingTask>>

    @Insert
    suspend fun insertTask(task: PendingTask): Long

    @Update
    suspend fun updateTask(task: PendingTask)
}
