package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted query and response cache used by the offline strategy layer
 * when network connectivity is lost.
 */
@Entity(
    tableName = "cached_query_responses",
    indices = [
        Index(value = ["normalizedQuery"]),
        Index(value = ["queryHash"])
    ]
)
data class CachedQueryResponse(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawQuery: String,
    val normalizedQuery: String,
    val queryHash: String,
    val responseText: String,
    val sourceSummary: String? = null,
    val confidenceScore: Float = 1.0f,
    val hitCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val isAiGenerated: Boolean = true
)
