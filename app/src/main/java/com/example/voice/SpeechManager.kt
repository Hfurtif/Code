package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Continuous WhatsApp-style speech manager.
 * Once activated by the user, it KEEPS LISTENING indefinitely without auto-cutting off,
 * even through pauses, breaths, or silences, until the user EXPLICITLY taps the mic to finish.
 */
class SpeechManager(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onRmsUpdated: (Float) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onErrorOccurred: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var userWantsToRecord = false
    private var currentLanguage = "fr-FR"
    private var accumulatedText = ""
    private var lastPartialText = ""
    private val mainHandler = Handler(Looper.getMainLooper())
    private var restartAttemptCount = 0

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(languageCode: String = "fr-FR") {
        mainHandler.post {
            if (!isAvailable()) {
                onErrorOccurred("Reconnaissance vocale non disponible sur cet appareil. Tu peux utiliser la zone de texte pour discuter avec Code !")
                return@post
            }

            userWantsToRecord = true
            currentLanguage = languageCode
            accumulatedText = ""
            lastPartialText = ""
            restartAttemptCount = 0

            onListeningStateChanged(true)
            initAndStartRecognizer()
        }
    }

    private fun initAndStartRecognizer() {
        if (!userWantsToRecord) return

        cleanupRecognizerOnly()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (userWantsToRecord) {
                            onListeningStateChanged(true)
                        }
                    }

                    override fun onBeginningOfSpeech() {
                        if (userWantsToRecord) {
                            onListeningStateChanged(true)
                        }
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        onRmsUpdated(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        // Crucial: Android's recognizer detects a pause in speech.
                        // DO NOT stop listening! If the user wants to keep recording,
                        // we let onResults deliver words, then seamlessly restart to capture more!
                    }

                    override fun onError(error: Int) {
                        Log.d("SpeechManager", "SpeechRecognizer onError: $error (userWantsToRecord=$userWantsToRecord)")

                        // If user has stopped recording, ignore errors
                        if (!userWantsToRecord) return

                        // If recognizer hit timeout, no match, or transient network/audio error during pause:
                        // NEVER cut off the user! Silently restart listening so user can speak again.
                        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                            error == SpeechRecognizer.ERROR_NO_MATCH ||
                            error == SpeechRecognizer.ERROR_NETWORK ||
                            error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                            error == SpeechRecognizer.ERROR_CLIENT
                        ) {
                            // If we have some partial text in flight, fold it into accumulatedText
                            if (lastPartialText.isNotBlank()) {
                                val chunk = lastPartialText.trim()
                                accumulatedText = if (accumulatedText.isBlank()) chunk else "$accumulatedText $chunk"
                                lastPartialText = ""
                                onPartialResult(accumulatedText)
                            }
                            restartListeningSafely()
                            return
                        }

                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            userWantsToRecord = false
                            onListeningStateChanged(false)
                            cleanupRecognizerOnly()
                            onErrorOccurred("Permission micro requise pour parler avec Code.")
                            return
                        }

                        // For any other unexpected error, attempt to keep listening
                        restartListeningSafely()
                    }

                    override fun onResults(results: Bundle?) {
                        Log.d("SpeechManager", "SpeechRecognizer onResults received")

                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenChunk = matches?.firstOrNull()?.trim() ?: lastPartialText.trim()
                        lastPartialText = ""

                        if (spokenChunk.isNotBlank()) {
                            accumulatedText = if (accumulatedText.isBlank()) spokenChunk else "$accumulatedText $spokenChunk"
                            onPartialResult(accumulatedText)
                        }

                        // If the user has NOT pressed the button to finish:
                        // DO NOT STOP! Keep listening for the next sentence!
                        if (userWantsToRecord) {
                            restartListeningSafely()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim()
                        if (!partial.isNullOrBlank()) {
                            lastPartialText = partial
                            val fullPreview = if (accumulatedText.isBlank()) partial else "$accumulatedText $partial"
                            onPartialResult(fullPreview)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Configure generous pause thresholds
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error starting speechRecognizer", e)
            if (userWantsToRecord) {
                restartListeningSafely()
            }
        }
    }

    private fun restartListeningSafely() {
        if (!userWantsToRecord) return

        mainHandler.postDelayed({
            if (!userWantsToRecord) return@postDelayed
            try {
                initAndStartRecognizer()
            } catch (e: Exception) {
                Log.e("SpeechManager", "Error in restartListeningSafely", e)
            }
        }, 120L)
    }

    /**
     * ONLY triggered when the user explicitly taps the mic button to finish speaking and send!
     */
    fun stopListeningAndCommit() {
        mainHandler.post {
            userWantsToRecord = false
            onListeningStateChanged(false)

            // Gather all text from previous chunks + any final partial text
            val inFlight = lastPartialText.trim()
            val totalSpoken = if (inFlight.isNotBlank()) {
                if (accumulatedText.isBlank()) inFlight else "$accumulatedText $inFlight"
            } else {
                accumulatedText
            }.trim()

            accumulatedText = ""
            lastPartialText = ""
            cleanupRecognizerOnly()

            if (totalSpoken.isNotBlank()) {
                onFinalResult(totalSpoken)
            } else {
                onErrorOccurred("Je n'ai pas capté de son. Appuie sur le gros micro et parle à Code !")
            }
        }
    }

    /**
     * User explicitly cancels the recording (trash/cancel without sending)
     */
    fun cancelListening() {
        mainHandler.post {
            userWantsToRecord = false
            accumulatedText = ""
            lastPartialText = ""
            onListeningStateChanged(false)
            cleanupRecognizerOnly()
        }
    }

    private fun cleanupRecognizerOnly() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error destroying speechRecognizer", e)
        } finally {
            speechRecognizer = null
        }
    }

    fun stopListening() {
        stopListeningAndCommit()
    }

    fun destroy() {
        cancelListening()
    }
}
