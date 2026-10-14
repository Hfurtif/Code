package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class CompanionPersonaType(val label: String, val defaultVoiceGender: String) {
    BOY("Garçon", "male"),
    GIRL("Fille", "female"),
    NEUTRAL("Code / Neutre", "female")
}

@Composable
fun CompanionNamingDialog(
    initialName: String = "Code",
    initialGender: String = "neutral",
    initialVoiceGender: String = "female",
    isFirstLaunch: Boolean = false,
    onConfirm: (name: String, gender: String, voiceGender: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember {
        mutableStateOf(
            when (initialGender.lowercase()) {
                "boy", "male", "garçon" -> CompanionPersonaType.BOY
                "girl", "female", "fille" -> CompanionPersonaType.GIRL
                else -> CompanionPersonaType.NEUTRAL
            }
        )
    }

    var companionName by remember {
        mutableStateOf(if (initialName.isNotBlank() && initialName != "Code") initialName else "Code")
    }

    var selectedVoiceGender by remember {
        mutableStateOf(initialVoiceGender.ifBlank { selectedType.defaultVoiceGender })
    }

    val boySuggestions = listOf("Léo", "Lucas", "Lilian", "Arthur", "Thomas", "Alex", "Hugo", "Nathan")
    val girlSuggestions = listOf("Lina", "Maya", "Léa", "Sarah", "Emma", "Chloé", "Jade", "Inès")
    val neutralSuggestions = listOf("Code", "Jarvis", "Nova", "Orion", "Pixel", "Atlas", "Echo")

    val currentSuggestions = when (selectedType) {
        CompanionPersonaType.BOY -> boySuggestions
        CompanionPersonaType.GIRL -> girlSuggestions
        CompanionPersonaType.NEUTRAL -> neutralSuggestions
    }

    AlertDialog(
        onDismissRequest = {
            if (!isFirstLaunch) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isFirstLaunch,
            dismissOnClickOutside = !isFirstLaunch
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NeonViolet.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeonViolet,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isFirstLaunch) "Bienvenue dans Code !" else "Personnaliser ton compagnon",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Donne un nom et une voix à ton ami(e)",
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "L'application s'appelle Code. Choisis maintenant comment tu souhaites appeler ton compagnon IA personnel au quotidien :",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selector: Boy / Girl / Neutral
                Text(
                    text = "Type de compagnon :",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompanionPersonaType.values().forEach { type ->
                        val isSelected = selectedType == type
                        Surface(
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) NeonCyan else DarkCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedType = type
                                    selectedVoiceGender = type.defaultVoiceGender
                                    if (companionName.isBlank() || companionName == "Code" || boySuggestions.contains(companionName) || girlSuggestions.contains(companionName)) {
                                        companionName = when (type) {
                                            CompanionPersonaType.BOY -> "Léo"
                                            CompanionPersonaType.GIRL -> "Lina"
                                            CompanionPersonaType.NEUTRAL -> "Code"
                                        }
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = when (type) {
                                        CompanionPersonaType.BOY -> Icons.Default.Male
                                        CompanionPersonaType.GIRL -> Icons.Default.Female
                                        CompanionPersonaType.NEUTRAL -> Icons.Default.SmartToy
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) NeonCyan else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = type.label,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name TextField
                Text(
                    text = "Prénom de ton compagnon :",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = companionName,
                    onValueChange = { companionName = it.take(25) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("companion_name_input"),
                    placeholder = { Text("Ex: Lina, Léo, Maya, Code…", color = TextSecondary.copy(alpha = 0.6f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = NeonCyan
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick suggestions chips
                Text(
                    text = "Suggestions rapides :",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentSuggestions.forEach { suggestion ->
                        val isChosen = companionName.equals(suggestion, ignoreCase = true)
                        Surface(
                            color = if (isChosen) NeonViolet.copy(alpha = 0.3f) else DarkSurfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isChosen) NeonViolet else DarkCardBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { companionName = suggestion }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = suggestion,
                                color = if (isChosen) NeonCyan else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice gender choice
                Text(
                    text = "Voix de synthèse (TTS) :",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "female" to "Voix Féminine",
                        "male" to "Voix Masculine"
                    ).forEach { (voiceKey, voiceLabel) ->
                        val isSelected = selectedVoiceGender == voiceKey
                        Surface(
                            color = if (isSelected) ElectricBlue.copy(alpha = 0.25f) else DarkSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) ElectricBlue else DarkCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedVoiceGender = voiceKey }
                                .padding(vertical = 8.dp, horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = voiceLabel,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = companionName.trim().ifBlank { "Code" }
                    val finalGender = when (selectedType) {
                        CompanionPersonaType.BOY -> "boy"
                        CompanionPersonaType.GIRL -> "girl"
                        CompanionPersonaType.NEUTRAL -> "neutral"
                    }
                    onConfirm(finalName, finalGender, selectedVoiceGender)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_companion_name_button")
            ) {
                Text(
                    text = "Enchanter & Commencer",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        dismissButton = {
            if (!isFirstLaunch) {
                TextButton(onClick = onDismiss) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        },
        containerColor = DarkSurfaceVariant,
        shape = RoundedCornerShape(20.dp)
    )
}
