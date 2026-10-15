package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.VoiceCyan

@Composable
fun AudioWaveVisualizer(
    isListening: Boolean,
    isThinking: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveAnimation")

    val animPhase1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase1"
    )

    val animPhase2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase2"
    )

    val animPhase3 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase3"
    )

    val barCount = 19
    val baseHeight = 56.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(baseHeight),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val distanceFromCenter = kotlin.math.abs(i - barCount / 2).toFloat()
            val centerWeight = 1f - (distanceFromCenter / (barCount / 2))

            val heightMultiplier = when {
                isListening -> {
                    // Combine actual RMS level + rhythmic oscillation
                    val phase = when (i % 3) {
                        0 -> animPhase1
                        1 -> animPhase2
                        else -> animPhase3
                    }
                    (0.2f + (rmsLevel * 0.7f * centerWeight) + (phase * 0.3f)).coerceIn(0.15f, 1.0f)
                }
                isThinking -> {
                    // Pulsing thinking wave
                    val phase = when (i % 3) {
                        0 -> animPhase2
                        1 -> animPhase1
                        else -> animPhase3
                    }
                    (0.15f + (phase * 0.6f * centerWeight)).coerceIn(0.15f, 0.95f)
                }
                else -> {
                    // Subtle idle presence
                    (0.12f + (centerWeight * 0.12f)).coerceIn(0.1f, 0.25f)
                }
            }

            val barColor = when {
                isListening -> Brush.verticalGradient(listOf(VoiceCyan, PrimaryBlue))
                isThinking -> Brush.verticalGradient(listOf(Color(0xFFA855F7), PrimaryBlue))
                else -> Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(baseHeight * heightMultiplier)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )

            if (i < barCount - 1) {
                Box(modifier = Modifier.width(4.dp))
            }
        }
    }
}
