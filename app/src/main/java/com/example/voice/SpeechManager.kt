package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onRmsUpdated: (Float) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onErrorOccurred: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(languageCode: String = "en-US") {
        if (!isAvailable()) {
            onErrorOccurred("La reconnaissance vocale n'est pas installée sur cet appareil. Tu peux utiliser le clavier ci-dessous.")
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        onListeningStateChanged(true)
                    }

                    override fun onBeginningOfSpeech() {
                        isListening = true
                        onListeningStateChanged(true)
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize roughly between 0.0f and 1.0f
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        onRmsUpdated(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        onListeningStateChanged(false)
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        onListeningStateChanged(false)
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Je n'ai pas bien compris. Peux-tu répéter ?"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Je ne t'entends plus. Dis-moi quand tu es prêt !"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission micro requise pour parler."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Micro hors ligne activé."
                            else -> "Prêt pour la suite."
                        }
                        Log.d("SpeechManager", "Error code: $error -> $errorMsg")
                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            onErrorOccurred(errorMsg)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        onListeningStateChanged(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()?.trim()
                        if (!spokenText.isNullOrBlank()) {
                            onFinalResult(spokenText)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim()
                        if (!partial.isNullOrBlank()) {
                            onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            onListeningStateChanged(false)
            onErrorOccurred("Impossible d'activer le micro : ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            if (isListening) {
                speechRecognizer?.stopListening()
            }
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error stopping", e)
        } finally {
            speechRecognizer = null
            isListening = false
            onListeningStateChanged(false)
        }
    }

    fun destroy() {
        stopListening()
    }
}
