package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Production-ready Android SpeechRecognizer manager providing:
 * - On-Device offline recognition when available (Android 12+) with fallback to standard service
 * - Real-time streaming partial transcripts
 * - Live microphone audio level (RMS) for reactive visual waveforms
 * - Detailed error handling with user-friendly diagnostics
 * - Haptic feedback on start and stop
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
        private const val MAX_CONSECUTIVE_RESTARTS = 2
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
            if (!isAvailable()) {
                onErrorOccurred("Reconnaissance vocale in-app indisponible sur cet appareil. Utilise la dictée système ou le clavier !")
                onListeningStateChanged(false)
                return@post
            }

            userWantsToRecord = true
            currentLanguage = languageCode
            accumulatedText = ""
            lastPartialText = ""
            restartAttemptCount = 0

            triggerHapticFeedback()
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
                        Log.d(TAG, "SpeechRecognizer onReadyForSpeech")
                        if (userWantsToRecord) {
                            onListeningStateChanged(true)
                        }
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "SpeechRecognizer onBeginningOfSpeech")
                        restartAttemptCount = 0
                        if (userWantsToRecord) {
                            onListeningStateChanged(true)
                        }
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize dB (-2 to 10 typical) to 0.0 .. 1.0 range
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        onRmsUpdated(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "SpeechRecognizer onEndOfSpeech")
                    }

                    override fun onError(error: Int) {
                        Log.d(TAG, "SpeechRecognizer onError: $error (${getErrorMessage(error)})")

                        if (!userWantsToRecord) return

                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            userWantsToRecord = false
                            onListeningStateChanged(false)
                            cleanupRecognizerOnly()
                            onErrorOccurred("Permission microphone requise pour interagir par la voix.")
                            return
                        }

                        // Collect any in-flight partial text
                        if (lastPartialText.isNotBlank()) {
                            val chunk = lastPartialText.trim()
                            accumulatedText = if (accumulatedText.isBlank()) chunk else "$accumulatedText $chunk"
                            lastPartialText = ""
                            onPartialResult(accumulatedText)
                        }

                        // If user has already spoken something, deliver it as final result on timeout/no_match
                        if (accumulatedText.isNotBlank()) {
                            val total = accumulatedText.trim()
                            userWantsToRecord = false
                            accumulatedText = ""
                            onListeningStateChanged(false)
                            cleanupRecognizerOnly()
                            triggerHapticFeedback()
                            onFinalResult(total)
                            return
                        }

                        // Check if we hit max consecutive restarts without speech input
                        if (restartAttemptCount >= MAX_CONSECUTIVE_RESTARTS) {
                            userWantsToRecord = false
                            onListeningStateChanged(false)
                            cleanupRecognizerOnly()
                            onErrorOccurred(getErrorMessage(error))
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

                        // If the user hasn't explicitly clicked to stop, continue listening seamlessly
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

            // Build speech recognition intent
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in initAndStartRecognizer", e)
            if (userWantsToRecord && restartAttemptCount < MAX_CONSECUTIVE_RESTARTS) {
                restartAttemptCount++
                restartListeningSafely()
            } else {
                userWantsToRecord = false
                onListeningStateChanged(false)
                cleanupRecognizerOnly()
                onErrorOccurred("Impossible d'initialiser le SpeechRecognizer Android (${e.message ?: "erreur"}).")
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
     * Triggered when the user taps the mic button to commit speech and execute command/query.
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
            triggerHapticFeedback()

            if (totalSpoken.isNotBlank()) {
                onFinalResult(totalSpoken)
            } else {
                onErrorOccurred("Aucune parole captée. Réessaie en parlant distinctement !")
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

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(40)
                }
            }
        } catch (e: Exception) {
            // Haptics optional
        }
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Erreur d'enregistrement audio. Vérifie ton microphone."
            SpeechRecognizer.ERROR_CLIENT -> "Service vocal temporairement indisponible."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission micro requise pour écouter ta voix."
            SpeechRecognizer.ERROR_NETWORK -> "Connexion internet requise pour la reconnaissance vocale."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Délai réseau dépassé. Réessaie."
            SpeechRecognizer.ERROR_NO_MATCH -> "Aucun mot reconnu. Parle bien distinctement !"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service de reconnaissance vocale occupé."
            SpeechRecognizer.ERROR_SERVER -> "Erreur du serveur de reconnaissance vocale."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Aucun son capté. Réappuie sur le micro quand tu es prêt !"
            10 -> "Trop de requêtes au service vocal. Patiente un instant."
            11 -> "Serveur vocal déconnecté."
            else -> "Erreur vocale ($errorCode)."
        }
    }

    fun stopListening() {
        stopListeningAndCommit()
    }

    fun destroy() {
        cancelListening()
    }
}
