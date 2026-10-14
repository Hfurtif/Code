package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.CodeViewModel
import com.example.ui.components.GlassCard
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
fun ProgressScreen(
    viewModel: CodeViewModel,
    onLaunchLevelTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val errors by viewModel.allErrors.collectAsState()
    val vocabList by viewModel.allVocabulary.collectAsState()
    val lessons by viewModel.allLessons.collectAsState()

    val totalWords = vocabList.size
    val totalCorrections = errors.sumOf { it.count }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Screen Header
        Text(
            text = "Ta Progression",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "${profile.companionName.ifBlank { "Code" }} mémorise chaque apprentissage pour mesurer ton évolution",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Level Card with CECRL gauge
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonCyan,
                    backgroundColor = DarkSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Niveau CECRL estimé",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = profile.level,
                                        color = NeonCyan,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Intermédiaire indépendant",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = onLaunchLevelTest,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("retake_level_test_button")
                            ) {
                                Text("Évaluer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Progression vers B2 : 65%",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { 0.65f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = NeonCyan,
                            trackColor = DarkSurface
                        )
                    }
                }
            }

            // Stats Cards Row: Streak, Words Learned, XP Score
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Streak Card
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        borderColor = OfflineAmber
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = OfflineAmber)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "${profile.streakDays} jours", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Régularité", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    // Words Learned
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        borderColor = NeonCyan
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "$totalWords mots", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Vocabulaire", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    // Total XP Score
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        borderColor = ElectricBlue
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = ElectricBlue)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "${profile.xpScore} XP", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Expérience", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Points Faibles (Frequent Errors with Repetition Count)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = AlertPink.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ThumbDown, contentDescription = null, tint = AlertPink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Points à consolider (erreurs fréquentes)",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (errors.isEmpty()) {
                            Text(text = "Aucune erreur répétée enregistrée. Excellent travail !", color = TextSecondary, fontSize = 12.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                errors.take(4).forEach { error ->
                                    Surface(
                                        color = DarkSurface,
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "« ${error.badPhrase} » ➔ « ${error.correction} »",
                                                    color = TextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = error.errorType,
                                                    color = AlertPink,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Surface(
                                                color = AlertPink.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${error.count}x",
                                                    color = AlertPink,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

            // Points Forts (Strengths)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = SuccessGreen.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Points forts validés",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "• Spontanéité lors des salutations et présentations", color = TextSecondary, fontSize = 12.sp)
                        Text(text = "• Bonne assimilation du vocabulaire du voyage et des commandes", color = TextSecondary, fontSize = 12.sp)
                        Text(text = "• Intonation naturelle lors des questions fermées", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            // Badges & Milestones
            item {
                Text(
                    text = "Trophées & Badges",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val badges = listOf(
                        Triple("Premier pas", "Première conversation vocale", true),
                        Triple("7 Jours", "Une semaine de pratique consécutive", false),
                        Triple("Polyglotte", "Plus de 50 mots assimilés", true),
                        Triple("Sans filtre", "10 minutes d'échange ininterrompu", true)
                    )
                    items(badges) { (title, desc, unlocked) ->
                        GlassCard(
                            modifier = Modifier.width(135.dp),
                            borderColor = if (unlocked) NeonViolet else DarkCardBorder
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color = if (unlocked) NeonViolet.copy(alpha = 0.3f) else DarkSurface
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = if (unlocked) NeonViolet else TextMuted,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = title,
                                    color = if (unlocked) TextPrimary else TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
