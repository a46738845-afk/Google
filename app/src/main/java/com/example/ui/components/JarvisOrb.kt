package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.agent.AgentState
import com.example.ui.theme.CyanCore
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HighRiskRed
import com.example.ui.theme.LowRiskGreen
import com.example.ui.theme.MediumRiskYellow

@Composable
fun JarvisOrb(
    state: AgentState,
    isListening: Boolean,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 110.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb")

    // Pulse animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Dynamic colors based on agent state
    val coreColor = when {
        isListening -> CyanGlow
        state is AgentState.Thinking -> ElectricBlue
        state is AgentState.ExecutingTool -> MediumRiskYellow
        state is AgentState.WaitingConfirmation -> HighRiskRed
        state is AgentState.Speaking -> LowRiskGreen
        state is AgentState.Error -> HighRiskRed
        else -> CyanCore
    }

    val glowColor = coreColor.copy(alpha = 0.45f)

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            val baseRadius = (size.toPx() / 2) * 0.72f

            // Dynamic expansion when listening or speaking
            val dynamicBoost = if (isListening) audioLevel * 25f else 0f
            val currentRadius = (baseRadius * pulseScale) + dynamicBoost

            // 1. Outer Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = center,
                    radius = currentRadius * 1.35f
                ),
                radius = currentRadius * 1.35f,
                center = center
            )

            // 2. Outer Technological Ring
            drawCircle(
                color = coreColor.copy(alpha = 0.6f),
                radius = currentRadius * 1.05f,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // 3. Dashed / segmented orbital arcs
            val arcCount = 4
            val arcAngle = 55f
            for (i in 0 until arcCount) {
                val start = rotation + (i * 90f)
                drawArc(
                    color = coreColor,
                    startAngle = start,
                    sweepAngle = arcAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - currentRadius * 0.9f, center.y - currentRadius * 0.9f),
                    size = androidx.compose.ui.geometry.Size(currentRadius * 1.8f, currentRadius * 1.8f),
                    style = Stroke(width = 3f)
                )
            }

            // 4. Inner Luminous Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, coreColor, coreColor.copy(alpha = 0.2f)),
                    center = center,
                    radius = currentRadius * 0.65f
                ),
                radius = currentRadius * 0.65f,
                center = center
            )

            // 5. Center Arc Reactor Point
            drawCircle(
                color = Color.White,
                radius = 6f + (if (isListening) audioLevel * 8f else 0f),
                center = center
            )
        }
    }
}
