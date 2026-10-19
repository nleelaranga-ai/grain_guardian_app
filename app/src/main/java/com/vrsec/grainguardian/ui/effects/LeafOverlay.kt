package com.vrsec.grainguardian.ui.effects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput

val LocalLeafBurstState = staticCompositionLocalOf { LeafBurstState() }

fun Modifier.leafBurst(state: LeafBurstState): Modifier = this.pointerInput(state) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        state.spawnBurst(down.position.x, down.position.y)
    }
}

@Composable
fun LeafOverlay(
    state: LeafBurstState,
    modifier: Modifier = Modifier
) {
    if (state.reduceMotion.value || state.particles.isEmpty()) return

    var currentMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(state.particles.isNotEmpty()) {
        while (state.particles.isNotEmpty()) {
            withFrameMillis {
                currentMillis = System.currentTimeMillis()
                state.tick(currentMillis)
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val leafPath = Path()
        for (p in state.particles) {
            val alpha = p.alpha(currentMillis)
            if (alpha <= 0.01f) continue

            drawLeaf(p, alpha, leafPath)
        }
    }
}

private fun DrawScope.drawLeaf(
    particle: LeafParticle,
    alpha: Float,
    reusablePath: Path
) {
    val halfLen = particle.size
    val halfWidth = particle.size * 0.45f

    reusablePath.reset()
    reusablePath.moveTo(0f, -halfLen)
    reusablePath.quadraticBezierTo(halfWidth, 0f, 0f, halfLen)
    reusablePath.quadraticBezierTo(-halfWidth, 0f, 0f, -halfLen)
    reusablePath.close()

    rotate(degrees = particle.rotation, pivot = Offset(particle.x, particle.y)) {
        translate(left = particle.x, top = particle.y) {
            drawPath(
                path = reusablePath,
                color = particle.color.copy(alpha = alpha.coerceIn(0f, 1f))
            )
        }
    }
}
