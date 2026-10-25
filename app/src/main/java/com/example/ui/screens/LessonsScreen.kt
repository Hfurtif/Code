package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CachedLesson
import com.example.data.model.DownloadedKnowledge
import com.example.ui.CodeViewModel
import com.example.ui.components.CachedLessonDetailDialog
import com.example.ui.components.CachedLessonsSection
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

data class LearningSubject(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val promptToTrigger: String,
    val badge: String,
    val category: String = "Général"
)

@Composable
fun LessonsScreen(
    viewModel: CodeViewModel,
    onNavigateToConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val knowledgeList by viewModel.allKnowledge.collectAsState()
    val cachedLessons by viewModel.cachedLessons.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val companionName = profile.companionName.ifBlank { "Code" }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Leçons Hors-Ligne, 1: Sujets oraux, 2: Savoirs en mémoire
    var selectedCategory by remember { mutableStateOf("Tous") }
    var customSubjectInput by remember { mutableStateOf("") }
    var readingCourse by remember { mutableStateOf<DownloadedKnowledge?>(null) }
    var viewingCachedLesson by remember { mutableStateOf<CachedLesson?>(null) }

    val subjects = listOf(
        LearningSubject(
            id = "english",
            title = "Pratiquer & Apprendre l'Anglais",
            subtitle = "$companionName passe en professeur d'anglais : vocabulaire, écoute et corrections orales bienveillantes.",
            icon = Icons.Default.Language,
            accentColor = ElectricBlue,
            promptToTrigger = "Apprends-moi l'anglais, s'il te plaît !",
            badge = "Langue",
            category = "Langue"
        ),
        LearningSubject(
            id = "entrepreneurship",
            title = "Pôle Entrepreneurs & Commerce",
            subtitle = "Management, business model, négociation commerciale et pitch de projet avec $companionName.",
            icon = Icons.Default.TrendingUp,
            accentColor = NeonCyan,
            promptToTrigger = "Apprends-moi le commerce et la création d'entreprise !",
            badge = "Commerce",
            category = "Pôle Entrepreneurs & Commerce"
        ),
        LearningSubject(
            id = "math",
            title = "Mathématiques & Logique",
            subtitle = "$companionName devient professeur de maths : calcul mental, algèbre, géométrie et théorèmes expliqués simplement.",
            icon = Icons.Default.Calculate,
            accentColor = NeonViolet,
            promptToTrigger = "Apprends-moi les maths et la logique !",
            badge = "Sciences",
            category = "Sciences"
        ),
        LearningSubject(
            id = "psychology",
            title = "Psychologie & Fonctionnement de l'esprit",
            subtitle = "Biais cognitifs, gestion des émotions, écoute active et communication sereine.",
            icon = Icons.Default.Psychology,
            accentColor = AlertPink,
            promptToTrigger = "Télécharge un cours sur la psychologie et les biais cognitifs",
            badge = "Développement",
            category = "Développement"
        ),
        LearningSubject(
            id = "space",
            title = "Sciences & Astronomie",
            subtitle = "Les trous noirs, la relativité générale, les galaxies et les mystères du cosmos.",
            icon = Icons.Default.Science,
            accentColor = NeonCyan,
            promptToTrigger = "Télécharge un cours sur les trous noirs et l'espace",
            badge = "Cosmos",
            category = "Cosmos"
        ),
        LearningSubject(
            id = "history",
            title = "Histoire & Grandes Civilisations",
            subtitle = "La République et l'Empire romain, Jules César, les révolutions et les leçons du passé.",
            icon = Icons.Default.Public,
            accentColor = OfflineAmber,
            promptToTrigger = "Télécharge un cours sur l'histoire de Rome et de l'antiquité",
            badge = "Histoire",
            category = "Histoire"
        ),
        LearningSubject(
            id = "tech",
            title = "Informatique & Intelligence Artificielle",
            subtitle = "Algorithmes, réseaux de neurones, fonctionnement des modèles de langage et futur de la tech.",
            icon = Icons.Default.AutoAwesome,
            accentColor = SuccessGreen,
            promptToTrigger = "Télécharge un cours sur l'intelligence artificielle",
            badge = "Technologie",
            category = "Technologie"
        ),
        LearningSubject(
            id = "donations",
            title = "Pôle Mécénat, Projets & Dons",
            subtitle = "Financement participatif, dons associatifs, recherche de sponsors et philanthropie.",
            icon = Icons.Default.VolunteerActivism,
            accentColor = AlertPink,
            promptToTrigger = "Explique-moi le fonctionnement des dons et mécénats !",
            badge = "Dons",
            category = "Dons"
        )
    )

    val categoryFilterList = listOf(
        "Tous",
        "Langue",
        "Sciences",
        "Pôle Entrepreneurs & Commerce",
        "Développement",
        "Cosmos",
        "Histoire",
        "Technologie",
        "Dons"
    )

    val displayedSubjects = remember(selectedCategory, subjects) {
        if (selectedCategory == "Tous") subjects
        else subjects.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // --- HEADER WITH BRANDING & TOP-RIGHT BADGES (Streak & En ligne) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Apprendre avec $companionName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Cours & pôles thématiques oraux avec ton tuteur",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Badges en haut à droite: Série (Flamme) & Statut En ligne
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
                            contentDescription = "Série d'apprentissage",
                            tint = OfflineAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${profile.streakDays} j",
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

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs Mode
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                "💾 Leçons Hors-Ligne (${cachedLessons.size})",
                "🎓 Sujets oraux",
                "📚 Savoirs (${knowledgeList.size})"
            )
            tabs.forEachIndexed { index, tabTitle ->
                val isSelected = selectedTab == index
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .widthIn(min = 90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedTab = index }
                        .testTag("learning_tab_$index"),
                    color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) NeonCyan else DarkCardBorder
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = tabTitle,
                        color = if (isSelected) NeonCyan else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- LIGNE D'ONGLETS DE FILTRAGE HORIZONTAUX (Défilante, min-width anti-écrasement pour Sujets) ---
        if (selectedTab == 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categoryFilterList.forEach { cat ->
                    val isCatSelected = selectedCategory == cat
                    Surface(
                        modifier = Modifier
                            .wrapContentWidth()
                            .widthIn(min = 60.dp)
                            .defaultMinSize(minWidth = 60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedCategory = cat },
                        color = if (isCatSelected) NeonCyan.copy(alpha = 0.25f) else DarkSurfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCatSelected) NeonCyan else DarkCardBorder
                        )
                    ) {
                        Text(
                            text = cat,
                            color = if (isCatSelected) NeonCyan else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        when (selectedTab) {
            0 -> {
                // Room Cached Lessons (English, French & Offline Cache)
                CachedLessonsSection(
                    lessons = cachedLessons,
                    viewModel = viewModel,
                    onOpenLessonDetail = { viewingCachedLesson = it },
                    onStartOralLesson = { lesson ->
                        viewModel.startCachedLesson(lesson)
                        onNavigateToConversation()
                    }
                )
            }

            1 -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // URL-Based English Lesson Card (YouTube, TikTok, Web)
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("url_lesson_banner_card"),
                            borderColor = ElectricBlue,
                            backgroundColor = DarkSurfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = ElectricBlue.copy(alpha = 0.2f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Link,
                                                    contentDescription = null,
                                                    tint = ElectricBlue,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Cours d'anglais par Lien URL",
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "YouTube • TikTok • Articles Web",
                                                color = ElectricBlue,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Surface(
                                        color = ElectricBlue.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "NOUVEAU",
                                            color = ElectricBlue,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Colle le lien d'une vidéo YouTube, d'un TikTok ou d'une page web. Ton professeur analysera le contenu et créera une leçon d'anglais interactive avec vocabulaire et quiz !",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        viewModel.showUrlLessonDialog()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("open_url_lesson_dialog_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Créer un cours depuis un lien (YouTube, TikTok, Web)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Custom Subject Input Card
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = NeonCyan,
                            backgroundColor = DarkSurfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = NeonCyan)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Demander un sujet sur mesure",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Tape n'importe quel sujet (ex: physique quantique, guitare, philosophie…). $companionName fera les recherches et téléchargera le cours sur ton portable !",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customSubjectInput,
                                        onValueChange = { customSubjectInput = it },
                                        placeholder = { Text("Ex: la mécanique quantique…", color = TextMuted, fontSize = 12.sp) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("custom_subject_input"),
                                        shape = RoundedCornerShape(12.dp),
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
                                    Button(
                                        onClick = {
                                            if (customSubjectInput.isNotBlank()) {
                                                val topic = customSubjectInput.trim()
                                                viewModel.processInput("Télécharge un cours complet sur $topic et apprends-moi ce sujet !")
                                                customSubjectInput = ""
                                                onNavigateToConversation()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("submit_custom_subject_button")
                                    ) {
                                        Text("Apprends-moi", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Domaines & Sujets d'apprentissage",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(displayedSubjects, key = { it.id }) { subject ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = subject.accentColor.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(36.dp),
                                            shape = CircleShape,
                                            color = subject.accentColor.copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, subject.accentColor)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = subject.icon,
                                                    contentDescription = null,
                                                    tint = subject.accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                        ) {
                                            Text(
                                                text = subject.title,
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                softWrap = true,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Surface(
                                                color = subject.accentColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = subject.badge,
                                                    color = subject.accentColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = {
                                            viewModel.processInput(subject.promptToTrigger)
                                            onNavigateToConversation()
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(subject.accentColor)
                                            .testTag("start_subject_${subject.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Démarrer",
                                            tint = DarkBackground
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = subject.subtitle,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        viewModel.processInput(subject.promptToTrigger)
                                        onNavigateToConversation()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = subject.accentColor.copy(alpha = 0.2f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, subject.accentColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubble,
                                        contentDescription = null,
                                        tint = subject.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Discuter à l'oral avec $companionName",
                                        color = subject.accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Downloaded Offline Knowledge in Memory
                if (knowledgeList.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aucun cours téléchargé pour l'instant.",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Choisis un sujet dans le premier onglet ou dis à $companionName :\n« Télécharge un cours sur les trous noirs » !",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(knowledgeList) { course ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = NeonCyan.copy(alpha = 0.4f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = course.topic,
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            color = NeonCyan.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "%.1f Mo • Hors-Ligne".format(course.sizeMb),
                                                color = NeonCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = course.summary,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.startDiscussionOnKnowledge(course)
                                                onNavigateToConversation()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChatBubble,
                                                contentDescription = null,
                                                tint = DarkBackground,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Discuter à l'oral", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { readingCourse = course },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Book,
                                                contentDescription = null,
                                                tint = TextPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Lire le cours", fontSize = 11.sp)
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

    // Reading Dialog for Offline Course
    readingCourse?.let { course ->
        AlertDialog(
            onDismissRequest = { readingCourse = null },
            title = {
                Text(text = course.topic, color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    item {
                        Text(
                            text = "Catégorie : ${course.category}",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = course.detailedContent,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        if (course.keyPoints.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Points clés :",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = course.keyPoints.replace(";", "\n• "),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val c = course
                        readingCourse = null
                        viewModel.startDiscussionOnKnowledge(c)
                        onNavigateToConversation()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Discuter avec $companionName", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { readingCourse = null }) {
                    Text("Fermer", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Interactive Dialog for Room Cached Lessons
    viewingCachedLesson?.let { lesson ->
        CachedLessonDetailDialog(
            lesson = lesson,
            viewModel = viewModel,
            onDismiss = { viewingCachedLesson = null },
            onStartOralLesson = {
                viewingCachedLesson = null
                viewModel.startCachedLesson(it)
                onNavigateToConversation()
            }
        )
    }
}

