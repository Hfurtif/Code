package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Vocabulary
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
fun FlashcardsReviewDialog(
    viewModel: CodeViewModel,
    onDismiss: () -> Unit
) {
    val vocabList by viewModel.allVocabulary.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Cartes (Révision), 1: Liste
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val safeIndex = if (vocabList.isNotEmpty()) {
        currentCardIndex.coerceIn(0, vocabList.size - 1)
    } else 0

    val currentCard = if (vocabList.isNotEmpty()) vocabList[safeIndex] else null

    // 3D Flip Card Animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "flip_card_rotation"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Top Bar with Title, Count & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = NeonCyan.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Mes Flashcards",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${vocabList.size} mot(s) mémorisé(s)",
                                color = NeonCyan,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick Add Word Button
                        IconButton(
                            onClick = { viewModel.showAddFlashcard() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonViolet.copy(alpha = 0.2f))
                                .testTag("open_add_flashcard_from_review")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Ajouter un mot",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher: Mode Révision vs Liste
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Mode Révision 🃏", "Tous les mots 📋").forEachIndexed { index, tabTitle ->
                        val isSelected = activeTab == index
                        Surface(
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) NeonCyan else DarkCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    activeTab = index
                                    isFlipped = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = tabTitle,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (vocabList.isEmpty()) {
                    // Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aucune flashcard enregistrée",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Pendant tes conversations avec ton professeur d'anglais, clique sur « Sauvegarder un mot » pour l'ajouter ici et le réviser !",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { viewModel.showAddFlashcard() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ajouter mon premier mot", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (activeTab == 0 && currentCard != null) {
                    // TAB 0: Interactive Flip Card Study Mode
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Progress bar & Card count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Carte ${safeIndex + 1} / ${vocabList.size}",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Mastery Stars
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                (1..5).forEach { starIndex ->
                                    Icon(
                                        imageVector = if (starIndex <= currentCard.masteryLevel) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = null,
                                        tint = if (starIndex <= currentCard.masteryLevel) OfflineAmber else TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (safeIndex + 1).toFloat() / vocabList.size.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = NeonCyan,
                            trackColor = DarkSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // FLIP CARD (Front = English / Back = French Translation)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .graphicsLayer {
                                    rotationY = rotation
                                    cameraDistance = 12f * density
                                }
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { isFlipped = !isFlipped }
                                .testTag("flashcard_flip_card")
                        ) {
                            if (rotation <= 90f) {
                                // FRONT OF CARD: English word + Listen button + Example
                                GlassCard(
                                    modifier = Modifier.fillMaxSize(),
                                    cornerRadius = 24.dp,
                                    borderColor = NeonCyan,
                                    backgroundColor = DarkSurfaceVariant
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            color = NeonCyan.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "ENGLISH",
                                                color = NeonCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = currentCard.word,
                                            color = TextPrimary,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Audio Pronunciation Button
                                        Surface(
                                            color = DarkSurface,
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .clickable { viewModel.speak(currentCard.word) }
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                                .testTag("listen_flashcard_pronunciation")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Écouter",
                                                    tint = NeonCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Écouter la prononciation",
                                                    color = NeonCyan,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        if (currentCard.example.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(20.dp))
                                            Surface(
                                                color = DarkSurface.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, DarkCardBorder)
                                            ) {
                                                Text(
                                                    text = "« ${currentCard.example} »",
                                                    color = TextSecondary,
                                                    fontSize = 13.sp,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(12.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.TouchApp,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Touche la carte pour voir la traduction",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                // BACK OF CARD: French translation & Mastery Rating
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationY = 180f },
                                    cornerRadius = 24.dp,
                                    borderColor = NeonViolet,
                                    backgroundColor = DarkSurfaceVariant
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            color = NeonViolet.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "TRADUCTION FRANÇAISE",
                                                color = NeonViolet,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Text(
                                            text = currentCard.translation.ifBlank { "Pas de traduction" },
                                            color = TextPrimary,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = "Mot original : ${currentCard.word}",
                                            color = NeonCyan,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Spacer(modifier = Modifier.height(20.dp))

                                        // Mastery Assessment Buttons
                                        Text(
                                            text = "Comment as-tu réussi à t'en souvenir ?",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Red: Needs Review
                                            Button(
                                                onClick = {
                                                    viewModel.updateWordMastery(currentCard, -1)
                                                    if (safeIndex < vocabList.size - 1) {
                                                        currentCardIndex++
                                                    } else {
                                                        currentCardIndex = 0
                                                    }
                                                    isFlipped = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AlertPink.copy(alpha = 0.2f)),
                                                border = BorderStroke(1.dp, AlertPink),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("À revoir", color = AlertPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text("Difficile", color = TextSecondary, fontSize = 9.sp)
                                                }
                                            }

                                            // Yellow: Good
                                            Button(
                                                onClick = {
                                                    viewModel.updateWordMastery(currentCard, 1)
                                                    if (safeIndex < vocabList.size - 1) {
                                                        currentCardIndex++
                                                    } else {
                                                        currentCardIndex = 0
                                                    }
                                                    isFlipped = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = OfflineAmber.copy(alpha = 0.2f)),
                                                border = BorderStroke(1.dp, OfflineAmber),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Bien", color = OfflineAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text("+1 ⭐", color = TextSecondary, fontSize = 9.sp)
                                                }
                                            }

                                            // Green: Mastered
                                            Button(
                                                onClick = {
                                                    viewModel.updateWordMastery(currentCard, 2)
                                                    if (safeIndex < vocabList.size - 1) {
                                                        currentCardIndex++
                                                    } else {
                                                        currentCardIndex = 0
                                                    }
                                                    isFlipped = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen.copy(alpha = 0.2f)),
                                                border = BorderStroke(1.dp, SuccessGreen),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Facile", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text("Maîtrisé", color = TextSecondary, fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controls: Previous / Flip / Next
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (safeIndex > 0) {
                                        currentCardIndex--
                                        isFlipped = false
                                    }
                                },
                                enabled = safeIndex > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (safeIndex > 0) DarkCardBorder else DarkSurfaceVariant)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Précédent", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Précédent", color = TextPrimary, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { isFlipped = !isFlipped },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, NeonViolet),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Flip, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isFlipped) "Face avant" else "Retourner", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (safeIndex < vocabList.size - 1) {
                                        currentCardIndex++
                                        isFlipped = false
                                    }
                                },
                                enabled = safeIndex < vocabList.size - 1,
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (safeIndex < vocabList.size - 1) DarkCardBorder else DarkSurfaceVariant)
                            ) {
                                Text("Suivant", color = TextPrimary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Suivant", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    // TAB 1: List View of all Flashcards
                    val filteredList = if (searchQuery.isBlank()) {
                        vocabList
                    } else {
                        vocabList.filter {
                            it.word.contains(searchQuery, ignoreCase = true) ||
                            it.translation.contains(searchQuery, ignoreCase = true) ||
                            it.example.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Rechercher un mot mémorisé…", color = TextSecondary.copy(alpha = 0.5f)) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Effacer", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredList, key = { it.id }) { vocab ->
                                Surface(
                                    color = DarkSurface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, DarkCardBorderViolet),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = vocab.word,
                                                    color = TextPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                // Mastery stars
                                                Row {
                                                    (1..5).forEach { star ->
                                                        Icon(
                                                            imageVector = if (star <= vocab.masteryLevel) Icons.Filled.Star else Icons.Outlined.Star,
                                                            contentDescription = null,
                                                            tint = if (star <= vocab.masteryLevel) OfflineAmber else TextMuted,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = vocab.translation,
                                                color = NeonCyan,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )

                                            if (vocab.example.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "« ${vocab.example} »",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { viewModel.speak(vocab.word) }) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Écouter",
                                                    tint = NeonCyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(onClick = { viewModel.deleteWord(vocab.id) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Supprimer",
                                                    tint = AlertPink.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(18.dp)
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
}
