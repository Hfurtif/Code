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
import com.example.agent.AgenticExecutionState
import com.example.agent.AgenticHarness
import com.example.voice.SpeechManager
import com.example.voice.TtsManager
import com.example.voice.TtsVoiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    private val _selectedMode = MutableStateFlow(TutorMode.FRIEND)
    val selectedMode: StateFlow<TutorMode> = _selectedMode.asStateFlow()

    private val _isWebSearching = MutableStateFlow(false)
    val isWebSearching: StateFlow<Boolean> = _isWebSearching.asStateFlow()

    private val _webSearchDialogVisible = MutableStateFlow(false)
    val webSearchDialogVisible: StateFlow<Boolean> = _webSearchDialogVisible.asStateFlow()

    private val _isLanguageDialogVisible = MutableStateFlow(false)
    val isLanguageDialogVisible: StateFlow<Boolean> = _isLanguageDialogVisible.asStateFlow()

    private val _isCompanionNamingDialogVisible = MutableStateFlow(false)
    val isCompanionNamingDialogVisible: StateFlow<Boolean> = _isCompanionNamingDialogVisible.asStateFlow()

    private val _showOnboardingScreen = MutableStateFlow(false)
    val showOnboardingScreen: StateFlow<Boolean> = _showOnboardingScreen.asStateFlow()

    private val _isFlashcardReviewVisible = MutableStateFlow(false)
    val isFlashcardReviewVisible: StateFlow<Boolean> = _isFlashcardReviewVisible.asStateFlow()

    private val _isAddFlashcardVisible = MutableStateFlow(false)
    val isAddFlashcardVisible: StateFlow<Boolean> = _isAddFlashcardVisible.asStateFlow()

    private val _addFlashcardPrefill = MutableStateFlow(Pair("", ""))
    val addFlashcardPrefill: StateFlow<Pair<String, String>> = _addFlashcardPrefill.asStateFlow()

    private val _isUrlLessonDialogVisible = MutableStateFlow(false)
    val isUrlLessonDialogVisible: StateFlow<Boolean> = _isUrlLessonDialogVisible.asStateFlow()

    val agenticHarness = AgenticHarness(application, database)
    val agenticExecutionState: StateFlow<AgenticExecutionState> = agenticHarness.executionState

    private val _isVoiceStudioVisible = MutableStateFlow(false)
    val isVoiceStudioVisible: StateFlow<Boolean> = _isVoiceStudioVisible.asStateFlow()

    private val _isAgenticHarnessDialogVisible = MutableStateFlow(false)
    val isAgenticHarnessDialogVisible: StateFlow<Boolean> = _isAgenticHarnessDialogVisible.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var recordingTimerJob: kotlinx.coroutines.Job? = null
    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

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

    private val _isIngestingSource = MutableStateFlow(false)
    val isIngestingSource: StateFlow<Boolean> = _isIngestingSource.asStateFlow()

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

    val allKnowledge: StateFlow<List<com.example.data.model.DownloadedKnowledge>> = database.knowledgeDao().getAllKnowledge()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDocuments: StateFlow<List<com.example.data.model.DownloadedDocument>> = brain.repository.allDocumentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeSourceForDiscussion = MutableStateFlow<com.example.data.model.DownloadedDocument?>(null)
    val activeSourceForDiscussion: StateFlow<com.example.data.model.DownloadedDocument?> = _activeSourceForDiscussion.asStateFlow()

    val totalAssimilatedWords: StateFlow<Int> = allDocuments.map { docs ->
        docs.sumOf { doc -> doc.fullTextContent.split("\\s+".toRegex()).size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Initial online check
        _isOnline.value = brain.isOnline()
        if (!_isOnline.value) {
            _orbState.value = OrbState.OFFLINE
        }

        // Initial auto-boot download for offline readiness from day one
        viewModelScope.launch {
            brain.repository.initializeBootPackIfNeeded { progress, step ->
                if (progress < 1.0f) {
                    _activeLessonPrep.value = "$step (${(progress * 100).toInt()}%)"
                } else {
                    _activeLessonPrep.value = null
                }
            }
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
                    _statusText.value = "En écoute continue… Parle, puis réappuie quand tu as fini !"
                    if (recordingTimerJob == null) {
                        _recordingDurationSeconds.value = 0
                        recordingTimerJob = viewModelScope.launch {
                            while (isActive) {
                                delay(1000L)
                                _recordingDurationSeconds.value += 1
                            }
                        }
                    }
                } else {
                    recordingTimerJob?.cancel()
                    recordingTimerJob = null
                    _recordingDurationSeconds.value = 0
                    if (!_isSpeaking.value) {
                        _orbState.value = if (_isOnline.value) OrbState.IDLE else OrbState.OFFLINE
                        _statusText.value = "Prêt à discuter"
                    }
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
        ttsManager?.setLanguage(profile.preferredLanguageCode)
        ttsManager?.setSpeechRate(profile.speechRate)
        ttsManager?.setSpeechPitch(profile.speechPitch)
        ttsManager?.setVoiceEngineConfig(
            engine = profile.voiceEngine,
            apiKey = profile.elevenLabsApiKey,
            voiceId = profile.elevenLabsVoiceId
        )
        if (profile.voiceId.isNotBlank()) {
            ttsManager?.setVoiceByName(profile.voiceId)
        } else {
            ttsManager?.setVoiceByGenderAndAccent(profile.voiceGender, profile.preferredLanguageCode)
        }
    }

    // Voice Studio & ElevenLabs actions
    fun showVoiceStudio() {
        _isVoiceStudioVisible.value = true
    }

    fun hideVoiceStudio() {
        _isVoiceStudioVisible.value = false
    }

    fun saveVoiceConfig(engine: String, apiKey: String, voiceId: String, rate: Float, pitch: Float) {
        viewModelScope.launch {
            val current = userProfile.value
            val updated = current.copy(
                voiceEngine = engine,
                elevenLabsApiKey = apiKey,
                elevenLabsVoiceId = voiceId,
                speechRate = rate,
                speechPitch = pitch
            )
            database.userDao().insertOrUpdate(updated)
            ttsManager?.setVoiceEngineConfig(engine, apiKey, voiceId)
            ttsManager?.setSpeechRate(rate)
            ttsManager?.setSpeechPitch(pitch)
            _infoBannerMessage.value = if (engine == "elevenlabs") {
                "Moteur vocal ElevenLabs activé !"
            } else {
                "Voix Google HD appliquée."
            }
        }
    }

    fun previewVoiceSample(sampleText: String, engine: String, apiKey: String, voiceId: String) {
        if (engine.equals("elevenlabs", ignoreCase = true) && apiKey.isNotBlank()) {
            viewModelScope.launch {
                _isSpeaking.value = true
                val ok = ttsManager?.elevenLabsService?.synthesizeAndPlay(
                    text = sampleText,
                    apiKey = apiKey,
                    voiceId = voiceId,
                    onPlaybackStarted = { _isSpeaking.value = true },
                    onPlaybackFinished = { _isSpeaking.value = false },
                    onError = {
                        _isSpeaking.value = false
                        ttsManager?.speak(sampleText)
                    }
                ) ?: false
                if (!ok) {
                    _isSpeaking.value = false
                    ttsManager?.speak(sampleText)
                }
            }
        } else {
            ttsManager?.speak(sampleText)
        }
    }

    // Agentic Harness actions
    fun showAgenticHarnessDialog() {
        _isAgenticHarnessDialogVisible.value = true
    }

    fun hideAgenticHarnessDialog() {
        _isAgenticHarnessDialogVisible.value = false
    }

    fun toggleAgenticHarness(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            database.userDao().insertOrUpdate(current.copy(isAgenticHarnessEnabled = enabled))
            _infoBannerMessage.value = if (enabled) "Harnais Agentique activé." else "Harnais Agentique désactivé."
        }
    }

    fun launchAgenticMission(goal: String) {
        viewModelScope.launch {
            _infoBannerMessage.value = "Harnais Agentique : Lancement de la mission..."
            _statusText.value = "Mission agentique..."
            _orbState.value = OrbState.THINKING
            try {
                val state = agenticHarness.runAgenticLoop(
                    userGoal = goal,
                    companionName = userProfile.value.companionName.ifBlank { "Code" }
                )
                _liveCodeReply.value = state.finalSynthesis
                _orbState.value = OrbState.SPEAKING
                speak(state.finalSynthesis)
                database.conversationDao().insertMessage(
                    ConversationMessage(
                        role = "code",
                        text = state.finalSynthesis,
                        mode = "AGENTIC"
                    )
                )
                _infoBannerMessage.value = "Mission terminée ! ${state.extractedFlashcardsCount} flashcards créées."
            } catch (e: Exception) {
                android.util.Log.e("CodeViewModel", "Agentic mission failed", e)
                _infoBannerMessage.value = "Erreur mission agentique : ${e.message}"
            } finally {
                _orbState.value = if (_isOnline.value) OrbState.IDLE else OrbState.OFFLINE
            }
        }
    }

    fun showLanguageDialog() {
        _isLanguageDialogVisible.value = true
    }

    fun hideLanguageDialog() {
        _isLanguageDialogVisible.value = false
    }

    fun showCompanionNamingDialog() {
        _isCompanionNamingDialogVisible.value = true
    }

    fun hideCompanionNamingDialog() {
        _isCompanionNamingDialogVisible.value = false
    }

    fun showOnboarding() {
        _showOnboardingScreen.value = true
    }

    fun hideOnboarding() {
        _showOnboardingScreen.value = false
    }

    // Flashcard & Vocabulary actions
    fun showFlashcardReview() {
        _isFlashcardReviewVisible.value = true
    }

    fun hideFlashcardReview() {
        _isFlashcardReviewVisible.value = false
    }

    fun showAddFlashcard(prefilledWord: String = "", prefilledExample: String = "") {
        _addFlashcardPrefill.value = Pair(prefilledWord, prefilledExample)
        _isAddFlashcardVisible.value = true
    }

    fun hideAddFlashcard() {
        _isAddFlashcardVisible.value = false
    }

    fun saveWord(word: String, translation: String, example: String) {
        viewModelScope.launch {
            val cleanWord = word.trim()
            val cleanTranslation = translation.trim()
            val cleanExample = example.trim()
            if (cleanWord.isNotBlank()) {
                val vocab = Vocabulary(
                    word = cleanWord,
                    translation = cleanTranslation.ifBlank { "À réviser" },
                    example = cleanExample,
                    masteryLevel = 1,
                    dateLearned = System.currentTimeMillis(),
                    nextReviewDate = System.currentTimeMillis() + 86400000L
                )
                database.vocabularyDao().insertWord(vocab)
                _infoBannerMessage.value = "Flashcard enregistrée : « $cleanWord » !"
                _isAddFlashcardVisible.value = false
            }
        }
    }

    fun updateWordMastery(vocab: Vocabulary, levelDelta: Int) {
        viewModelScope.launch {
            val newLevel = (vocab.masteryLevel + levelDelta).coerceIn(1, 5)
            val reviewDelay = when (newLevel) {
                1 -> 86400000L // 1 day
                2 -> 86400000L * 3 // 3 days
                3 -> 86400000L * 7 // 7 days
                4 -> 86400000L * 14 // 14 days
                else -> 86400000L * 30 // 30 days
            }
            val updated = vocab.copy(
                masteryLevel = newLevel,
                nextReviewDate = System.currentTimeMillis() + reviewDelay
            )
            database.vocabularyDao().updateWord(updated)
        }
    }

    fun deleteWord(id: Long) {
        viewModelScope.launch {
            database.vocabularyDao().deleteWordById(id)
            _infoBannerMessage.value = "Flashcard supprimée"
        }
    }

    fun showUrlLessonDialog() {
        _isUrlLessonDialogVisible.value = true
    }

    fun hideUrlLessonDialog() {
        _isUrlLessonDialogVisible.value = false
    }

    fun previewTeacherVoice(sampleText: String, voiceGender: String, voiceAccent: String) {
        ttsManager?.setVoiceByGenderAndAccent(voiceGender, voiceAccent)
        speak(sampleText)
    }

    fun completeOnboarding(
        teacherName: String,
        personaGender: String,
        voiceGender: String,
        voiceAccent: String,
        level: String
    ) {
        viewModelScope.launch {
            val cleanName = teacherName.trim().ifBlank {
                when (personaGender.lowercase()) {
                    "boy" -> "Alex"
                    "girl" -> "Lina"
                    else -> "Code"
                }
            }
            val current = userProfile.value
            val updated = current.copy(
                companionName = cleanName,
                companionGender = personaGender,
                voiceGender = voiceGender,
                voiceAccent = voiceAccent,
                level = level,
                activeRole = "english_teacher",
                hasCompletedOnboarding = true,
                hasNamedCompanion = true,
                hasSelectedInitialLanguage = true
            )
            database.userDao().insertOrUpdate(updated)
            _selectedMode.value = TutorMode.ENGLISH_TEACHER
            _showOnboardingScreen.value = false

            ttsManager?.setVoiceByGenderAndAccent(voiceGender, voiceAccent)

            val greeting = when (personaGender.lowercase()) {
                "boy" -> "Hello! I'm $cleanName, your personal AI English teacher! I'm super excited to practice with you. Let's make your English confident and fluent! How are you doing today?"
                "girl" -> "Hello there! I'm $cleanName, your AI English teacher and conversation partner. Together, we'll master English speaking with confidence and fun! How are you feeling today?"
                else -> "Welcome! I am $cleanName, your personal AI English teacher. I am configured and ready to guide your conversational practice. What would you like to explore today?"
            }

            _liveCodeReply.value = greeting
            _infoBannerMessage.value = "Professeur d'anglais « $cleanName » configuré avec succès !"
            speak(greeting)

            database.conversationDao().insertMessage(
                ConversationMessage(
                    role = "code",
                    text = greeting,
                    mode = TutorMode.ENGLISH_TEACHER.name
                )
            )
        }
    }

    fun setCompanionName(name: String, gender: String, voiceGender: String) {
        viewModelScope.launch {
            val cleanName = name.trim().ifBlank { "Code" }
            val currentProfile = userProfile.value
            val updated = currentProfile.copy(
                companionName = cleanName,
                companionGender = gender,
                voiceGender = voiceGender,
                hasNamedCompanion = true
            )
            database.userDao().insertOrUpdate(updated)
            ttsManager?.setVoiceByGenderAndAccent(voiceGender, updated.preferredLanguageCode)
            _isCompanionNamingDialogVisible.value = false

            val welcomeMsg = when (gender.lowercase()) {
                "boy", "male", "garçon" -> "Enchanté ! À partir de maintenant, je m'appelle $cleanName et je suis ton ami personnel sur ton smartphone dans l'application Code. De quoi aimerais-tu parler ?"
                "girl", "female", "fille" -> "Enchantée ! À partir de maintenant, je m'appelle $cleanName et je suis ton amie personnelle sur ton smartphone dans l'application Code. De quoi as-tu envie de discuter aujourd'hui ?"
                else -> "Ravi de faire ta connaissance ! Je m'appelle $cleanName et je suis ton compagnon personnel dans l'application Code. Que souhaites-tu explorer ou apprendre ?"
            }

            _liveCodeReply.value = welcomeMsg
            _infoBannerMessage.value = "Ton compagnon s'appelle désormais « $cleanName »"
            speak(welcomeMsg)
            database.conversationDao().insertMessage(
                ConversationMessage(
                    role = "code",
                    text = welcomeMsg,
                    mode = _selectedMode.value.name
                )
            )
        }
    }

    fun selectLanguage(language: com.example.data.model.AppLanguage) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(
                preferredLanguageCode = language.code,
                preferredLanguageName = language.displayName,
                voiceAccent = language.code,
                hasSelectedInitialLanguage = true
            )
            database.userDao().insertOrUpdate(updated)
            ttsManager?.setLanguage(language.code)
            _isLanguageDialogVisible.value = false
            val personalizedGreeting = language.formatGreeting(userProfile.value.companionName)
            _liveCodeReply.value = personalizedGreeting
            _statusText.value = "Prêt (${language.displayName})"
            speak(personalizedGreeting)
            database.conversationDao().insertMessage(
                ConversationMessage(
                    role = "code",
                    text = personalizedGreeting,
                    mode = _selectedMode.value.name
                )
            )
        }
    }

    fun updateGeminiApiKey(apiKey: String) {
        viewModelScope.launch {
            val clean = apiKey.trim()
            val updated = userProfile.value.copy(geminiApiKey = clean)
            database.userDao().insertOrUpdate(updated)
            _infoBannerMessage.value = if (clean.isNotBlank()) "Clé Gemini configurée ! IA Cloud active." else "Clé Gemini réinitialisée (Mode local actif)"
        }
    }

    fun expandStorage(amountGb: Double = 20.0) {
        viewModelScope.launch {
            val newLimit = userProfile.value.maxStorageLimitGb + amountGb
            database.userDao().insertOrUpdate(userProfile.value.copy(maxStorageLimitGb = newLimit))
            val msg = "Capacité de stockage étendue à ${newLimit.toInt()} Go sur ton portable !"
            _infoBannerMessage.value = msg
            speak(msg)
        }
    }

    fun ingestSourceFromUrl(url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return
        viewModelScope.launch {
            _isIngestingSource.value = true
            _infoBannerMessage.value = "Téléchargement intégral style NotebookLM..."
            try {
                val result = com.example.data.remote.SourceIngestionService.ingestFromUrl(cleanUrl)
                brain.repository.saveIngestedSource(result)
                _infoBannerMessage.value = "Source « ${result.title} » (${result.wordCount} mots) assimilée style NotebookLM !"
                val spoken = "J'ai téléchargé et mémorisé tout le contenu de ta source ${result.title}. Tu peux me poser n'importe quelle question dessus !"
                speak(spoken)
            } catch (e: Exception) {
                android.util.Log.e("CodeViewModel", "Error ingesting source", e)
                _infoBannerMessage.value = "Impossible de récupérer ce lien pour l'instant."
            } finally {
                _isIngestingSource.value = false
            }
        }
    }

    private fun extractKeyVocabularyFromSource(result: com.example.data.remote.IngestedSourceResult): List<Vocabulary> {
        val now = System.currentTimeMillis()
        val text = result.fullContent
        val potentialTerms = listOf(
            Triple("Insight", "Perspective / Aperçu", "The video provides deep insights into human behavior."),
            Triple("Perspective", "Point de vue", "From this perspective, we see things in a new light."),
            Triple("Resilience", "Capacité à surmonter les épreuves", "Resilience is essential to build lasting success."),
            Triple("Challenge", "Défi stimulant", "She embraced the challenge with confidence."),
            Triple("Breakthrough", "Avancée majeure", "This scientific discovery represents a major breakthrough."),
            Triple("Highlight", "Mettre en valeur / Souligner", "The speaker highlighted three fundamental rules."),
            Triple("Mindset", "État d'esprit", "A positive mindset helps you learn from mistakes.")
        )

        val selected = potentialTerms.filter { text.contains(it.first, ignoreCase = true) }
            .take(3)
            .ifEmpty { potentialTerms.shuffled().take(3) }

        return selected.mapIndexed { idx, (word, translation, example) ->
            Vocabulary(
                word = word,
                translation = translation,
                example = example,
                masteryLevel = 1,
                dateLearned = now,
                nextReviewDate = now + 86400000L * (idx + 1)
            )
        }
    }

    fun createEnglishLessonFromUrl(url: String, focus: String = "conversation") {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return
        viewModelScope.launch {
            _isIngestingSource.value = true
            _isUrlLessonDialogVisible.value = false
            _infoBannerMessage.value = "Analyse de la vidéo/page pour ta leçon d'anglais..."
            try {
                val result = com.example.data.remote.SourceIngestionService.ingestFromUrl(cleanUrl)
                val docId = brain.repository.saveIngestedSource(result)
                val savedDoc = database.documentDao().getDocumentById(docId)
                _activeSourceForDiscussion.value = savedDoc
                _selectedMode.value = TutorMode.ENGLISH_TEACHER

                // Extract 3 vocabulary words and store them in Flashcards
                val extractedWords = extractKeyVocabularyFromSource(result)
                for (vocab in extractedWords) {
                    database.vocabularyDao().insertWord(vocab)
                }

                // Register lesson in local database
                database.lessonDao().insertLesson(
                    com.example.data.model.Lesson(
                        title = "Leçon : ${result.title.take(35)}",
                        type = "url_lesson",
                        subject = result.title,
                        summary = "Leçon d'anglais basée sur : ${result.title} (${result.author}). Focus : $focus.",
                        wordsLearned = extractedWords.size
                    )
                )

                val vocabSummary = extractedWords.joinToString(", ") { "${it.word} (${it.translation})" }
                val lessonIntro = when (result.type) {
                    "youtube" -> "I analyzed this YouTube video: \"${result.title}\" by ${result.author}!"
                    "tiktok" -> "I checked out this TikTok: \"${result.title}\" by ${result.author}!"
                    else -> "I read through this article: \"${result.title}\"!"
                }

                val lessonMessage = buildString {
                    append("$lessonIntro Today, we're using it as context for our English lesson. ")
                    if (extractedWords.isNotEmpty()) {
                        append("I've saved ${extractedWords.size} key words to your Flashcards: $vocabSummary. ")
                    }
                    when (focus) {
                        "vocabulary" -> append("Let's focus on vocabulary: How would you summarize what happened in this content using your own English words?")
                        "quiz" -> append("Quick comprehension question: What was the most important takeaway or argument presented in this content?")
                        else -> append("Let's practice conversational English: What is your personal opinion or reaction to this topic?")
                    }
                }

                _liveCodeReply.value = lessonMessage
                _infoBannerMessage.value = "Leçon d'anglais prête sur « ${result.title} » !"
                speak(lessonMessage)

                database.conversationDao().insertMessage(
                    com.example.data.model.ConversationMessage(
                        role = "code",
                        text = lessonMessage,
                        mode = TutorMode.ENGLISH_TEACHER.name,
                        webSourceUrl = cleanUrl,
                        webSearchTopic = result.title
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("CodeViewModel", "Error creating lesson from url", e)
                _infoBannerMessage.value = "Impossible d'analyser cette URL pour la leçon d'anglais."
            } finally {
                _isIngestingSource.value = false
            }
        }
    }

    fun startEnglishLessonFromSource(doc: com.example.data.model.DownloadedDocument) {
        viewModelScope.launch {
            _activeSourceForDiscussion.value = doc
            _selectedMode.value = TutorMode.ENGLISH_TEACHER
            val intro = "Awesome! Let's practice English using your source: \"${doc.title}\". What is the main idea you remember from this content?"
            _liveCodeReply.value = intro
            _infoBannerMessage.value = "Leçon d'anglais démarrée sur « ${doc.title} » !"
            speak(intro)
            database.conversationDao().insertMessage(
                com.example.data.model.ConversationMessage(
                    role = "code",
                    text = intro,
                    mode = TutorMode.ENGLISH_TEACHER.name,
                    webSourceUrl = doc.authorOrSource,
                    webSearchTopic = doc.title
                )
            )
        }
    }

    fun ingestSourceFromText(title: String, content: String) {
        val cleanContent = content.trim()
        if (cleanContent.isBlank()) return
        viewModelScope.launch {
            _isIngestingSource.value = true
            try {
                val result = com.example.data.remote.SourceIngestionService.ingestFromDocumentText(title, cleanContent)
                brain.repository.saveIngestedSource(result)
                _infoBannerMessage.value = "Document « ${result.title} » (${result.wordCount} mots) mémorisé style NotebookLM !"
                val spoken = "J'ai mémorisé l'intégralité de ton document ${result.title}. Tu peux me poser toutes tes questions dessus !"
                speak(spoken)
            } catch (e: Exception) {
                android.util.Log.e("CodeViewModel", "Error saving document", e)
                _infoBannerMessage.value = "Erreur lors de l'enregistrement du document."
            } finally {
                _isIngestingSource.value = false
            }
        }
    }

    fun ingestDeviceFiles(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        val context = getApplication<Application>()
        viewModelScope.launch {
            _isIngestingSource.value = true
            _infoBannerMessage.value = "Importation de ${uris.size} fichier(s) depuis le téléphone..."
            try {
                val results = com.example.data.remote.LocalDeviceIngestionService.ingestMultipleFileUris(context, uris)
                results.forEach { result ->
                    brain.repository.saveIngestedSource(result)
                }
                val msg = if (results.size == 1) {
                    "« ${results.first().title} » mémorisé dans tes Sources !"
                } else {
                    "${results.size} documents/images mémorisés dans tes Sources !"
                }
                _infoBannerMessage.value = msg
                val spoken = if (results.size == 1) {
                    "J'ai bien enregistré ${results.first().title} dans mes sources hors ligne !"
                } else {
                    "J'ai bien importé tes ${results.size} fichiers. Nous pouvons en discuter !"
                }
                speak(spoken)
            } catch (e: Exception) {
                android.util.Log.e("CodeViewModel", "Error ingesting device files", e)
                _infoBannerMessage.value = "Erreur lors de l'importation des fichiers du téléphone."
            } finally {
                _isIngestingSource.value = false
            }
        }
    }

    fun ingestDeviceFolder(treeUri: android.net.Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _isIngestingSource.value = true
            _infoBannerMessage.value = "Analyse et importation du dossier en cours..."
            try {
                val results = com.example.data.remote.LocalDeviceIngestionService.ingestFolderTree(context, treeUri)
                results.forEach { result ->
                    brain.repository.saveIngestedSource(result)
                }
                val msg = if (results.isNotEmpty()) {
                    "Dossier importé : ${results.size} fichier(s) mémorisé(s) dans tes Sources !"
                } else {
                    "Aucun fichier pris en charge trouvé dans ce dossier."
                }
                _infoBannerMessage.value = msg
                val spoken = if (results.isNotEmpty()) {
                    "J'ai bien scanné et appris les ${results.size} fichiers de ton dossier !"
                } else {
                    "Je n'ai pas trouvé de documents ou d'images à mémoriser dans ce dossier."
                }
                speak(spoken)
            } catch (e: Exception) {
                android.util.Log.e("CodeViewModel", "Error ingesting folder", e)
                _infoBannerMessage.value = "Impossible de lire ce dossier du téléphone."
            } finally {
                _isIngestingSource.value = false
            }
        }
    }

    fun deleteSourceDocument(doc: com.example.data.model.DownloadedDocument) {
        viewModelScope.launch {
            brain.repository.deleteDocument(doc)
            _infoBannerMessage.value = "Source « ${doc.title} » supprimée."
        }
    }

    fun setActiveSource(doc: com.example.data.model.DownloadedDocument?) {
        _activeSourceForDiscussion.value = doc
    }

    fun clearActiveSource() {
        _activeSourceForDiscussion.value = null
    }

    fun startDiscussionOnSource(doc: com.example.data.model.DownloadedDocument, questionType: String? = null) {
        _activeSourceForDiscussion.value = doc
        val query = when (questionType) {
            "summary" -> "Résume-moi l'essentiel de ma source : « ${doc.title} »"
            "quiz" -> "Fais-moi un quiz interactif sur ma source : « ${doc.title} »"
            "key_points" -> "Quels sont les points clés essentiels de : « ${doc.title} » ?"
            "explain" -> "Explique-moi les concepts majeurs décrits dans : « ${doc.title} »"
            else -> "Parlons de ma source : « ${doc.title} »"
        }
        processInput(query)
    }

    fun queryAllSources() {
        processInput("Fais-moi le point sur toutes mes sources et ce que tu as appris")
    }

    fun showWebSearchDialog() {
        _webSearchDialogVisible.value = true
    }

    fun hideWebSearchDialog() {
        _webSearchDialogVisible.value = false
    }

    fun triggerWebSearch(query: String) {
        _webSearchDialogVisible.value = false
        val clean = query.trim()
        if (clean.isNotBlank()) {
            processInput("Fais des recherches sur internet à propos de $clean")
        }
    }

    fun startListening() {
        if (_isSpeaking.value) {
            stopSpeaking()
        }
        _isOnline.value = brain.isOnline()
        val lang = if (_selectedMode.value == TutorMode.ENGLISH_TEACHER) "en-US" else userProfile.value.preferredLanguageCode
        _liveUserTranscript.value = ""
        _statusText.value = "En écoute continue… Parle à ton rythme, réappuie quand tu as fini !"
        _orbState.value = OrbState.LISTENING
        speechManager?.startListening(lang)
    }

    fun stopListeningAndSend() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        _statusText.value = "Envoi à ${userProfile.value.companionName.ifBlank { "Code" }}…"
        _orbState.value = OrbState.THINKING
        speechManager?.stopListeningAndCommit()
    }

    fun cancelRecording() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        _recordingDurationSeconds.value = 0
        speechManager?.cancelListening()
        _isListening.value = false
        _liveUserTranscript.value = ""
        _orbState.value = if (_isOnline.value) OrbState.IDLE else OrbState.OFFLINE
        _statusText.value = "Enregistrement annulé"
    }

    fun stopListening() {
        stopListeningAndSend()
    }

    fun toggleListening() {
        if (_isListening.value) {
            stopListeningAndSend()
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

            val currentProfile = userProfile.value
            if (currentProfile.isAgenticHarnessEnabled && agenticHarness.shouldTriggerAgenticWorkflow(text)) {
                _statusText.value = "Harnais Agentique : Action en cours..."
                try {
                    val agenticResult = agenticHarness.runAgenticLoop(
                        userGoal = text,
                        companionName = currentProfile.companionName.ifBlank { "Code" }
                    )
                    _liveCodeReply.value = agenticResult.finalSynthesis
                    _orbState.value = OrbState.SPEAKING
                    speak(agenticResult.finalSynthesis)
                    database.conversationDao().insertMessage(
                        ConversationMessage(
                            role = "code",
                            text = agenticResult.finalSynthesis,
                            mode = "AGENTIC"
                        )
                    )
                    _infoBannerMessage.value = "Mission achevée (${agenticResult.extractedFlashcardsCount} flashcards ajoutées)."
                } catch (e: Exception) {
                    android.util.Log.e("CodeViewModel", "Agentic harness failed", e)
                    _infoBannerMessage.value = "Erreur mission agentique : ${e.message}"
                } finally {
                    _orbState.value = if (_isOnline.value) OrbState.IDLE else OrbState.OFFLINE
                }
                return@launch
            }

            val isUrl = com.example.data.remote.SourceIngestionService.extractUrl(text) != null
            if (isUrl) {
                _statusText.value = "📥 Analyse et mémorisation de la source…"
            } else {
                _statusText.value = "Je réfléchis…"
            }

            // Brain response
            val response = brain.processUserSpeech(
                userText = text,
                currentMode = _selectedMode.value,
                userProfile = userProfile.value,
                recentMessages = allMessages.value,
                preferredDocId = _activeSourceForDiscussion.value?.id
            )

            _isWebSearching.value = false

            // Check if mode switch was requested by speech
            if (response.newMode != null) {
                _selectedMode.value = response.newMode
            }

            // Check if companion was renamed by speech
            if (response.newCompanionName != null) {
                val clean = response.newCompanionName.trim()
                database.userDao().insertOrUpdate(userProfile.value.copy(companionName = clean, hasNamedCompanion = true))
                _infoBannerMessage.value = "Nom du compagnon mis à jour : « $clean »"
            }

            // Save reply to DB with web search details
            val codeMsg = ConversationMessage(
                role = "code",
                text = response.replyText,
                correctionNote = response.correctionNote,
                mode = _selectedMode.value.name,
                isWebSearch = response.isWebSearch,
                webSearchTopic = response.webSearchTopic,
                webSourceUrl = response.webSourceUrl
            )
            database.conversationDao().insertMessage(codeMsg)

            if (response.isWebSearch && !response.webSearchTopic.isNullOrBlank()) {
                _infoBannerMessage.value = "🌐 Recherche Web effectuée sur « ${response.webSearchTopic} »"
            }

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
                "faster" -> {
                    val newRate = (userProfile.value.speechRate + 0.15f).coerceAtMost(1.4f)
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
                "expand_storage_20gb" -> {
                    speak(response.replyText)
                    _infoBannerMessage.value = "Capacité de stockage étendue de +20 Go avec ton autorisation !"
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

    fun downloadNewKnowledge(topic: String) {
        if (topic.isBlank()) return
        viewModelScope.launch {
            if (_isOnline.value) {
                _activeLessonPrep.value = "Recherche et téléchargement du cours sur $topic…"
                _orbState.value = OrbState.THINKING
                val knowledge = brain.downloadAndSaveKnowledge(topic)
                _activeLessonPrep.value = null
                _orbState.value = OrbState.IDLE
                val msg = "Le cours sur \"${knowledge.topic}\" (${knowledge.category}) a été téléchargé et sauvegardé sur ton portable !"
                _infoBannerMessage.value = msg
                _liveCodeReply.value = msg
                speak(msg)
            } else {
                database.pendingTaskDao().insertTask(PendingTask(query = "Télécharger : $topic"))
                val msg = "Hors ligne : la demande pour \"$topic\" a été mise en attente. Elle sera téléchargée dès ton retour en ligne !"
                _infoBannerMessage.value = msg
                speak(msg)
            }
        }
    }

    fun startDiscussionOnKnowledge(knowledge: com.example.data.model.DownloadedKnowledge) {
        viewModelScope.launch {
            val text = "Parlons de ${knowledge.topic}. ${knowledge.summary} Que souhaites-tu explorer en premier ?"
            _liveCodeReply.value = text
            _statusText.value = "Discussion : ${knowledge.topic}"
            speak(text)
            database.conversationDao().insertMessage(
                ConversationMessage(
                    role = "code",
                    text = text,
                    mode = _selectedMode.value.name
                )
            )
        }
    }

    fun startDiscussionOnDocument(document: com.example.data.model.DownloadedDocument) {
        viewModelScope.launch {
            val text = "J'ai ouvert le document \"${document.title}\" (${document.topicCategory}). ${document.summary} Que veux-tu qu'on approfondisse ensemble ?"
            _liveCodeReply.value = text
            _statusText.value = "Document : ${document.title}"
            speak(text)
            database.conversationDao().insertMessage(
                ConversationMessage(
                    role = "code",
                    text = text,
                    mode = _selectedMode.value.name
                )
            )
        }
    }

    fun deleteDocument(document: com.example.data.model.DownloadedDocument) {
        viewModelScope.launch {
            brain.repository.deleteDocument(document)
            _infoBannerMessage.value = "Le document \"${document.title}\" a été supprimé (${document.sizeMb} Mo libérés)."
        }
    }

    fun deleteKnowledge(knowledge: com.example.data.model.DownloadedKnowledge) {
        viewModelScope.launch {
            brain.repository.deleteKnowledge(knowledge)
            _infoBannerMessage.value = "Le cours \"${knowledge.topic}\" a été supprimé (${knowledge.sizeMb} Mo libérés)."
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

    fun syncAllPendingTasks() {
        viewModelScope.launch {
            _isOnline.value = brain.isOnline()
            if (!_isOnline.value) {
                _infoBannerMessage.value = "Tu es actuellement hors ligne. Connecte-toi à Internet pour synchroniser les recherches."
                return@launch
            }
            val count = pendingTasks.value.size
            if (count == 0) {
                _infoBannerMessage.value = "Aucune tâche en attente."
                return@launch
            }
            _activeLessonPrep.value = "Téléchargement de $count recherche(s) en attente…"
            resolvePendingTasksIfAny()
            _activeLessonPrep.value = null
            val msg = "Toutes les recherches et documents en attente ont été téléchargés et enregistrés sur ton téléphone !"
            _infoBannerMessage.value = msg
            speak(msg)
        }
    }

    fun deletePendingTask(task: PendingTask) {
        viewModelScope.launch {
            database.pendingTaskDao().updateTask(task.copy(isResolved = true, resolvedAnswer = "Annulé par l'utilisateur"))
            _infoBannerMessage.value = "Demande retirée de la liste."
        }
    }

    suspend fun resolvePendingTasksIfAny() {
        val list = pendingTasks.value
        for (task in list) {
            val topic = if (task.query.startsWith("Télécharger :")) {
                task.query.removePrefix("Télécharger :").trim()
            } else if (task.query.startsWith("Rechercher et télécharger :")) {
                task.query.removePrefix("Rechercher et télécharger :").trim()
            } else {
                task.query.trim()
            }
            if (topic.isNotBlank()) {
                brain.downloadAndSaveKnowledge(topic)
                _infoBannerMessage.value = "Connexion rétablie : les recherches et documents sur \"$topic\" ont été téléchargés avec succès !"
            }
            database.pendingTaskDao().updateTask(
                task.copy(isResolved = true, resolvedAnswer = "Téléchargé et stocké en local")
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
