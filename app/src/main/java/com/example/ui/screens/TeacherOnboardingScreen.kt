package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.theme.NeonMagenta
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class TeacherPersona(
    val id: String,
    val title: String,
    val subtitle: String,
    val defaultName: String,
    val defaultVoiceGender: String,
    val icon: ImageVector,
    val accentColor: Color,
    val description: String
) {
    BOY(
        id = "boy",
        title = "Garçon",
        subtitle = "Dynamique & Complice",
        defaultName = "Alex",
        defaultVoiceGender = "male",
        icon = Icons.Default.Male,
        accentColor = NeonCyan,
        description = "Un style moderne, énergique et motivant. Idéal pour pratiquer sans complexe et parler comme un natif."
    ),
    GIRL(
        id = "girl",
        title = "Fille",
        subtitle = "Bienveillante & Pédagogue",
        defaultName = "Sarah",
        defaultVoiceGender = "female",
        icon = Icons.Default.Female,
        accentColor = AlertPink,
        description = "Une professeure chaleureuse, patiente et très à l'écoute. Parfait pour prendre confiance pas à pas."
    ),
    NEUTRAL(
        id = "neutral",
        title = "Code / Neutre",
        subtitle = "IA Précise & Adaptative",
        defaultName = "Code",
        defaultVoiceGender = "female",
        icon = Icons.Default.SmartToy,
        accentColor = NeonViolet,
        description = "Une intelligence artificielle analytique et polyvalente. Corrige avec rigueur et s'adapte à tous tes sujets."
    )
}

@Composable
fun TeacherOnboardingScreen(
    viewModel: CodeViewModel,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val profile by viewModel.userProfile.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

    // Selected Persona
    var selectedPersona by remember {
        mutableStateOf(
            when (profile.companionGender.lowercase()) {
                "boy", "male", "garçon" -> TeacherPersona.BOY
                "girl", "female", "fille" -> TeacherPersona.GIRL
                else -> TeacherPersona.NEUTRAL
            }
        )
    }

    // Teacher Custom Name
    var teacherName by remember {
        mutableStateOf(
            if (profile.companionName.isNotBlank() && profile.companionName != "Code") {
                profile.companionName
            } else {
                selectedPersona.defaultName
            }
        )
    }

    // Voice gender and accent
    var voiceGender by remember {
        mutableStateOf(if (profile.voiceGender.isNotBlank()) profile.voiceGender else selectedPersona.defaultVoiceGender)
    }
    var voiceAccent by remember {
        mutableStateOf(if (profile.voiceAccent.isNotBlank()) profile.voiceAccent else "en-US")
    }

    // CEFR Level
    var selectedLevel by remember {
        mutableStateOf(if (profile.level.isNotBlank()) profile.level else "B1")
    }

    // Suggestions lists
    val boySuggestions = listOf("Alex", "Leo", "Lucas", "Oliver", "Arthur", "Thomas", "Nathan")
    val girlSuggestions = listOf("Sarah", "Lina", "Emma", "Maya", "Sophie", "Chloe", "Jade")
    val neutralSuggestions = listOf("Code", "Nova", "Atlas", "Orion", "Jarvis", "Echo", "Pixel")

    val currentSuggestions = when (selectedPersona) {
        TeacherPersona.BOY -> boySuggestions
        TeacherPersona.GIRL -> girlSuggestions
        TeacherPersona.NEUTRAL -> neutralSuggestions
    }

    // Pulse animation for voice preview
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with optional Close button (if reopened from settings)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = NeonViolet.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PROFESSEUR D'ANGLAIS IA",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                if (onDismiss != null && profile.hasCompletedOnboarding) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Hero App Icon Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(3.dp, NeonCyan),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_icon),
                    contentDescription = "Code Companion Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Title & Hook
            Text(
                text = "Crée ton Professeur",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Personnalise son nom, sa voix et son style pour une immersion totale et sur-mesure.",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // STEP 1: Persona Choice (Boy, Girl, Neutral)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = NeonCyan.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("1", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Choisis le persona du professeur",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Persona Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TeacherPersona.values().forEach { persona ->
                        val isSelected = selectedPersona == persona
                        val borderColor = if (isSelected) persona.accentColor else DarkCardBorder

                        Surface(
                            color = if (isSelected) persona.accentColor.copy(alpha = 0.15f) else DarkSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedPersona = persona
                                    voiceGender = persona.defaultVoiceGender
                                    if (teacherName.isBlank() ||
                                        boySuggestions.contains(teacherName) ||
                                        girlSuggestions.contains(teacherName) ||
                                        neutralSuggestions.contains(teacherName) ||
                                        teacherName == "Code"
                                    ) {
                                        teacherName = persona.defaultName
                                    }
                                }
                                .testTag("persona_option_${persona.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 14.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) persona.accentColor.copy(alpha = 0.25f)
                                            else DarkSurfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = persona.icon,
                                        contentDescription = persona.title,
                                        tint = if (isSelected) persona.accentColor else TextSecondary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = persona.title,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = persona.subtitle,
                                    color = if (isSelected) persona.accentColor else TextMuted,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Persona description box
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = selectedPersona.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = selectedPersona.description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // STEP 2: Custom Name Input
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = NeonCyan.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("2", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Donne-lui un prénom personnalisé",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it.take(25) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("teacher_name_input"),
                    placeholder = {
                        Text(
                            "Ex: ${selectedPersona.defaultName}, Sarah, Alex, Arthur...",
                            color = TextSecondary.copy(alpha = 0.5f)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedPersona.accentColor,
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
                            tint = selectedPersona.accentColor
                        )
                    },
                    trailingIcon = {
                        if (teacherName.isNotBlank()) {
                            IconButton(onClick = { teacherName = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Effacer",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick suggestions chips
                Text(
                    text = "Prénoms suggérés (${selectedPersona.title}) :",
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
                        val isChosen = teacherName.equals(suggestion, ignoreCase = true)
                        Surface(
                            color = if (isChosen) selectedPersona.accentColor.copy(alpha = 0.25f) else DarkSurfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isChosen) selectedPersona.accentColor else DarkCardBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { teacherName = suggestion }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("name_suggestion_$suggestion")
                        ) {
                            Text(
                                text = suggestion,
                                color = if (isChosen) selectedPersona.accentColor else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // STEP 3: Voice & Accent Preview Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = NeonViolet,
                backgroundColor = DarkSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = NeonViolet.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("3", color = NeonViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Voix & Prononciation",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Voice Gender choice
                    Text(
                        text = "Genre de la voix :",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "female" to "Voix Féminine",
                            "male" to "Voix Masculine"
                        ).forEach { (vKey, vLabel) ->
                            val isSelected = voiceGender == vKey
                            Surface(
                                color = if (isSelected) NeonViolet.copy(alpha = 0.25f) else DarkSurfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonViolet else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { voiceGender = vKey }
                                    .padding(vertical = 8.dp, horizontal = 8.dp)
                                    .testTag("voice_gender_$vKey")
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
                                        text = vLabel,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Accent choice: US vs UK
                    Text(
                        text = "Accent d'anglais :",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "en-US" to "🇺🇸 Américain (US)",
                            "en-GB" to "🇬🇧 Britannique (UK)"
                        ).forEach { (accKey, accLabel) ->
                            val isSelected = voiceAccent == accKey
                            Surface(
                                color = if (isSelected) ElectricBlue.copy(alpha = 0.25f) else DarkSurfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ElectricBlue else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { voiceAccent = accKey }
                                    .padding(vertical = 8.dp, horizontal = 8.dp)
                                    .testTag("voice_accent_$accKey")
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
                                        text = accLabel,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preview Button
                    val previewCleanName = teacherName.trim().ifBlank { selectedPersona.defaultName }
                    Button(
                        onClick = {
                            val sample = when (selectedPersona) {
                                TeacherPersona.BOY -> "Hey! I'm $previewCleanName. Let's make your English speak smooth and confident!"
                                TeacherPersona.GIRL -> "Hello! I'm $previewCleanName. I'm excited to practice English with you today!"
                                TeacherPersona.NEUTRAL -> "Hello. I am $previewCleanName, your personal AI tutor. Let's begin our session."
                            }
                            viewModel.previewTeacherVoice(sample, voiceGender, voiceAccent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        border = BorderStroke(1.dp, if (isSpeaking) NeonCyan else DarkCardBorderViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("preview_teacher_voice_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = if (isSpeaking) NeonCyan else NeonViolet,
                                modifier = Modifier
                                    .size(18.dp)
                                    .scale(if (isSpeaking) pulseScale else 1.0f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSpeaking) "Écoute en cours…" else "Tester la voix de $previewCleanName",
                                color = if (isSpeaking) NeonCyan else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // STEP 4: English Level (CEFR)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = NeonCyan.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("4", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ton niveau d'anglais actuel",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("A1-A2", "Débutant", "Bases & simple"),
                        Triple("B1-B2", "Intermédiaire", "Conversation"),
                        Triple("C1-C2", "Avancé", "Fluide & pro")
                    ).forEach { (lvlCode, lvlName, lvlDesc) ->
                        val isSelected = selectedLevel == lvlCode || (lvlCode == "B1-B2" && (selectedLevel == "B1" || selectedLevel == "B2"))
                        Surface(
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) NeonCyan else DarkCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedLevel = lvlCode }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("level_option_$lvlCode")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = lvlCode,
                                    color = if (isSelected) NeonCyan else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = lvlName,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = lvlDesc,
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Primary Call to Action: Start with customized teacher
            val finalCleanName = teacherName.trim().ifBlank { selectedPersona.defaultName }

            Button(
                onClick = {
                    val finalGender = selectedPersona.id
                    viewModel.completeOnboarding(
                        teacherName = finalCleanName,
                        personaGender = finalGender,
                        voiceGender = voiceGender,
                        voiceAccent = voiceAccent,
                        level = selectedLevel
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("complete_onboarding_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Commencer avec $finalCleanName",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tu pourras modifier le prénom, la voix ou le persona à tout moment dans les Réglages.",
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
