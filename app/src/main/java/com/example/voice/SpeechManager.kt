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
    private var lastPartialText: String = ""
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(languageCode: String = "fr-FR") {
        mainHandler.post {
            if (!isAvailable()) {
                onErrorOccurred("Reconnaissance vocale non disponible sur cet appareil. Tu peux utiliser la zone de texte pour discuter avec Lilian !")
                return@post
            }

            stopListeningInternal()
            lastPartialText = ""

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

                            // If we captured speech during partial recognition, use it instead of failing!
                            if (lastPartialText.isNotBlank()) {
                                val recovered = lastPartialText
                                lastPartialText = ""
                                onFinalResult(recovered)
                                return
                            }

                            val errorMsg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "Je n'ai pas bien entendu ce que tu as dit. Peux-tu répéter ?"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Je ne t'entends plus mon ami. Appuie sur le micro quand tu veux parler !"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission micro requise pour parler avec Lilian."
                                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Micro hors ligne activé."
                                else -> "Micro en pause. Tu peux réappuyer pour parler."
                            }
                            Log.d("SpeechManager", "SpeechRecognizer error: $error -> $errorMsg")
                            onErrorOccurred(errorMsg)
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            onListeningStateChanged(false)
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val spokenText = matches?.firstOrNull()?.trim()
                            if (!spokenText.isNullOrBlank()) {
                                lastPartialText = ""
                                onFinalResult(spokenText)
                            } else if (lastPartialText.isNotBlank()) {
                                val recovered = lastPartialText
                                lastPartialText = ""
                                onFinalResult(recovered)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull()?.trim()
                            if (!partial.isNullOrBlank()) {
                                lastPartialText = partial
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
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1400L)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                isListening = false
                onListeningStateChanged(false)
                onErrorOccurred("Erreur lors de l'activation du micro : ${e.localizedMessage}")
            }
        }
    }

    private fun stopListeningInternal() {
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

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
        }
    }

    fun destroy() {
        stopListening()
    }
}
