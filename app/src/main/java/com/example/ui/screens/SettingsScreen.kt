package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: CodeViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val voices by viewModel.availableVoices.collectAsState()

    var rateSlider by remember(profile.speechRate) { mutableFloatStateOf(profile.speechRate) }
    var pitchSlider by remember(profile.speechPitch) { mutableFloatStateOf(profile.speechPitch) }
    var customApiKeyInput by remember(profile.geminiApiKey) { mutableStateOf(profile.geminiApiKey) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Screen Header
        Text(
            text = "Réglages & Voix",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Personnalise le nom, la voix et la personnalité de ton compagnon",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Companion Profile & Name Customization Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonViolet,
                    backgroundColor = DarkSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Face, contentDescription = null, tint = NeonViolet)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compagnon Personnel",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                color = NeonViolet.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = profile.companionName.ifBlank { "Code" },
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val genderLabel = when (profile.companionGender.lowercase()) {
                            "boy", "male", "garçon" -> "Garçon 👦 (ami complice)"
                            "girl", "female", "fille" -> "Fille 👧 (amie complice)"
                            else -> "Neutre / Code 🤖"
                        }

                        Text(
                            text = "L'application s'appelle Code. Tu as nommé ton compagnon « ${profile.companionName.ifBlank { "Code" }} » ($genderLabel).",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.showCompanionNamingDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("change_companion_name_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Changer le prénom ou le genre", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Voice Customization Card (Gender, Accent, Pitch, Rate, Audio Preview)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonCyan,
                    backgroundColor = DarkSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Voix de synthèse (SpeechSynthesis)",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Voice Gender Filter: Féminine / Masculine
                        Text(text = "Timbre de voix", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val genders = listOf("female" to "Féminine", "male" to "Masculine")
                            genders.forEach { (genderKey, genderLabel) ->
                                val isSelected = profile.voiceGender == genderKey
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.updateVoiceSettings(
                                                genderKey,
                                                profile.voiceAccent,
                                                rateSlider,
                                                pitchSlider,
                                                ""
                                            )
                                        }
                                        .testTag("voice_gender_$genderKey"),
                                    color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonCyan else DarkCardBorder
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = genderLabel,
                                        color = if (isSelected) NeonCyan else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Accent Selection (American, British, French)
                        Text(text = "Accent cible", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val accents = listOf(
                                "en-US" to "Américain",
                                "en-GB" to "Britannique",
                                "fr-FR" to "Français"
                            )
                            accents.forEach { (accentKey, accentLabel) ->
                                val isSelected = profile.voiceAccent == accentKey
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.updateVoiceSettings(
                                                profile.voiceGender,
                                                accentKey,
                                                rateSlider,
                                                pitchSlider,
                                                ""
                                            )
                                        }
                                        .testTag("voice_accent_$accentKey"),
                                    color = if (isSelected) NeonViolet.copy(alpha = 0.25f) else DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonViolet else DarkCardBorder
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = accentLabel,
                                        color = if (isSelected) NeonViolet else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Speech Rate Slider (0.6x to 1.4x)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Vitesse d'élocution", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "%.1fx".format(rateSlider), color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = rateSlider,
                            onValueChange = {
                                rateSlider = it
                                viewModel.updateVoiceSettings(
                                    profile.voiceGender,
                                    profile.voiceAccent,
                                    rateSlider,
                                    pitchSlider,
                                    profile.voiceId
                                )
                            },
                            valueRange = 0.6f..1.4f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = DarkSurface
                            ),
                            modifier = Modifier.testTag("speech_rate_slider")
                        )

                        // Sample Voice Audio Preview Button
                        Button(
                            onClick = {
                                viewModel.speak("Bonjour ! Je m'appelle ${profile.companionName.ifBlank { "Code" }}, ton compagnon personnel dans l'application Code. Discutons de ce qui te passionne ou de tes sources !")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_voice_sample_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = DarkBackground)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Écouter un échantillon audio", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Continuous Conversation Mode Card (Section 3.3)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = DarkCardBorder
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mode conversation continue",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Le micro se réactive automatiquement après chaque réponse de ${profile.companionName.ifBlank { "Code" }}, comme au téléphone.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = profile.continuousMode,
                            onCheckedChange = { viewModel.toggleContinuousMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = NeonCyan,
                                uncheckedTrackColor = DarkSurface
                            ),
                            modifier = Modifier.testTag("continuous_mode_switch")
                        )
                    }
                }
            }

            // Pedagogical Settings: Correction Frequency & Explanation Level
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = DarkCardBorderViolet
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = NeonViolet)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pédagogie & Corrections",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Fréquence des corrections orales", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val frequencies = listOf("rare" to "Douce", "normal" to "Standard", "often" to "Intensive")
                            frequencies.forEach { (freqKey, freqLabel) ->
                                val isSelected = profile.correctionFrequency == freqKey
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.setCorrectionFrequency(freqKey) },
                                    color = if (isSelected) ElectricBlue.copy(alpha = 0.25f) else DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) ElectricBlue else DarkCardBorder
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = freqLabel,
                                        color = if (isSelected) ElectricBlue else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Langue des explications", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val langs = listOf("fr" to "Français (recommandé A1-B1)", "en" to "Full English (B2-C2)")
                            langs.forEach { (langKey, langLabel) ->
                                val isSelected = profile.explanationLang == langKey
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.setExplanationLang(langKey) },
                                    color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonCyan else DarkCardBorder
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = langLabel,
                                        color = if (isSelected) NeonCyan else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI Brain & Jarvis Engine Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonCyan.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Intelligence Artificielle & Jarvis",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Code intègre le moteur de conversation Jarvis : il discute avec toi comme un véritable ami intime (prend des nouvelles de toi et de ta famille, taquine, rigole, partage ses avis et débat de tes sources sans réciter de cours).",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Clé API Gemini (Optionnelle - pour IA Cloud Gemini 2.5 Flash) :",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = customApiKeyInput,
                            onValueChange = { customApiKeyInput = it },
                            placeholder = { Text("Colle ta clé API Gemini...", fontSize = 12.sp, color = TextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_api_key_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (customApiKeyInput != profile.geminiApiKey) {
                                Button(
                                    onClick = { viewModel.updateGeminiApiKey(customApiKeyInput) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("save_api_key_button")
                                ) {
                                    Text("Enregistrer la clé", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // About & Privacy Statement (Section 0 & 9)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = SuccessGreen.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confidentialité absolue",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "L'application Code fonctionne directement sur ton téléphone. Tes conversations avec ${profile.companionName.ifBlank { "Code" }}, tes connaissances et tes cours sont stockés exclusivement dans la mémoire locale de ton appareil. Aucun compte requis, aucune donnée personnelle collectée.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
