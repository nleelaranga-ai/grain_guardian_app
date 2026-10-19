package com.vrsec.grainguardian.ui.effects

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class LeafParticleTest {

    @Test
    fun testParticleInitialization() {
        val birthTime = 1000L
        val particle = LeafParticle(
            id = 1L,
            x = 100f,
            y = 200f,
            velocityX = 50f,
            velocityY = -30f,
            rotation = 45f,
            angularVelocity = 90f,
            size = 14f,
            color = Color.Green,
            birthTimeMillis = birthTime,
            lifetimeMillis = 1000L
        )

        assertEquals(1L, particle.id)
        assertEquals(100f, particle.x, 0.001f)
        assertEquals(200f, particle.y, 0.001f)
        assertFalse(particle.isExpired(1500L))
        assertTrue(particle.isExpired(2000L))
        assertTrue(particle.isExpired(2500L))
    }

    @Test
    fun testParticleUpdateMovement() {
        val birthTime = 1000L
        val particle = LeafParticle(
            id = 2L,
            x = 100f,
            y = 100f,
            velocityX = 100f,
            velocityY = 50f,
            rotation = 0f,
            angularVelocity = 60f,
            size = 12f,
            color = Color(0xFF2E7D32),
            birthTimeMillis = birthTime,
            lifetimeMillis = 1000L
        )

        val updated = particle.update(currentTimeMillis = 1100L)
        // Position and rotation should have advanced
        assertNotEquals(particle.x, updated.x, 0.001f)
        assertNotEquals(particle.y, updated.y, 0.001f)
        assertNotEquals(particle.rotation, updated.rotation, 0.001f)
    }

    @Test
    fun testParticleAlphaFadeInOut() {
        val birthTime = 0L
        val lifetime = 1000L
        val particle = LeafParticle(
            id = 3L,
            x = 0f,
            y = 0f,
            velocityX = 0f,
            velocityY = 0f,
            rotation = 0f,
            angularVelocity = 0f,
            size = 10f,
            color = Color.Yellow,
            birthTimeMillis = birthTime,
            lifetimeMillis = lifetime
        )

        // At start (0ms), alpha should be near 0
        assertEquals(0f, particle.alpha(0L), 0.01f)

        // Mid life (400ms), alpha should be 1.0
        assertEquals(1.0f, particle.alpha(400L), 0.01f)

        // Near end of life (950ms), alpha should be fading out (< 0.5)
        val endAlpha = particle.alpha(950L)
        assertTrue("Alpha should fade out near expiration, but was $endAlpha", endAlpha < 0.5f)
        assertTrue("Alpha should remain non-negative, but was $endAlpha", endAlpha >= 0f)
    }

    @Test
    fun testBurstStateSpawning() {
        val state = LeafBurstState()
        assertEquals(0, state.particles.size)

        state.spawnBurst(x = 150f, y = 300f, count = 8, randomSeed = Random(42))
        assertEquals(8, state.particles.size)

        for (p in state.particles) {
            assertEquals(150f, p.x, 0.001f)
            assertEquals(300f, p.y, 0.001f)
            assertTrue("Particle lifetime should be > 500ms", p.lifetimeMillis > 500L)
            assertTrue("Particle size should be > 5", p.size > 5f)
        }
    }

    @Test
    fun testReduceMotionSuppressesBursts() {
        val state = LeafBurstState()
        state.reduceMotion.value = true

        state.spawnBurst(x = 100f, y = 200f, count = 6)
        assertEquals(
            "When reduceMotion is active, no particles must be spawned",
            0,
            state.particles.size
        )
    }

    @Test
    fun testParticleTickAndExpirationCleanup() {
        val state = LeafBurstState()
        state.spawnBurst(x = 50f, y = 50f, count = 5, randomSeed = Random(123))
        assertEquals(5, state.particles.size)

        // Advance time significantly past lifetime (e.g. +3000ms)
        val futureTime = System.currentTimeMillis() + 3000L
        state.tick(futureTime)

        assertEquals("Expired particles must be automatically purged", 0, state.particles.size)
    }

    @Test
    fun testClearParticles() {
        val state = LeafBurstState()
        state.spawnBurst(x = 100f, y = 100f, count = 10)
        assertEquals(10, state.particles.size)

        state.clear()
        assertEquals(0, state.particles.size)
    }
}
