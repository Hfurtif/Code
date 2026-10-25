package com.example.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class ElevenLabsVoice(
    val id: String,
    val name: String,
    val description: String,
    val gender: String, // "female", "male"
    val accent: String = "Neutral / International",
    val samplePhrase: String = "Hello! I am your AI teacher, ready to help you learn English."
)

/**
 * ElevenLabs Audio Service.
 * Provides studio-quality human voice generation using ElevenLabs REST API.
 * Includes local audio caching to conserve API characters (free tier gives 10,000 characters/month).
 */
class ElevenLabsVoiceService(private val context: Context) {

    companion object {
        private const val TAG = "ElevenLabsService"
        private const val BASE_URL = "https://api.elevenlabs.io/v1/text-to-speech"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val PRESET_VOICES = listOf(
            ElevenLabsVoice(
                id = "21m00Tcm4TlvDq8ikWAM",
                name = "Rachel",
                description = "Voix féminine calme, chaleureuse et pédagogue",
                gender = "female",
                accent = "Américain / International",
                samplePhrase = "Hi! I'm Rachel, your English companion. Let's practice speaking today."
            ),
            ElevenLabsVoice(
                id = "pNInz6obpgDQGcFmaJgB",
                name = "Adam",
                description = "Voix masculine profonde, claire et engageante",
                gender = "male",
                accent = "Américain profond",
                samplePhrase = "Hello! I'm Adam. We can discuss any topic, from science to daily life."
            ),
            ElevenLabsVoice(
                id = "ErXwobaYiN019PkySvjV",
                name = "Antoni",
                description = "Voix masculine dynamique et amicale",
                gender = "male",
                accent = "Américain énergique",
                samplePhrase = "Hey there! Ready to boost your English fluency and learn new words?"
            ),
            ElevenLabsVoice(
                id = "EXAVITQu4vr4xnSDxMaL",
                name = "Bella",
                description = "Voix féminine douce, expressive et naturelle",
                gender = "female",
                accent = "International doux",
                samplePhrase = "Welcome back! Take your time, I am here to guide you step by step."
            ),
            ElevenLabsVoice(
                id = "TxGEqnHWrfWFTfGW9XjX",
                name = "Josh",
                description = "Voix jeune, moderne et spontanée",
                gender = "male",
                accent = "Jeune dynamique",
                samplePhrase = "What's up! Let's explore new English expressions and have fun."
            ),
            ElevenLabsVoice(
                id = "AZnzlk1XvdvUeBnXmlld",
                name = "Domi",
                description = "Voix féminine vive, assurée et déterminée",
                gender = "female",
                accent = "International énergique",
                samplePhrase = "Great to see you! Let's dive straight into today's challenge."
            )
        )
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    val isPlayingAudio: Boolean
        get() = isPlaying

    private val cacheDir = File(context.cacheDir, "elevenlabs_audio").apply {
        if (!exists()) mkdirs()
    }

    suspend fun synthesizeAndPlay(
        text: String,
        apiKey: String,
        voiceId: String = "21m00Tcm4TlvDq8ikWAM",
        onPlaybackStarted: () -> Unit = {},
        onPlaybackFinished: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return@withContext false
        if (apiKey.isBlank()) {
            withContext(Dispatchers.Main) {
                onError("Clé API ElevenLabs absente. Bascule automatique sur la voix Google HD.")
            }
            return@withContext false
        }

        try {
            val audioFile = getCachedOrFetchAudio(trimmed, apiKey, voiceId)
            if (audioFile == null || !audioFile.exists() || audioFile.length() == 0L) {
                withContext(Dispatchers.Main) {
                    onError("Échec de la génération audio ElevenLabs. Bascule sur la voix Google HD.")
                }
                return@withContext false
            }

            withContext(Dispatchers.Main) {
                playAudioFile(audioFile, onPlaybackStarted, onPlaybackFinished, onError)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error in ElevenLabs synthesis", e)
            withContext(Dispatchers.Main) {
                onError("Erreur ElevenLabs (${e.message}). Bascule sur voix Google HD.")
            }
            return@withContext false
        }
    }

    private fun getCachedOrFetchAudio(text: String, apiKey: String, voiceId: String): File? {
        val cacheKey = hashText("$voiceId:$text")
        val cacheFile = File(cacheDir, "$cacheKey.mp3")

        // Return cached audio if present and recent
        if (cacheFile.exists() && cacheFile.length() > 200L) {
            return cacheFile
        }

        // Call ElevenLabs API
        val endpoint = "$BASE_URL/$voiceId"
        val payload = JSONObject().apply {
            put("text", text.take(1500))
            put("model_id", "eleven_multilingual_v2")
            val voiceSettings = JSONObject().apply {
                put("stability", 0.55)
                put("similarity_boost", 0.80)
                put("style", 0.20)
                put("use_speaker_boost", true)
            }
            put("voice_settings", voiceSettings)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("xi-api-key", apiKey.trim())
            .addHeader("Accept", "audio/mpeg")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            Log.w(TAG, "ElevenLabs API returned HTTP ${response.code}: $errBody")
            return null
        }

        val body = response.body ?: return null
        val tempFile = File(cacheDir, "$cacheKey.tmp")
        body.byteStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        if (tempFile.exists() && tempFile.length() > 0) {
            tempFile.renameTo(cacheFile)
            return cacheFile
        }
        return null
    }

    private fun playAudioFile(
        file: File,
        onStarted: () -> Unit,
        onFinished: () -> Unit,
        onError: (String) -> Unit
    ) {
        stopPlayback()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    this@ElevenLabsVoiceService.isPlaying = true
                    onStarted()
                }
                setOnCompletionListener {
                    this@ElevenLabsVoiceService.isPlaying = false
                    stopPlayback()
                    onFinished()
                }
                setOnErrorListener { _, what, extra ->
                    this@ElevenLabsVoiceService.isPlaying = false
                    stopPlayback()
                    onError("Erreur lecture audio ($what/$extra)")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file", e)
            this@ElevenLabsVoiceService.isPlaying = false
            onError("Impossible de lire le fichier vocal ElevenLabs")
        }
    }

    fun stopPlayback() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
            isPlaying = false
        }
    }

    fun isSpeaking(): Boolean = isPlaying

    private fun hashText(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
