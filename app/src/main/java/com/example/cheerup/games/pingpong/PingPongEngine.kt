package com.example.cheerup.games.pingpong

import androidx.compose.ui.geometry.Offset
import com.example.cheerup.core.score.ScoreManager
import com.example.cheerup.domain.model.GameIdentifier
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class PingPongEngine {

    // MARK: - Court Constants (Portrait Reference Space: 360 x 620)
    companion object {
        const val courtWidth: Float = 360.0f
        const val courtHeight: Float = 620.0f
        const val wallInset: Float = 18.0f
        const val netY: Float = 310.0f

        const val playerY: Float = 68.0f
        const val cpuY: Float = 552.0f

        const val basePaddleWidth: Float = 78.0f
        const val giantPaddleWidth: Float = 122.0f
        const val cpuPaddleWidth: Float = 74.0f
        const val paddleHeight: Float = 14.0f
        const val ballRadius: Float = 8.5f
    }

    // MARK: - Game State
    var mode: PingPongMode = PingPongMode.ENDLESS
        private set
    var playerScore: Int = 0
        private set
    var cpuScore: Int = 0
        private set
    var currentRally: Int = 0
        private set
    var longestRally: Int = 0
        private set
    var isGameOver: Boolean = false
        private set
    var playerWon: Boolean = false
        private set
    var hasUsedRewardedContinue: Boolean = false
        private set
    private var hasSubmittedScoreForCurrentEnding: Boolean = false

    var difficultyTier: PingPongDifficultyTier = PingPongDifficultyTier.ROOKIE
        private set
    var highestDifficultyReached: PingPongDifficultyTier = PingPongDifficultyTier.ROOKIE
        private set

    // MARK: - Entities
    var playerX: Float = 180.0f
        private set
    var playerVX: Float = 0.0f
        private set
    var cpuX: Float = 180.0f
        private set
    var cpuVX: Float = 0.0f
        private set

    var ballPos: Offset = Offset(180.0f, 310.0f)
        private set
    var ballVel: Offset = Offset.Zero
        private set
    var baseSpeed: Float = 340.0f
        private set

    // Double ball state
    var hasExtraBall: Boolean = false
        private set
    var extraBallPos: Offset = Offset(180.0f, 310.0f)
        private set
    var extraBallVel: Offset = Offset.Zero
        private set

    // Sticky paddle state
    var isBallStuckToPlayer: Boolean = false
        private set
    private var stuckOffsetX: Float = 0.0f
    private var stuckTimer: Double = 0.0

    // Serve pause timer
    var serveDelayTimer: Double = 0.0
        private set
    private var nextServeTowardPlayer: Boolean = true

    // Power-ups
    val spawnedPowerUps = mutableListOf<SpawnedPowerUp>()
    val activePowerUps = mutableListOf<ActivePowerUpState>()
    private var powerUpSpawnTimer: Double = 4.5

    // AI tracking state
    private var aiTargetX: Float = 180.0f
    private var aiReactionTimer: Double = 0.0
    private var aiAimErrorOffset: Float = 0.0f

    private var calloutCooldown: Double = 0.0

    val hasShieldActive: Boolean
        get() = isPowerUpActive(PingPongPowerUpType.SHIELD)

    val currentPlayerPaddleWidth: Float
        get() = if (isPowerUpActive(PingPongPowerUpType.GIANT_PADDLE)) giantPaddleWidth else basePaddleWidth

    val currentSpeedMultiplier: Float
        get() = when {
            isPowerUpActive(PingPongPowerUpType.SLOW_MOTION) -> 0.64f
            isPowerUpActive(PingPongPowerUpType.SPEED_BOOST) -> 1.32f
            else -> 1.0f
        }

    val isDoublePoints: Boolean
        get() = isPowerUpActive(PingPongPowerUpType.DOUBLE_POINTS)

    val isChaosBall: Boolean
        get() = isPowerUpActive(PingPongPowerUpType.CHAOS_BALL)

    init {
        reset(PingPongMode.ENDLESS)
    }

    fun reset(mode: PingPongMode = PingPongMode.ENDLESS) {
        this.mode = mode
        playerScore = 0
        cpuScore = 0
        currentRally = 0
        longestRally = 0
        isGameOver = false
        playerWon = false
        hasUsedRewardedContinue = false
        hasSubmittedScoreForCurrentEnding = false
        difficultyTier = PingPongDifficultyTier.ROOKIE
        highestDifficultyReached = PingPongDifficultyTier.ROOKIE

        playerX = courtWidth / 2f
        playerVX = 0f
        cpuX = courtWidth / 2f
        cpuVX = 0f

        spawnedPowerUps.clear()
        activePowerUps.clear()
        powerUpSpawnTimer = 4.2
        calloutCooldown = 0.0

        isBallStuckToPlayer = false
        hasExtraBall = false
        extraBallVel = Offset.Zero
        prepareServe(towardPlayer = false, initialDelay = 0.55)
    }

    fun continueAfterReward() {
        if (hasUsedRewardedContinue) return
        hasUsedRewardedContinue = true
        hasSubmittedScoreForCurrentEnding = false
        isGameOver = false
        playerWon = false
        cpuScore = if (mode == PingPongMode.ENDLESS) {
            max(0, cpuScore - 1)
        } else {
            max(0, mode.targetScore - 1)
        }
        isBallStuckToPlayer = false
        hasExtraBall = false
        extraBallVel = Offset.Zero
        prepareServe(towardPlayer = false, initialDelay = 0.75)
    }

    private fun prepareServe(towardPlayer: Boolean, initialDelay: Double = 0.65) {
        isBallStuckToPlayer = false
        hasExtraBall = false
        extraBallVel = Offset.Zero
        currentRally = 0
        ballPos = Offset(courtWidth / 2f, netY)
        ballVel = Offset.Zero
        serveDelayTimer = initialDelay
        nextServeTowardPlayer = towardPlayer
        aiTargetX = courtWidth / 2f
        aiAimErrorOffset = Random.nextDouble(-22.0, 22.0).toFloat()
    }

    private fun launchServe() {
        val tierBoost = (difficultyTier.rawValue - 1) * 26.0f
        baseSpeed = min(560.0f, 345.0f + tierBoost)

        val angle = Random.nextDouble(-0.38, 0.38).toFloat()
        val dirY = if (nextServeTowardPlayer) -1.0f else 1.0f

        ballVel = Offset(
            x = sin(angle) * baseSpeed,
            y = cos(angle) * baseSpeed * dirY
        )
    }

    fun spawnExtraBall() {
        if (hasExtraBall) return
        hasExtraBall = true
        extraBallPos = ballPos
        val speed = max(baseSpeed, hypot(ballVel.x, ballVel.y)) * currentSpeedMultiplier
        val primaryAngle = atan2(ballVel.x, max(80.0f, abs(ballVel.y)))
        val offsetAngle = if (primaryAngle >= 0f) -0.34f else 0.34f
        val twinAngle = max((-PI * 0.36).toFloat(), min((PI * 0.36).toFloat(), primaryAngle + offsetAngle))
        extraBallVel = Offset(
            x = sin(twinAngle) * speed,
            y = abs(cos(twinAngle) * speed)
        )
    }

    fun releaseStuckBall(): List<PingPongEvent> {
        if (!isBallStuckToPlayer) return emptyList()
        isBallStuckToPlayer = false

        val normalizedOffset = max(-1.0f, min(1.0f, stuckOffsetX / (currentPlayerPaddleWidth * 0.5f)))
        val bounceAngle = normalizedOffset * (PI.toFloat() * 0.34f)
        val speed = min(740.0f, baseSpeed * 1.12f) * currentSpeedMultiplier

        ballVel = Offset(
            x = sin(bounceAngle) * speed + playerVX * 0.18f,
            y = abs(cos(bounceAngle) * speed)
        )
        ballPos = Offset(ballPos.x, playerY + paddleHeight * 0.5f + ballRadius + 2.0f)
        return listOf(PingPongEvent.StickyLaunched(ballPos))
    }

    fun update(rawDt: Double, targetPlayerX: Float?): List<PingPongEvent> {
        if (isGameOver) return emptyList()
        val dt = min(max(rawDt, 0.0), 1.0 / 20.0)
        val events = mutableListOf<PingPongEvent>()

        if (calloutCooldown > 0) {
            calloutCooldown = max(0.0, calloutCooldown - dt)
        }

        // 1. Update Player Paddle Position
        val halfPlayerW = currentPlayerPaddleWidth * 0.5f
        val minPlayerX = wallInset + halfPlayerW
        val maxPlayerX = courtWidth - wallInset - halfPlayerW

        if (targetPlayerX != null) {
            val clampedTarget = max(minPlayerX, min(maxPlayerX, targetPlayerX))
            val newX = playerX + (clampedTarget - playerX) * min(1.0f, (dt * 38.0).toFloat())
            playerVX = if (dt > 0.0001) (newX - playerX) / dt.toFloat() else 0f
            playerX = newX
        } else {
            playerVX *= 0.8f
        }
        playerX = max(minPlayerX, min(maxPlayerX, playerX))

        // 2. Update Active Power-Up Timers
        for (i in activePowerUps.indices.reversed()) {
            activePowerUps[i].remaining -= dt
            if (activePowerUps[i].remaining <= 0) {
                activePowerUps.removeAt(i)
            }
        }

        // 3. Update Spawned Field Power-Ups & Spawning Logic
        updatePowerUpSpawning(dt, events)

        // 4. Handle Serve Delay
        if (serveDelayTimer > 0) {
            serveDelayTimer -= dt
            if (serveDelayTimer <= 0) {
                launchServe()
            }
            updateCPU(dt)
            return events
        }

        // 5. Handle Ball Stuck on Sticky Paddle
        if (isBallStuckToPlayer) {
            ballPos = Offset(
                x = max(wallInset + ballRadius, min(courtWidth - wallInset - ballRadius, playerX + stuckOffsetX)),
                y = playerY + paddleHeight * 0.5f + ballRadius + 1.0f
            )
            stuckTimer -= dt
            if (stuckTimer <= 0) {
                events.addAll(releaseStuckBall())
            }
            if (hasExtraBall) {
                val subSteps = 3
                val subDt = (dt / subSteps).toFloat()
                for (s in 0 until subSteps) {
                    if (isGameOver || !hasExtraBall) break
                    stepExtraBallPhysics(subDt, events)
                }
            }
            updateCPU(dt)
            return events
        }

        // 6. Sub-stepped Ball Physics
        val subSteps = 3
        val subDt = (dt / subSteps).toFloat()

        for (s in 0 until subSteps) {
            if (isGameOver || serveDelayTimer > 0 || isBallStuckToPlayer) break
            if (hasExtraBall) {
                stepExtraBallPhysics(subDt, events)
            }
            if (!isGameOver && serveDelayTimer <= 0 && !isBallStuckToPlayer) {
                stepBallPhysics(subDt, events)
            }
        }

        // 7. Update CPU Opponent
        updateCPU(dt)

        return events
    }

    private fun stepBallPhysics(dt: Float, events: MutableList<PingPongEvent>) {
        val prevPos = ballPos
        var nextPos = Offset(
            x = ballPos.x + ballVel.x * dt,
            y = ballPos.y + ballVel.y * dt
        )

        val minBallX = wallInset + ballRadius
        val maxBallX = courtWidth - wallInset - ballRadius

        if (nextPos.x < minBallX) {
            nextPos = nextPos.copy(x = minBallX + (minBallX - nextPos.x))
            ballVel = ballVel.copy(x = abs(ballVel.x))
            ballVel = applyChaosDeflection(ballVel)
            events.add(PingPongEvent.WallBounce(nextPos))
        } else if (nextPos.x > maxBallX) {
            nextPos = nextPos.copy(x = maxBallX - (nextPos.x - maxBallX))
            ballVel = ballVel.copy(x = -abs(ballVel.x))
            ballVel = applyChaosDeflection(ballVel)
            events.add(PingPongEvent.WallBounce(nextPos))
        }

        checkPowerUpCollection(nextPos, events)

        // Player Paddle Collision
        val playerHitY = playerY + paddleHeight * 0.5f + ballRadius
        if (ballVel.y < 0f && prevPos.y >= playerHitY - 6.0f && nextPos.y <= playerHitY) {
            val halfW = currentPlayerPaddleWidth * 0.5f + ballRadius + 4.0f
            if (abs(nextPos.x - playerX) <= halfW) {
                nextPos = nextPos.copy(y = playerHitY)
                handlePlayerPaddleContact(nextPos, events)
                ballPos = nextPos
                return
            }
        }

        // CPU Paddle Collision
        val cpuHitY = cpuY - paddleHeight * 0.5f - ballRadius
        if (ballVel.y > 0f && prevPos.y <= cpuHitY + 6.0f && nextPos.y >= cpuHitY) {
            val halfW = cpuPaddleWidth * 0.5f + ballRadius + 3.0f
            if (abs(nextPos.x - cpuX) <= halfW) {
                nextPos = nextPos.copy(y = cpuHitY)
                handleCPUPaddleContact(nextPos, events)
                ballPos = nextPos
                return
            }
        }

        // Shield Bottom Barrier Save
        val shieldLineY = 34.0f
        if (ballVel.y < 0f && hasShieldActive && nextPos.y <= shieldLineY) {
            nextPos = nextPos.copy(y = shieldLineY + 2.0f)
            ballVel = ballVel.copy(y = abs(ballVel.y))
            removePowerUp(PingPongPowerUpType.SHIELD)
            events.add(PingPongEvent.ShieldSaved(nextPos))
            ballPos = nextPos
            return
        }

        // Check Scoring / Out of Bounds
        if (nextPos.y < -15.0f) {
            val missPos = Offset(nextPos.x, 26.0f)
            events.add(PingPongEvent.BallMissedLine(missPos, byPlayer = true))
            if (hasExtraBall) {
                ballPos = extraBallPos
                ballVel = extraBallVel
                hasExtraBall = false
                extraBallVel = Offset.Zero
                return
            }
            ballPos = nextPos
            handlePlayerMissed(events)
            return
        } else if (nextPos.y > courtHeight + 15.0f) {
            val missPos = Offset(nextPos.x, courtHeight - 26.0f)
            events.add(PingPongEvent.BallMissedLine(missPos, byPlayer = false))
            if (hasExtraBall) {
                val basePts = if (mode == PingPongMode.ENDLESS) 3 else 1
                val pts = if (isDoublePoints) basePts * 2 else basePts
                playerScore += pts
                events.add(PingPongEvent.PlayerScored(pts, playerScore, nextPos))
                updateDifficultyProgression(events)

                if (mode == PingPongMode.CLASSIC && playerScore >= mode.targetScore) {
                    ballPos = nextPos
                    finishGame(playerWon = true, events = events)
                    return
                }

                ballPos = extraBallPos
                ballVel = extraBallVel
                hasExtraBall = false
                extraBallVel = Offset.Zero
                return
            }
            ballPos = nextPos
            handleCPUMissed(events)
            return
        }

        ballPos = nextPos
    }

    private fun stepExtraBallPhysics(dt: Float, events: MutableList<PingPongEvent>) {
        if (!hasExtraBall) return
        val prevPos = extraBallPos
        var nextPos = Offset(
            x = extraBallPos.x + extraBallVel.x * dt,
            y = extraBallPos.y + extraBallVel.y * dt
        )

        val minBallX = wallInset + ballRadius
        val maxBallX = courtWidth - wallInset - ballRadius

        if (nextPos.x < minBallX) {
            nextPos = nextPos.copy(x = minBallX + (minBallX - nextPos.x))
            extraBallVel = extraBallVel.copy(x = abs(extraBallVel.x))
            extraBallVel = applyChaosDeflection(extraBallVel)
            events.add(PingPongEvent.WallBounce(nextPos))
        } else if (nextPos.x > maxBallX) {
            nextPos = nextPos.copy(x = maxBallX - (nextPos.x - maxBallX))
            extraBallVel = extraBallVel.copy(x = -abs(extraBallVel.x))
            extraBallVel = applyChaosDeflection(extraBallVel)
            events.add(PingPongEvent.WallBounce(nextPos))
        }

        checkPowerUpCollection(nextPos, events)

        // Player Paddle Collision
        val playerHitY = playerY + paddleHeight * 0.5f + ballRadius
        if (extraBallVel.y < 0f && prevPos.y >= playerHitY - 6.0f && nextPos.y <= playerHitY) {
            val halfW = currentPlayerPaddleWidth * 0.5f + ballRadius + 4.0f
            if (abs(nextPos.x - playerX) <= halfW) {
                nextPos = nextPos.copy(y = playerHitY)
                handleExtraBallPlayerContact(nextPos, events)
                extraBallPos = nextPos
                return
            }
        }

        // CPU Paddle Collision
        val cpuHitY = cpuY - paddleHeight * 0.5f - ballRadius
        if (extraBallVel.y > 0f && prevPos.y <= cpuHitY + 6.0f && nextPos.y >= cpuHitY) {
            val halfW = cpuPaddleWidth * 0.5f + ballRadius + 3.0f
            if (abs(nextPos.x - cpuX) <= halfW) {
                nextPos = nextPos.copy(y = cpuHitY)
                handleExtraBallCPUContact(nextPos, events)
                extraBallPos = nextPos
                return
            }
        }

        // Shield Bottom Barrier Save
        val shieldLineY = 34.0f
        if (extraBallVel.y < 0f && hasShieldActive && nextPos.y <= shieldLineY) {
            nextPos = nextPos.copy(y = shieldLineY + 2.0f)
            extraBallVel = extraBallVel.copy(y = abs(extraBallVel.y))
            removePowerUp(PingPongPowerUpType.SHIELD)
            events.add(PingPongEvent.ShieldSaved(nextPos))
            extraBallPos = nextPos
            return
        }

        if (nextPos.y < -15.0f) {
            val missPos = Offset(nextPos.x, 26.0f)
            events.add(PingPongEvent.BallMissedLine(missPos, byPlayer = true))
            hasExtraBall = false
            extraBallVel = Offset.Zero
            return
        } else if (nextPos.y > courtHeight + 15.0f) {
            val missPos = Offset(nextPos.x, courtHeight - 26.0f)
            events.add(PingPongEvent.BallMissedLine(missPos, byPlayer = false))
            val basePts = if (mode == PingPongMode.ENDLESS) 3 else 1
            val pts = if (isDoublePoints) basePts * 2 else basePts
            playerScore += pts
            events.add(PingPongEvent.PlayerScored(pts, playerScore, nextPos))
            updateDifficultyProgression(events)

            if (mode == PingPongMode.CLASSIC && playerScore >= mode.targetScore) {
                extraBallPos = nextPos
                finishGame(playerWon = true, events = events)
                return
            }

            hasExtraBall = false
            extraBallVel = Offset.Zero
            return
        }

        extraBallPos = nextPos
    }

    private fun handlePlayerPaddleContact(pos: Offset, events: MutableList<PingPongEvent>) {
        currentRally += 1
        longestRally = max(longestRally, currentRally)

        if (mode == PingPongMode.ENDLESS && currentRally % 2 == 0) {
            val pts = if (isDoublePoints) 2 else 1
            playerScore += pts
            events.add(PingPongEvent.PlayerScored(pts, playerScore, pos))
            updateDifficultyProgression(events)
        }

        if (isPowerUpActive(PingPongPowerUpType.STICKY_PADDLE)) {
            isBallStuckToPlayer = true
            stuckOffsetX = max(
                -currentPlayerPaddleWidth * 0.42f,
                min(currentPlayerPaddleWidth * 0.42f, pos.x - playerX)
            )
            stuckTimer = 1.15
            ballVel = Offset.Zero
            events.add(PingPongEvent.StickyCaught(pos))
            return
        }

        val halfW = currentPlayerPaddleWidth * 0.5f
        val relativeHit = max(-1.0f, min(1.0f, (pos.x - playerX) / halfW))
        val maxBounceAngle = PI.toFloat() * 0.37f

        var bounceAngle = relativeHit * maxBounceAngle
        if (isChaosBall) {
            bounceAngle += Random.nextDouble(-0.28, 0.28).toFloat()
            bounceAngle = max(-PI.toFloat() * 0.42f, min(PI.toFloat() * 0.42f, bounceAngle))
        }

        baseSpeed = min(780.0f, baseSpeed + 14.0f)
        val isSmash = abs(relativeHit) > 0.72f || abs(playerVX) > 420f || isPowerUpActive(PingPongPowerUpType.SPEED_BOOST)
        val smashMultiplier = if (isSmash) 1.10f else 1.0f
        val effectiveSpeed = min(840.0f, baseSpeed * currentSpeedMultiplier * smashMultiplier)

        val vx = sin(bounceAngle) * effectiveSpeed + playerVX * 0.12f
        val vy = max(effectiveSpeed * 0.42f, abs(cos(bounceAngle) * effectiveSpeed))
        ballVel = Offset(vx, vy)

        events.add(PingPongEvent.PaddleHit(isPlayer = true, position = pos, speed = effectiveSpeed, isSmash = isSmash))
        scheduleAIReactionForIncomingShot()
    }

    private fun handleExtraBallPlayerContact(pos: Offset, events: MutableList<PingPongEvent>) {
        currentRally += 1
        longestRally = max(longestRally, currentRally)

        if (mode == PingPongMode.ENDLESS && currentRally % 2 == 0) {
            val pts = if (isDoublePoints) 2 else 1
            playerScore += pts
            events.add(PingPongEvent.PlayerScored(pts, playerScore, pos))
            updateDifficultyProgression(events)
        }

        val halfW = currentPlayerPaddleWidth * 0.5f
        val relativeHit = max(-1.0f, min(1.0f, (pos.x - playerX) / halfW))
        var bounceAngle = relativeHit * (PI.toFloat() * 0.37f)
        if (isChaosBall) {
            bounceAngle += Random.nextDouble(-0.28, 0.28).toFloat()
            bounceAngle = max(-PI.toFloat() * 0.42f, min(PI.toFloat() * 0.42f, bounceAngle))
        }

        baseSpeed = min(780.0f, baseSpeed + 10.0f)
        val isSmash = abs(relativeHit) > 0.72f || abs(playerVX) > 420f || isPowerUpActive(PingPongPowerUpType.SPEED_BOOST)
        val smashMultiplier = if (isSmash) 1.10f else 1.0f
        val effectiveSpeed = min(840.0f, baseSpeed * currentSpeedMultiplier * smashMultiplier)

        val vx = sin(bounceAngle) * effectiveSpeed + playerVX * 0.12f
        val vy = max(effectiveSpeed * 0.42f, abs(cos(bounceAngle) * effectiveSpeed))
        extraBallVel = Offset(vx, vy)

        events.add(PingPongEvent.PaddleHit(isPlayer = true, position = pos, speed = effectiveSpeed, isSmash = isSmash))
        scheduleAIReactionForIncomingShot()
    }

    private fun handleExtraBallCPUContact(pos: Offset, events: MutableList<PingPongEvent>) {
        currentRally += 1
        longestRally = max(longestRally, currentRally)

        val halfW = cpuPaddleWidth * 0.5f
        val relativeHit = max(-1.0f, min(1.0f, (pos.x - cpuX) / halfW))
        var bounceAngle = relativeHit * (PI.toFloat() * 0.34f)
        bounceAngle += Random.nextDouble(-0.15, 0.15).toFloat()
        if (isChaosBall) {
            bounceAngle += Random.nextDouble(-0.26, 0.26).toFloat()
        }
        bounceAngle = max(-PI.toFloat() * 0.40f, min(PI.toFloat() * 0.40f, bounceAngle))

        baseSpeed = min(780.0f, baseSpeed + 8.0f)
        val effectiveSpeed = min(820.0f, baseSpeed * currentSpeedMultiplier)
        val minVerticalSpeed = effectiveSpeed * 0.45f

        val vx = sin(bounceAngle) * effectiveSpeed
        val rawVy = -abs(cos(bounceAngle) * effectiveSpeed)
        val vy = if (abs(rawVy) < minVerticalSpeed) -minVerticalSpeed else rawVy
        extraBallVel = Offset(vx, vy)

        events.add(PingPongEvent.PaddleHit(isPlayer = false, position = pos, speed = effectiveSpeed, isSmash = false))
    }

    private fun handleCPUPaddleContact(pos: Offset, events: MutableList<PingPongEvent>) {
        currentRally += 1
        longestRally = max(longestRally, currentRally)

        val halfW = cpuPaddleWidth * 0.5f
        val relativeHit = max(-1.0f, min(1.0f, (pos.x - cpuX) / halfW))
        var bounceAngle = relativeHit * (PI.toFloat() * 0.34f)
        bounceAngle += Random.nextDouble(-0.15, 0.15).toFloat()
        if (isChaosBall) {
            bounceAngle += Random.nextDouble(-0.26, 0.26).toFloat()
        }
        bounceAngle = max(-PI.toFloat() * 0.40f, min(PI.toFloat() * 0.40f, bounceAngle))

        baseSpeed = min(780.0f, baseSpeed + 10.0f)
        val effectiveSpeed = min(820.0f, baseSpeed * currentSpeedMultiplier)
        val minVerticalSpeed = effectiveSpeed * 0.45f

        val vx = sin(bounceAngle) * effectiveSpeed
        val rawVy = -abs(cos(bounceAngle) * effectiveSpeed)
        val vy = if (abs(rawVy) < minVerticalSpeed) -minVerticalSpeed else rawVy
        ballVel = Offset(vx, vy)

        events.add(PingPongEvent.PaddleHit(isPlayer = false, position = pos, speed = effectiveSpeed, isSmash = false))
    }

    private fun applyChaosDeflection(vel: Offset): Offset {
        if (!isChaosBall) return vel
        val speed = hypot(vel.x, vel.y)
        if (speed <= 10f) return vel
        val currentAngle = atan2(vel.x, abs(vel.y))
        val tweakedAngle = max(
            -PI.toFloat() * 0.40f,
            min(PI.toFloat() * 0.40f, currentAngle + Random.nextDouble(-0.32, 0.32).toFloat())
        )
        val signY = if (vel.y >= 0f) 1.0f else -1.0f
        return Offset(
            x = sin(tweakedAngle) * speed,
            y = cos(tweakedAngle) * speed * signY
        )
    }

    private fun handleCPUMissed(events: MutableList<PingPongEvent>) {
        val basePts = if (mode == PingPongMode.ENDLESS) 3 else 1
        val pts = if (isDoublePoints) basePts * 2 else basePts
        playerScore += pts

        events.add(PingPongEvent.PlayerScored(pts, playerScore, ballPos))
        updateDifficultyProgression(events)

        if (mode == PingPongMode.CLASSIC && playerScore >= mode.targetScore) {
            finishGame(playerWon = true, events = events)
            return
        }

        prepareServe(towardPlayer = false, initialDelay = 0.75)
    }

    private fun handlePlayerMissed(events: MutableList<PingPongEvent>) {
        cpuScore += 1
        events.add(PingPongEvent.CpuScored(cpuScore, ballPos))

        if (mode == PingPongMode.ENDLESS) {
            finishGame(playerWon = false, events = events)
        } else {
            if (cpuScore >= mode.targetScore) {
                finishGame(playerWon = false, events = events)
            } else {
                prepareServe(towardPlayer = true, initialDelay = 0.85)
            }
        }
    }

    private fun finishGame(playerWon: Boolean, events: MutableList<PingPongEvent>) {
        if (isGameOver) return
        isGameOver = true
        this.playerWon = playerWon
        ballVel = Offset.Zero
        hasExtraBall = false
        extraBallVel = Offset.Zero

        val previousBest = ScoreManager.shared.highScore(GameIdentifier.PING_PONG)
        val isNewBest = playerScore > previousBest && playerScore > 0
        ScoreManager.shared.updateHighScore(GameIdentifier.PING_PONG, playerScore)

        if (!hasSubmittedScoreForCurrentEnding) {
            hasSubmittedScoreForCurrentEnding = true
            PingPongGameCenterManager.shared.submitScore(playerScore)
        }

        events.add(PingPongEvent.GameOver(playerScore, playerWon, isNewBest))
    }

    private fun updateDifficultyProgression(events: MutableList<PingPongEvent>) {
        val newTier = PingPongDifficultyTier.forScore(playerScore, longestRally)
        if (newTier.rawValue > difficultyTier.rawValue) {
            difficultyTier = newTier
            if (newTier.rawValue > highestDifficultyReached.rawValue) {
                highestDifficultyReached = newTier
            }
            events.add(PingPongEvent.DifficultyIncreased(newTier))
        }
    }

    private fun scheduleAIReactionForIncomingShot() {
        val tierIndex = (difficultyTier.rawValue - 1).toFloat()
        val baseReaction = max(0.04, 0.18 - tierIndex * 0.026)
        aiReactionTimer = baseReaction + Random.nextDouble(0.0, 0.05)

        val maxError = max(14.0f, 38.0f - tierIndex * 4.2f)
        val mistakeChance = max(0.08, 0.24 - tierIndex * 0.025)
        aiAimErrorOffset = if (Random.nextDouble() < mistakeChance) {
            val sign = if (Random.nextBoolean()) 1.0f else -1.0f
            sign * Random.nextDouble((maxError * 0.9).toDouble(), (maxError * 1.45).toDouble()).toFloat()
        } else {
            Random.nextDouble(-maxError.toDouble(), maxError.toDouble()).toFloat()
        }
    }

    private fun updateCPU(dt: Double) {
        val halfCPU = cpuPaddleWidth * 0.5f
        val minX = wallInset + halfCPU
        val maxX = courtWidth - wallInset - halfCPU

        var trackedPos = ballPos
        var trackedVel = ballVel
        if (hasExtraBall && extraBallVel.y > 0f) {
            if (ballVel.y <= 0f) {
                trackedPos = extraBallPos
                trackedVel = extraBallVel
            } else {
                val primaryETA = max(1.0f, cpuY - ballPos.y) / max(80.0f, ballVel.y)
                val extraETA = max(1.0f, cpuY - extraBallPos.y) / max(80.0f, extraBallVel.y)
                if (extraETA < primaryETA) {
                    trackedPos = extraBallPos
                    trackedVel = extraBallVel
                }
            }
        }

        if (trackedVel.y > 0f) {
            if (aiReactionTimer > 0) {
                aiReactionTimer -= dt
            } else {
                val distY = max(1.0f, cpuY - trackedPos.y)
                val timeToReach = distY / max(80.0f, trackedVel.y)
                var predictedX = trackedPos.x + trackedVel.x * timeToReach

                val playableMin = wallInset + ballRadius
                val playableMax = courtWidth - wallInset - ballRadius
                val width = playableMax - playableMin
                if (width > 0f) {
                    var rel = predictedX - playableMin
                    val bounces = floor(rel / width).toInt()
                    rel -= bounces * width
                    predictedX = if (abs(bounces) % 2 == 1) {
                        playableMax - rel
                    } else {
                        playableMin + rel
                    }
                }
                aiTargetX = predictedX + aiAimErrorOffset
            }
        } else {
            aiTargetX = courtWidth * 0.5f + (trackedPos.x - courtWidth * 0.5f) * 0.28f
        }

        val clampedTarget = max(minX, min(maxX, aiTargetX))
        val tierIndex = (difficultyTier.rawValue - 1).toFloat()
        val maxCpuSpeed = 250.0f + tierIndex * 42.0f

        val diff = clampedTarget - cpuX
        val maxStep = maxCpuSpeed * dt.toFloat()
        val step = max(-maxStep, min(maxStep, diff))
        cpuVX = if (dt > 0.0001) step / dt.toFloat() else 0f
        cpuX = max(minX, min(maxX, cpuX + step))
    }

    fun isPowerUpActive(type: PingPongPowerUpType): Boolean =
        activePowerUps.any { it.type == type }

    private fun removePowerUp(type: PingPongPowerUpType) {
        activePowerUps.removeAll { it.type == type }
    }

    private fun updatePowerUpSpawning(dt: Double, events: MutableList<PingPongEvent>) {
        for (i in spawnedPowerUps.indices.reversed()) {
            spawnedPowerUps[i].age += dt
            if (spawnedPowerUps[i].age >= spawnedPowerUps[i].lifetime) {
                spawnedPowerUps.removeAt(i)
            }
        }

        if (spawnedPowerUps.size >= 2) return
        powerUpSpawnTimer -= dt
        if (powerUpSpawnTimer > 0) return

        powerUpSpawnTimer = Random.nextDouble(5.5, 8.5)

        val availableTypes = PingPongPowerUpType.entries.filter { candidate ->
            if (spawnedPowerUps.any { it.type == candidate }) return@filter false
            if (candidate == PingPongPowerUpType.SHIELD && hasShieldActive) return@filter false
            if (candidate == PingPongPowerUpType.DOUBLE_BALL && (hasExtraBall || isPowerUpActive(PingPongPowerUpType.DOUBLE_BALL))) return@filter false
            true
        }

        val chosenType = availableTypes.randomOrNull() ?: return
        val spawnX = Random.nextDouble((wallInset + 42f).toDouble(), (courtWidth - wallInset - 42f).toDouble()).toFloat()
        val spawnY = Random.nextDouble(185.0, 415.0).toFloat()
        val item = SpawnedPowerUp(type = chosenType, position = Offset(spawnX, spawnY))
        spawnedPowerUps.add(item)
        events.add(PingPongEvent.PowerUpSpawned(item))
    }

    private fun checkPowerUpCollection(ballPoint: Offset, events: MutableList<PingPongEvent>) {
        if (spawnedPowerUps.isEmpty()) return

        for (i in spawnedPowerUps.indices.reversed()) {
            val item = spawnedPowerUps[i]
            val distBall = hypot(ballPoint.x - item.position.x, ballPoint.y - item.position.y)
            val hitRadius = item.radius + ballRadius + 4.0f
            if (distBall <= hitRadius) {
                val collected = spawnedPowerUps.removeAt(i)
                activatePowerUp(collected.type, collected.position, events)
            }
        }
    }

    private fun activatePowerUp(type: PingPongPowerUpType, position: Offset, events: MutableList<PingPongEvent>) {
        if (type == PingPongPowerUpType.DOUBLE_BALL) {
            spawnExtraBall()
            events.add(PingPongEvent.PowerUpCollected(type, position))
            return
        }

        if (type.isPhysicsDisruptive) {
            activePowerUps.removeAll { it.type.isPhysicsDisruptive && it.type != type }
        }

        val existingIndex = activePowerUps.indexOfFirst { it.type == type }
        if (existingIndex >= 0) {
            activePowerUps[existingIndex].remaining = type.duration
        } else {
            activePowerUps.add(
                ActivePowerUpState(
                    type = type,
                    remaining = type.duration,
                    totalDuration = type.duration
                )
            )
        }
        events.add(PingPongEvent.PowerUpCollected(type, position))
    }
}
