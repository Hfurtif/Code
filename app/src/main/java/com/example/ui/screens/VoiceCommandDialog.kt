package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VoiceChat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.CodeViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardBorderViolet
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.voice.VoiceCommandHelpItem
import com.example.voice.VoiceCommandManager

@Composable
fun VoiceCommandDialog(
    viewModel: CodeViewModel,
    onDismiss: () -> Unit
) {
    val commands = VoiceCommandManager.availableCommands
    val categories = commands.map { it.category }.distinct()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(NeonCyan, NeonViolet.copy(alpha = 0.3f), DarkCardBorder)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .testTag("voice_commands_dialog"),
            color = DarkSurface,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.16f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Commandes Vocales & STT",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Parle naturellement, ton assistant exécute",
                                color = NeonCyan,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .testTag("close_voice_commands_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Intro explanation card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = DarkBackground,
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = null,
                            tint = NeonMagenta,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Active le gros micro central et prononce l'une de ces commandes ou pose ta question. L'assistant comprend le français et l'anglais.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Command Categories List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    categories.forEach { category ->
                        item {
                            Text(
                                text = category.uppercase(),
                                color = NeonViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                            )
                        }

                        val categoryItems = commands.filter { it.category == category }
                        items(categoryItems) { item ->
                            VoiceCommandCard(
                                item = item,
                                onTestCommand = { spoken ->
                                    viewModel.executeVoiceCommandText(spoken)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            viewModel.startListening()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("start_listening_from_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = DarkBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Parler maintenant",
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("dismiss_voice_commands_dialog")
                    ) {
                        Text("Fermer", color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceCommandCard(
    item: VoiceCommandHelpItem,
    onTestCommand: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DarkBackground.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = item.title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.description,
                color = TextMuted,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Examples Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.examples.take(2).forEach { example ->
                    val cleanText = example.removeSurrounding("\"")
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTestCommand(cleanText) },
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant,
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Tester",
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = example,
                                color = NeonCyan,
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
