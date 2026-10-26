package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.CodeViewModel
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

/**
 * Interactive full-duplex hands-free English conversation modal dialog.
 * Enables users to practice spoken English continuously with SpeechRecognizer API & TTS.
 * Features live audio RMS visualizer, dynamic turn-taking state, real-time transcriptions,
 * grammar correction tips, quick topic starters, and barge-in / interrupt capability.
 */
@Composable
fun HandsFreeConversationSheet(
    viewModel: CodeViewModel,
    onDismiss: () -> Unit
) {
    val isHandsFreeMode by viewModel.isHandsFreeMode.collectAsState()
    val handsFreeStatus by viewModel.handsFreeStatus.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val orbState by viewModel.orbState.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val liveTranscript by viewModel.liveUserTranscript.collectAsState()
    val liveReply by viewModel.liveCodeReply.collectAsState()
    val correctionNote by viewModel.currentCorrectionNote.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val companionName = userProfile.companionName.ifBlank { "Code" }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hands_free_conversation_dialog"),
        containerColor = DarkBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = NeonCyan.copy(alpha = 0.18f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                    ) {
                        Text(
                            text = "🇬🇧",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(6.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Hands-Free Practice",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = if (isListening) SuccessGreen.copy(alpha = 0.2f)
                                else if (isSpeaking) ElectricBlue.copy(alpha = 0.2f)
                                else NeonViolet.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isListening) "🟢 EN ÉCOUTE"
                                    else if (isSpeaking) "🔵 CODE PARLE"
                                    else "🟡 CONTINU",
                                    color = if (isListening) SuccessGreen
                                    else if (isSpeaking) ElectricBlue
                                    else NeonCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "• Full Duplex en-US",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("hands_free_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Central Animated Orb Visualizer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedOrb(
                        state = orbState,
                        audioRms = audioRms,
                        size = 140.dp
                    )
                }

                // Dynamic Status Pill
                Surface(
                    color = when {
                        isSpeaking -> ElectricBlue.copy(alpha = 0.15f)
                        isListening -> SuccessGreen.copy(alpha = 0.15f)
                        orbState == OrbState.THINKING -> OfflineAmber.copy(alpha = 0.15f)
                        else -> DarkSurfaceVariant
                    },
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            isSpeaking -> ElectricBlue.copy(alpha = 0.6f)
                            isListening -> SuccessGreen.copy(alpha = 0.6f)
                            orbState == OrbState.THINKING -> OfflineAmber.copy(alpha = 0.6f)
                            else -> DarkCardBorder
                        }
                    ),
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .testTag("hands_free_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isSpeaking -> Icons.Default.RecordVoiceOver
                                isListening -> Icons.Default.GraphicEq
                                orbState == OrbState.THINKING -> Icons.Default.Hearing
                                else -> Icons.Default.Mic
                            },
                            contentDescription = null,
                            tint = when {
                                isSpeaking -> ElectricBlue
                                isListening -> SuccessGreen
                                orbState == OrbState.THINKING -> OfflineAmber
                                else -> NeonCyan
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = handsFreeStatus,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-Time Transcript Display
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // User's speech
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "👤 You:",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (liveTranscript.isNotBlank()) liveTranscript else "Parle en anglais à tout moment...",
                                color = if (liveTranscript.isNotBlank()) TextPrimary else TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.testTag("hands_free_user_transcript")
                            )
                        }

                        // Tutor's response
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "🇬🇧 $companionName:",
                                color = NeonViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (liveReply.isNotBlank()) liveReply else "Code est prêt à discuter avec toi en anglais.",
                                color = if (liveReply.isNotBlank()) TextPrimary else TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.testTag("hands_free_code_reply")
                            )
                        }

                        // Optional grammar tip
                        if (!correctionNote.isNullOrBlank()) {
                            Surface(
                                color = OfflineAmber.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OfflineAmber.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "💡 Conseil:",
                                        color = OfflineAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = correctionNote!!,
                                        color = TextPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Topic Starters (Tap or speak)
                Text(
                    text = "Idées de sujets à aborder :",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickTopics = listOf(
                        "Tell me about London" to "Tell me about London and what to visit there.",
                        "Order at a cafe" to "Let's roleplay: I enter a London cafe to order breakfast.",
                        "Job interview" to "Can we practice a job interview in English for a software engineer?",
                        "Correct my grammar" to "Please correct my English mistakes strictly during our chat.",
                        "Speak slower please" to "Can you speak a little bit slower please?"
                    )

                    for ((label, prompt) in quickTopics) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderViolet),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.processInput(prompt)
                                }
                        ) {
                            Text(
                                text = label,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Control Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Barge-in / Interrupt button (when Code is speaking)
                    if (isSpeaking) {
                        Button(
                            onClick = { viewModel.bargeInHandsFree() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AlertPink,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hands_free_interrupt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Interrompre",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isListening) {
                        // Pause mic
                        OutlinedButton(
                            onClick = { viewModel.pauseHandsFreeListening() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hands_free_pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pause micro",
                                color = NeonCyan,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        // Resume mic
                        Button(
                            onClick = { viewModel.resumeHandsFreeListening() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hands_free_resume_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reprendre",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Repeat tutor reply
                    OutlinedButton(
                        onClick = { viewModel.repeatLastReply() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hands_free_repeat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            tint = NeonViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Répéter",
                            color = NeonViolet,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.testTag("hands_free_exit_button")
            ) {
                Text(
                    text = "Quitter le mode mains libres",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
