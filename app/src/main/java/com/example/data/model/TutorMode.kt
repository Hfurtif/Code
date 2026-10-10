package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AlertPink
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet

enum class TutorMode(
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val iconName: String
) {
    ENGLISH(
        title = "Professeur d'anglais",
        subtitle = "Conversation, grammaire & corrections en direct",
        accentColor = NeonCyan,
        iconName = "Language"
    ),
    PERSONAL_GROWTH(
        title = "Développement personnel",
        subtitle = "Habitudes, motivation, organisation & défis",
        accentColor = NeonViolet,
        iconName = "EmojiEvents"
    ),
    PSYCHOLOGY(
        title = "Psychologie & Communication",
        subtitle = "Écoute active, émotions & conversations clés",
        accentColor = ElectricBlue,
        iconName = "Psychology"
    ),
    FREE_DISCUSSION(
        title = "Discussion libre",
        subtitle = "Tout sujet, échange spontané sans contrainte",
        accentColor = AlertPink,
        iconName = "ChatBubble"
    )
}
