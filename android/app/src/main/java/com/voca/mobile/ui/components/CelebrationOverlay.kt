package com.voca.mobile.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.voca.mobile.ui.theme.DuoBlue
import com.voca.mobile.ui.theme.DuoGreen
import com.voca.mobile.ui.theme.DuoPurple
import com.voca.mobile.ui.theme.DuoRed
import com.voca.mobile.ui.theme.DuoYellow
import kotlin.random.Random

private data class Confetto(
    val x: Float,
    val startY: Float,
    val endY: Float,
    val drift: Float,
    val color: Color,
    val size: Float,
    val delay: Float,
)

/**
 * A lightweight, dependency-free confetti burst. Renders a one-shot fall of
 * colored squares over the full size of its parent. Drop it into a Box on top
 * of the celebratory content.
 */
@Composable
fun CelebrationOverlay(modifier: Modifier = Modifier, pieces: Int = 60) {
    val colors = listOf(DuoGreen, DuoBlue, DuoYellow, DuoRed, DuoPurple)
    val confetti = remember {
        List(pieces) {
            Confetto(
                x = Random.nextFloat(),
                startY = -Random.nextFloat() * 0.2f,
                endY = 0.7f + Random.nextFloat() * 0.4f,
                drift = (Random.nextFloat() - 0.5f) * 0.3f,
                color = colors[Random.nextInt(colors.size)],
                size = 10f + Random.nextFloat() * 14f,
                delay = Random.nextFloat() * 0.25f,
            )
        }
    }

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 1600, easing = LinearEasing),
        label = "confetti",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        confetti.forEach { c ->
            val local = ((progress - c.delay) / (1f - c.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val y = (c.startY + (c.endY - c.startY) * local) * size.height
            val x = (c.x + c.drift * local) * size.width
            val alpha = (1f - local).coerceIn(0f, 1f)
            drawRect(
                color = c.color.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(c.size, c.size),
            )
        }
    }
}
