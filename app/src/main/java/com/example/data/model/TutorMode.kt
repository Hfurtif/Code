package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonCyan

/**
 * Modes opératoires du système.
 * Centré exclusivement sur J.A.R.V.I.S. : majordome vocal exécutif autonome Mark VII.
 */
enum class TutorMode(
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val iconName: String
) {
    JARVIS(
        title = "J.A.R.V.I.S.",
        subtitle = "Majordome vocal d'élite, concis et distingué",
        accentColor = NeonCyan,
        iconName = "Hub"
    )
}
