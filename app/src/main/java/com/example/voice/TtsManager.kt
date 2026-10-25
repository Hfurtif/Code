package com.example.voice

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.model.AppLanguage
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class TtsVoiceInfo(
    val id: String,
    val displayName: String,
    val locale: Locale,
    val isFemale: Boolean,
    val isMale: Boolean,
    val isHighQuality: Boolean = false,
    val isNetworkRequired: Boolean = false
)

data class TtsEngineInfo(
    val packageName: String,
    val label: String,
    val isDefault: Boolean = false
)

data class PronunciationFeedbackResult(
    val targetWord: String,
    val userSpoken: String,
    val score: Int, // 0 to 100
    val isMastered: Boolean,
    val feedbackText: String
) {
    val matchScore: Int get() = score
    val isAccurate: Boolean get() = isMastered
    val feedbackScript: String get() = feedbackText
}

/**
 * Enterprise-grade Android TextToSpeech engine with:
 * - Direct Android TextToSpeech API integration
 * - Intelligent utterance chunking for long AI responses (prevents Android 4000-char limits)
 * - Sentence-level & word-level range progress tracking (API 26+)
 * - AudioAttributes configured for voice assistant usage (ASSISTANT / CONTENT_TYPE_SPEECH)
 * - Oral text cleaning (code blocks, markdown, emojis, math, currency, units, abbreviations)
 * - Multi-language voice selection, pitch, rate, and volume controls
 * - Robust error code diagnostics and automatic fallback
 */
class TtsManager(
    private val context: Context,
    private val onInitComplete: (Boolean) -> Unit,
    private val onSpeakingStateChanged: (Boolean) -> Unit,
    private val onSpeechCompleted: () -> Unit,
    private val onWordSpoken: ((word: String, startOffset: Int, endOffset: Int) -> Unit)? = null
) : TextToSpeech.OnInitListener {

    val elevenLabsService = ElevenLabsVoiceService(context)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    var voiceEngine: String = "google_hd" // "google_hd", "elevenlabs"
    var elevenLabsApiKey: String = ""
    var elevenLabsVoiceId: String = "21m00Tcm4TlvDq8ikWAM" // Rachel default

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentRate = 1.0f
    private var currentPitch = 1.0f
    private var currentVolume = 1.0f
    private var currentVoiceName: String? = null
    private var currentLanguageCode: String = "fr-FR"
    private var pendingSpeechText: String? = null
    private var activeEnginePackage: String? = null

    // Track active chunk batches
    private var currentBatchId: String? = null
    private val batchChunkMap = ConcurrentHashMap<String, List<String>>()
    private val batchTotalChunksMap = ConcurrentHashMap<String, Int>()

    init {
        initializeTtsEngine(null)
    }

    fun initializeTtsEngine(enginePackage: String?) {
        try {
            tts?.shutdown()
            tts = null
            isInitialized = false
            activeEnginePackage = enginePackage
            tts = if (enginePackage.isNullOrBlank()) {
                TextToSpeech(context.applicationContext, this)
            } else {
                TextToSpeech(context.applicationContext, this, enginePackage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to construct TextToSpeech with engine $enginePackage", e)
            onInitComplete(false)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupAudioAttributes()
            applyCurrentLanguage()
            setupUtteranceListener()
            onInitComplete(true)

            // Speak queued speech if any was requested prior to initialization
            pendingSpeechText?.let { pending ->
                pendingSpeechText = null
                speak(pending)
            }
        } else {
            isInitialized = false
            Log.e(TAG, "TextToSpeech onInit failed with status code: $status")
            onInitComplete(false)
        }
    }

    private fun setupAudioAttributes() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .build()
            tts?.setAudioAttributes(audioAttributes)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting AudioAttributes on TextToSpeech", e)
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeakingStateChanged(true)
            }

            override fun onDone(utteranceId: String?) {
                handleUtteranceDone(utteranceId)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS utterance error on ID: $utteranceId")
                onSpeakingStateChanged(false)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                val errorMsg = when (errorCode) {
                    TextToSpeech.ERROR_SYNTHESIS -> "Erreur de synthèse vocale"
                    TextToSpeech.ERROR_SERVICE -> "Service TTS inaccessible"
                    TextToSpeech.ERROR_OUTPUT -> "Erreur de périphérique audio"
                    TextToSpeech.ERROR_NETWORK -> "Erreur réseau de synthèse vocale"
                    TextToSpeech.ERROR_NETWORK_TIMEOUT -> "Délai réseau TTS dépassé"
                    TextToSpeech.ERROR_INVALID_REQUEST -> "Requête de synthèse vocale invalide"
                    TextToSpeech.ERROR_NOT_INSTALLED_YET -> "Données vocales en cours d'installation"
                    else -> "Erreur TTS ($errorCode)"
                }
                Log.e(TAG, "TTS Utterance error [$errorCode]: $errorMsg (ID: $utteranceId)")
                onSpeakingStateChanged(false)
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                super.onRangeStart(utteranceId, start, end, frame)
                if (utteranceId != null && onWordSpoken != null) {
                    try {
                        val parts = utteranceId.split("_chunk_")
                        if (parts.size == 2) {
                            val batchId = parts[0]
                            val chunkIndex = parts[1].substringBefore("_of_").toIntOrNull() ?: 0
                            val chunks = batchChunkMap[batchId]
                            val currentChunk = chunks?.getOrNull(chunkIndex)
                            if (currentChunk != null && start >= 0 && end <= currentChunk.length && start < end) {
                                val word = currentChunk.substring(start, end)
                                onWordSpoken.invoke(word, start, end)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing onRangeStart word bounds", e)
                    }
                }
            }
        })
    }

    private fun handleUtteranceDone(utteranceId: String?) {
        if (utteranceId == null) {
            onSpeakingStateChanged(false)
            onSpeechCompleted()
            return
        }

        try {
            // Utterance ID pattern: "code_{batchId}_chunk_{index}_of_{total}"
            if (utteranceId.contains("_chunk_") && utteranceId.contains("_of_")) {
                val chunkPart = utteranceId.substringAfter("_chunk_")
                val index = chunkPart.substringBefore("_of_").toIntOrNull() ?: 0
                val total = chunkPart.substringAfter("_of_").toIntOrNull() ?: 1
                val batchId = utteranceId.substringBefore("_chunk_")

                // Only complete the whole speech lifecycle if this was the final chunk
                if (index >= total - 1) {
                    batchChunkMap.remove(batchId)
                    batchTotalChunksMap.remove(batchId)
                    onSpeakingStateChanged(false)
                    onSpeechCompleted()
                }
            } else {
                onSpeakingStateChanged(false)
                onSpeechCompleted()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in handleUtteranceDone", e)
            onSpeakingStateChanged(false)
            onSpeechCompleted()
        }
    }

    fun setLanguage(languageCode: String) {
        currentLanguageCode = languageCode
        if (isInitialized) {
            applyCurrentLanguage()
        }
    }

    private fun applyCurrentLanguage() {
        val appLang = AppLanguage.findByCode(currentLanguageCode)
        val targetLocale = appLang.locale
        try {
            val result = tts?.setLanguage(targetLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to general language code
                tts?.setLanguage(Locale.forLanguageTag(targetLocale.language))
            }

            // Pick the highest quality, most natural voice for this locale
            val voices = tts?.voices ?: emptySet()
            val langVoices = voices.filter {
                it.locale.language.equals(targetLocale.language, ignoreCase = true)
            }

            // Priority: High quality (neural/wavenet/very-high), without network latency requirement
            val bestVoice = langVoices.sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenByDescending { !it.isNetworkConnectionRequired }
                    .thenByDescending {
                        val name = it.name.lowercase()
                        name.contains("fr-fr-x") || name.contains("wavenet") || name.contains("neural") || name.contains("female")
                    }
            ).firstOrNull()

            if (bestVoice != null) {
                tts?.voice = bestVoice
                currentVoiceName = bestVoice.name
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting language on TTS", e)
        }
    }

    fun getAvailableVoices(): List<TtsVoiceInfo> {
        if (!isInitialized) return emptyList()
        val voices = tts?.voices ?: return emptyList()
        return voices.map { voice ->
            val nameLower = voice.name.lowercase()
            val isFemale = nameLower.contains("female") || nameLower.contains("fem") || nameLower.contains("f0") || nameLower.contains("wom")
            val isMale = nameLower.contains("male") || nameLower.contains("masc") || nameLower.contains("m0") || nameLower.contains("man")
            val cleanName = voice.name.substringAfterLast("/").substringAfterLast(":")
            TtsVoiceInfo(
                id = voice.name,
                displayName = cleanName.ifBlank { voice.name },
                locale = voice.locale,
                isFemale = isFemale || (!isMale && voice.name.hashCode() % 2 == 0),
                isMale = isMale || (!isFemale && voice.name.hashCode() % 2 != 0),
                isHighQuality = voice.quality >= Voice.QUALITY_HIGH,
                isNetworkRequired = voice.isNetworkConnectionRequired
            )
        }
    }

    fun getInstalledEngines(): List<TtsEngineInfo> {
        if (!isInitialized) return emptyList()
        val engines = tts?.engines ?: return emptyList()
        val defaultEngineName = tts?.defaultEngine
        return engines.map { engine ->
            TtsEngineInfo(
                packageName = engine.name,
                label = engine.label.ifBlank { engine.name },
                isDefault = engine.name == defaultEngineName
            )
        }
    }

    fun setSpeechRate(rate: Float) {
        currentRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(currentRate)
    }

    fun setSpeechPitch(pitch: Float) {
        currentPitch = pitch.coerceIn(0.5f, 1.5f)
        tts?.setPitch(currentPitch)
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0.0f, 1.0f)
    }

    fun setVoiceByName(voiceName: String) {
        if (!isInitialized) return
        currentVoiceName = voiceName
        val targetVoice = tts?.voices?.firstOrNull { it.name == voiceName }
        if (targetVoice != null) {
            tts?.voice = targetVoice
        }
    }

    fun setVoiceByGenderAndAccent(gender: String, accentCode: String) {
        if (!isInitialized) return
        val targetLocale = AppLanguage.findByCode(accentCode).locale
        val voices = tts?.voices?.filter { it.locale.language == targetLocale.language } ?: emptyList()
        val isTargetFemale = gender.equals("female", ignoreCase = true)
        val selected = voices.sortedWith(
            compareByDescending<Voice> { it.quality }
                .thenByDescending {
                    val nameLower = it.name.lowercase()
                    if (isTargetFemale) {
                        nameLower.contains("female") || nameLower.contains("fem") || nameLower.contains("f")
                    } else {
                        nameLower.contains("male") || nameLower.contains("man") || nameLower.contains("m")
                    }
                }
        ).firstOrNull() ?: voices.firstOrNull()

        if (selected != null) {
            tts?.voice = selected
            currentVoiceName = selected.name
        } else {
            tts?.setLanguage(targetLocale)
        }
    }

    fun setVoiceEngineConfig(engine: String, apiKey: String, voiceId: String) {
        voiceEngine = engine
        elevenLabsApiKey = apiKey
        elevenLabsVoiceId = voiceId
    }

    /**
     * Cleans text and adds natural oral punctuation and micro-pauses
     * to make synthesized speech sound like a warm, fluid companion.
     * Also expands abbreviations, units, currencies, and strips code fences/markdown.
     */
    fun cleanOralText(text: String): String = Companion.cleanOralText(text)

    /**
     * Splits long text into natural sentence-based chunks smaller than [maxCharsPerChunk]
     * so that Android TextToSpeech never truncates text or errors due to length limits.
     */
    fun splitIntoUtteranceChunks(text: String, maxCharsPerChunk: Int = 2800): List<String> =
        Companion.splitIntoUtteranceChunks(text, maxCharsPerChunk)

    /**
     * Speaks the given text using the active voice engine (Android TTS by default).
     * Automatically splits long responses into queued chunks.
     */
    fun speak(text: String) {
        val oralText = cleanOralText(text)
        if (oralText.isBlank()) return

        stop()

        // ElevenLabs engine option (if selected and configured)
        if (voiceEngine.equals("elevenlabs", ignoreCase = true) && elevenLabsApiKey.isNotBlank()) {
            coroutineScope.launch {
                val success = elevenLabsService.synthesizeAndPlay(
                    text = oralText,
                    apiKey = elevenLabsApiKey,
                    voiceId = elevenLabsVoiceId,
                    onPlaybackStarted = { onSpeakingStateChanged(true) },
                    onPlaybackFinished = {
                        onSpeakingStateChanged(false)
                        onSpeechCompleted()
                    },
                    onError = { errMsg ->
                        Log.w(TAG, "ElevenLabs failed: $errMsg. Falling back to Android TextToSpeech.")
                        fallbackGoogleSpeak(oralText)
                    }
                )
                if (!success) {
                    fallbackGoogleSpeak(oralText)
                }
            }
            return
        }

        // Standard Android TextToSpeech API
        fallbackGoogleSpeak(oralText)
    }

    private fun fallbackGoogleSpeak(oralText: String) {
        if (!isInitialized) {
            pendingSpeechText = oralText
            return
        }

        val chunks = splitIntoUtteranceChunks(oralText)
        val batchId = "code_batch_${System.currentTimeMillis()}"
        currentBatchId = batchId
        batchChunkMap[batchId] = chunks
        batchTotalChunksMap[batchId] = chunks.size

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, currentVolume)
        }

        // Apply rate and pitch before speaking
        tts?.setSpeechRate(currentRate)
        tts?.setPitch(currentPitch)

        for (index in chunks.indices) {
            val chunk = chunks[index]
            val utteranceId = "${batchId}_chunk_${index}_of_${chunks.size}"
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD

            val res = tts?.speak(chunk, queueMode, params, utteranceId)
            if (res != TextToSpeech.SUCCESS) {
                Log.e(TAG, "TTS speak failed on chunk $index with code $res")
            }
        }
    }

    /**
     * Speaks text with an optional language override (e.g. "en-US", "en-GB")
     * and optional speech rate (e.g. 0.65f for slow pronunciation).
     */
    fun speakWithSettings(
        text: String,
        languageOverride: String? = null,
        rateOverride: Float? = null,
        pitchOverride: Float? = null
    ) {
        val oralText = cleanOralText(text)
        if (oralText.isBlank()) return

        stop()

        if (!isInitialized) {
            pendingSpeechText = oralText
            return
        }

        // Apply temporary language if specified
        if (!languageOverride.isNullOrBlank()) {
            val targetLocale = AppLanguage.findByCode(languageOverride).locale
            try {
                tts?.setLanguage(targetLocale)
                val voices = tts?.voices ?: emptySet()
                val langVoices = voices.filter { it.locale.language.equals(targetLocale.language, ignoreCase = true) }
                val bestVoice = langVoices.sortedWith(
                    compareByDescending<Voice> { it.quality }
                        .thenByDescending { !it.isNetworkConnectionRequired }
                        .thenByDescending {
                            val name = it.name.lowercase()
                            name.contains("en-us") || name.contains("en-gb") || name.contains("wavenet") || name.contains("neural")
                        }
                ).firstOrNull()
                if (bestVoice != null) {
                    tts?.voice = bestVoice
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error applying language override on TTS", e)
            }
        }

        // Apply temporary speech rate
        val appliedRate = rateOverride ?: currentRate
        val appliedPitch = pitchOverride ?: currentPitch
        tts?.setSpeechRate(appliedRate)
        tts?.setPitch(appliedPitch)

        val chunks = splitIntoUtteranceChunks(oralText)
        val batchId = "code_batch_${System.currentTimeMillis()}"
        currentBatchId = batchId
        batchChunkMap[batchId] = chunks
        batchTotalChunksMap[batchId] = chunks.size

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, currentVolume)
        }

        for (index in chunks.indices) {
            val chunk = chunks[index]
            val utteranceId = "${batchId}_chunk_${index}_of_${chunks.size}"
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(chunk, queueMode, params, utteranceId)
        }
    }

    /**
     * Specialized English pronunciation guidance routine:
     * - Speaks target English word with native enunciation at normal speed or slow speed.
     * - Speaks syllabic articulation with micro-pauses for difficult sounds.
     */
    fun speakEnglishPronunciationGuide(
        word: String,
        phoneticIpa: String? = null,
        syllables: String? = null,
        slowPace: Boolean = false,
        accent: String = "en-US"
    ) {
        val cleanWord = word.trim()
        val speechText = buildString {
            if (slowPace) {
                append(cleanWord)
                if (!syllables.isNullOrBlank()) {
                    append(". ")
                    append(syllables.replace("-", "... "))
                }
            } else {
                append(cleanWord)
            }
        }

        val rate = if (slowPace) 0.65f else 0.95f
        speakWithSettings(
            text = speechText,
            languageOverride = accent,
            rateOverride = rate
        )
    }

    /**
     * Compares user speech to target word and speaks intelligent vocal feedback in English.
     */
    fun evaluateAndSpeakPronunciationFeedback(
        targetWord: String,
        userSpoken: String,
        accent: String = "en-US"
    ): PronunciationFeedbackResult {
        val result = evaluateMatch(targetWord, userSpoken)
        val feedbackScript = buildString {
            when {
                result.score >= 85 -> {
                    append("Excellent pronunciation of '$targetWord'! That sounded very natural and accurate.")
                }
                result.score >= 50 -> {
                    append("Good attempt! You said '${userSpoken.trim()}'. Notice the difference with '$targetWord'. Listen carefully: $targetWord.")
                }
                else -> {
                    append("Let's practice together. The target word is '$targetWord'. Listen closely: $targetWord.")
                }
            }
        }

        speakWithSettings(
            text = feedbackScript,
            languageOverride = accent,
            rateOverride = 0.95f
        )
        return result
    }

    fun calculatePronunciationMatch(target: String, spoken: String): PronunciationFeedbackResult {
        return evaluateMatch(target, spoken)
    }

    fun isSpeaking(): Boolean {
        return (tts?.isSpeaking == true) || elevenLabsService.isPlayingAudio
    }


    fun stop() {
        try {
            currentBatchId = null
            batchChunkMap.clear()
            batchTotalChunksMap.clear()
            elevenLabsService.stopPlayback()
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
            onSpeakingStateChanged(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun destroy() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "TtsManager"

        fun calculatePronunciationMatch(target: String, spoken: String): Int {
            val normTarget = target.trim().lowercase()
            val normSpoken = spoken.trim().lowercase()
            if (normTarget.isEmpty() || normSpoken.isEmpty()) return 0
            if (normTarget == normSpoken || normSpoken.contains(normTarget)) return 100
            val dist = levenshtein(normTarget, normSpoken)
            val maxLen = maxOf(normTarget.length, normSpoken.length)
            val similarityRatio = (1.0 - (dist.toDouble() / maxLen.toDouble())).coerceIn(0.0, 1.0)
            return (similarityRatio * 100).toInt()
        }

        fun evaluateMatch(target: String, spoken: String): PronunciationFeedbackResult {
            val normTarget = target.trim().lowercase()
            val normSpoken = spoken.trim().lowercase()

            if (normTarget.isEmpty()) {
                return PronunciationFeedbackResult(target, spoken, 0, false, "No target word provided.")
            }
            if (normSpoken.isEmpty()) {
                return PronunciationFeedbackResult(target, spoken, 0, false, "No speech detected. Please speak clearly into the microphone.")
            }

            if (normTarget == normSpoken || normSpoken.contains(normTarget)) {
                return PronunciationFeedbackResult(target, spoken, 100, true, "Perfect! Native-level clarity.")
            }

            val dist = levenshtein(normTarget, normSpoken)
            val maxLen = maxOf(normTarget.length, normSpoken.length)
            val similarityRatio = (1.0 - (dist.toDouble() / maxLen.toDouble())).coerceIn(0.0, 1.0)
            val score = (similarityRatio * 100).toInt()

            val msg = when {
                score >= 85 -> "Great job! Very close to native pronunciation."
                score >= 60 -> "Almost there! Watch out for vowel length and silent letters."
                else -> "Keep practicing! Listen to the slow audio guidance and repeat."
            }

            return PronunciationFeedbackResult(target, spoken, score, score >= 85, msg)
        }

        fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
            val lhsLength = lhs.length
            val rhsLength = rhs.length
            var cost = Array(lhsLength + 1) { it }
            var newCost = Array(lhsLength + 1) { 0 }

            for (i in 1..rhsLength) {
                newCost[0] = i
                for (j in 1..lhsLength) {
                    val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                    val costReplace = cost[j - 1] + match
                    val costInsert = cost[j] + 1
                    val costDelete = newCost[j - 1] + 1
                    newCost[j] = minOf(costInsert, costDelete, costReplace)
                }
                val swap = cost
                cost = newCost
                newCost = swap
            }
            return cost[lhsLength]
        }

        /**
         * Cleans text and adds natural oral punctuation and micro-pauses
         * to make synthesized speech sound like a warm, fluid companion.
         * Also expands abbreviations, units, currencies, and strips code fences/markdown.
         */
        fun cleanOralText(text: String): String {
            var clean = text
                // Strip code fences: ```python ... ``` -> "un extrait de code"
                .replace(Regex("```[\\s\\S]*?```"), " un extrait de code. ")
                // Strip inline code: `code` -> code
                .replace(Regex("`([^`]+)`"), "$1")
                // Strip markdown formatting, asterisks, hashtags, bullets, tables
                .replace(Regex("[*#_~]"), "")
                .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1") // markdown links -> title
                .replace(Regex("\\[.*?\\]"), "")
                .replace(Regex("(?m)^[-*•]\\s+"), "")
                // Strip URLs: https://... -> sur le web
                .replace(Regex("https?://\\S+"), "sur le web")
                // Strip academic parentheticals like (né en 1920) or (/pɑ.ʁi/)
                .replace(Regex("\\s*\\([^)]*?\\)"), "")
                // Expand currency, degrees, and math symbols BEFORE emoji/symbol stripping
                .replace("€", " euros")
                .replace("$", " dollars")
                .replace("£", " livres")
                .replace("%", " pour cent")
                .replace("°C", " degrés Celsius")
                .replace("°", " degrés ")
                // Strip emojis to prevent robotic unicode name speech
                .replace(Regex("[\\p{So}\\p{Sk}\\p{Cs}]"), "")
                // Expand common abbreviations for fluid oral French & English
                .replace(Regex("(?i)(?<=\\d|\\b)km/h\\b"), " kilomètres par heure")
                .replace(Regex("(?i)(?<=\\d|\\b)km²\\b"), " kilomètres carrés")
                .replace(Regex("(?i)(?<=\\d|\\b)km\\b"), " kilomètres")
                .replace(Regex("(?i)(?<=\\d|\\b)kg\\b"), " kilos")
                .replace(Regex("(?i)(?<=\\d|\\b)cm\\b"), " centimètres")
                .replace(Regex("(?i)(?<=\\d|\\b)mm\\b"), " millimètres")
                .replace(Regex("(?i)\\bc\\.-?à-?d\\.?"), "c'est-à-dire")
                .replace(Regex("(?i)\\bex\\."), "par exemple")
                .replace(Regex("(?i)\\bapprox\\."), "environ")
                .replace(Regex("(?i)\\b1er\\b"), "premier")
                .replace(Regex("(?i)\\b1ère\\b"), "première")
                .replace(Regex("(?i)\\b2e\\b"), "deuxième")
                .replace(Regex("(?i)\\b3e\\b"), "troisième")
                .replace(Regex("(?i)\\bav\\.? j\\.?-c\\.?"), "avant Jésus-Christ")
                .replace(Regex("(?i)\\bap\\.? j\\.?-c\\.?"), "après Jésus-Christ")
                .replace(Regex("(?i)\\bIA\\b"), "I A")
                .replace(Regex("(?i)\\bAI\\b"), "A I")
                // Natural pauses: replace multiple exclamation or question marks
                .replace(Regex("!+"), "!")
                .replace(Regex("\\?+"), "?")
                .replace("...", ", ")
                .replace(Regex("\\s+"), " ")
                .trim()

            return clean
        }

        /**
         * Splits long text into natural sentence-based chunks smaller than [maxCharsPerChunk]
         * so that Android TextToSpeech never truncates text or errors due to length limits.
         */
        fun splitIntoUtteranceChunks(text: String, maxCharsPerChunk: Int = 2800): List<String> {
            val oral = cleanOralText(text)
            if (oral.length <= maxCharsPerChunk) return listOf(oral)

            // Split by sentence boundaries (. ! ? \n)
            val sentences = oral.split(Regex("(?<=[.!?\\n])\\s+"))
            val chunks = mutableListOf<String>()
            var currentChunk = StringBuilder()

            for (sentence in sentences) {
                if (currentChunk.length + sentence.length > maxCharsPerChunk && currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }
                currentChunk.append(sentence).append(" ")
            }
            if (currentChunk.isNotBlank()) {
                chunks.add(currentChunk.toString().trim())
            }
            return if (chunks.isEmpty()) listOf(oral) else chunks
        }
    }
}
