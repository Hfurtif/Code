package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.model.TutorMode
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.CodeViewModel
import com.example.ui.screens.AddFlashcardDialog
import com.example.ui.screens.CompanionNamingDialog
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.FlashcardsReviewDialog
import com.example.ui.screens.LanguageSelectionDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SourcesScreen
import com.example.ui.screens.UrlLessonDialog
import com.example.ui.screens.VoiceStudioDialog
import com.example.ui.screens.AgenticHarnessDialog
import com.example.ui.screens.VoiceCommandDialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.screens.TeacherOnboardingScreen

sealed class NavTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Conversation : NavTab("conversation", "J.A.R.V.I.S.", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline)
    object Sources : NavTab("sources", "Sources", Icons.Filled.Folder, Icons.Outlined.Folder)
    object Settings : NavTab("settings", "Réglages", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: CodeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContainer(viewModel: CodeViewModel) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val infoBanner by viewModel.infoBannerMessage.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val showOnboardingScreen by viewModel.showOnboardingScreen.collectAsState()
    val isLanguageDialogVisible by viewModel.isLanguageDialogVisible.collectAsState()
    val isCompanionNamingDialogVisible by viewModel.isCompanionNamingDialogVisible.collectAsState()
    val isFlashcardReviewVisible by viewModel.isFlashcardReviewVisible.collectAsState()
    val isAddFlashcardVisible by viewModel.isAddFlashcardVisible.collectAsState()
    val addFlashcardPrefill by viewModel.addFlashcardPrefill.collectAsState()
    val isUrlLessonDialogVisible by viewModel.isUrlLessonDialogVisible.collectAsState()
    val isVoiceStudioVisible by viewModel.isVoiceStudioVisible.collectAsState()
    val isAutoSpeakEnabled by viewModel.isAutoSpeakEnabled.collectAsState()
    val installedTtsEngines by viewModel.installedTtsEngines.collectAsState()
    val isAgenticHarnessDialogVisible by viewModel.isAgenticHarnessDialogVisible.collectAsState()
    val isVoiceCommandDialogVisible by viewModel.isVoiceCommandDialogVisible.collectAsState()
    val isHandsFreeSheetVisible by viewModel.isHandsFreeSheetVisible.collectAsState()
    val agenticState by viewModel.agenticExecutionState.collectAsState()

    val shouldShowOnboarding = !userProfile.hasCompletedOnboarding || showOnboardingScreen

    // Runtime Permission for Microphone
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordAudioPermission = isGranted
        if (isGranted) {
            viewModel.startListening()
        }
    }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.processSpokenVoice(spoken)
            }
        }
    }

    val launchSystemSpeech: () -> Unit = {
        val lang = userProfile.preferredLanguageCode
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Parle à ton assistant Code...")
        }
        try {
            systemSpeechLauncher.launch(intent)
        } catch (e: Exception) {
            viewModel.showVoiceCommandHelp()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasRecordAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (shouldShowOnboarding) {
        TeacherOnboardingScreen(
            viewModel = viewModel,
            onDismiss = if (userProfile.hasCompletedOnboarding) {
                { viewModel.hideOnboarding() }
            } else null
        )
        return
    }

    val tabs = listOf(
        NavTab.Conversation,
        NavTab.Sources,
        NavTab.Settings
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                windowInsets = WindowInsets.navigationBars
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = if (isSelected) NeonCyan else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = NeonCyan.copy(alpha = 0.15f),
                            selectedIconColor = NeonCyan,
                            unselectedIconColor = TextSecondary,
                            selectedTextColor = NeonCyan,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Switcher
            when (selectedTabIndex) {
                0 -> ConversationScreen(
                    viewModel = viewModel,
                    hasRecordAudioPermission = hasRecordAudioPermission,
                    onRequestRecordAudioPermission = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onLaunchSystemSpeech = launchSystemSpeech
                )
                1 -> SourcesScreen(
                    viewModel = viewModel,
                    onNavigateToConversation = { selectedTabIndex = 0 }
                )
                2 -> SettingsScreen(viewModel = viewModel)
            }

            // Info Banner Notification Overlay (if any info/error message)
            infoBanner?.let { message ->
                Surface(
                    color = DarkSurface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = NeonViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.dismissInfoBanner() }) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer", tint = TextSecondary)
                        }
                    }
                }
            }

            // Companion Naming Modal Dialog (if opened manually from Settings)
            if (isCompanionNamingDialogVisible) {
                CompanionNamingDialog(
                    initialName = userProfile.companionName,
                    initialGender = userProfile.companionGender,
                    initialVoiceGender = userProfile.voiceGender,
                    isFirstLaunch = false,
                    onConfirm = { name, gender, voiceGender ->
                        viewModel.setCompanionName(name, gender, voiceGender)
                    },
                    onDismiss = {
                        viewModel.hideCompanionNamingDialog()
                    }
                )
            }

            // Language Selection Modal Dialog (if opened manually)
            if (isLanguageDialogVisible) {
                LanguageSelectionDialog(
                    currentLanguageCode = userProfile.preferredLanguageCode,
                    onSelectLanguage = { lang ->
                        viewModel.selectLanguage(lang)
                    },
                    onDismiss = {
                        viewModel.hideLanguageDialog()
                    },
                    isFirstLaunch = false
                )
            }

            // Flashcards Review Fullscreen Dialog
            if (isFlashcardReviewVisible) {
                FlashcardsReviewDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.hideFlashcardReview() }
                )
            }

            // Add Flashcard Modal Dialog
            if (isAddFlashcardVisible) {
                AddFlashcardDialog(
                    initialWord = addFlashcardPrefill.first,
                    initialExample = addFlashcardPrefill.second,
                    onSaveWord = { word, translation, example ->
                        viewModel.saveWord(word, translation, example)
                    },
                    onSpeakWord = { word ->
                        viewModel.speak(word)
                    },
                    onDismiss = { viewModel.hideAddFlashcard() }
                )
            }

            // URL English Lesson Modal Dialog
            if (isUrlLessonDialogVisible) {
                UrlLessonDialog(
                    companionName = userProfile.companionName.ifBlank { "Code" },
                    onCreateLesson = { url, focus ->
                        viewModel.createEnglishLessonFromUrl(url, focus)
                    },
                    onDismiss = { viewModel.hideUrlLessonDialog() }
                )
            }

            // Voice Studio Dialog (Google HD & ElevenLabs)
            if (isVoiceStudioVisible) {
                VoiceStudioDialog(
                    currentEngine = userProfile.voiceEngine,
                    currentApiKey = userProfile.elevenLabsApiKey,
                    currentVoiceId = userProfile.elevenLabsVoiceId,
                    currentSpeechRate = userProfile.speechRate,
                    currentSpeechPitch = userProfile.speechPitch,
                    installedEngines = installedTtsEngines,
                    isAutoSpeakEnabled = isAutoSpeakEnabled,
                    onToggleAutoSpeak = { viewModel.toggleAutoSpeak() },
                    onSelectEnginePackage = { viewModel.switchTtsEngine(it) },
                    onSaveVoiceConfig = { engine, apiKey, voiceId, rate, pitch ->
                        viewModel.saveVoiceConfig(engine, apiKey, voiceId, rate, pitch)
                    },
                    onPreviewVoice = { sampleText, engine, apiKey, voiceId ->
                        viewModel.previewVoiceSample(sampleText, engine, apiKey, voiceId)
                    },
                    onDismiss = { viewModel.hideVoiceStudio() }
                )
            }

            // Agentic Harness Dialog (Autonomous ReAct loops & trace inspector)
            if (isAgenticHarnessDialogVisible) {
                AgenticHarnessDialog(
                    agenticState = agenticState,
                    isHarnessEnabled = userProfile.isAgenticHarnessEnabled,
                    onToggleHarness = { viewModel.toggleAgenticHarness(it) },
                    onLaunchMission = { viewModel.launchAgenticMission(it) },
                    onDismiss = { viewModel.hideAgenticHarnessDialog() }
                )
            }

            // Voice Commands & STT Help Dialog
            if (isVoiceCommandDialogVisible) {
                VoiceCommandDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.hideVoiceCommandHelp() }
                )
            }

            // Hands-Free Conversation Sheet
            if (isHandsFreeSheetVisible) {
                com.example.ui.components.HandsFreeConversationSheet(
                    viewModel = viewModel,
                    onDismiss = { viewModel.hideHandsFreeSheet() }
                )
            }
        }
    }
}
