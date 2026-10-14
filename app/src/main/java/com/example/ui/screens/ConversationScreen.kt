package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
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
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.OfflineAmber
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ConversationScreen(
    viewModel: CodeViewModel,
    modifier: Modifier = Modifier
) {
    val orbState by viewModel.orbState.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val liveTranscript by viewModel.liveUserTranscript.collectAsState()
    val liveReply by viewModel.liveCodeReply.collectAsState()
    val correctionNote by viewModel.currentCorrectionNote.collectAsState()
    val currentMode by viewModel.selectedMode.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isHistoryVisible by viewModel.isHistoryVisible.collectAsState()
    val lessonPrep by viewModel.activeLessonPrep.collectAsState()
    val storagePromptResource by viewModel.storagePromptResource.collectAsState()
    val allMessages by viewModel.allMessages.collectAsState()
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    val isWebSearching by viewModel.isWebSearching.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val companionDisplayName = userProfile.companionName.ifBlank { "Code" }

    var textInput by remember { mutableStateOf("") }
    val historyListState = rememberLazyListState()

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP BAR: App branding, Network Status & Mode Chips ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LILIAN",
                        color = NeonCyan,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AMI OMNISCIENT",
                        color = NeonViolet,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Web Search Trigger Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.showWebSearchDialog() }
                            .padding(end = 6.dp)
                            .testTag("top_web_search_button"),
                        color = ElectricBlue.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Recherche Web",
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Recherche Web",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Language Switcher Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.showLanguageDialog() }
                            .padding(end = 6.dp)
                            .testTag("top_language_switcher_button"),
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(16.dp),
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
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Langue",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Online/Offline Status Pill
                    Surface(
                        color = if (isOnline) DarkSurfaceVariant else OfflineAmber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isOnline) SuccessGreen.copy(alpha = 0.5f) else OfflineAmber
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = if (isOnline) SuccessGreen else OfflineAmber,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOnline) "En ligne" else "Hors ligne",
                                color = if (isOnline) TextPrimary else OfflineAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Mode Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TutorMode.values().forEach { mode ->
                    val isSelected = mode == currentMode
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setMode(mode) }
                            .testTag("mode_chip_${mode.name.lowercase()}"),
                        color = if (isSelected) mode.accentColor.copy(alpha = 0.25f) else DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) mode.accentColor else DarkCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = mode.title,
                            color = if (isSelected) mode.accentColor else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Pending tasks banner if any requests are waiting for internet
            if (pendingTasks.isNotEmpty()) {
                Surface(
                    color = OfflineAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OfflineAmber.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏳ ${pendingTasks.size} recherche(s) en attente d'Internet",
                            color = OfflineAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (isOnline) {
                            TextButton(
                                onClick = { viewModel.syncAllPendingTasks() },
                                modifier = Modifier.padding(0.dp)
                            ) {
                                Text("Télécharger", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Quick Knowledge / Download prompt chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val quickIdeas = listOf(
                    "🌐 Recherche le télescope James Webb",
                    "🔍 Recherche l'intelligence artificielle",
                    "🌌 Parle-moi des trous noirs",
                    "🌍 C'est quoi le réchauffement climatique ?",
                    "🏛 Raconte-moi l'histoire de Rome",
                    "⚡ Explique-moi la relativité d'Einstein",
                    "🧠 C'est quoi un biais cognitif ?",
                    "📚 Télécharge un cours sur l'astronomie"
                )
                quickIdeas.forEach { idea ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.processInput(idea) },
                        color = DarkSurfaceVariant.copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = idea,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

                    // Lesson Prep Indicator (if preparing lesson)
                    AnimatedVisibility(
                        visible = !lessonPrep.isNullOrBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        lessonPrep?.let { prepStep ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .padding(vertical = 4.dp),
                                cornerRadius = 12.dp,
                                borderColor = NeonCyan
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = prepStep,
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
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
                    }

                    // Live Web Searching Banner (if actively querying the Internet)
                    AnimatedVisibility(
                        visible = isWebSearching,
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
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Recherche en direct sur Internet en cours…",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$companionDisplayName explore les sources du Web pour t'informer et mémoriser le sujet.",
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
                // Action Buttons Row: Stop speaking, Main Mic, Repeat last, History toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // History Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleHistory() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkCardBorder, CircleShape)
                            .testTag("history_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historique",
                            tint = if (isHistoryVisible) NeonCyan else TextSecondary
                        )
                    }

                    // Stop Speaking Button (Active when speaking)
                    IconButton(
                        onClick = { viewModel.stopSpeaking() },
                        enabled = isSpeaking,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isSpeaking) AlertPink.copy(alpha = 0.25f) else DarkSurfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, if (isSpeaking) AlertPink else DarkCardBorder, CircleShape)
                            .testTag("stop_speaking_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Interrompre $companionDisplayName",
                            tint = if (isSpeaking) AlertPink else TextMuted
                        )
                    }

                    // Central Big Microphone Button
                    Surface(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.toggleListening() }
                            .testTag("main_mic_button"),
                        color = if (isListening) NeonCyan else ElectricBlue,
                        shape = CircleShape,
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            3.dp,
                            if (isListening) Color.White else NeonCyan.copy(alpha = 0.7f)
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Microphone",
                                tint = if (isListening) DarkBackground else Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    // Repeat Button
                    IconButton(
                        onClick = { viewModel.repeatLastReply() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkCardBorder, CircleShape)
                            .testTag("repeat_reply_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Répéter la réponse",
                            tint = NeonViolet
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Text Input Bar with Send Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.showWebSearchDialog() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.2f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape)
                            .testTag("open_web_search_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Recherche Web",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Écris ou recherche…", color = TextMuted, fontSize = 12.sp) },
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
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer",
                            tint = DarkBackground
                        )
                    }
                }
            }
        }
    }
}
