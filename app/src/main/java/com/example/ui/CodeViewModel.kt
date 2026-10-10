package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ConversationMessage
import com.example.data.model.LearnedError
import com.example.data.model.Lesson
import com.example.data.model.PendingTask
import com.example.data.model.ResourcePack
import com.example.data.model.TutorMode
import com.example.data.model.UserProfile
import com.example.data.model.Vocabulary
import com.example.engine.CodeBrain
import com.example.ui.components.OrbState
import com.example.voice.SpeechManager
import com.example.voice.TtsManager
import com.example.voice.TtsVoiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class CodeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val brain = CodeBrain(application, database)

    // Speech & TTS
    private var speechManager: SpeechManager? = null
    private var ttsManager: TtsManager? = null

    // UI States
    private val _orbState = MutableStateFlow(OrbState.IDLE)
    val orbState: StateFlow<OrbState> = _orbState.asStateFlow()

    private val _statusText = MutableStateFlow("Je suis prêt. Dis-moi tout !")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _liveUserTranscript = MutableStateFlow("")
    val liveUserTranscript: StateFlow<String> = _liveUserTranscript.asStateFlow()

    private val _liveCodeReply = MutableStateFlow("")
    val liveCodeReply: StateFlow<String> = _liveCodeReply.asStateFlow()

    private val _currentCorrectionNote = MutableStateFlow<String?>(null)
    val currentCorrectionNote: StateFlow<String?> = _currentCorrectionNote.asStateFlow()

    private val _selectedMode = MutableStateFlow(TutorMode.ENGLISH)
    val selectedMode: StateFlow<TutorMode> = _selectedMode.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isHistoryVisible = MutableStateFlow(false)
    val isHistoryVisible: StateFlow<Boolean> = _isHistoryVisible.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceInfo>> = _availableVoices.asStateFlow()

    private val _activeLessonPrep = MutableStateFlow<String?>(null)
    val activeLessonPrep: StateFlow<String?> = _activeLessonPrep.asStateFlow()

    private val _storagePromptResource = MutableStateFlow<ResourcePack?>(null)
    val storagePromptResource: StateFlow<ResourcePack?> = _storagePromptResource.asStateFlow()

    private val _infoBannerMessage = MutableStateFlow<String?>(null)
    val infoBannerMessage: StateFlow<String?> = _infoBannerMessage.asStateFlow()

    // Database Flows
    val userProfile: StateFlow<UserProfile> = database.userDao().getUserProfile()
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val allMessages: StateFlow<List<ConversationMessage>> = database.conversationDao().getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLessons: StateFlow<List<Lesson>> = database.lessonDao().getAllLessons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allErrors: StateFlow<List<LearnedError>> = database.learnedErrorDao().getAllErrors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVocabulary: StateFlow<List<Vocabulary>> = database.vocabularyDao().getAllVocabulary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allResources: StateFlow<List<ResourcePack>> = database.resourceDao().getAllResources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTasks: StateFlow<List<PendingTask>> = database.pendingTaskDao().getUnresolvedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initial online check
        _isOnline.value = brain.isOnline()
        if (!_isOnline.value) {
            _orbState.value = OrbState.OFFLINE
        }

        // Initialize Speech Manager
        speechManager = SpeechManager(
            context = application,
            onPartialResult = { partial ->
                _liveUserTranscript.value = partial
                _statusText.value = "Je t'écoute…"
                _orbState.value = OrbState.LISTENING
            },
            onFinalResult = { finalResult ->
                _liveUserTranscript.value = finalResult
                processInput(finalResult)
            },
            onRmsUpdated = { rms ->
                _audioRms.value = rms
            },
            onListeningStateChanged = { listening ->
                _isListening.value = listening
                if (listening) {
                    _orbState.value = OrbState.LISTENING
                    _statusText.value = "Je t'écoute…"
                } else if (!_isSpeaking.value) {
                    _orbState.value = if (_isOnline.value) OrbState.IDLE else OrbState.OFFLINE
                    _statusText.value = "Prêt à t'écouter"
                }
            },
            onErrorOccurred = { error ->
                _infoBannerMessage.value = error
                _orbState.value = if (_isOnline.value) OrbState.IDLE else OrbState.OFFLINE
                _statusText.value = "Micro en pause"
            }
        )

        // Initialize TTS Manager
        ttsManager = TtsManager(
            context = application,
            onInitComplete = { success ->
                if (success) {
                    val voices = ttsManager?.getAvailableVoices() ?: emptyList()
                    _availableVoices.value = voices
                    applyVoicePreferences()
                }
            },
            onSpeakingStateChanged = { speaking ->
                _isSpeaking.value = speaking
                if (speaking) {
                    _orbState.value = OrbState.SPEAKING
                    _statusText.value = "Je parle…"
                } else {
                    _orbState.value = if (_isOnline.value) OrbState.IDLE else OrbState.OFFLINE
                    _statusText.value = "Prêt"
                }
            },
            onSpeechCompleted = {
                // Continuous conversation mode: auto-rearm microphone
                viewModelScope.launch {
                    val profile = userProfile.value
                    if (profile.continuousMode && !_isListening.value) {
                        delay(600) // comfortable pause before mic opens
                        startListening()
                    }
                }
            }
        )

        // Periodic network refresh & pending task resolution
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val currentlyOnline = brain.isOnline()
                if (currentlyOnline != _isOnline.value) {
                    _isOnline.value = currentlyOnline
                    if (currentlyOnline) {
                        resolvePendingTasksIfAny()
                        if (_orbState.value == OrbState.OFFLINE) {
                            _orbState.value = OrbState.IDLE
                        }
                    } else {
                        if (_orbState.value == OrbState.IDLE) {
                            _orbState.value = OrbState.OFFLINE
                        }
                    }
                }
                delay(6000)
            }
        }
    }

    private fun applyVoicePreferences() {
        val profile = userProfile.value
        ttsManager?.setSpeechRate(profile.speechRate)
        ttsManager?.setSpeechPitch(profile.speechPitch)
        if (profile.voiceId.isNotBlank()) {
            ttsManager?.setVoiceByName(profile.voiceId)
        } else {
            ttsManager?.setVoiceByGenderAndAccent(profile.voiceGender, profile.voiceAccent)
        }
    }

    fun startListening() {
        if (_isSpeaking.value) {
            stopSpeaking()
        }
        _isOnline.value = brain.isOnline()
        val lang = if (_selectedMode.value == TutorMode.ENGLISH) "en-US" else "fr-FR"
        _statusText.value = "Je t'écoute…"
        _orbState.value = OrbState.LISTENING
        speechManager?.startListening(lang)
    }

    fun stopListening() {
        speechManager?.stopListening()
        _isListening.value = false
        _orbState.value = if (_isOnline.value) OrbState.IDLE else OrbState.OFFLINE
        _statusText.value = "Prêt"
    }

    fun toggleListening() {
        if (_isListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun stopSpeaking() {
        ttsManager?.stop()
        _isSpeaking.value = false
        _orbState.value = if (_isOnline.value) OrbState.IDLE else OrbState.OFFLINE
        _statusText.value = "Prêt"
    }

    fun processInput(text: String) {
        if (text.isBlank()) return
        stopListening()

        viewModelScope.launch {
            // Save user message to database
            val userMsg = ConversationMessage(
                role = "user",
                text = text,
                mode = _selectedMode.value.name
            )
            database.conversationDao().insertMessage(userMsg)

            // Update UI
            _liveUserTranscript.value = text
            _orbState.value = OrbState.THINKING
            _statusText.value = "Je réfléchis…"

            // Check if user requested a structured lesson prep flow
            val lower = text.lowercase()
            if (lower.contains("apprends-moi") || lower.contains("fais-moi une leçon") || lower.contains("nouvelle leçon")) {
                handleLessonPrepFlow()
                return@launch
            }

            // Brain response
            val response = brain.processUserSpeech(
                userText = text,
                currentMode = _selectedMode.value,
                userProfile = userProfile.value,
                recentMessages = allMessages.value
            )

            // Save Code reply to DB
            val codeMsg = ConversationMessage(
                role = "code",
                text = response.replyText,
                correctionNote = response.correctionNote,
                mode = _selectedMode.value.name
            )
            database.conversationDao().insertMessage(codeMsg)

            // Update state
            _liveCodeReply.value = response.replyText
            _currentCorrectionNote.value = response.correctionNote

            // Check for triggered commands
            when (response.triggeredCommand) {
                "stop" -> stopSpeaking()
                "slower" -> {
                    val newRate = (userProfile.value.speechRate - 0.15f).coerceAtLeast(0.6f)
                    updateVoiceSettings(userProfile.value.voiceGender, userProfile.value.voiceAccent, newRate, userProfile.value.speechPitch, userProfile.value.voiceId)
                    speak(response.replyText)
                }
                "voice_female" -> {
                    updateVoiceSettings("female", userProfile.value.voiceAccent, userProfile.value.speechRate, userProfile.value.speechPitch, "")
                    speak(response.replyText)
                }
                "voice_male" -> {
                    updateVoiceSettings("male", userProfile.value.voiceAccent, userProfile.value.speechRate, userProfile.value.speechPitch, "")
                    speak(response.replyText)
                }
                "storage_cleanup_prompt" -> {
                    val candidate = allResources.value.firstOrNull { it.isDownloaded && !it.isProtected }
                    _storagePromptResource.value = candidate
                    speak(response.replyText)
                }
                else -> {
                    speak(response.replyText)
                }
            }
        }
    }

    private suspend fun handleLessonPrepFlow() {
        val prep1 = "Super ! Laisse-moi me préparer."
        speak(prep1)
        _activeLessonPrep.value = "Recherche des ressources pédagogiques…"
        delay(1600)
        _activeLessonPrep.value = "Téléchargement du pack vocabulaire ciblé…"
        delay(1400)
        _activeLessonPrep.value = "Génération des exercices sur mesure…"
        delay(1200)
        _activeLessonPrep.value = null

        val prepDone = "Je suis prêt, on commence ! Aujourd'hui nous allons pratiquer la commande en restaurant et le vocabulaire des saveurs. Tu es assis à une table à Londres. Que demandes-tu au serveur ?"
        _liveCodeReply.value = prepDone
        speak(prepDone)

        database.conversationDao().insertMessage(
            ConversationMessage(
                role = "code",
                text = prepDone,
                mode = _selectedMode.value.name
            )
        )
    }

    fun speak(text: String) {
        ttsManager?.speak(text)
    }

    fun repeatLastReply() {
        val reply = _liveCodeReply.value
        if (reply.isNotBlank()) {
            speak(reply)
        }
    }

    fun setMode(mode: TutorMode) {
        _selectedMode.value = mode
        val switchText = "Nous passons en mode ${mode.title}. De quoi as-tu envie de parler ?"
        _liveCodeReply.value = switchText
        speak(switchText)
    }

    fun updateVoiceSettings(
        gender: String,
        accent: String,
        rate: Float,
        pitch: Float,
        voiceId: String
    ) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(
                voiceGender = gender,
                voiceAccent = accent,
                speechRate = rate,
                speechPitch = pitch,
                voiceId = voiceId
            )
            database.userDao().insertOrUpdate(updated)
            ttsManager?.setSpeechRate(rate)
            ttsManager?.setSpeechPitch(pitch)
            if (voiceId.isNotBlank()) {
                ttsManager?.setVoiceByName(voiceId)
            } else {
                ttsManager?.setVoiceByGenderAndAccent(gender, accent)
            }
        }
    }

    fun toggleContinuousMode(enabled: Boolean) {
        viewModelScope.launch {
            database.userDao().insertOrUpdate(userProfile.value.copy(continuousMode = enabled))
        }
    }

    fun setCorrectionFrequency(freq: String) {
        viewModelScope.launch {
            database.userDao().insertOrUpdate(userProfile.value.copy(correctionFrequency = freq))
        }
    }

    fun setExplanationLang(lang: String) {
        viewModelScope.launch {
            database.userDao().insertOrUpdate(userProfile.value.copy(explanationLang = lang))
        }
    }

    fun updateProfileLevel(level: String) {
        viewModelScope.launch {
            database.userDao().insertOrUpdate(userProfile.value.copy(level = level))
        }
    }

    fun toggleHistory() {
        _isHistoryVisible.value = !_isHistoryVisible.value
    }

    fun dismissInfoBanner() {
        _infoBannerMessage.value = null
    }

    fun deleteResourceWithConfirmation(resource: ResourcePack) {
        viewModelScope.launch {
            database.resourceDao().markDeleted(resource.id)
            _storagePromptResource.value = null
            _infoBannerMessage.value = "Le pack \"${resource.name}\" a été supprimé (${resource.sizeMb} Mo libérés)."
        }
    }

    fun cancelStoragePrompt() {
        _storagePromptResource.value = null
    }

    fun downloadResource(resource: ResourcePack) {
        viewModelScope.launch {
            database.resourceDao().updateResource(
                resource.copy(
                    isDownloaded = true,
                    downloadDate = System.currentTimeMillis(),
                    lastUsedDate = System.currentTimeMillis()
                )
            )
            _infoBannerMessage.value = "\"${resource.name}\" est maintenant disponible hors ligne !"
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            database.conversationDao().clearHistory()
            _liveCodeReply.value = ""
            _liveUserTranscript.value = ""
            _infoBannerMessage.value = "Historique effacé avec succès."
        }
    }

    private suspend fun resolvePendingTasksIfAny() {
        val tasks = database.pendingTaskDao().getUnresolvedTasks()
        // Handle pending items
        val list = pendingTasks.value
        for (task in list) {
            database.pendingTaskDao().updateTask(
                task.copy(isResolved = true, resolvedAnswer = "Traité en ligne")
            )
        }
    }

    fun exportDataAsJson(): String {
        val root = JSONObject()
        root.put("profileName", userProfile.value.name)
        root.put("level", userProfile.value.level)
        root.put("streakDays", userProfile.value.streakDays)
        root.put("xpScore", userProfile.value.xpScore)

        val vocabArr = JSONArray()
        for (v in allVocabulary.value) {
            val obj = JSONObject()
            obj.put("word", v.word)
            obj.put("translation", v.translation)
            vocabArr.put(obj)
        }
        root.put("vocabulary", vocabArr)
        return root.toString(2)
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
        ttsManager?.destroy()
    }
}
