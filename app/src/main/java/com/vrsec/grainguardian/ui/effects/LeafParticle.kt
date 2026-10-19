package com.vrsec.grainguardian.ui.effects

import androidx.compose.ui.graphics.Color
import kotlin.math.sin

data class LeafParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val rotation: Float,
    val angularVelocity: Float,
    val size: Float,
    val color: Color,
    val birthTimeMillis: Long,
    val lifetimeMillis: Long,
    val flutterFrequency: Float = 6f
) {
    fun update(currentTimeMillis: Long): LeafParticle {
        val elapsed = (currentTimeMillis - birthTimeMillis).coerceAtLeast(0L)
        val progress = (elapsed.toFloat() / lifetimeMillis.toFloat()).coerceIn(0f, 1f)
        val dt = 0.016f

        // Horizontal sway (flutter) and gentle gravity pull
        val flutter = sin(progress * flutterFrequency * Math.PI.toFloat()) * 25f
        val newX = x + (velocityX + flutter) * dt
        val newY = y + (velocityY + 160f * progress) * dt
        val newRotation = rotation + angularVelocity * dt

        return copy(
            x = newX,
            y = newY,
            rotation = newRotation
        )
    }

    fun alpha(currentTimeMillis: Long): Float {
        val elapsed = (currentTimeMillis - birthTimeMillis).coerceAtLeast(0L)
        val progress = (elapsed.toFloat() / lifetimeMillis.toFloat()).coerceIn(0f, 1f)
        return when {
            progress < 0.15f -> progress / 0.15f
            progress > 0.65f -> 1f - ((progress - 0.65f) / 0.35f)
            else -> 1f
        }
    }

    fun isExpired(currentTimeMillis: Long): Boolean {
        return (currentTimeMillis - birthTimeMillis) >= lifetimeMillis
    }
}
