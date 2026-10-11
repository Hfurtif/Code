package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadedDocument
import com.example.data.model.DownloadedKnowledge
import com.example.data.model.PendingTask
import com.example.data.model.ResourcePack
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StorageScreen(
    viewModel: CodeViewModel,
    modifier: Modifier = Modifier,
    onNavigateToConversation: () -> Unit = {}
) {
    val resources by viewModel.allResources.collectAsState()
    val knowledgeList by viewModel.allKnowledge.collectAsState()
    val docList by viewModel.allDocuments.collectAsState()
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var downloadTopicInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    var packToDelete by remember { mutableStateOf<ResourcePack?>(null) }
    var knowledgeToDelete by remember { mutableStateOf<DownloadedKnowledge?>(null) }
    var docToDelete by remember { mutableStateOf<DownloadedDocument?>(null) }
    var previewKnowledge by remember { mutableStateOf<DownloadedKnowledge?>(null) }
    var previewDoc by remember { mutableStateOf<DownloadedDocument?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showExpandStorageDialog by remember { mutableStateOf(false) }

    val totalKnowledgeMb = knowledgeList.sumOf { it.sizeMb }
    val totalDocMb = docList.sumOf { it.sizeMb }
    val usedMb = resources.filter { it.isDownloaded }.sumOf { it.sizeMb } + totalKnowledgeMb + totalDocMb
    val maxLimitGb = userProfile.maxStorageLimitGb
    val totalAppLimitMb = maxLimitGb * 1024.0

    val filteredKnowledge = remember(knowledgeList, searchQuery) {
        if (searchQuery.isBlank()) knowledgeList
        else knowledgeList.filter {
            it.topic.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true) ||
                    it.summary.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredDocs = remember(docList, searchQuery) {
        if (searchQuery.isBlank()) docList
        else docList.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.topicCategory.contains(searchQuery, ignoreCase = true) ||
                    it.summary.contains(searchQuery, ignoreCase = true) ||
                    it.keywords.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Screen Header
        Text(
            text = "Stockage & Savoirs Hors-Ligne",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Jusqu'à 20 Go alloués sur ton portable • Données 100% privées et locales.",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Storage Gauge Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonCyan,
                    backgroundColor = DarkSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Espace occupé par Lilian",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "%.1f Mo / %.0f Go".format(usedMb, maxLimitGb),
                                color = NeonCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val progress = (usedMb / totalAppLimitMb).toFloat().coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = NeonCyan,
                            trackColor = DarkSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Capacité allouée : ${maxLimitGb.toInt()} Go",
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${knowledgeList.size} cours • ${docList.size} documents",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val removable = resources.firstOrNull { it.isDownloaded && !it.isProtected }
                                    if (removable != null) {
                                        packToDelete = removable
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("clean_space_auto_button")
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Libérer espace", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { showExpandStorageDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("expand_storage_button")
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajouter +20 Go", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Download New Knowledge / Courses Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = ElectricBlue,
                    backgroundColor = DarkSurface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = ElectricBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Télécharger de nouvelles informations",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Lilian fait les recherches, télécharge le cours et les documents pour discuter avec toi hors ligne.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = downloadTopicInput,
                                onValueChange = { downloadTopicInput = it },
                                placeholder = { Text("Ex: Trous noirs, Rome, Psychologie, Sommeil...", fontSize = 12.sp, color = TextMuted) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("download_knowledge_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricBlue,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = ElectricBlue
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (downloadTopicInput.isNotBlank()) {
                                        viewModel.downloadNewKnowledge(downloadTopicInput)
                                        downloadTopicInput = ""
                                    }
                                })
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (downloadTopicInput.isNotBlank()) {
                                        viewModel.downloadNewKnowledge(downloadTopicInput)
                                        downloadTopicInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                modifier = Modifier.testTag("download_knowledge_submit")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Télécharger", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Suggestions en 1 clic :",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val suggestions = listOf(
                                "🪐 Astrophysique",
                                "🏛 Rome Antique",
                                "🧠 Biais cognitifs",
                                "💼 Anglais des affaires",
                                "🧘 Gestion du stress",
                                "🤖 Intelligence Artificielle",
                                "🧬 Génétique",
                                "🗣 Éloquence"
                            )
                            suggestions.forEach { sug ->
                                Surface(
                                    color = DarkSurfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                ) {
                                    TextButton(
                                        onClick = { viewModel.downloadNewKnowledge(sug.substringAfter(" ")) },
                                        modifier = Modifier.padding(0.dp)
                                    ) {
                                        Text(text = sug, fontSize = 11.sp, color = NeonCyan)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pending Offline Tasks Section
            if (pendingTasks.isNotEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = OfflineAmber,
                        backgroundColor = DarkSurfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = OfflineAmber)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Demandes en attente d'Internet (${pendingTasks.size})",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    color = if (isOnline) SuccessGreen.copy(alpha = 0.2f) else OfflineAmber.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (isOnline) "En ligne (prêt)" else "Hors ligne",
                                        color = if (isOnline) SuccessGreen else OfflineAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Questions ou cours demandés hors ligne. Lilian téléchargera tout dès que possible.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )

                            pendingTasks.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = task.query, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(task.timestamp)),
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deletePendingTask(task) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer tâche", tint = TextMuted)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.syncAllPendingTasks() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOnline) SuccessGreen else OfflineAmber
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOnline) "Télécharger tout maintenant" else "En attente de connexion Internet",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Local Search Bar for Offline Knowledge & Documents
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filtrer parmi les cours et documents enregistrés...", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_library_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan
                    ),
                    singleLine = true
                )
            }

            // Downloaded Knowledge & Courses Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cours & Savoirs en mémoire (${filteredKnowledge.size})",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Disponibles 100% hors-ligne",
                        color = NeonCyan,
                        fontSize = 10.sp
                    )
                }
            }

            if (filteredKnowledge.isEmpty()) {
                item {
                    Text(
                        text = "Aucun cours ne correspond à la recherche.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }

            items(filteredKnowledge) { k ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = ElectricBlue
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = k.topic,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = ElectricBlue.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = k.category,
                                            color = ElectricBlue,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "%.1f Mo".format(k.sizeMb),
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { knowledgeToDelete = k },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = AlertPink)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = k.summary,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.startDiscussionOnKnowledge(k)
                                    onNavigateToConversation()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("discuss_knowledge_${k.id}")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Discuter à la voix", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { previewKnowledge = k },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                modifier = Modifier.testTag("read_knowledge_${k.id}")
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aperçu", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Downloaded Documents Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Documents de référence (${filteredDocs.size})",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Recherches & Synthèses",
                        color = NeonViolet,
                        fontSize = 10.sp
                    )
                }
            }

            if (filteredDocs.isEmpty()) {
                item {
                    Text(
                        text = "Aucun document ne correspond à la recherche.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }

            items(filteredDocs) { doc ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonViolet
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.title,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = NeonViolet.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = doc.topicCategory,
                                            color = NeonViolet,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "%.1f Mo • ${doc.authorOrSource}".format(doc.sizeMb),
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { docToDelete = doc },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = AlertPink)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = doc.summary,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.startDiscussionOnDocument(doc)
                                    onNavigateToConversation()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("discuss_document_${doc.id}")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Discuter à la voix", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { previewDoc = doc },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                modifier = Modifier.testTag("read_document_${doc.id}")
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lire", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Downloaded System Resource Packs
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Packs & Modèles IA embarqués",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(resources) { resource ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (resource.isProtected) SuccessGreen.copy(alpha = 0.4f) else DarkCardBorder
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = resource.name,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (resource.isProtected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Protégé",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${resource.type} • %.1f Mo".format(resource.sizeMb),
                                color = if (resource.isDownloaded) NeonCyan else TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        if (resource.isDownloaded) {
                            if (!resource.isProtected) {
                                IconButton(
                                    onClick = { packToDelete = resource },
                                    modifier = Modifier.testTag("delete_resource_${resource.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = AlertPink)
                                }
                            } else {
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Conservé",
                                        color = SuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.downloadResource(resource) },
                                modifier = Modifier.testTag("download_resource_${resource.id}")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Télécharger", tint = NeonCyan)
                            }
                        }
                    }
                }
            }

            // Export & Data Management Actions
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sauvegarde & Confidentialité",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_data_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exporter (JSON)", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { showClearAllConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertPink),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("clear_all_data_button")
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Effacer tout", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // Modal: Preview Knowledge
    previewKnowledge?.let { k ->
        AlertDialog(
            onDismissRequest = { previewKnowledge = null },
            title = {
                Text(
                    text = k.topic,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    item {
                        Surface(
                            color = ElectricBlue.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Catégorie : ${k.category}",
                                color = ElectricBlue,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Résumé oral :",
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontSize = 12.sp
                        )
                        Text(
                            text = k.summary,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Contenu détaillé en mémoire :",
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontSize = 12.sp
                        )
                        Text(
                            text = k.detailedContent,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Points clés :",
                            fontWeight = FontWeight.Bold,
                            color = NeonViolet,
                            fontSize = 12.sp
                        )
                        Text(
                            text = k.keyPoints,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Questions types pour s'entraîner :",
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            fontSize = 12.sp
                        )
                        Text(
                            text = k.sampleQuestions,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startDiscussionOnKnowledge(k)
                        previewKnowledge = null
                        onNavigateToConversation()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lancer la discussion vocale", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { previewKnowledge = null }) {
                    Text("Fermer", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Modal: Preview Document
    previewDoc?.let { doc ->
        AlertDialog(
            onDismissRequest = { previewDoc = null },
            title = {
                Text(
                    text = doc.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    item {
                        Text(
                            text = "Source : ${doc.authorOrSource} • ${doc.topicCategory}",
                            color = NeonViolet,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "Résumé :",
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontSize = 12.sp
                        )
                        Text(
                            text = doc.summary,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Texte intégral du document :",
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontSize = 12.sp
                        )
                        Text(
                            text = doc.fullTextContent,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Mots-clés indexés : ${doc.keywords}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startDiscussionOnDocument(doc)
                        previewDoc = null
                        onNavigateToConversation()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discuter à la voix", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { previewDoc = null }) {
                    Text("Fermer", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Confirmation Dialog for Resource Deletion
    packToDelete?.let { pack ->
        AlertDialog(
            onDismissRequest = { packToDelete = null },
            title = {
                Text(
                    text = "Confirmer la suppression",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Veux-tu vraiment supprimer « ${pack.name} » ?\n\nTu libèreras ${pack.sizeMb} Mo. Tu pourras le re-télécharger à tout moment.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteResourceWithConfirmation(pack)
                        packToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertPink)
                ) {
                    Text("Oui, supprimer", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { packToDelete = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Confirmation Dialog for Knowledge Deletion
    knowledgeToDelete?.let { k ->
        AlertDialog(
            onDismissRequest = { knowledgeToDelete = null },
            title = {
                Text(
                    text = "Supprimer le cours",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Supprimer le cours « ${k.topic} » ?\n\nTu libèreras ${k.sizeMb} Mo de mémoire locale.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteKnowledge(k)
                        knowledgeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertPink)
                ) {
                    Text("Supprimer", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { knowledgeToDelete = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Confirmation Dialog for Document Deletion
    docToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToDelete = null },
            title = {
                Text(
                    text = "Supprimer le document",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Supprimer le document « ${doc.title} » ?\n\nTu libèreras ${doc.sizeMb} Mo.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(doc)
                        docToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertPink)
                ) {
                    Text("Supprimer", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { docToDelete = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Clear All Confirmation
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = {
                Text("Effacer tout l'historique ?", color = AlertPink, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Cette action réinitialisera l'historique de conversation. Les connaissances et cours téléchargés resteront intacts.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertPink)
                ) {
                    Text("Effacer l'historique", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Export Dialog
    if (showExportDialog) {
        val jsonExport = remember { viewModel.exportDataAsJson() }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text("Sauvegarde locale (JSON)", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    item {
                        Text(
                            text = jsonExport,
                            color = NeonCyan,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Fermer", color = Color.Black)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }
    // Dialog for Storage Expansion (+20 GB Authorization)
    if (showExpandStorageDialog) {
        AlertDialog(
            onDismissRequest = { showExpandStorageDialog = false },
            title = {
                Text(
                    text = "Autorisation d'extension de stockage",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Lilian utilise actuellement un quota de ${maxLimitGb.toInt()} Go.\n\nAutorises-tu Lilian à étendre sa capacité de stockage de +20 Go (nouvelle limite : ${(maxLimitGb + 20).toInt()} Go) ?\n\nCet espace supplémentaire permettra de télécharger et conserver davantage de cours, dictionnaires et documents de recherche pour vos discussions vocales.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.expandStorage(20.0)
                        showExpandStorageDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Autoriser (+20 Go)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpandStorageDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }
}
