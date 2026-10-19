package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.model.AppLanguage
import java.util.Locale

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class TtsVoiceInfo(
    val id: String,
    val displayName: String,
    val locale: Locale,
    val isFemale: Boolean,
    val isMale: Boolean
)

class TtsManager(
    private val context: Context,
    private val onInitComplete: (Boolean) -> Unit,
    private val onSpeakingStateChanged: (Boolean) -> Unit,
    private val onSpeechCompleted: () -> Unit
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
    private var currentVoiceName: String? = null
    private var currentLanguageCode: String = "fr-FR"
    private var pendingSpeechText: String? = null

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("TtsManager", "Failed to construct TextToSpeech", e)
            onInitComplete(false)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            applyCurrentLanguage()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                    onSpeechCompleted()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    onSpeakingStateChanged(false)
                }
            })
            onInitComplete(true)

            // Speak queued speech if any was requested prior to initialization
            pendingSpeechText?.let { pending ->
                pendingSpeechText = null
                speak(pending)
            }
        } else {
            isInitialized = false
            onInitComplete(false)
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
                // Fallback to language code
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
            Log.e("TtsManager", "Error setting language on TTS", e)
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
                isMale = isMale || (!isFemale && voice.name.hashCode() % 2 != 0)
            )
        }
    }

    fun setSpeechRate(rate: Float) {
        currentRate = rate.coerceIn(0.6f, 1.4f)
        tts?.setSpeechRate(currentRate)
    }

    fun setSpeechPitch(pitch: Float) {
        currentPitch = pitch.coerceIn(0.7f, 1.3f)
        tts?.setPitch(currentPitch)
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

    /**
     * Cleans text and adds natural oral punctuation and micro-pauses
     * to make synthesized speech sound like a warm, fluid friend chatting.
     */
    fun cleanOralText(text: String): String {
        var clean = text
            // Strip markdown, asterisks, hashtags, bullets
            .replace(Regex("[*#_`~]"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("(?m)^[-*•]\\s+"), "")
            // Strip technical URL protocols
            .replace(Regex("https?://\\S+"), "sur le web")
            // Strip academic parentheticals like (né en 1920) or (/pɑ.ʁi/)
            .replace(Regex("\\s*\\([^)]*?\\)"), "")
            // Expand common abbreviations for fluid French pronunciation
            .replace(Regex("(?i)\\bkm/h\\b"), "kilomètres par heure")
            .replace(Regex("(?i)\\bkm²\\b"), "kilomètres carrés")
            .replace(Regex("(?i)\\bkm\\b"), "kilomètres")
            .replace(Regex("(?i)\\bkg\\b"), "kilos")
            .replace(Regex("(?i)\\bc.-à-d.\\b"), "c'est-à-dire")
            .replace(Regex("(?i)\\bex.\\b"), "par exemple")
            .replace(Regex("(?i)\\bapprox.\\b"), "environ")
            .replace(Regex("(?i)\\b1er\\b"), "premier")
            .replace(Regex("(?i)\\b1ère\\b"), "première")
            .replace(Regex("(?i)\\b2e\\b"), "deuxième")
            .replace(Regex("(?i)\\b3e\\b"), "troisième")
            .replace(Regex("(?i)\\bav. j.-c.\\b"), "avant Jésus-Christ")
            .replace(Regex("(?i)\\bap. j.-c.\\b"), "après Jésus-Christ")
            .replace(Regex("(?i)\\bIA\\b"), "I A")
            .replace("%", " pour cent")
            .replace("°C", " degrés Celsius")
            // Natural pauses: replace multiple exclamation or question marks
            .replace(Regex("!+"), "!")
            .replace(Regex("\\?+"), "?")
            .replace("...", ", ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return clean
    }

    fun setVoiceEngineConfig(engine: String, apiKey: String, voiceId: String) {
        voiceEngine = engine
        elevenLabsApiKey = apiKey
        elevenLabsVoiceId = voiceId
    }

    fun speak(text: String) {
        val oralText = cleanOralText(text)
        if (oralText.isBlank()) return

        stop()

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
                        Log.w("TtsManager", "ElevenLabs failed: $errMsg. Falling back to Google HD.")
                        fallbackGoogleSpeak(oralText)
                    }
                )
                if (!success) {
                    fallbackGoogleSpeak(oralText)
                }
            }
            return
        }

        fallbackGoogleSpeak(oralText)
    }

    private fun fallbackGoogleSpeak(oralText: String) {
        if (!isInitialized) {
            pendingSpeechText = oralText
            return
        }

        val utteranceId = "code_utterance_${System.currentTimeMillis()}"
        val res = tts?.speak(oralText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (res == TextToSpeech.SUCCESS) {
            onSpeakingStateChanged(true)
        } else {
            Log.e("TtsManager", "TTS speak returned error code: $res")
        }
    }

    fun stop() {
        try {
            elevenLabsService.stopPlayback()
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
            onSpeakingStateChanged(false)
        } catch (e: Exception) {
            Log.e("TtsManager", "Error stopping TTS", e)
        }
    }

    fun destroy() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
