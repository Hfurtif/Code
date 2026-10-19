package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Continuous Speech-to-Text manager with on-device / system recognizer support,
 * real-time partial transcription, RMS audio level feedback, and error recovery.
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

    companion object {
        private const val TAG = "SpeechManager"
        private const val MAX_CONSECUTIVE_RESTARTS = 3
    }

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun isOnDeviceAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } else {
            false
        }
    }

    fun startListening(languageCode: String = "fr-FR") {
        mainHandler.post {
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
            val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            ) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }

            speechRecognizer = recognizer.apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (userWantsToRecord) {
                            onListeningStateChanged(true)
                        }
                    }

                    override fun onBeginningOfSpeech() {
                        restartAttemptCount = 0
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
                        // Pause detected in speech. We let onResults fold in the chunk,
                        // and continue listening if user hasn't clicked stop.
                    }

                    override fun onError(error: Int) {
                        Log.d(TAG, "SpeechRecognizer onError: $error (userWantsToRecord=$userWantsToRecord, restarts=$restartAttemptCount)")

                        if (!userWantsToRecord) return

                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            userWantsToRecord = false
                            onListeningStateChanged(false)
                            cleanupRecognizerOnly()
                            onErrorOccurred("Permission microphone requise pour utiliser la reconnaissance vocale.")
                            return
                        }

                        // Fold any in-flight partial text into accumulated text
                        if (lastPartialText.isNotBlank()) {
                            val chunk = lastPartialText.trim()
                            accumulatedText = if (accumulatedText.isBlank()) chunk else "$accumulatedText $chunk"
                            lastPartialText = ""
                            onPartialResult(accumulatedText)
                        }

                        // Check if we hit max consecutive restarts without speech input
                        if (restartAttemptCount >= MAX_CONSECUTIVE_RESTARTS) {
                            val total = accumulatedText.trim()
                            userWantsToRecord = false
                            onListeningStateChanged(false)
                            cleanupRecognizerOnly()

                            if (total.isNotBlank()) {
                                onFinalResult(total)
                            } else {
                                onErrorOccurred("Microphone : fin d'écoute. Tu peux réappuyer sur le micro ou taper ton message.")
                            }
                            return
                        }

                        restartAttemptCount++
                        restartListeningSafely()
                    }

                    override fun onResults(results: Bundle?) {
                        Log.d(TAG, "SpeechRecognizer onResults received")
                        restartAttemptCount = 0

                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenChunk = matches?.firstOrNull()?.trim() ?: lastPartialText.trim()
                        lastPartialText = ""

                        if (spokenChunk.isNotBlank()) {
                            accumulatedText = if (accumulatedText.isBlank()) spokenChunk else "$accumulatedText $spokenChunk"
                            onPartialResult(accumulatedText)
                        }

                        // If user is still recording, continue listening seamlessly
                        if (userWantsToRecord) {
                            restartListeningSafely()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        restartAttemptCount = 0
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
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Relaxed silence thresholds for natural pausing
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speechRecognizer", e)
            if (userWantsToRecord && restartAttemptCount < MAX_CONSECUTIVE_RESTARTS) {
                restartAttemptCount++
                restartListeningSafely()
            } else {
                userWantsToRecord = false
                onListeningStateChanged(false)
                cleanupRecognizerOnly()
                onErrorOccurred("Reconnaissance vocale : impossible de démarrer le service vocal.")
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
                Log.e(TAG, "Error in restartListeningSafely", e)
            }
        }, 150L)
    }

    /**
     * Triggered when the user taps the mic button to commit speech and process command/message.
     */
    fun stopListeningAndCommit() {
        mainHandler.post {
            userWantsToRecord = false
            onListeningStateChanged(false)

            val inFlight = lastPartialText.trim()
            val totalSpoken = if (inFlight.isNotBlank()) {
                if (accumulatedText.isBlank()) inFlight else "$accumulatedText $inFlight"
            } else {
                accumulatedText
            }.trim()

            accumulatedText = ""
            lastPartialText = ""
            restartAttemptCount = 0
            cleanupRecognizerOnly()

            if (totalSpoken.isNotBlank()) {
                onFinalResult(totalSpoken)
            } else {
                onErrorOccurred("Aucune parole détectée. Réessaie en parlant distinctement !")
            }
        }
    }

    fun cancelListening() {
        mainHandler.post {
            userWantsToRecord = false
            accumulatedText = ""
            lastPartialText = ""
            restartAttemptCount = 0
            onListeningStateChanged(false)
            cleanupRecognizerOnly()
        }
    }

    private fun cleanupRecognizerOnly() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speechRecognizer", e)
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
