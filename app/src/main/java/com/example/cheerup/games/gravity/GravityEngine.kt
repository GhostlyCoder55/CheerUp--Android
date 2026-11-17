package com.example.cheerup.games.gravity

import com.example.cheerup.core.score.ScoreManager
import com.example.cheerup.domain.model.GameIdentifier
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GravityEngine {

    companion object {
        const val gridColumns: Int = 17
        const val gridRows: Int = 17

        fun randomElementalMoteType(roll: Double): ElementalMoteType {
            return when {
                roll < 0.60 -> ElementalMoteType.EARTH
                roll < 0.78 -> ElementalMoteType.WIND
                roll < 0.89 -> ElementalMoteType.FIRE
                else -> ElementalMoteType.WATER
            }
        }
    }

    var score: Int = 0
        private set
    var motesCollectedCount: Int = 0
        private set
    var isGameOver: Boolean = false
        private set
    var hasUsedRewardedContinue: Boolean = false
        private set
    private var hasSubmittedScoreForCurrentEnding: Boolean = false

    val segments = mutableListOf<GridCoord>()
    val previousSegments = mutableListOf<GridCoord>()
    var moveDirection: GravityDirection = GravityDirection.UP
        private set
    private val queuedDirections = mutableListOf<GravityDirection>()
    private var pendingGrowth: Int = 0

    var orientation: GravityOrientation = GravityOrientation.SOUTH
        private set
    private var shiftGraceTimer: Double = 0.0

    val spawnedMotes = mutableListOf<SpawnedMote>()
    val activeEffects = mutableListOf<ActiveElementalEffect>()

    private var stepAccumulator: Double = 0.0
    var stepProgress: Float = 0.0f
        private set

    val currentStepInterval: Double
        get() {
            val base = 0.22
            return when {
                isEffectActive(ElementalMoteType.WATER) -> base * 1.38
                isEffectActive(ElementalMoteType.FIRE) -> base * 0.86
                else -> base
            }
        }

    val isWindPhaseActive: Boolean
        get() = isEffectActive(ElementalMoteType.WIND)

    val isFireBoostActive: Boolean
        get() = isEffectActive(ElementalMoteType.FIRE)

    val isWaterCalmActive: Boolean
        get() = isEffectActive(ElementalMoteType.WATER)

    init {
        reset()
    }

    fun reset() {
        score = 0
        motesCollectedCount = 0
        isGameOver = false
        hasUsedRewardedContinue = false
        hasSubmittedScoreForCurrentEnding = false
        orientation = GravityOrientation.SOUTH
        moveDirection = GravityDirection.UP
        queuedDirections.clear()
        pendingGrowth = 0
        shiftGraceTimer = 0.0
        stepAccumulator = 0.0
        stepProgress = 0.0f

        val cx = gridColumns / 2
        val cy = gridRows / 2
        segments.clear()
        segments.addAll(
            listOf(
                GridCoord(x = cx, y = cy),
                GridCoord(x = cx, y = cy - 1),
                GridCoord(x = cx, y = cy - 2),
                GridCoord(x = cx, y = cy - 3)
            )
        )
        previousSegments.clear()
        previousSegments.addAll(segments)

        spawnedMotes.clear()
        activeEffects.clear()

        spawnMote(preferredType = ElementalMoteType.EARTH)
        spawnMote(preferredType = null)
        spawnMote(preferredType = null)
    }

    fun continueAfterReward() {
        if (hasUsedRewardedContinue) return
        hasUsedRewardedContinue = true
        hasSubmittedScoreForCurrentEnding = false
        isGameOver = false

        queuedDirections.clear()
        pendingGrowth = 0
        stepAccumulator = -0.35
        stepProgress = 0.0f

        activeEffects.removeAll { it.type == ElementalMoteType.WIND }
        activeEffects.add(ActiveElementalEffect(type = ElementalMoteType.WIND, remaining = 7.5, totalDuration = 7.5))
        shiftGraceTimer = 3.5
    }

    fun queueScreenDirection(screenDir: GravityDirection) {
        if (isGameOver) return
        val worldDir = orientation.screenToWorldDirection(screenDir)
        val referenceDir = queuedDirections.lastOrNull() ?: moveDirection

        if (worldDir == referenceDir || worldDir == referenceDir.opposite) return
        if (queuedDirections.size < 2) {
            queuedDirections.add(worldDir)
        }
    }

    fun update(rawDt: Double): List<GravityEvent> {
        if (isGameOver) return emptyList()
        val dt = min(max(rawDt, 0.0), 1.0 / 20.0)
        val events = mutableListOf<GravityEvent>()

        if (shiftGraceTimer > 0) {
            shiftGraceTimer = max(0.0, shiftGraceTimer - dt)
        }

        for (i in activeEffects.indices.reversed()) {
            activeEffects[i].remaining -= dt
            if (activeEffects[i].remaining <= 0) {
                activeEffects.removeAt(i)
            }
        }

        val interval = currentStepInterval
        stepAccumulator += dt

        while (stepAccumulator >= interval && !isGameOver) {
            stepAccumulator -= interval
            advanceOneGridStep(events)
        }

        stepProgress = min(1.0, max(0.0, stepAccumulator / interval)).toFloat()
        return events
    }

    private fun advanceOneGridStep(events: MutableList<GravityEvent>) {
        previousSegments.clear()
        previousSegments.addAll(segments)

        if (queuedDirections.isNotEmpty()) {
            val nextDir = queuedDirections.removeAt(0)
            if (nextDir != moveDirection.opposite) {
                moveDirection = nextDir
            }
        }

        val head = segments.firstOrNull() ?: return
        val rawNextX = head.x + moveDirection.dx
        val rawNextY = head.y + moveDirection.dy

        var crossedBoundary: GravityDirection? = null
        var wrappedX = rawNextX
        var wrappedY = rawNextY

        if (rawNextX >= gridColumns) {
            wrappedX = 0
            crossedBoundary = GravityDirection.RIGHT
        } else if (rawNextX < 0) {
            wrappedX = gridColumns - 1
            crossedBoundary = GravityDirection.LEFT
        }

        if (rawNextY >= gridRows) {
            wrappedY = 0
            crossedBoundary = GravityDirection.UP
        } else if (rawNextY < 0) {
            wrappedY = gridRows - 1
            crossedBoundary = GravityDirection.DOWN
        }

        val nextHead = GridCoord(x = wrappedX, y = wrappedY)

        val bodyToCheck = if (pendingGrowth > 0) segments else segments.dropLast(1)
        if (bodyToCheck.contains(nextHead)) {
            if (isWindPhaseActive || shiftGraceTimer > 0) {
                events.add(GravityEvent.WindPhasedThroughSelf(nextHead))
            } else {
                triggerGameOver(events)
                return
            }
        }

        segments.add(0, nextHead)
        if (pendingGrowth > 0) {
            pendingGrowth -= 1
        } else if (segments.size > 1) {
            segments.removeAt(segments.lastIndex)
        }

        events.add(GravityEvent.Moved)

        val moteIndex = spawnedMotes.indexOfFirst { it.coord == nextHead }
        if (moteIndex >= 0) {
            val collected = spawnedMotes.removeAt(moteIndex)
            handleMoteCollected(collected, events)
        }

        if (crossedBoundary != null) {
            applyBoundaryGravityShift(crossedBoundary, events)
        }
    }

    private fun applyBoundaryGravityShift(crossedEdge: GravityDirection, events: MutableList<GravityEvent>) {
        val quarterTurns = when (crossedEdge) {
            GravityDirection.RIGHT -> 1
            GravityDirection.LEFT -> -1
            GravityDirection.UP -> if (score >= 4) 2 else 1
            GravityDirection.DOWN -> -1
        }

        val newOrientation = orientation.shifted(quarterTurns)
        if (newOrientation == orientation) return

        orientation = newOrientation
        queuedDirections.clear()
        shiftGraceTimer = 0.45
        events.add(GravityEvent.GravityShifted(newOrientation, crossedEdge))
    }

    private fun handleMoteCollected(mote: SpawnedMote, events: MutableList<GravityEvent>) {
        motesCollectedCount += 1

        val multiplier = if (isFireBoostActive) 2 else 1
        val pointsAdded = mote.type.points * multiplier
        score += pointsAdded
        pendingGrowth += mote.type.growthSegments

        if (mote.type.effectDuration > 0) {
            if (mote.type == ElementalMoteType.FIRE) {
                activeEffects.removeAll { it.type == ElementalMoteType.WATER }
            } else if (mote.type == ElementalMoteType.WATER) {
                activeEffects.removeAll { it.type == ElementalMoteType.FIRE }
            }

            val idx = activeEffects.indexOfFirst { it.type == mote.type }
            if (idx >= 0) {
                activeEffects[idx].remaining = mote.type.effectDuration
            } else {
                activeEffects.add(
                    ActiveElementalEffect(
                        type = mote.type,
                        remaining = mote.type.effectDuration,
                        totalDuration = mote.type.effectDuration
                    )
                )
            }
        }

        events.add(GravityEvent.MoteCollected(mote.type, mote.coord, pointsAdded, score))

        while (spawnedMotes.size < 3) {
            spawnMote(preferredType = null)
        }
    }

    private fun spawnMote(preferredType: ElementalMoteType?) {
        val occupied = segments.toMutableSet()
        for (m in spawnedMotes) {
            occupied.add(m.coord)
        }

        val candidates = ArrayList<GridCoord>(gridColumns * gridRows)
        for (x in 0 until gridColumns) {
            for (y in 0 until gridRows) {
                val c = GridCoord(x, y)
                if (!occupied.contains(c)) {
                    candidates.add(c)
                }
            }
        }

        val chosenCoord = candidates.randomOrNull() ?: return
        val chosenType = preferredType ?: randomElementalMoteType(Random.nextDouble(0.0, 1.0))
        spawnedMotes.add(SpawnedMote(type = chosenType, coord = chosenCoord))
    }

    fun isEffectActive(type: ElementalMoteType): Boolean =
        activeEffects.any { it.type == type }

    private fun triggerGameOver(events: MutableList<GravityEvent>) {
        if (isGameOver) return
        isGameOver = true
        val previousBest = ScoreManager.shared.highScore(GameIdentifier.GRAVITY)
        val isNewBest = score > previousBest && score > 0
        ScoreManager.shared.updateHighScore(GameIdentifier.GRAVITY, score)
        if (!hasSubmittedScoreForCurrentEnding) {
            hasSubmittedScoreForCurrentEnding = true
            GravityGameCenterManager.shared.submitScore(score)
        }
        events.add(GravityEvent.GameOver(score, isNewBest))
    }
}
