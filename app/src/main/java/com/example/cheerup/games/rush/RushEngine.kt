package com.example.cheerup.games.rush

import androidx.compose.ui.geometry.Offset
import com.example.cheerup.core.score.ScoreManager
import com.example.cheerup.domain.model.GameIdentifier
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class RushEngine {

    companion object {
        const val secondsPerPoint: Double = 3.0
    }

    // Playfield Bounds (Portrait Logical Coordinates: y=0 at bottom, y=worldHeight at top)
    var worldWidth: Float = 393.0f
        private set
    var worldHeight: Float = 852.0f
        private set

    private val minShipX: Float get() = 26.0f
    private val maxShipX: Float get() = worldWidth - 26.0f
    private val minShipY: Float get() = 86.0f
    private val maxShipY: Float get() = worldHeight - 190.0f

    // Player Spaceship State
    val shipCollisionRadius: Float = 15.0f
    var shipX: Float = 196.5f
        private set
    var shipY: Float = 190.0f
        private set
    var shipVX: Float = 0.0f
        private set
    var shipTilt: Float = 0.0f
        private set

    // Game State
    var score: Int = 0
        private set
    var nearMissStreak: Int = 0
        private set
    var isGameOver: Boolean = false
        private set
    var hasUsedRewardedContinue: Boolean = false
        private set
    private var hasSubmittedScoreForCurrentEnding: Boolean = false
    var elapsedTime: Double = 0.0
        private set

    val asteroids = mutableListOf<RushMeteor>()
    val collectibles = mutableListOf<RushCollectible>()
    val activeEffects = mutableListOf<ActiveRushEffect>()

    // Surprise Event State
    var pendingWarningEvent: RushSpecialEventKind? = null
        private set
    var pendingSafeCorridorX: Float? = null
        private set
    private var warningTimer: Double = 0.0
    var activeSpecialEvent: RushSpecialEventKind? = null
        private set
    private var specialEventTimer: Double = 0.0

    // Spawning & Timers
    private var meteorSpawnTimer: Double = 0.0
    private var powerUpSpawnTimer: Double = 6.5
    private var nextSurpriseEventTime: Double = 15.0
    var invulnerabilityTimer: Double = 0.0
        private set

    val hasShield: Boolean
        get() = activeEffects.any { it.type == RushPowerUpType.SHIELD }

    val isSlowTimeActive: Boolean
        get() = activeEffects.any { it.type == RushPowerUpType.SLOW_TIME }

    val isFlameBoostActive: Boolean
        get() = activeEffects.any { it.type == RushPowerUpType.FLAME_BOOST }

    init {
        reset()
    }

    fun updateWorldSize(width: Float, height: Float) {
        if (width <= 100f || height <= 200f) return
        worldWidth = width
        worldHeight = height
        shipX = min(max(shipX, minShipX), maxShipX)
        shipY = min(max(shipY, minShipY), maxShipY)
    }

    fun reset() {
        score = 0
        nearMissStreak = 0
        isGameOver = false
        hasUsedRewardedContinue = false
        hasSubmittedScoreForCurrentEnding = false
        elapsedTime = 0.0

        shipX = worldWidth * 0.5f
        shipY = min(max(worldHeight * 0.24f, minShipY), maxShipY)
        shipVX = 0.0f
        shipTilt = 0.0f

        asteroids.clear()
        collectibles.clear()
        activeEffects.clear()

        pendingWarningEvent = null
        pendingSafeCorridorX = null
        warningTimer = 0.0
        activeSpecialEvent = null
        specialEventTimer = 0.0

        meteorSpawnTimer = 0.45
        powerUpSpawnTimer = 6.5
        nextSurpriseEventTime = 15.0
        invulnerabilityTimer = 0.0
    }

    fun setInvulnerable(duration: Double) {
        invulnerabilityTimer = duration
    }

    fun continueAfterReward() {
        if (hasUsedRewardedContinue) return
        hasUsedRewardedContinue = true
        hasSubmittedScoreForCurrentEnding = false
        isGameOver = false

        shipVX = 0.0f
        shipTilt = 0.0f

        asteroids.removeAll { meteor ->
            hypot(meteor.x - shipX, meteor.y - shipY) < 95.0f
        }

        invulnerabilityTimer = 3.5
    }

    fun update(rawDt: Double, targetShipPosition: Offset?): List<RushEvent> {
        if (isGameOver) return emptyList()
        val dt = min(max(rawDt, 0.0), 1.0 / 20.0)
        val events = mutableListOf<RushEvent>()

        val survivalMultiplier = if (isFlameBoostActive) 2.25 else 1.0
        elapsedTime += dt * survivalMultiplier
        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer = max(0.0, invulnerabilityTimer - dt)
        }

        // 1. Survival Duration Scoring: strictly 3 seconds = 1 point
        val targetScore = ((elapsedTime + 0.0001) / secondsPerPoint).toInt()
        if (targetScore > score) {
            score = targetScore
            events.add(RushEvent.ScoreChanged(score))
        }

        // 2. Smooth 1-finger ship movement
        updateShipMovement(dt, targetShipPosition)

        // 3. Active Power-Up Orb Timers
        for (i in activeEffects.indices.reversed()) {
            activeEffects[i].remaining -= dt
            if (activeEffects[i].remaining <= 0) {
                val expired = activeEffects.removeAt(i)
                if (expired.type == RushPowerUpType.FLAME_BOOST) {
                    invulnerabilityTimer = max(invulnerabilityTimer, 0.45)
                }
            }
        }

        // 4. Surprise Event Warning & Execution
        updateSpecialEvents(dt, events)

        // 5. Progressive Meteor Spawning
        val speedScale = when {
            isFlameBoostActive -> 2.35f
            isSlowTimeActive -> 0.50f
            else -> 1.0f
        }
        meteorSpawnTimer -= dt * speedScale
        if (meteorSpawnTimer <= 0) {
            spawnRandomMeteor()
            val ramp = min(1.0, elapsedTime / 90.0)
            val lateBonus = min(0.12, max(0.0, elapsedTime - 90.0) * 0.0018)
            val baseInterval = 0.76 - ramp * 0.45 - lateBonus
            val eventFactor = if (activeSpecialEvent == RushSpecialEventKind.METEOR_SHOWER) 0.52 else 1.0
            meteorSpawnTimer = max(0.20, baseInterval * eventFactor * Random.nextDouble(0.84, 1.16))
        }

        // 6. Little Power-Up Orb Spawning
        powerUpSpawnTimer -= dt
        if (powerUpSpawnTimer <= 0) {
            spawnPowerUpOrb()
            powerUpSpawnTimer = Random.nextDouble(8.5, 13.0)
        }

        // 7. Update Collectibles & Check Pickup
        updateCollectibles(dt, events)

        // 8. Update Meteors, Near-Miss Graze Feedback & Collisions
        updateMeteors(dt, speedScale, events)

        return events
    }

    private fun updateShipMovement(dt: Double, target: Offset?) {
        val prevX = shipX
        if (target != null) {
            val clampedX = min(max(target.x, minShipX), maxShipX)
            val clampedY = min(max(target.y, minShipY), maxShipY)
            val responseRate = if (isFlameBoostActive) 0.000001 else 0.00008
            val alpha = (1.0 - responseRate.pow(dt)).toFloat()
            shipX += (clampedX - shipX) * alpha
            shipY += (clampedY - shipY) * alpha
        }
        shipX = min(max(shipX, minShipX), maxShipX)
        shipY = min(max(shipY, minShipY), maxShipY)

        if (dt > 0.0001) {
            shipVX = (shipX - prevX) / dt.toFloat()
            val targetTilt = max(-1.0f, min(1.0f, shipVX / 420.0f))
            shipTilt += (targetTilt - shipTilt) * min(1.0f, (dt * 14.0).toFloat())
        }
    }

    private fun updateSpecialEvents(dt: Double, events: MutableList<RushEvent>) {
        val pending = pendingWarningEvent
        if (pending != null) {
            warningTimer -= dt
            if (warningTimer <= 0) {
                pendingWarningEvent = null
                triggerSpecialEvent(pending, events)
            }
            return
        }

        if (activeSpecialEvent != null) {
            specialEventTimer -= dt
            if (specialEventTimer <= 0) {
                activeSpecialEvent = null
                pendingSafeCorridorX = null
            }
            return
        }

        if (elapsedTime >= nextSurpriseEventTime) {
            nextSurpriseEventTime = elapsedTime + Random.nextDouble(16.0, 22.0)
            val chosen = RushSpecialEventKind.entries.random()
            pendingWarningEvent = chosen
            warningTimer = 1.25

            pendingSafeCorridorX = if (chosen == RushSpecialEventKind.TWIN_GATE) {
                Random.nextDouble((worldWidth * 0.24).toDouble(), (worldWidth * 0.76).toDouble()).toFloat()
            } else {
                null
            }

            events.add(RushEvent.SpecialEventWarning(chosen, pendingSafeCorridorX))
        }
    }

    private fun triggerSpecialEvent(kind: RushSpecialEventKind, events: MutableList<RushEvent>) {
        events.add(RushEvent.SpecialEventStarted(kind))
        when (kind) {
            RushSpecialEventKind.METEOR_SHOWER -> {
                activeSpecialEvent = RushSpecialEventKind.METEOR_SHOWER
                specialEventTimer = 4.2
            }
            RushSpecialEventKind.TWIN_GATE -> {
                activeSpecialEvent = RushSpecialEventKind.TWIN_GATE
                specialEventTimer = 2.2
                spawnTwinGateFormation(pendingSafeCorridorX ?: (worldWidth * 0.5f))
            }
        }
    }

    private fun spawnRandomMeteor() {
        val ramp = min(1.0, elapsedTime / 90.0)
        val extraEndurance = min(55.0, max(0.0, elapsedTime - 90.0) * 0.65)
        val baseSpeed = (175.0 + ramp * 150.0 + extraEndurance).toFloat()

        val spawnX = Random.nextDouble(28.0, (worldWidth - 28.0).toDouble()).toFloat()
        val spawnY = worldHeight + 42.0f

        val vy = -baseSpeed * Random.nextDouble(0.88, 1.14).toFloat()
        val vx = Random.nextDouble(-32.0, 32.0).toFloat()

        asteroids.add(
            RushMeteor(
                x = spawnX,
                y = spawnY,
                vx = vx,
                vy = vy,
                rotation = Random.nextDouble(0.0, 2.0 * PI).toFloat(),
                angularVelocity = Random.nextDouble(-2.0, 2.0).toFloat()
            )
        )
    }

    private fun spawnTwinGateFormation(safeCenterX: Float) {
        val gapHalfWidth = 58.0f
        val cols = 7
        val spacing = (worldWidth - 40.0f) / max(1, cols - 1)
        val spawnY = worldHeight + 42.0f
        val ramp = min(1.0, elapsedTime / 90.0)
        val vy = -(195.0 + ramp * 105.0).toFloat()

        for (i in 0 until cols) {
            val x = 20.0f + i * spacing
            if (abs(x - safeCenterX) < gapHalfWidth) continue
            asteroids.add(
                RushMeteor(
                    x = x,
                    y = spawnY,
                    vx = 0f,
                    vy = vy,
                    rotation = i * 0.6f,
                    angularVelocity = 1.5f
                )
            )
        }
    }

    private fun spawnPowerUpOrb() {
        if (collectibles.size >= 2) return
        val type = when {
            elapsedTime >= 30.0 && !isFlameBoostActive && Random.nextDouble() < 0.12 -> RushPowerUpType.FLAME_BOOST
            !hasShield && Random.nextBoolean() -> RushPowerUpType.SHIELD
            else -> if (Random.nextBoolean()) RushPowerUpType.SLOW_TIME else RushPowerUpType.SHIELD
        }

        val x = Random.nextDouble(42.0, (worldWidth - 42.0).toDouble()).toFloat()
        collectibles.add(
            RushCollectible(
                type = type,
                x = x,
                y = worldHeight + 26.0f,
                vy = if (type == RushPowerUpType.FLAME_BOOST) -175.0f else -145.0f
            )
        )
    }

    private fun updateCollectibles(dt: Double, events: MutableList<RushEvent>) {
        val pickupRadius = shipCollisionRadius + 18.0f
        for (i in collectibles.indices.reversed()) {
            collectibles[i].y += collectibles[i].vy * dt.toFloat()
            collectibles[i].bobPhase += dt * 4.2
            collectibles[i].x += sin(collectibles[i].bobPhase).toFloat() * 16.0f * dt.toFloat()

            val dx = collectibles[i].x - shipX
            val dy = collectibles[i].y - shipY
            val distSq = dx * dx + dy * dy

            if (distSq <= pickupRadius * pickupRadius) {
                val item = collectibles.removeAt(i)
                applyPowerUp(item, events)
                continue
            }

            if (collectibles[i].y < -40.0f) {
                collectibles.removeAt(i)
            }
        }
    }

    private fun applyPowerUp(item: RushCollectible, events: MutableList<RushEvent>) {
        val idx = activeEffects.indexOfFirst { it.type == item.type }
        if (idx >= 0) {
            activeEffects[idx].remaining = item.type.duration
        } else {
            activeEffects.add(
                ActiveRushEffect(
                    type = item.type,
                    remaining = item.type.duration,
                    totalDuration = item.type.duration
                )
            )
        }
        events.add(RushEvent.PowerUpCollected(item.type, item.x, item.y))
    }

    /** Checks whether a meteor's solid rocky core contacts the spaceship's hull (excluding exhaust flame). */
    fun isMeteorContactingShip(meteor: RushMeteor): Boolean {
        val dx = meteor.x - shipX
        val dy = meteor.y - shipY
        val combinedRadiusX = RushMeteor.collisionRadius + shipCollisionRadius // 34.5f
        val combinedRadiusY = RushMeteor.collisionRadius + shipCollisionRadius + 1.5f // 36.0f
        val normX = dx / combinedRadiusX
        val normY = dy / combinedRadiusY
        return (normX * normX + normY * normY) < 1.0f
    }

    private fun updateMeteors(dt: Double, speedScale: Float, events: MutableList<RushEvent>) {
        for (i in asteroids.indices.reversed()) {
            asteroids[i].x += asteroids[i].vx * dt.toFloat() * speedScale
            asteroids[i].y += asteroids[i].vy * dt.toFloat() * speedScale
            asteroids[i].rotation += asteroids[i].angularVelocity * dt.toFloat() * speedScale

            if (asteroids[i].x < 24.0f) {
                asteroids[i].x = 24.0f
                asteroids[i].vx = abs(asteroids[i].vx)
            } else if (asteroids[i].x > worldWidth - 24.0f) {
                asteroids[i].x = worldWidth - 24.0f
                asteroids[i].vx = -abs(asteroids[i].vx)
            }

            val meteor = asteroids[i]
            val dist = hypot(meteor.x - shipX, meteor.y - shipY)

            if (isMeteorContactingShip(meteor)) {
                if (isFlameBoostActive) {
                    asteroids[i].hasGrantedNearMiss = true
                    continue
                }
                if (invulnerabilityTimer > 0) {
                    asteroids.removeAt(i)
                    continue
                }
                val shieldIdx = activeEffects.indexOfFirst { it.type == RushPowerUpType.SHIELD }
                if (shieldIdx >= 0) {
                    activeEffects.removeAt(shieldIdx)
                    invulnerabilityTimer = 0.65
                    val hitX = meteor.x
                    val hitY = meteor.y
                    asteroids.removeAt(i)
                    events.add(RushEvent.ShieldAbsorbedHit(hitX, hitY))
                    continue
                } else {
                    val hitX = meteor.x
                    val hitY = meteor.y
                    asteroids.removeAt(i)
                    triggerGameOver(hitX, hitY, events)
                    return
                }
            }

            if (!isFlameBoostActive && !meteor.hasGrantedNearMiss && dist < RushMeteor.grazeRadius && meteor.y <= shipY + 14.0f) {
                asteroids[i].hasGrantedNearMiss = true
                nearMissStreak += 1
                events.add(RushEvent.NearMiss((meteor.x + shipX) * 0.5f, (meteor.y + shipY) * 0.5f, nearMissStreak))
            }

            if (asteroids[i].y < -60.0f) {
                asteroids.removeAt(i)
            }
        }
    }

    private fun triggerGameOver(impactX: Float, impactY: Float, events: MutableList<RushEvent>) {
        if (isGameOver) return
        isGameOver = true
        val previousBest = ScoreManager.shared.highScore(GameIdentifier.RUSH)
        val isNewBest = score > previousBest && score > 0
        ScoreManager.shared.updateHighScore(GameIdentifier.RUSH, score)
        if (!hasSubmittedScoreForCurrentEnding) {
            hasSubmittedScoreForCurrentEnding = true
            RushGameCenterManager.shared.submitScore(score)
        }
        events.add(RushEvent.GameOver(score, isNewBest, impactX, impactY))
    }
}
