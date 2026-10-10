package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StorageScreen(
    viewModel: CodeViewModel,
    modifier: Modifier = Modifier
) {
    val resources by viewModel.allResources.collectAsState()

    var packToDelete by remember { mutableStateOf<ResourcePack?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val usedMb = resources.filter { it.isDownloaded }.sumOf { it.sizeMb }
    val totalAppLimitMb = 250.0 // Visual budget quota for local assistant

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Screen Header
        Text(
            text = "Gestion du Stockage",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Toutes les données restent 100% sur ton téléphone. Aucun serveur externe.",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Espace occupé par Code",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "%.1f Mo".format(usedMb),
                                color = NeonCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
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
                        Text(
                            text = "Quota recommandé : %.0f Mo • Mémoire appareil saine".format(totalAppLimitMb),
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                // Propose cleanup of finished non-protected packs
                                val removable = resources.firstOrNull { it.isDownloaded && !it.isProtected }
                                if (removable != null) {
                                    packToDelete = removable
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clean_space_auto_button")
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Libérer de l'espace (ressources terminées)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Downloaded Resources List
            item {
                Text(
                    text = "Packs & Modèles téléchargés",
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
                            if (resource.isProtected) {
                                Text(
                                    text = "Données protégées : mémoire des leçons & dictionnaire",
                                    color = SuccessGreen,
                                    fontSize = 10.sp
                                )
                            }
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

    // Confirmation Dialog for Resource Deletion (Strict Rule Section 7.3)
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
                    text = "Veux-tu vraiment supprimer « ${pack.name} » ?\n\nTu libèreras ${pack.sizeMb} Mo. Tu pourras le re-télécharger à tout moment si une leçon en a besoin.",
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

    // Export Dialog
    if (showExportDialog) {
        val json = viewModel.exportDataAsJson()
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export de tes données", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        text = "Voici tes données exportées (profil, vocabulaire, progression). Elles ne quittent jamais ton téléphone :",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = json.take(300) + if (json.length > 300) "\n..." else "",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Fermer", color = DarkBackground)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Double confirmation for Clear All
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Effacer toutes les données ?", color = AlertPink) },
            text = {
                Text(
                    text = "Attention : ceci réinitialisera l'historique des conversations et les statistiques de progression. Cette action est irréversible.",
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
                    Text("Confirmer l'effacement", color = Color.White)
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
}
