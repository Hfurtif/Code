package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DownloadedDocument
import com.example.service.CacheStats
import com.example.ui.CodeViewModel
import com.example.ui.components.AnimatedOrb
import com.example.ui.components.GlassCard
import com.example.ui.components.HudDisplayCardView
import com.example.ui.components.OrbState
import com.example.ui.theme.AlertPink
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardBorderViolet
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.OfflineAmber
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

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
    val isOnline by viewModel.isOnline.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val currentlySpokenText by viewModel.currentlySpokenText.collectAsState()
    val isHistoryVisible by viewModel.isHistoryVisible.collectAsState()
    val isIngestingSource by viewModel.isIngestingSource.collectAsState()
    val allMessages by viewModel.allMessages.collectAsState()
    val allDocuments by viewModel.allDocuments.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val hudDisplayCards by viewModel.hudDisplayCards.collectAsState()
    val intelligenceStrategy by viewModel.intelligenceStrategy.collectAsState()
    val cacheStats by viewModel.cacheStats.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showMemoryManagerDialog by remember { mutableStateOf(false) }
    var showAddSourceFileDialog by remember { mutableStateOf(false) }

    val historyListState = rememberLazyListState()

    val totalMb = allDocuments.sumOf { it.sizeMb }
    val maxLimitGb = userProfile.maxStorageLimitGb
    val usedFraction = ((totalMb / (maxLimitGb * 1024.0))).coerceIn(0.0, 1.0).toFloat()

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
        // --- J.A.R.V.I.S. Mark VII Holographic Corner Brackets ---
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(20.dp)
        ) {
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 2.dp.toPx())
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 2.dp.toPx())
        }
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(20.dp)
        ) {
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 2.dp.toPx())
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = Offset(size.width, 0f), end = Offset(size.width, size.height), strokeWidth = 2.dp.toPx())
        }
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .size(20.dp)
        ) {
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = 2.dp.toPx())
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 2.dp.toPx())
        }
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(20.dp)
        ) {
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = 2.dp.toPx())
            drawLine(color = NeonCyan.copy(alpha = 0.5f), start = Offset(size.width, 0f), end = Offset(size.width, size.height), strokeWidth = 2.dp.toPx())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP BAR: J.A.R.V.I.S. MARK VII & Status ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // J.A.R.V.I.S. Identity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(
                                BorderStroke(
                                    2.dp,
                                    Brush.linearGradient(listOf(NeonCyan, NeonMagenta, NeonViolet))
                                ),
                                CircleShape
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_icon),
                            contentDescription = "J.A.R.V.I.S.",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column {
                        Text(
                            text = "J.A.R.V.I.S. MARK VII",
                            color = NeonCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "LOGÉ SUR LE TÉLÉPHONE • EN LIGNE & HORS LIGNE",
                            color = NeonViolet,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            maxLines = 1
                        )
                    }
                }

                // Online / Offline & Intelligence Strategy Badge
                Surface(
                    color = when {
                        !isOnline && intelligenceStrategy == com.example.service.IntelligenceStrategy.OFFLINE_CACHED_STRATEGY -> ElectricBlue.copy(alpha = 0.2f)
                        !isOnline -> OfflineAmber.copy(alpha = 0.2f)
                        else -> DarkSurfaceVariant
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            !isOnline && intelligenceStrategy == com.example.service.IntelligenceStrategy.OFFLINE_CACHED_STRATEGY -> ElectricBlue
                            !isOnline -> OfflineAmber
                            else -> SuccessGreen.copy(alpha = 0.6f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = when {
                                !isOnline && intelligenceStrategy == com.example.service.IntelligenceStrategy.OFFLINE_CACHED_STRATEGY -> ElectricBlue
                                !isOnline -> OfflineAmber
                                else -> SuccessGreen
                            },
                            modifier = Modifier.size(8.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isOnline) "En ligne (Gemini)" else intelligenceStrategy.badge,
                            color = when {
                                !isOnline && intelligenceStrategy == com.example.service.IntelligenceStrategy.OFFLINE_CACHED_STRATEGY -> ElectricBlue
                                !isOnline -> OfflineAmber
                                else -> TextPrimary
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // --- PHONE MEMORY & STORAGE PILL (Clickable to manage sources & capacity) ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showMemoryManagerDialog = true }
                    .testTag("phone_memory_status_bar"),
                color = DarkSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mémoire Téléphone : ",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${allDocuments.size} source(s) (${String.format(Locale.US, "%.1f", totalMb)} Mo)",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Capacité : ${maxLimitGb.toInt()} Go >",
                            color = NeonViolet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    LinearProgressIndicator(
                        progress = { usedFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeonCyan,
                        trackColor = DarkSurfaceVariant
                    )
                }
            }

            // --- QUICK ACTION CHIPS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Download a new source
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDownloadDialog = true }
                        .testTag("action_download_source"),
                    color = NeonCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonCyan)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "📥 Télécharger une source",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Phone Memory & Sources list
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showMemoryManagerDialog = true }
                        .testTag("action_view_memory"),
                    color = NeonViolet.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = NeonViolet,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "🧠 Sources en mémoire (${allDocuments.size})",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Storage Capacity Check
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.processInput("Donne-moi le statut de ta mémoire et du stockage") }
                        .testTag("action_storage_status"),
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "⚡ Statut & Capacité",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Silence TTS
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.stopSpeaking() }
                        .testTag("action_stop_tts"),
                    color = if (isSpeaking) AlertPink.copy(alpha = 0.25f) else DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSpeaking) AlertPink else DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            tint = if (isSpeaking) AlertPink else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Silence",
                            color = if (isSpeaking) AlertPink else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // --- HOLOGRAPHIC HUD CARDS (Downloads, Results, Storage info) ---
            if (hudDisplayCards.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AFFICHAGE HOLOGRAPHIQUE (${hudDisplayCards.size})",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Effacer",
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clickable { viewModel.clearHudCards() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    for (card in hudDisplayCards.take(2)) {
                        HudDisplayCardView(
                            card = card,
                            onDismiss = { viewModel.dismissHudCard(card.id) }
                        )
                    }
                }
            }

            // --- MAIN BODY: ORB OR CONVERSATION HISTORY ---
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
                                text = "Historique J.A.R.V.I.S.",
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
                                        border = BorderStroke(
                                            1.dp,
                                            if (isUser) NeonCyan.copy(alpha = 0.4f) else DarkCardBorderViolet
                                        ),
                                        modifier = Modifier.fillMaxWidth(0.88f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isUser) "Vous" else "J.A.R.V.I.S.",
                                                    color = if (isUser) NeonCyan else NeonViolet,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (!isUser && msg.isWebSearch) {
                                                    Surface(
                                                        color = ElectricBlue.copy(alpha = 0.25f),
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
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
                                                                text = msg.webSearchTopic ?: "Recherche",
                                                                color = NeonCyan,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = msg.text,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val isThisMsgSpeaking = isSpeaking && currentlySpokenText == msg.text
                                                Surface(
                                                    color = if (isThisMsgSpeaking) AlertPink.copy(alpha = 0.25f) else DarkSurface,
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isThisMsgSpeaking) AlertPink else NeonViolet.copy(alpha = 0.4f)
                                                    ),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { viewModel.speakMessage(msg) }
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = if (isThisMsgSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                                            contentDescription = null,
                                                            tint = if (isThisMsgSpeaking) AlertPink else NeonViolet,
                                                            modifier = Modifier.size(12.dp)
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
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Central Arc Reactor Orb Experience
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedOrb(
                        state = orbState,
                        audioRms = audioRms,
                        size = 200.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = statusText,
                        color = when (orbState) {
                            OrbState.LISTENING -> NeonCyan
                            OrbState.THINKING -> NeonViolet
                            OrbState.SPEAKING -> ElectricBlue
                            OrbState.OFFLINE -> OfflineAmber
                            else -> TextSecondary
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Ingesting / Downloading Progress Bar
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
                                        text = "Téléchargement et mémorisation dans le téléphone…",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = NeonCyan,
                                    trackColor = DarkSurfaceVariant
                                )
                            }
                        }
                    }

                    // Live Transcript and Reply Box
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .padding(vertical = 6.dp),
                        cornerRadius = 16.dp,
                        borderColor = when (orbState) {
                            OrbState.LISTENING -> NeonCyan
                            OrbState.SPEAKING -> NeonViolet
                            else -> DarkCardBorder
                        }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // User Spoken Transcript
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

                            // J.A.R.V.I.S. Spoken Answer
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
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )
                                }
                            } else if (liveTranscript.isBlank()) {
                                Text(
                                    text = "« Parlez à Jarvis ou demandez-lui de télécharger des informations… »",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Correction Note (if any)
                            if (!correctionNote.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = AlertPink.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, AlertPink.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
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

                            // Live action buttons (Listen / Repeat)
                            if (liveReply.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isReplySpeaking = isSpeaking && (currentlySpokenText == liveReply || currentlySpokenText == null)
                                    Surface(
                                        color = if (isReplySpeaking) AlertPink.copy(alpha = 0.2f) else ElectricBlue.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
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
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isReplySpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = if (isReplySpeaking) AlertPink else ElectricBlue,
                                                modifier = Modifier.size(13.dp)
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
                    .padding(bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(650),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                // Recording Visualizer Bar
                AnimatedVisibility(
                    visible = isListening,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val minutes = recordingDuration / 60
                    val seconds = recordingDuration % 60
                    val formattedTime = "%02d:%02d".format(minutes, seconds)

                    Surface(
                        color = AlertPink.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, AlertPink.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(AlertPink)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formattedTime,
                                        color = AlertPink,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "J.A.R.V.I.S. vous écoute…",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.cancelRecording() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Annuler",
                                        tint = AlertPink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (liveTranscript.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "« $liveTranscript »",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            // Audio visualizer bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = AlertPink,
                                    modifier = Modifier.size(14.dp)
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
                                            .background(Brush.horizontalGradient(listOf(NeonCyan, AlertPink)))
                                    )
                                }
                            }
                        }
                    }
                }

                // Microphone & Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // History Toggle
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

                    // Hero Central Microphone (Tap to talk / stop)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        if (isListening) {
                            Box(
                                modifier = Modifier
                                    .size((88 * pulseScale).dp)
                                    .clip(CircleShape)
                                    .background(AlertPink.copy(alpha = 0.28f))
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .clickable { onMicClick() }
                                .testTag("main_mic_button"),
                            color = if (isListening) AlertPink else ElectricBlue,
                            shape = CircleShape,
                            shadowElevation = 12.dp,
                            border = BorderStroke(
                                3.dp,
                                if (isListening) Color.White else NeonCyan
                            )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                    contentDescription = if (isListening) "Envoyer" else "Parler à J.A.R.V.I.S.",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }

                    // Attach / Add Local Files
                    IconButton(
                        onClick = { showAddSourceFileDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.6f), CircleShape)
                            .testTag("add_source_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Ajouter des fichiers du téléphone",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Helper micro text
                Text(
                    text = if (isListening) "🔴 Écoute en cours… Réappuyez pour envoyer !" else "Appuyez sur le micro pour parler à J.A.R.V.I.S. 🎙️",
                    color = if (isListening) AlertPink else NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                // Text Input Bar with Send Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                "Écrivez ou demandez à Jarvis de télécharger...",
                                color = TextMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
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
                                    contentDescription = "Microphone",
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

        // =========================================================================
        // MODAL 1: TÉLÉCHARGER UNE SOURCE / DOSSIER DANS LE TÉLÉPHONE
        // =========================================================================
        if (showDownloadDialog) {
            DownloadSourceModalDialog(
                onDownloadTopic = { topic ->
                    showDownloadDialog = false
                    viewModel.downloadNewKnowledge(topic)
                },
                onDownloadUrl = { url ->
                    showDownloadDialog = false
                    viewModel.ingestSourceFromUrl(url)
                },
                onDismiss = { showDownloadDialog = false }
            )
        }

        // =========================================================================
        // MODAL 2: GESTION DE LA MÉMOIRE & SOURCES DE J.A.R.V.I.S.
        // =========================================================================
        if (showMemoryManagerDialog) {
            JarvisPhoneMemoryDialog(
                documents = allDocuments,
                usedMb = totalMb,
                maxLimitGb = maxLimitGb,
                cacheStats = cacheStats,
                onClearCache = { viewModel.clearResponseCache() },
                onOpenDownloadDialog = {
                    showMemoryManagerDialog = false
                    showDownloadDialog = true
                },
                onDiscussSource = { doc ->
                    showMemoryManagerDialog = false
                    viewModel.startDiscussionOnDocument(doc)
                },
                onDeleteSource = { doc ->
                    viewModel.deleteDocument(doc)
                },
                onDismiss = { showMemoryManagerDialog = false }
            )
        }

        // =========================================================================
        // MODAL 3: AJOUT DE FICHIERS LOCAUX DU TÉLÉPHONE
        // =========================================================================
        if (showAddSourceFileDialog) {
            AddSourceFileDialog(
                onPickDocuments = {
                    showAddSourceFileDialog = false
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
                    showAddSourceFileDialog = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickFolder = {
                    showAddSourceFileDialog = false
                    folderPickerLauncher.launch(null)
                },
                onDismiss = { showAddSourceFileDialog = false }
            )
        }
    }
}

/**
 * Modal to ask Jarvis to download information on a subject or URL.
 */
@Composable
private fun DownloadSourceModalDialog(
    onDownloadTopic: (String) -> Unit,
    onDownloadUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var subjectInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Sujet / Thématique, 1: Lien Web / URL

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Télécharger dans la Mémoire",
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
                    text = "Demandez à J.A.R.V.I.S. de télécharger un dossier complet. Il sera enregistré directement sur votre téléphone et disponible en ligne comme hors ligne :",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // Tabs: Sujet vs Lien
                Row(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                            .clickable { selectedTab = 0 },
                        color = if (selectedTab == 0) NeonCyan else DarkSurfaceVariant
                    ) {
                        Text(
                            text = "Sujet / Cours",
                            color = if (selectedTab == 0) DarkBackground else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                            .clickable { selectedTab = 1 },
                        color = if (selectedTab == 1) NeonCyan else DarkSurfaceVariant
                    ) {
                        Text(
                            text = "Lien URL / Web",
                            color = if (selectedTab == 1) DarkBackground else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                if (selectedTab == 0) {
                    Column {
                        OutlinedTextField(
                            value = subjectInput,
                            onValueChange = { subjectInput = it },
                            placeholder = { Text("Ex: Les trous noirs, Histoire de Rome...", color = TextMuted, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Idées rapides :", color = TextMuted, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("L'intelligence artificielle", "Les trous noirs", "La Rome antique", "Physique quantique").forEach { idea ->
                                Surface(
                                    color = DarkSurfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { subjectInput = idea }
                                ) {
                                    Text(idea, color = NeonCyan, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("https://fr.wikipedia.org/wiki/...", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0 && subjectInput.isNotBlank()) {
                        onDownloadTopic(subjectInput.trim())
                    } else if (selectedTab == 1 && urlInput.isNotBlank()) {
                        onDownloadUrl(urlInput.trim())
                    }
                },
                enabled = (selectedTab == 0 && subjectInput.isNotBlank()) || (selectedTab == 1 && urlInput.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Télécharger dans le téléphone", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = TextSecondary)
            }
        }
    )
}

/**
 * Modal dialog showing all downloaded sources stored on the device, their size, and storage capacity.
 */
@Composable
private fun JarvisPhoneMemoryDialog(
    documents: List<DownloadedDocument>,
    usedMb: Double,
    maxLimitGb: Double,
    cacheStats: CacheStats,
    onClearCache: () -> Unit,
    onOpenDownloadDialog: () -> Unit,
    onDiscussSource: (DownloadedDocument) -> Unit,
    onDeleteSource: (DownloadedDocument) -> Unit,
    onDismiss: () -> Unit
) {
    val usedFraction = ((usedMb / (maxLimitGb * 1024.0))).coerceIn(0.0, 1.0).toFloat()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mémoire Téléphone",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                // Storage Gauge Box
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Capacité allouée au téléphone",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", usedMb)} Mo / ${maxLimitGb.toInt()} Go",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { usedFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = NeonCyan,
                            trackColor = DarkBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "J.A.R.V.I.S. exploite cette mémoire locale pour discuter avec vous hors ligne.",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cache Strategy Status Card
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DarkCardBorderViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cache Réponses Hors-Ligne",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${cacheStats.totalCachedCount} requêtes prêtes instantanément",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        if (cacheStats.totalCachedCount > 0) {
                            TextButton(
                                onClick = onClearCache,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Vider",
                                    color = AlertPink,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action to download a new source
                Button(
                    onClick = onOpenDownloadDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Télécharger une nouvelle source", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Dossiers & Connaissances en mémoire (${documents.size}) :",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (documents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucune source téléchargée pour l'instant.\nCliquez sur le bouton ci-dessus pour que Jarvis télécharge un dossier sur votre téléphone !",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(documents) { doc ->
                            Surface(
                                color = DarkSurfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, DarkCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = doc.title,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            color = SuccessGreen.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "${doc.sizeMb} Mo • Hors-ligne",
                                                color = SuccessGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = doc.summary.take(120) + "...",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { onDeleteSource(doc) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertPink),
                                            border = BorderStroke(1.dp, AlertPink.copy(alpha = 0.6f)),
                                            modifier = Modifier.height(30.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = AlertPink, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Supprimer", fontSize = 10.sp, color = AlertPink)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = { onDiscussSource(doc) },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                            modifier = Modifier.height(30.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Discuter avec Jarvis", fontSize = 10.sp, color = DarkBackground, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
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

/**
 * Modal to pick documents, images, or folders from phone storage.
 */
@Composable
private fun AddSourceFileDialog(
    onPickDocuments: () -> Unit,
    onPickImages: () -> Unit,
    onPickFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachFile, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ajouter des fichiers du téléphone",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choisissez des fichiers stockés sur votre téléphone pour les intégrer à la mémoire de J.A.R.V.I.S. :",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                // 1. Documents
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPickDocuments() },
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Documents (PDF, Word, TXT, JSON)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Sélectionnez des fichiers de vos documents", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }

                // 2. Images
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPickImages() },
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Images & Photos de la galerie", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Photos, captures d'écran...", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }

                // 3. Folder
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPickFolder() },
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Dossier complet du téléphone", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Importer tous les fichiers d'un dossier", color = TextMuted, fontSize = 10.sp)
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
