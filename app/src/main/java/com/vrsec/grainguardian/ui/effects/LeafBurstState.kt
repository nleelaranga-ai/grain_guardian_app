package com.vrsec.grainguardian.ui.effects

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class LeafBurstState {
    private val nextId = AtomicLong(1)
    val particles = mutableStateListOf<LeafParticle>()
    val reduceMotion = mutableStateOf(false)

    private val leafPalette = listOf(
        Color(0xFF2E7D32), // Deep Grain Green
        Color(0xFF4CAF50), // Fresh Leaf Green
        Color(0xFF81C784), // Sprout Green
        Color(0xFFEAB308), // Harvest Golden Paddy
        Color(0xFFD97706)  // Sun-dried Amber
    )

    fun spawnBurst(x: Float, y: Float, count: Int = 6, randomSeed: Random = Random.Default) {
        if (reduceMotion.value) return

        val now = System.currentTimeMillis()
        for (i in 0 until count) {
            val angle = randomSeed.nextDouble(0.0, Math.PI * 2)
            val speed = randomSeed.nextFloat() * 180f + 80f
            val vx = (cos(angle) * speed).toFloat()
            // Bias upwards initially to simulate leaves catching an updraft
            val vy = (sin(angle) * speed * 0.7f - 60f).toFloat()
            val rot = randomSeed.nextFloat() * 360f
            val aRot = (randomSeed.nextFloat() - 0.5f) * 240f
            val size = randomSeed.nextFloat() * 8f + 10f
            val color = leafPalette[randomSeed.nextInt(leafPalette.size)]
            val lifetime = randomSeed.nextLong(750, 1150)

            particles.add(
                LeafParticle(
                    id = nextId.getAndIncrement(),
                    x = x,
                    y = y,
                    velocityX = vx,
                    velocityY = vy,
                    rotation = rot,
                    angularVelocity = aRot,
                    size = size,
                    color = color,
                    birthTimeMillis = now,
                    lifetimeMillis = lifetime
                )
            )
        }
    }

    fun tick(currentTimeMillis: Long) {
        if (particles.isEmpty()) return

        val updated = mutableListOf<LeafParticle>()
        for (p in particles) {
            if (!p.isExpired(currentTimeMillis)) {
                updated.add(p.update(currentTimeMillis))
            }
        }
        particles.clear()
        particles.addAll(updated)
    }

    fun clear() {
        particles.clear()
    }
}
