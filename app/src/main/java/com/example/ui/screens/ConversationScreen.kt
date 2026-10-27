package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TutorMode
import com.example.ui.CodeViewModel
import com.example.ui.components.AnimatedOrb
import com.example.ui.components.GlassCard
import com.example.ui.components.OrbState
import com.example.ui.theme.AlertPink
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardBorderViolet
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.OfflineAmber
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ConversationScreen(
    viewModel: CodeViewModel,
    hasRecordAudioPermission: Boolean = true,
    onRequestRecordAudioPermission: () -> Unit = {},
    onLaunchSystemSpeech: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val onMicClick: () -> Unit = {
        if (!hasRecordAudioPermission) {
            onRequestRecordAudioPermission()
        } else {
            viewModel.toggleListening()
        }
    }

    val orbState by viewModel.orbState.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val liveTranscript by viewModel.liveUserTranscript.collectAsState()
    val liveReply by viewModel.liveCodeReply.collectAsState()
    val correctionNote by viewModel.currentCorrectionNote.collectAsState()
    val currentMode by viewModel.selectedMode.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isAutoSpeakEnabled by viewModel.isAutoSpeakEnabled.collectAsState()
    val currentlySpokenText by viewModel.currentlySpokenText.collectAsState()
    val currentSpokenWord by viewModel.currentSpokenWord.collectAsState()
    val isHistoryVisible by viewModel.isHistoryVisible.collectAsState()
    val isIngestingSource by viewModel.isIngestingSource.collectAsState()
    val storagePromptResource by viewModel.storagePromptResource.collectAsState()
    val allMessages by viewModel.allMessages.collectAsState()
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val activeSource by viewModel.activeSourceForDiscussion.collectAsState()
    val companionDisplayName = userProfile.companionName.ifBlank { "Code" }
    val vocabList by viewModel.allVocabulary.collectAsState()
    val agenticState by viewModel.agenticExecutionState.collectAsState()
    val lastVoiceCommandBanner by viewModel.lastVoiceCommandBanner.collectAsState()
    val isHandsFreeMode by viewModel.isHandsFreeMode.collectAsState()
    val isHandsFreeSheetVisible by viewModel.isHandsFreeSheetVisible.collectAsState()
    val hudDisplayCards by viewModel.hudDisplayCards.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showAddSourceDialog by remember { mutableStateOf(false) }
    val historyListState = rememberLazyListState()

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.ingestDeviceFiles(uris)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.ingestDeviceFiles(uris)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.ingestDeviceFolder(treeUri)
        }
    }

    LaunchedEffect(allMessages.size) {
        if (allMessages.isNotEmpty()) {
            historyListState.animateScrollToItem(allMessages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // --- JARVIS Mark VII HUD Corner Brackets ---
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(20.dp)
        ) {
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = 2.dp.toPx())
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(0f, size.height), strokeWidth = 2.dp.toPx())
        }
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(20.dp)
        ) {
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = 2.dp.toPx())
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(size.width, 0f), end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = 2.dp.toPx())
        }
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .size(20.dp)
        ) {
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = 2.dp.toPx())
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(0f, size.height), strokeWidth = 2.dp.toPx())
        }
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(20.dp)
        ) {
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = 2.dp.toPx())
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(size.width, 0f), end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = 2.dp.toPx())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP BAR: App branding & Top-Right Badges (Streak Flame & Online Status) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Companion Avatar & Branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(
                                androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    Brush.linearGradient(listOf(NeonCyan, NeonMagenta, NeonViolet))
                                ),
                                CircleShape
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_icon),
                            contentDescription = "Icône $companionDisplayName",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column {
                        Text(
                            text = companionDisplayName.uppercase(),
                            color = NeonCyan,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "COMPAGNON VOCAL IA",
                            color = NeonViolet,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            maxLines = 1
                        )
                    }
                }

                // Badges en haut à droite: Indicateur de série (Flamme) & Indicateur En ligne
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    // Badge Série / Streak avec Flamme
                    Surface(
                        color = OfflineAmber.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OfflineAmber.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Série de jours",
                                tint = OfflineAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${userProfile.streakDays} j",
                                color = OfflineAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Badge Statut de Connexion (En ligne / Hors ligne)
                    Surface(
                        color = if (isOnline) DarkSurfaceVariant else OfflineAmber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isOnline) SuccessGreen.copy(alpha = 0.6f) else OfflineAmber
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = if (isOnline) SuccessGreen else OfflineAmber,
                                modifier = Modifier.size(8.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOnline) "En ligne" else "Hors ligne",
                                color = if (isOnline) TextPrimary else OfflineAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // --- LIGNE DE BOUTONS D'ACTIONS (Horizontal Scrollable pour empêcher tout écrasement) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hands-Free English Practice Button
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .defaultMinSize(minWidth = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.startHandsFreeConversation() }
                        .testTag("top_hands_free_button"),
                    color = if (isHandsFreeMode) SuccessGreen.copy(alpha = 0.25f) else ElectricBlue.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isHandsFreeMode) SuccessGreen else ElectricBlue.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🇬🇧",
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHandsFreeMode) "Mains Libres Actif" else "Mains Libres",
                            color = if (isHandsFreeMode) SuccessGreen else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // J.A.R.V.I.S. Mode Button (Iron Man Mark VII Butler)
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .defaultMinSize(minWidth = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.setMode(com.example.data.model.TutorMode.JARVIS) }
                        .testTag("top_jarvis_mode_button"),
                    color = if (currentMode == com.example.data.model.TutorMode.JARVIS) NeonCyan.copy(alpha = 0.3f) else DarkSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (currentMode == com.example.data.model.TutorMode.JARVIS) NeonCyan else DarkCardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = "Mode J.A.R.V.I.S.",
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentMode == com.example.data.model.TutorMode.JARVIS) "J.A.R.V.I.S. Actif" else "J.A.R.V.I.S.",
                            color = if (currentMode == com.example.data.model.TutorMode.JARVIS) NeonCyan else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }


                // Flashcards Review Button
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .defaultMinSize(minWidth = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.showFlashcardReview() }
                        .testTag("top_flashcards_button"),
                    color = NeonViolet.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Flashcards",
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Flashcards (${vocabList.size})",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // URL Lesson (YouTube, TikTok, Web) Button
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .defaultMinSize(minWidth = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.showUrlLessonDialog() }
                        .testTag("top_url_lesson_button"),
                    color = ElectricBlue.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Leçon URL",
                            tint = ElectricBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lien URL",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Harnais Agentique Button
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .defaultMinSize(minWidth = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.showAgenticHarnessDialog() }
                        .testTag("top_agentic_harness_button"),
                    color = NeonCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = "Harnais Agentique",
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Harnais IA",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Voix Studio Button (Google HD & ElevenLabs)
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .defaultMinSize(minWidth = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.showVoiceStudio() }
                        .testTag("top_voice_studio_button"),
                    color = NeonViolet.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Studio Voix",
                            tint = NeonViolet,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Voix",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }


                // Auto-Speech / TTS Toggle
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .defaultMinSize(minWidth = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.toggleAutoSpeak() }
                        .testTag("top_auto_speech_toggle_button"),
                    color = if (isAutoSpeakEnabled) NeonCyan.copy(alpha = 0.2f) else AlertPink.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAutoSpeakEnabled) NeonCyan.copy(alpha = 0.6f) else AlertPink.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAutoSpeakEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isAutoSpeakEnabled) "Synthèse vocale activée" else "Synthèse vocale en sourdine",
                            tint = if (isAutoSpeakEnabled) NeonCyan else AlertPink,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAutoSpeakEnabled) "TTS Actif" else "TTS Muet",
                            color = if (isAutoSpeakEnabled) NeonCyan else AlertPink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Commandes Vocales Button
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .defaultMinSize(minWidth = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.showVoiceCommandHelp() }
                        .testTag("top_voice_commands_button"),
                    color = NeonCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = "Commandes Vocales",
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Commandes",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Language Switcher Button
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .defaultMinSize(minWidth = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.showLanguageDialog() }
                        .testTag("top_language_switcher_button"),
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Changer la langue",
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Langue",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Voice Command Acknowledgment Banner
            lastVoiceCommandBanner?.let { banner ->
                Surface(
                    color = NeonCyan.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("voice_command_feedback_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = banner,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearVoiceCommandBanner() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Live Agentic Harness Active Banner
            if (agenticState.isActive) {
                Surface(
                    color = NeonCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.showAgenticHarnessDialog() }
                        .testTag("active_agentic_harness_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "🧠 Harnais Agentique (${agenticState.currentStepIndex}/${agenticState.totalSteps}) : ${agenticState.currentActivity}",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Objectif : « ${agenticState.goal.take(45)} »",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text("Détails >", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // JARVIS Holographic HUD Cards Stream (display_card)
            if (hudDisplayCards.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CARTES HUD (${hudDisplayCards.size})",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Effacer tout",
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clickable { viewModel.clearHudCards() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    for (card in hudDisplayCards.take(2)) {
                        com.example.ui.components.HudDisplayCardView(
                            card = card,
                            onDismiss = { viewModel.dismissHudCard(card.id) }
                        )
                    }
                }
            }

            // Active Source Grounding Card (NotebookLM Mode)
            activeSource?.let { src ->
                Surface(
                    color = NeonCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Source active : « ${src.title} »",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearActiveSource() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Détacher", tint = TextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "💡 Résumer" to "summary",
                                "🎯 Quiz" to "quiz",
                                "🔍 Points clés" to "key_points",
                                "❓ Expliquer" to "explain"
                            ).forEach { (label, qType) ->
                                Surface(
                                    color = DarkSurface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.startDiscussionOnSource(src, qType) }
                                ) {
                                    Text(
                                        text = label,
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Knowledge & Progression conversation prompt chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val quickIdeas = listOf(
                    "🤖 Statut système Mark VII",
                    "🚀 Ouvre YouTube",
                    "🎵 Lance Spotify",
                    "🌐 Ouvre mes e-mails",
                    "🔍 Recherche les actus IA",
                    "📚 Interroger mes sources (NotebookLM)",
                    "⚡ Analyse et calculs",
                    "❌ Annule la tâche"
                )
                quickIdeas.forEach { idea ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (idea.startsWith("📚 Interroger")) {
                                    viewModel.queryAllSources()
                                } else {
                                    viewModel.processInput(idea)
                                }
                            },
                        color = DarkSurfaceVariant.copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = idea,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // --- MAIN CONTENT: ORBE OR HISTORY ---
            if (isHistoryVisible) {
                // Scrollable History Drawer
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    cornerRadius = 16.dp,
                    borderColor = DarkCardBorderViolet
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Historique de conversation",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            IconButton(onClick = { viewModel.toggleHistory() }) {
                                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = TextSecondary)
                            }
                        }

                        LazyColumn(
                            state = historyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allMessages) { msg ->
                                val isUser = msg.role == "user"
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                ) {
                                    Surface(
                                        color = if (isUser) NeonCyan.copy(alpha = 0.18f) else DarkSurfaceVariant,
                                        shape = RoundedCornerShape(14.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isUser) NeonCyan.copy(alpha = 0.4f) else DarkCardBorderViolet
                                        ),
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isUser) "Toi" else companionDisplayName,
                                                    color = if (isUser) NeonCyan else NeonViolet,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (!isUser && msg.isWebSearch) {
                                                    Surface(
                                                        color = ElectricBlue.copy(alpha = 0.25f),
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Public,
                                                                contentDescription = null,
                                                                tint = NeonCyan,
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = msg.webSearchTopic ?: "Recherche Web",
                                                                color = NeonCyan,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = msg.text,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp
                                            )
                                            if (!msg.correctionNote.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = msg.correctionNote,
                                                    color = AlertPink,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            // Calame Passage Citation Chips
                                            val citationIndices = remember(msg.text) {
                                                Regex("\\[(\\d+)\\]").findAll(msg.text).mapNotNull { it.groupValues[1].toIntOrNull() }.distinct().toList()
                                            }
                                            if (!isUser && citationIndices.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Citations :", color = TextMuted, fontSize = 10.sp)
                                                    citationIndices.forEach { citeNum ->
                                                        Surface(
                                                            color = NeonCyan.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(6.dp),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .clickable { viewModel.openCitationByIndex(citeNum - 1) }
                                                        ) {
                                                            Text(
                                                                text = "[$citeNum]",
                                                                color = NeonCyan,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }


                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Audio Play / TTS Listen button for this message
                                                val isThisMsgSpeaking = isSpeaking && currentlySpokenText == msg.text
                                                Surface(
                                                    color = if (isThisMsgSpeaking) AlertPink.copy(alpha = 0.25f) else DarkSurface,
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isThisMsgSpeaking) AlertPink else NeonViolet.copy(alpha = 0.4f)
                                                    ),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { viewModel.speakMessage(msg) }
                                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                                        .testTag("tts_message_button_${msg.id}")
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = if (isThisMsgSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                                            contentDescription = if (isThisMsgSpeaking) "Arrêter" else "Écouter la voix",
                                                            tint = if (isThisMsgSpeaking) AlertPink else NeonViolet,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = if (isThisMsgSpeaking) "Arrêter" else "Écouter",
                                                            color = if (isThisMsgSpeaking) AlertPink else NeonViolet,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }

                                                if (!isUser) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = DarkSurface,
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.4f)),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { viewModel.showAddFlashcard(prefilledExample = msg.text) }
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Default.BookmarkAdd,
                                                                contentDescription = "Sauvegarder un mot",
                                                                tint = NeonCyan,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "+ Flashcard",
                                                                color = NeonCyan,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Central Orb Experience
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animated Orb
                    AnimatedOrb(
                        state = orbState,
                        audioRms = audioRms,
                        size = 210.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status Text with Orb State Feedback
                    Text(
                        text = statusText,
                        color = when (orbState) {
                            OrbState.LISTENING -> NeonCyan
                            OrbState.THINKING -> NeonViolet
                            OrbState.SPEAKING -> ElectricBlue
                            OrbState.OFFLINE -> OfflineAmber
                            else -> TextSecondary
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Local Source Ingestion & Learning Indicator
                    AnimatedVisibility(
                        visible = isIngestingSource,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .padding(vertical = 4.dp),
                            cornerRadius = 14.dp,
                            borderColor = NeonCyan
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = NeonCyan,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Mémorisation et apprentissage de ta source…",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$companionDisplayName apprend ce contenu pour que vous puissiez en discuter !",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = NeonCyan,
                                    trackColor = DarkSurfaceVariant
                                )
                            }
                        }
                    }

                    // Dedicated TTS Speaking Visualizer Banner
                    AnimatedVisibility(
                        visible = isSpeaking,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val ttsTransition = rememberInfiniteTransition(label = "ttsWave")
                        Surface(
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = NeonViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (!currentSpokenWord.isNullOrBlank()) "« $currentSpokenWord »" else "$companionDisplayName parle...",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Audio equalizer bars animation
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(5) { barIndex ->
                                        val barHeight by ttsTransition.animateFloat(
                                            initialValue = 4f,
                                            targetValue = 18f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(durationMillis = 350 + barIndex * 120),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "tts_bar_$barIndex"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(barHeight.dp)
                                                .clip(RoundedCornerShape(1.5.dp))
                                                .background(if (barIndex % 2 == 0) NeonCyan else NeonViolet)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Direct stop button
                                    IconButton(
                                        onClick = { viewModel.stopSpeaking() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Arrêter la voix",
                                            tint = AlertPink,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live Subtitles Display (User Speech & Code Speech)
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        cornerRadius = 16.dp,
                        borderColor = DarkCardBorder
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // User Spoken / Typed Transcript
                            if (liveTranscript.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = liveTranscript,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (liveTranscript.isNotBlank() && liveReply.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Code's Spoken Answer
                            if (liveReply.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = NeonViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = liveReply,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            } else if (liveTranscript.isBlank()) {
                                Text(
                                    text = "« Parle librement à $companionDisplayName ou demande-lui d'apprendre ce que tu veux… »",
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Oral Correction Pill
                            if (!correctionNote.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = AlertPink.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertPink.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = AlertPink,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = correctionNote ?: "",
                                            color = AlertPink,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            if (liveReply.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Live reply speak / stop button
                                    val isReplySpeaking = isSpeaking && (currentlySpokenText == liveReply || currentlySpokenText == null)
                                    Surface(
                                        color = if (isReplySpeaking) AlertPink.copy(alpha = 0.2f) else ElectricBlue.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isReplySpeaking) AlertPink.copy(alpha = 0.6f) else ElectricBlue.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                if (isReplySpeaking) {
                                                    viewModel.stopSpeaking()
                                                } else {
                                                    viewModel.speak(liveReply)
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                            .testTag("tts_live_reply_button")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isReplySpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = if (isReplySpeaking) "Arrêter la voix" else "Écouter la réponse",
                                                tint = if (isReplySpeaking) AlertPink else ElectricBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isReplySpeaking) "Silence" else "Écouter",
                                                color = if (isReplySpeaking) AlertPink else ElectricBlue,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Surface(
                                        color = DarkSurface,
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { viewModel.showAddFlashcard(prefilledExample = liveReply) }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                            .testTag("save_live_word_button")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.BookmarkAdd,
                                                contentDescription = "Sauvegarder un mot",
                                                tint = NeonCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Sauvegarder",
                                                color = NeonCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Storage Cleanup Proposal Card (Section 7.3)
                    storagePromptResource?.let { resource ->
                        Spacer(modifier = Modifier.height(6.dp))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = AlertPink,
                            backgroundColor = DarkSurfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "J'ai fini avec ${resource.name} (${resource.sizeMb} Mo). Veux-tu que je le supprime pour libérer de l'espace ?",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.cancelStoragePrompt() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text("Non, garder", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.deleteResourceWithConfirmation(resource) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AlertPink)
                                    ) {
                                        Text("Oui, supprimer", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- BOTTOM CONTROLS & INPUT BAR ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.16f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(650),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 0.95f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(650),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )

                // WhatsApp-style Vocal Recording Bar (Visible when isListening)
                AnimatedVisibility(
                    visible = isListening,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val minutes = recordingDuration / 60
                    val seconds = recordingDuration % 60
                    val formattedTime = "%02d:%02d".format(minutes, seconds)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Surface(
                            color = AlertPink.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, AlertPink.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(AlertPink.copy(alpha = pulseAlpha))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = formattedTime,
                                            color = AlertPink,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Écoute continue illimitée (ne se coupe jamais)",
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Discard / Cancel Recording
                                    IconButton(
                                        onClick = { viewModel.cancelRecording() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Annuler l'enregistrement",
                                            tint = AlertPink,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (liveTranscript.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "« $liveTranscript »",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 4,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Prends ton temps, parle et fais des pauses librement. C'est toi qui réappuieras pour envoyer !",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                // Audio level visualizer
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = AlertPink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(DarkSurfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(audioRms.coerceIn(0.08f, 1f))
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(NeonCyan, AlertPink)
                                                    )
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                // Quick voice command chips
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // System Voice Input Fallback Chip
                                    item {
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onLaunchSystemSpeech() }
                                                .testTag("system_speech_dictation_chip"),
                                            shape = RoundedCornerShape(12.dp),
                                            color = ElectricBlue.copy(alpha = 0.25f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Dictée système",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Voice Commands Help Chip
                                    item {
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { viewModel.showVoiceCommandHelp() }
                                                .testTag("show_voice_commands_chip"),
                                            shape = RoundedCornerShape(12.dp),
                                            color = NeonViolet.copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.6f))
                                        ) {
                                            Text(
                                                text = "Commandes vocales ?",
                                                color = NeonViolet,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    val quickHints = listOf(
                                        "Mode prof d'anglais",
                                        "Recherche sur internet...",
                                        "Lance un quiz",
                                        "Répète",
                                        "Stop",
                                        "Change de voix"
                                    )
                                    items(quickHints) { hint ->
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    viewModel.executeVoiceCommandText(hint)
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = DarkSurfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = hint,
                                                color = NeonCyan,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Hands-Free Quick Launch & Status Chip
                Surface(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (isHandsFreeMode) {
                                viewModel.showHandsFreeSheet()
                            } else {
                                viewModel.startHandsFreeConversation()
                            }
                        }
                        .testTag("hands_free_quick_chip"),
                    color = if (isHandsFreeMode) SuccessGreen.copy(alpha = 0.2f) else DarkSurfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isHandsFreeMode) SuccessGreen else DarkCardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🇬🇧", fontSize = 14.sp)
                        Text(
                            text = if (isHandsFreeMode) "Mains Libres Actif • Appuie pour ouvrir l'atelier" else "Pratique d'anglais Mains Libres (Full Duplex)",
                            color = if (isHandsFreeMode) SuccessGreen else NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Action Buttons Row with Huge Center Microphone
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: History & Stop Speaking
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // History Toggle Button
                        IconButton(
                            onClick = { viewModel.toggleHistory() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkCardBorder, CircleShape)
                                .testTag("history_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Historique",
                                tint = if (isHistoryVisible) NeonCyan else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Stop Speaking Button (Active when speaking)
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            enabled = isSpeaking,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSpeaking) AlertPink.copy(alpha = 0.25f) else DarkSurfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, if (isSpeaking) AlertPink else DarkCardBorder, CircleShape)
                                .testTag("stop_speaking_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Interrompre $companionDisplayName",
                                tint = if (isSpeaking) AlertPink else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Central Hero Microphone Button (96dp - Highly visible & easy to tap)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        if (isListening) {
                            // Pulsating glowing outer ring when recording
                            Box(
                                modifier = Modifier
                                    .size((96 * pulseScale).dp)
                                    .clip(CircleShape)
                                    .background(AlertPink.copy(alpha = 0.28f))
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .clickable { onMicClick() }
                                .testTag("main_mic_button"),
                            color = if (isListening) AlertPink else ElectricBlue,
                            shape = CircleShape,
                            shadowElevation = 14.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                4.dp,
                                if (isListening) Color.White else NeonCyan
                            )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                    contentDescription = if (isListening) "Envoyer le message vocal" else "Parler à $companionDisplayName",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }

                    // Right Column: Repeat & Add Sources
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Repeat Button
                        IconButton(
                            onClick = { viewModel.repeatLastReply() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkCardBorder, CircleShape)
                                .testTag("repeat_reply_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Répéter la réponse",
                                tint = NeonViolet,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Add Sources button (Documents, Images, Folders, Links)
                        IconButton(
                            onClick = { showAddSourceDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.2f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.6f), CircleShape)
                                .testTag("add_source_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Ajouter des Sources du téléphone",
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Helper indicator text directly under the big mic button
                Text(
                    text = if (isListening) "🔴 Écoute active… Réappuie UNIQUEMENT quand tu as fini pour envoyer !" else "Appuie sur le gros micro pour parler à $companionDisplayName 🎙️",
                    color = if (isListening) AlertPink else NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = if (isListening) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 3.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Text Input Bar with Send Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Ou écris un message à $companionDisplayName…", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = { onMicClick() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("inline_mic_dictation_button")
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Dictée vocale STT",
                                    tint = if (isListening) AlertPink else NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.processInput(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer",
                            tint = DarkBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Modal Dialog to add Sources (Documents, Images, Folders, Links) directly
        if (showAddSourceDialog) {
            AddSourceModalDialog(
                companionName = companionDisplayName,
                onPickDocuments = {
                    showAddSourceDialog = false
                    documentPickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "text/*",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/json",
                            "*/*"
                        )
                    )
                },
                onPickImages = {
                    showAddSourceDialog = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickFolder = {
                    showAddSourceDialog = false
                    folderPickerLauncher.launch(null)
                },
                onAddLink = { url ->
                    showAddSourceDialog = false
                    viewModel.ingestSourceFromUrl(url)
                },
                onDismiss = { showAddSourceDialog = false }
            )
        }

        // Calame Citation Excerpt Dialog
        val activeCitationPreview by viewModel.activeCitationPreview.collectAsState()
        activeCitationPreview?.let { citation ->
            AlertDialog(
                onDismissRequest = { viewModel.closeCitation() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = citation.sourceTitle,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Passage exact cité (indices ${citation.startOffset}..${citation.endOffset}) :",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = DarkSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = citation.text,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.closeCitation() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Fermer", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = DarkSurfaceVariant
            )
        }
    }
}


@Composable
private fun AddSourceModalDialog(
    companionName: String,
    onPickDocuments: () -> Unit,
    onPickImages: () -> Unit,
    onPickFolder: () -> Unit,
    onAddLink: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var linkInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachFile, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ajouter des Sources à $companionName",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Choisis un document, une image, un dossier de ton téléphone ou donne un lien pour que $companionName l'apprenne et en discute avec toi :",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // 1. Documents picker button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPickDocuments() },
                    color = DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Documents du téléphone", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("PDF, Word, TXT, Code, JSON…", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }

                // 2. Images picker button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPickImages() },
                    color = DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonViolet.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Images & Photos de la galerie", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Photos, captures d'écran, schémas…", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }

                // 3. Folder picker button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPickFolder() },
                    color = DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Dossier complet du téléphone", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Importer tous les fichiers d'un répertoire", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }

                // 4. Link Input (YouTube, TikTok, Web)
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("Ou coller un lien (YouTube, TikTok, Web) :", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = linkInput,
                            onValueChange = { linkInput = it },
                            placeholder = { Text("https://...", color = TextMuted, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (linkInput.isNotBlank()) {
                                    onAddLink(linkInput.trim())
                                }
                            },
                            enabled = linkInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Apprendre", color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = TextSecondary)
            }
        }
    )
}
