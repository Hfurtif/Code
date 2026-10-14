package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CurriculumRepository
import com.example.ui.CodeViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LevelTestDialog(
    viewModel: CodeViewModel,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var currentAnswer by remember { mutableStateOf("") }
    var isTestCompleted by remember { mutableStateOf(false) }
    var estimatedLevel by remember { mutableStateOf("B1") }

    val questions = CurriculumRepository.levelQuestions
    val currentQuestion = questions.getOrNull(currentIndex)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isTestCompleted) "Résultat de ton test" else "Évaluation CECRL (${currentIndex + 1}/${questions.size})",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            if (isTestCompleted) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Niveau estimé : $estimatedLevel",
                        color = NeonCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Félicitations ! Ton compagnon adaptera ses enseignements, son vocabulaire et la complexité des phrases à ce niveau.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else if (currentQuestion != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val progress = (currentIndex + 1).toFloat() / questions.size
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonCyan,
                        trackColor = DarkSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentQuestion.questionEn,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.speak(currentQuestion.questionEn) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Écouter", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentQuestion.questionFr,
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = currentAnswer,
                        onValueChange = { currentAnswer = it },
                        placeholder = { Text("Réponds en anglais…", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("level_test_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            if (isTestCompleted) {
                Button(
                    onClick = {
                        viewModel.updateProfileLevel(estimatedLevel)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Valider mon niveau", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        if (currentIndex + 1 < questions.size) {
                            currentIndex++
                            currentAnswer = ""
                        } else {
                            // Calculate estimated level
                            estimatedLevel = "B2"
                            isTestCompleted = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.testTag("level_test_next_button")
                ) {
                    Text(if (currentIndex + 1 < questions.size) "Question suivante" else "Calculer mon niveau", color = DarkBackground)
                }
            }
        },
        dismissButton = {
            if (!isTestCompleted) {
                TextButton(onClick = onDismiss) {
                    Text("Plus tard", color = TextSecondary)
                }
            }
        },
        containerColor = DarkSurfaceVariant
    )
}
