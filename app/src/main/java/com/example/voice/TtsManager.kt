package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.model.AppLanguage
import java.util.Locale

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
                // Try language only without country
                tts?.setLanguage(Locale(targetLocale.language))
            }

            // Pick matching voice if available
            val voices = tts?.voices ?: emptySet()
            val matchingVoice = voices.firstOrNull {
                it.locale.language.equals(targetLocale.language, ignoreCase = true) &&
                        !it.isNetworkConnectionRequired
            } ?: voices.firstOrNull {
                it.locale.language.equals(targetLocale.language, ignoreCase = true)
            }

            if (matchingVoice != null) {
                tts?.voice = matchingVoice
                currentVoiceName = matchingVoice.name
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
        val selected = voices.firstOrNull { voice ->
            val nameLower = voice.name.lowercase()
            if (isTargetFemale) {
                nameLower.contains("female") || nameLower.contains("fem") || nameLower.contains("f")
            } else {
                nameLower.contains("male") || nameLower.contains("man") || nameLower.contains("m")
            }
        } ?: voices.firstOrNull()

        if (selected != null) {
            tts?.voice = selected
            currentVoiceName = selected.name
        } else {
            tts?.setLanguage(targetLocale)
        }
    }

    fun speak(text: String) {
        val cleanOralText = text
            .replace(Regex("[*#_`~]"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("(?m)^[-*•]\\s+"), "")
            .trim()

        if (cleanOralText.isBlank()) return

        if (!isInitialized) {
            pendingSpeechText = cleanOralText
            return
        }

        stop()
        val utteranceId = "lilian_utterance_${System.currentTimeMillis()}"
        val res = tts?.speak(cleanOralText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (res == TextToSpeech.SUCCESS) {
            onSpeakingStateChanged(true)
        } else {
            Log.e("TtsManager", "TTS speak returned error code: $res")
        }
    }

    fun stop() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
                onSpeakingStateChanged(false)
            }
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
