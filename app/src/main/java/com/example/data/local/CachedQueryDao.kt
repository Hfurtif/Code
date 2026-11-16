package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CachedQueryResponse
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedQueryDao {

    @Query("SELECT * FROM cached_query_responses WHERE normalizedQuery = :normalized LIMIT 1")
    suspend fun findExactMatch(normalized: String): CachedQueryResponse?

    @Query("SELECT * FROM cached_query_responses WHERE queryHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): CachedQueryResponse?

    @Query("SELECT * FROM cached_query_responses WHERE normalizedQuery LIKE '%' || :keyword || '%' ORDER BY hitCount DESC, lastAccessedAt DESC LIMIT 3")
    suspend fun searchByKeyword(keyword: String): List<CachedQueryResponse>

    @Query("SELECT * FROM cached_query_responses ORDER BY lastAccessedAt DESC")
    fun getAllCachedResponsesFlow(): Flow<List<CachedQueryResponse>>

    @Query("SELECT COUNT(*) FROM cached_query_responses")
    suspend fun countCachedResponses(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cached: CachedQueryResponse): Long

    @Query("UPDATE cached_query_responses SET hitCount = hitCount + 1, lastAccessedAt = :now WHERE id = :id")
    suspend fun incrementHitCount(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM cached_query_responses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM cached_query_responses")
    suspend fun clearAll()
}
