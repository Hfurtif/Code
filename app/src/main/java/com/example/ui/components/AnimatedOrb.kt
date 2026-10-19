package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.OfflineAmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState {
    IDLE,       // Repos: slow breathing, gentle glow
    LISTENING,  // Écoute: pulses with voice volume
    THINKING,   // Réflexion: rotates with orbiting particles
    SPEAKING,   // Parole: ripples and undulates
    OFFLINE     // Hors ligne: amber/violet hue
}

@Composable
fun AnimatedOrb(
    state: OrbState,
    audioRms: Float = 0f, // 0f to 1f
    size: Dp = 230.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransitions")

    // Slow breathing animation (Idle)
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreatheScale"
    )

    // Fast rotation for Thinking particles
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )

    // Wave phase for Speaking ripples
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2.6f

            when (state) {
                OrbState.IDLE -> {
                    val radius = baseRadius * breatheScale
                    // Outer ambient halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.28f), NeonViolet.copy(alpha = 0.08f), Color.Transparent),
                            center = center,
                            radius = radius * 1.5f
                        ),
                        radius = radius * 1.5f,
                        center = center
                    )
                    // Inner glowing orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, NeonCyan, NeonMagenta, DarkBackground),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                    // Core perimeter ring
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.7f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                }

                OrbState.LISTENING -> {
                    // Reactive to microphone audio level
                    val reactiveScale = 0.95f + (audioRms * 0.45f)
                    val radius = baseRadius * reactiveScale

                    // Energetic soundwave rings
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.25f + audioRms * 0.35f),
                        radius = radius * 1.35f,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.45f),
                        radius = radius * 1.18f,
                        center = center,
                        style = Stroke(width = 3.5f)
                    )
                    // Glowing active core
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, NeonCyan, ElectricBlue, DarkBackground),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 4f)
                    )
                }

                OrbState.THINKING -> {
                    val radius = baseRadius * 0.95f

                    // Deep swirling background
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonMagenta, NeonViolet, ElectricBlue, DarkBackground),
                            center = center,
                            radius = radius * 1.3f
                        ),
                        radius = radius * 1.3f,
                        center = center
                    )

                    // Orbiting rings
                    drawCircle(
                        color = NeonViolet.copy(alpha = 0.5f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2f)
                    )

                    // Orbiting particles around the sphere
                    val particleCount = 8
                    for (i in 0 until particleCount) {
                        val angleDeg = rotationAngle + (i * (360f / particleCount))
                        val angleRad = (angleDeg * PI / 180.0).toFloat()
                        val orbitRadius = radius * (1.1f + 0.12f * sin(angleRad * 2f))
                        val px = center.x + orbitRadius * cos(angleRad)
                        val py = center.y + orbitRadius * sin(angleRad)
                        val pColor = if (i % 2 == 0) NeonCyan else NeonMagenta

                        drawCircle(
                            color = pColor,
                            radius = 5.5f,
                            center = Offset(px, py)
                        )
                    }
                }

                OrbState.SPEAKING -> {
                    val radius = baseRadius * 0.98f

                    // Radiant pulse glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.35f), NeonMagenta.copy(alpha = 0.18f), Color.Transparent),
                            center = center,
                            radius = radius * 1.4f
                        ),
                        radius = radius * 1.4f,
                        center = center
                    )

                    // Undulating waveform boundary (16 points on a wavy circle)
                    val wavePath = Path()
                    val points = 36
                    for (i in 0..points) {
                        val angle = (i.toFloat() / points) * (2 * PI).toFloat()
                        // Ripple formula: base + sin(frequency * angle + phase)
                        val waveOffset = (sin(angle * 5f + wavePhase) * 12f) + (cos(angle * 3f + wavePhase * 1.5f) * 6f)
                        val r = radius + waveOffset
                        val x = center.x + r * cos(angle)
                        val y = center.y + r * sin(angle)
                        if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                    }
                    wavePath.close()

                    drawPath(
                        path = wavePath,
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, NeonCyan, NeonMagenta, NeonViolet, ElectricBlue),
                            center = center,
                            radius = radius * 1.1f
                        )
                    )

                    drawPath(
                        path = wavePath,
                        color = Color.White.copy(alpha = 0.85f),
                        style = Stroke(width = 3.5f)
                    )
                }

                OrbState.OFFLINE -> {
                    val radius = baseRadius * breatheScale
                    // Amber / violet offline glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(OfflineAmber.copy(alpha = 0.25f), NeonViolet.copy(alpha = 0.1f), Color.Transparent),
                            center = center,
                            radius = radius * 1.35f
                        ),
                        radius = radius * 1.35f,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD54F), OfflineAmber, Color(0xFF4A148C), DarkBackground),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = OfflineAmber.copy(alpha = 0.8f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                }
            }
        }
    }
}
