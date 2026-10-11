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
    FRIEND(
        title = "Ami omniscient",
        subtitle = "Ton ami qui sait tout, disponible en ligne et hors ligne",
        accentColor = NeonCyan,
        iconName = "ChatBubble"
    ),
    ENGLISH_TEACHER(
        title = "Professeur d'anglais",
        subtitle = "Activé à ta demande pour pratiquer et apprendre l'anglais",
        accentColor = ElectricBlue,
        iconName = "Language"
    ),
    MATH_TEACHER(
        title = "Professeur de mathématiques",
        subtitle = "Activé à ta demande pour les mathématiques et calculs",
        accentColor = NeonViolet,
        iconName = "Calculate"
    ),
    CUSTOM_TEACHER(
        title = "Professeur particulier",
        subtitle = "Activé pour t'enseigner tout sujet que tu demandes",
        accentColor = AlertPink,
        iconName = "School"
    )
}
