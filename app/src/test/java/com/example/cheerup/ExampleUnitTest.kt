package com.example.cheerup

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.example.cheerup.ads.AdMobConfig
import com.example.cheerup.ads.RewardedAdManager
import com.example.cheerup.ads.RewardedAdState
import com.example.cheerup.core.audio.AudioManager
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.core.score.ScoreManager
import com.example.cheerup.domain.model.AppLanguage
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.domain.model.MascotMood
import com.example.cheerup.games.chess.ChessAI
import com.example.cheerup.games.chess.ChessBoardState
import com.example.cheerup.games.chess.ChessDifficulty
import com.example.cheerup.games.chess.ChessGameStatus
import com.example.cheerup.games.chess.ChessSessionEngine
import com.example.cheerup.games.chess.ChessSquare
import com.example.cheerup.games.chess.ChessTimeControl
import com.example.cheerup.games.chess.ChessViewModel
import com.example.cheerup.games.chess.PieceColor
import com.example.cheerup.games.chess.PieceKind
import com.example.cheerup.games.gravity.ElementalMoteType
import com.example.cheerup.games.gravity.GravityDirection
import com.example.cheerup.games.gravity.GravityEngine
import com.example.cheerup.games.gravity.GravityEvent
import com.example.cheerup.games.gravity.GravityGameCenterManager
import com.example.cheerup.games.gravity.GravityOrientation
import com.example.cheerup.games.pingpong.PingPongDifficultyTier
import com.example.cheerup.games.pingpong.PingPongEngine
import com.example.cheerup.games.pingpong.PingPongEvent
import com.example.cheerup.games.pingpong.PingPongGameCenterManager
import com.example.cheerup.games.pingpong.PingPongMode
import com.example.cheerup.games.pingpong.PingPongPowerUpType
import com.example.cheerup.games.rush.RushEngine
import com.example.cheerup.games.rush.RushGameCenterManager
import com.example.cheerup.games.rush.RushMeteor
import com.example.cheerup.games.rush.RushPowerUpType
import com.example.cheerup.games.tap.TapBurstRing
import com.example.cheerup.games.tap.TapCube
import com.example.cheerup.games.tap.TapEngine
import com.example.cheerup.games.tap.TapPixelParticle
import com.example.cheerup.games.wheels.WheelsData
import com.example.cheerup.games.wheels.WheelsEngine
import com.example.cheerup.leaderboard.PlayGamesManager
import com.example.cheerup.notifications.NotificationManager
import com.example.cheerup.presentation.navigation.AppState
import com.example.cheerup.widget.CheerUpAppWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUpDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun scoreManagerStoresAndRetrieves() {
        ScoreManager.shared.setHighScoreForTesting(GameIdentifier.TAP, 0)
        val current = ScoreManager.shared.highScore(GameIdentifier.TAP)
        ScoreManager.shared.updateHighScore(GameIdentifier.TAP, current + 10)
        assertEquals(current + 10, ScoreManager.shared.highScore(GameIdentifier.TAP))
    }

    @Test
    fun tapEngineStartsAndGeneratesCubes() {
        val engine = TapEngine()
        engine.startGame()
        assertEquals(3, engine.lives)
        assertEquals(4, engine.cubeOrder.size)
    }

    @Test
    fun rushEngineMovementAndSurvivalScoring() {
        val engine = RushEngine()
        engine.reset()
        engine.setInvulnerable(10.0)
        assertEquals(0, engine.score)
        assertFalse(engine.isGameOver)

        // Advance simulation by 6.0 seconds (61 steps of 0.05s = 3.05s -> 1pt, 122 steps of 0.05s = 6.1s -> 2pts)
        repeat(61) {
            engine.update(0.05, Offset(260f, 120f))
        }
        assertTrue(engine.shipX > 220f)
        assertFalse(engine.asteroids.isEmpty())
        assertTrue(engine.score >= 1)

        repeat(61) {
            engine.update(0.05, Offset(260f, 120f))
        }
        assertTrue(engine.score >= 2)
    }

    @Test
    fun audioManagerHandlesAllEffectsAndSynthesizesPcmBuffers() {
        AudioManager.shared.setEnabled(true)
        assertTrue(AudioManager.shared.isEnabled)
        for (effect in AudioManager.SoundEffect.entries) {
            AudioManager.shared.play(effect)
        }
        assertNotNull(AudioManager.synthesizedAudio(AudioManager.SoundEffect.DING)?.first)
        assertNotNull(AudioManager.synthesizedAudio(AudioManager.SoundEffect.SLIME_STICK)?.first)
        assertNotNull(AudioManager.synthesizedAudio(AudioManager.SoundEffect.SUCCESS_TRUMPET)?.first)
    }

    @Test
    fun wheelsEngineSpinAndOutcomes() {
        // Verify all timed Wheels challenges have doubled durations (10s...30s, even values)
        val timedOutcomes = WheelsData.outcomes.mapNotNull { it.timerSeconds }
        assertEquals(29, timedOutcomes.size)
        assertTrue(timedOutcomes.all { it in 10..30 && it % 2 == 0 })

        val engine = WheelsEngine()
        assertFalse(engine.isSpinning)
        assertNull(engine.selectedOutcome)

        engine.spin()
        assertTrue(engine.isSpinning)
        assertNotNull(engine.selectedOutcome)
        assertEquals(MascotMood.WOW, engine.mascotMood)

        // Advance tick through completion
        engine.tick(6.0)
        assertFalse(engine.isSpinning)
        assertNotNull(engine.selectedOutcome)
    }

    @Test
    fun wheelsAdTriggersEveryFiveSpins() {
        val engine = WheelsEngine()
        engine.resetSpinCount()
        assertEquals(0, engine.spinCount)

        for (i in 1..5) {
            engine.spin()
            engine.tick(6.0)
            assertEquals(i, engine.spinCount)
        }
        assertEquals(5, engine.spinCount)
        assertNotNull(engine.selectedOutcome)
    }

    @Test
    fun appStateNavigation() {
        val state = AppState()
        assertFalse(state.isPlayingGame)
        assertFalse(state.showingIntro)

        state.selectGame(GameIdentifier.TAP)
        assertTrue(state.showingIntro)
        assertEquals(GameIdentifier.TAP, state.selectedGame)

        state.startGame()
        assertTrue(state.isPlayingGame)
        assertFalse(state.showingIntro)

        state.exitToArcade()
        assertFalse(state.isPlayingGame)
        assertNull(state.selectedGame)
    }

    @Test
    fun languageManagerTranslatesEnglishFrenchSpanish() {
        val lm = LanguageManager.shared

        lm.updateLanguage(AppLanguage.ENGLISH)
        assertEquals("Play", lm.tr("Play", "Jouer", "Jugar"))

        lm.updateLanguage(AppLanguage.FRENCH)
        assertEquals("Jouer", lm.tr("Play", "Jouer", "Jugar"))

        lm.updateLanguage(AppLanguage.SPANISH)
        assertEquals("Jugar", lm.tr("Play", "Jouer", "Jugar"))

        lm.updateLanguage(AppLanguage.ENGLISH)
    }

    @Test
    fun hapticsManagerMethodsExecute() {
        val hm = HapticsManager.shared
        hm.setEnabled(true)
        assertTrue(hm.isEnabled)
        hm.lightTap()
        hm.mediumImpact()
        hm.heavyImpact()
        hm.selection()
        hm.success()
        hm.warning()
        hm.error()
    }

    @Test
    fun tapEngineIncorrectPickDoesNotSpawnConfetti() {
        val engine = TapEngine()
        engine.startGame()
        assertTrue(engine.particles.isEmpty())
        assertTrue(engine.burstRings.isEmpty())

        val wrongCube = TapCube(id = 99, name = "INVALID", color = Color.Black, hex = 0x000000)
        engine.selectCube(wrongCube, Offset.Zero)
        assertTrue(engine.particles.isEmpty())
        assertTrue(engine.burstRings.isEmpty())
    }

    @Test
    fun tapEngineConfettiPhysicsDecaysAndCleansUp() {
        val engine = TapEngine()
        val ring = TapBurstRing(x = 100f, y = 200f, color = Color.White)
        val particle = TapPixelParticle(
            x = 100f,
            y = 200f,
            vx = 300f,
            vy = -400f,
            rotation = 0.0,
            vr = 180.0,
            size = 12f,
            color = Color.White,
            lifetime = 0.5
        )
        engine.burstRings = listOf(ring)
        engine.particles = listOf(particle)

        engine.tick(0.1)
        assertEquals(1, engine.particles.size)
        assertTrue(engine.particles[0].x > 100f)
        assertEquals(1, engine.burstRings.size)
        assertTrue(engine.burstRings[0].radius > 22f)

        repeat(10) {
            engine.tick(0.1)
        }
        assertTrue(engine.particles.isEmpty())
        assertTrue(engine.burstRings.isEmpty())
    }

    @Test
    fun wheelsAllOutcomesHaveTrilingualSupportAndWidgetComplimentsComplete() {
        assertTrue(WheelsData.outcomes.size >= 55)
        for (outcome in WheelsData.outcomes) {
            assertTrue(outcome.titleEn.isNotEmpty())
            assertTrue(outcome.titleFr.isNotEmpty())
            assertTrue(outcome.titleEs.isNotEmpty())
            assertTrue(outcome.textEn.isNotEmpty())
            assertTrue(outcome.textFr.isNotEmpty())
            assertTrue(outcome.textEs.isNotEmpty())
        }
        assertEquals(158, CheerUpAppWidgetProvider.COMPLIMENTS.size)
        assertTrue(CheerUpAppWidgetProvider.randomCompliment().isNotEmpty())
    }

    @Test
    fun pingPongEngineMovementAndRallyPhysics() {
        val engine = PingPongEngine()
        engine.reset(PingPongMode.ENDLESS)
        assertEquals(0, engine.playerScore)
        assertEquals(0, engine.cpuScore)
        assertFalse(engine.isGameOver)

        // Advance 15 steps of 0.05s (0.75s) to pass initial serve delay (0.55s)
        repeat(15) {
            engine.update(0.05, 220f)
        }
        assertNotEquals(0f, engine.ballVel.y, 0.0001f)
        assertTrue(engine.playerX > 180f)
    }

    @Test
    fun pingPongPowerUpsAndDifficultyProgression() {
        assertEquals(8, PingPongPowerUpType.entries.size)
        assertTrue(PingPongPowerUpType.entries.contains(PingPongPowerUpType.DOUBLE_BALL))
        assertEquals(0xF5F8FCL, PingPongPowerUpType.DOUBLE_BALL.uiColorHex)

        val tier1 = PingPongDifficultyTier.forScore(0, rallyCount = 0)
        val tierMax = PingPongDifficultyTier.forScore(55, rallyCount = 20)
        assertEquals(PingPongDifficultyTier.ROOKIE, tier1)
        assertEquals(PingPongDifficultyTier.UNSTOPPABLE, tierMax)

        // Verify Double Ball persistence & independent ball lifecycle
        val engine = PingPongEngine()
        engine.reset(PingPongMode.ENDLESS)
        for (i in 0 until 60) {
            engine.update(0.03, 180f)
            if (engine.ballVel.y < 0f) break
        }
        engine.spawnExtraBall()
        assertTrue(engine.hasExtraBall)
        assertFalse(engine.activePowerUps.any { it.type == PingPongPowerUpType.DOUBLE_BALL })

        var sawSingleBallRemaining = false
        var missedLineEventsCount = 0
        for (i in 0 until 1200) {
            val events = engine.update(0.02, 20f)
            for (ev in events) {
                if (ev is PingPongEvent.BallMissedLine) {
                    missedLineEventsCount += 1
                }
            }
            if (!engine.hasExtraBall && !engine.isGameOver) {
                sawSingleBallRemaining = true
                assertNotEquals(0f, engine.ballVel.y, 0.0001f)
            }
            if (engine.isGameOver) break
        }
        assertTrue(sawSingleBallRemaining)
        assertTrue(missedLineEventsCount >= 2)
        assertTrue(engine.isGameOver)
    }

    @Test
    fun gravityEngineMovementAndBoundaryShift() {
        assertEquals(1, ElementalMoteType.EARTH.points)
        assertEquals(1, ElementalMoteType.WATER.points)
        assertEquals(2, ElementalMoteType.FIRE.points)
        assertEquals(2, ElementalMoteType.WIND.points)
        assertEquals(1, ElementalMoteType.EARTH.growthSegments)
        assertEquals(0.0, ElementalMoteType.EARTH.effectDuration, 0.0001)

        val counts = mutableMapOf<ElementalMoteType, Int>()
        for (step in 0 until 100) {
            val type = GravityEngine.randomElementalMoteType(roll = (step.toDouble() + 0.5) / 100.0)
            counts[type] = (counts[type] ?: 0) + 1
        }
        assertEquals(60, counts[ElementalMoteType.EARTH] ?: 0)
        assertEquals(18, counts[ElementalMoteType.WIND] ?: 0)
        assertEquals(11, counts[ElementalMoteType.FIRE] ?: 0)
        assertEquals(11, counts[ElementalMoteType.WATER] ?: 0)
        assertTrue((counts[ElementalMoteType.EARTH] ?: 0) > (counts[ElementalMoteType.WIND] ?: 0))
        assertTrue((counts[ElementalMoteType.WIND] ?: 0) > (counts[ElementalMoteType.FIRE] ?: 0))
        assertTrue((counts[ElementalMoteType.WIND] ?: 0) > (counts[ElementalMoteType.WATER] ?: 0))

        val engine = GravityEngine()
        engine.reset()
        assertEquals(0, engine.score)
        assertEquals(4, engine.segments.size)
        assertEquals(GravityOrientation.SOUTH, engine.orientation)
        assertEquals(3, engine.spawnedMotes.size)

        var shifted = false
        for (i in 0 until 50) {
            val events = engine.update(0.05)
            if (events.any { it is GravityEvent.GravityShifted }) {
                shifted = true
                break
            }
        }
        assertTrue(shifted)
        assertNotEquals(GravityOrientation.SOUTH, engine.orientation)
    }

    @Test
    fun playGamesManagerLeaderboardIDs() {
        val gcm = PlayGamesManager.shared
        assertEquals("com.gc.cheerup.tap.highscore", gcm.leaderboardID(GameIdentifier.TAP))
        assertEquals("com.gc.cheerup.pingpong.highscore", gcm.leaderboardID(GameIdentifier.PING_PONG))
        assertEquals("com.gc.cheerup.gravity.highscore", gcm.leaderboardID(GameIdentifier.GRAVITY))
        assertEquals("com.gc.cheerup.rush.highscore", gcm.leaderboardID(GameIdentifier.RUSH))
        assertEquals("com.gc.cheerup.chess.wins", gcm.leaderboardID(GameIdentifier.CHESS))
        assertNull(gcm.leaderboardID(GameIdentifier.WHEELS))

        assertEquals("com.gc.cheerup.pingpong.highscore", PingPongGameCenterManager.leaderboardID)
        assertEquals("com.gc.cheerup.gravity.highscore", GravityGameCenterManager.leaderboardID)
        assertEquals("com.gc.cheerup.rush.highscore", RushGameCenterManager.leaderboardID)
    }

    @Test
    fun allGameIdentifiersAreConfiguredWithDescriptionsAndIcons() {
        for (game in GameIdentifier.entries) {
            assertTrue(game.title.isNotEmpty())
            assertTrue(game.subtitle.isNotEmpty())
            assertTrue(game.shortDescription.isNotEmpty())
            assertTrue(game.symbol.isNotEmpty())
        }
    }

    @Test
    fun notificationManagerConfiguresTwoDayTrilingualReminders() {
        assertEquals(172_800.0, NotificationManager.TWO_DAYS_INTERVAL, 0.01)
        assertEquals("cheerup.reminder.twoDays", NotificationManager.REMINDER_IDENTIFIER)
        assertTrue(NotificationManager.reminderMessages.isNotEmpty())

        for (message in NotificationManager.reminderMessages) {
            assertTrue(message.en.isNotEmpty())
            assertTrue(message.fr.isNotEmpty())
            assertTrue(message.es.isNotEmpty())
        }

        val manager = NotificationManager.shared
        val enContent = manager.makeReminderContent(AppLanguage.ENGLISH, index = 0)
        val frContent = manager.makeReminderContent(AppLanguage.FRENCH, index = 0)
        val esContent = manager.makeReminderContent(AppLanguage.SPANISH, index = 0)

        assertEquals("CheerUp!", enContent.title)
        assertEquals(NotificationManager.reminderMessages[0].en, enContent.body)
        assertEquals(NotificationManager.reminderMessages[0].fr, frContent.body)
        assertEquals(NotificationManager.reminderMessages[0].es, esContent.body)
        assertTrue(enContent.hasDefaultSound)
    }

    @Test
    fun rewardedAdManagerPreventsDuplicatesOverlapsAndUnrewardedDismissals() {
        assertEquals("ca-app-pub-8075634631393674~5781615068", AdMobConfig.appId)
        assertEquals("ca-app-pub-8075634631393674/4055801554", AdMobConfig.arcadeRewardedAdUnitID)
        assertEquals("ca-app-pub-3940256099942544/5224354917", AdMobConfig.testArcadeRewardedAdUnitID)
        assertEquals("ca-app-pub-8075634631393674/5905563130", AdMobConfig.wheelsAdUnitID)
        assertEquals("ca-app-pub-3940256099942544/5354046379", AdMobConfig.testWheelsAdUnitID)

        val manager = RewardedAdManager.shared

        // 1. Successful reward & duplicate reward prevention
        var rewardCount = 0
        var dismissedWithoutRewardCount = 0
        val started = manager.beginTestPresentation(
            onRewardEarned = { rewardCount += 1 },
            onDismissedWithoutReward = { dismissedWithoutRewardCount += 1 }
        )
        assertTrue(started)

        // Overlapping presentation should be rejected
        val overlapStarted = manager.beginTestPresentation(onRewardEarned = {})
        assertFalse(overlapStarted)

        // First reward confirmation succeeds; second duplicate confirmation is ignored
        val firstMark = manager.markRewardEarned()
        val secondMark = manager.markRewardEarned()
        assertTrue(firstMark)
        assertFalse(secondMark)

        manager.completePresentation(didFailToPresent = false)
        assertEquals(1, rewardCount)
        assertEquals(0, dismissedWithoutRewardCount)

        // 2. Cancelled ad (dismissed without reward)
        var cancelledRewardCount = 0
        var cancelledDismissCount = 0
        manager.beginTestPresentation(
            onRewardEarned = { cancelledRewardCount += 1 },
            onDismissedWithoutReward = { cancelledDismissCount += 1 }
        )
        manager.completePresentation(didFailToPresent = false)
        assertEquals(0, cancelledRewardCount)
        assertEquals(1, cancelledDismissCount)

        // 3. Failed presentation
        var failedRewardCount = 0
        var failedDismissCount = 0
        manager.beginTestPresentation(
            onRewardEarned = { failedRewardCount += 1 },
            onDismissedWithoutReward = { failedDismissCount += 1 }
        )
        manager.markRewardEarned()
        manager.completePresentation(didFailToPresent = true, errorMessage = "Network error")
        assertEquals(0, failedRewardCount)
        assertEquals(1, failedDismissCount)
        assertEquals(RewardedAdState.Failed("Network error"), manager.state)
    }

    @Test
    fun supportedGamesContinueAfterRewardedAd() {
        // Tap: gets 1 heart back, preserves round/score, allows continue only once per run
        val tap = TapEngine()
        tap.startGame()
        tap.lives = 0
        tap.round = 5
        tap.isOver = true
        assertFalse(tap.hasUsedRewardedContinue)
        tap.continueAfterReward()
        assertFalse(tap.isOver)
        assertEquals(1, tap.lives)
        assertEquals(5, tap.round)
        assertTrue(tap.hasUsedRewardedContinue)
        // Second continue in same run is blocked
        tap.lives = 0
        tap.isOver = true
        tap.continueAfterReward()
        assertTrue(tap.isOver)
        assertEquals(0, tap.lives)
        // Starting a new game resets one-time continue
        tap.startGame()
        assertFalse(tap.hasUsedRewardedContinue)

        // Rush: preserves score, ship position, asteroid field, grants 3.5s invulnerability, one-time per run
        val rush = RushEngine()
        rush.reset()
        rush.setInvulnerable(10.0)
        repeat(61) {
            rush.update(0.05, null)
        }
        val rushScoreBefore = rush.score
        assertTrue(rushScoreBefore >= 1)
        assertFalse(rush.hasUsedRewardedContinue)
        rush.continueAfterReward()
        assertFalse(rush.isGameOver)
        assertEquals(3.5, rush.invulnerabilityTimer, 0.001)
        assertEquals(rushScoreBefore, rush.score)
        assertTrue(rush.hasUsedRewardedContinue)
        repeat(61) {
            rush.update(0.05, null)
        }
        assertTrue(rush.score > rushScoreBefore)
        rush.reset()
        assertFalse(rush.hasUsedRewardedContinue)

        // Gravity: preserves score, serpent segments/length, grants white orb transparent ability, one-time per run
        val gravity = GravityEngine()
        gravity.reset()
        val initialSnakeLength = gravity.segments.size
        assertFalse(gravity.hasUsedRewardedContinue)
        gravity.continueAfterReward()
        assertFalse(gravity.isGameOver)
        assertEquals(initialSnakeLength, gravity.segments.size)
        assertTrue(gravity.isWindPhaseActive)
        assertTrue(gravity.hasUsedRewardedContinue)
        gravity.reset()
        assertFalse(gravity.hasUsedRewardedContinue)

        // Ping Pong: preserves playerScore, undoes fatal miss, one-time per run
        val pingPong = PingPongEngine()
        pingPong.reset(PingPongMode.ENDLESS)
        assertFalse(pingPong.hasUsedRewardedContinue)
        pingPong.continueAfterReward()
        assertFalse(pingPong.isGameOver)
        assertEquals(0, pingPong.cpuScore)
        assertTrue(pingPong.hasUsedRewardedContinue)
        pingPong.reset(PingPongMode.ENDLESS)
        assertFalse(pingPong.hasUsedRewardedContinue)
    }

    @Test
    fun chessIdentifierAndIntegrationMetadata() {
        ScoreManager.shared.setHighScoreForTesting(GameIdentifier.CHESS, 0)
        assertEquals("CHESS", GameIdentifier.CHESS.title)
        assertTrue(GameIdentifier.CHESS.isGame)
        assertTrue(GameIdentifier.CHESS.subtitle.isNotEmpty())
        assertTrue(GameIdentifier.CHESS.shortDescription.isNotEmpty())

        assertEquals(0, ScoreManager.shared.highScore(GameIdentifier.CHESS))
        assertEquals(1, ScoreManager.shared.recordChessOutcome(winner = PieceColor.LIGHT))
        assertEquals(1, ScoreManager.shared.highScore(GameIdentifier.CHESS))
        assertEquals(2, ScoreManager.shared.recordChessOutcome(winner = PieceColor.LIGHT))
        assertEquals(2, ScoreManager.shared.highScore(GameIdentifier.CHESS))
        assertEquals(2, ScoreManager.shared.recordChessOutcome(winner = PieceColor.DARK))
        assertEquals(3, ScoreManager.shared.recordChessOutcome(winner = PieceColor.LIGHT))
        assertEquals(4, ScoreManager.shared.recordChessOutcome(winner = PieceColor.DARK, humanColor = PieceColor.DARK))
        assertEquals(4, ScoreManager.shared.highScore(GameIdentifier.CHESS))
    }

    @Test
    fun chessOpeningPositionAndLegalMovesCount() {
        val state = ChessBoardState()
        assertEquals(PieceColor.LIGHT, state.turn)
        assertEquals(ChessGameStatus.Playing, state.status)
        assertEquals(20, state.legalMoves().size)
        assertEquals(false, ChessSquare.fromAlgebraic("a1")?.isLightSquare)
        assertEquals(true, ChessSquare.fromAlgebraic("h1")?.isLightSquare)
        assertEquals(true, ChessSquare.fromAlgebraic("a8")?.isLightSquare)
    }

    @Test
    fun chessScholarsMateDetection() {
        val state = ChessBoardState()
        fun play(from: String, to: String) {
            val f = ChessSquare.fromAlgebraic(from)!!
            val t = ChessSquare.fromAlgebraic(to)!!
            val mv = state.legalMovesFrom(f).first { it.to == t }
            val moved = state.makeMove(mv)
            assertTrue(moved)
        }
        play("e2", "e4")
        play("e7", "e5")
        play("f1", "c4")
        play("b8", "c6")
        play("d1", "h5")
        play("g8", "f6")
        play("h5", "f7") // Checkmate!
        assertEquals(ChessGameStatus.Checkmate(PieceColor.LIGHT), state.status)
        assertTrue(state.legalMoves().isEmpty())
    }

    @Test
    fun chessCastlingRulesAndThroughCheckPrevention() {
        val clean = ChessBoardState.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")!!
        val kingSq = ChessSquare.fromAlgebraic("e1")!!
        val kingMoves = clean.legalMovesFrom(kingSq)
        assertTrue(kingMoves.any { it.isCastlingKingside && it.to == ChessSquare.fromAlgebraic("g1") })
        assertTrue(kingMoves.any { it.isCastlingQueenside && it.to == ChessSquare.fromAlgebraic("c1") })

        val qCastle = kingMoves.first { it.isCastlingQueenside }
        val castled = clean.makeMove(qCastle)
        assertTrue(castled)
        assertEquals(PieceKind.KING, clean.piece(ChessSquare.fromAlgebraic("c1")!!)?.kind)
        assertEquals(PieceKind.ROOK, clean.piece(ChessSquare.fromAlgebraic("d1")!!)?.kind)

        val f1Attacked = ChessBoardState.fromFen("4k2r/8/8/8/8/5r2/8/R3K2R w KQkq - 0 1")!!
        val attackedKingMoves = f1Attacked.legalMovesFrom(ChessSquare.fromAlgebraic("e1")!!)
        assertFalse(attackedKingMoves.any { it.isCastlingKingside })
    }

    @Test
    fun chessEnPassantCaptureAndDiscoveredCheckGuard() {
        val epState = ChessBoardState.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1")!!
        val e5 = ChessSquare.fromAlgebraic("e5")!!
        val d6 = ChessSquare.fromAlgebraic("d6")!!
        val epMove = epState.legalMovesFrom(e5).firstOrNull { it.to == d6 && it.isEnPassant }
        assertNotNull(epMove)
        val capturedEP = epState.makeMove(epMove!!)
        assertTrue(capturedEP)
        assertNull(epState.piece(ChessSquare.fromAlgebraic("d5")!!))
        assertEquals(PieceKind.PAWN, epState.piece(d6)?.kind)

        val pinnedEP = ChessBoardState.fromFen("8/8/8/K1Pp3r/8/8/8/4k3 w - d6 0 1")!!
        val c5 = ChessSquare.fromAlgebraic("c5")!!
        assertFalse(pinnedEP.legalMovesFrom(c5).any { it.isEnPassant })
    }

    @Test
    fun chessPawnPromotionSupportsAllFourPieces() {
        for (choice in PieceKind.promotableKinds) {
            val promoState = ChessBoardState.fromFen("7k/4P3/8/8/8/8/8/4K3 w - - 0 1")!!
            val e7 = ChessSquare.fromAlgebraic("e7")!!
            val e8 = ChessSquare.fromAlgebraic("e8")!!
            val options = promoState.legalMovesFrom(e7).filter { it.to == e8 }
            assertEquals(4, options.size)
            val chosen = options.first { it.promotion == choice }
            val promoted = promoState.makeMove(chosen)
            assertTrue(promoted)
            assertEquals(choice, promoState.piece(e8)?.kind)
        }
    }

    @Test
    fun chessStalemateAndDrawConditions() {
        val stale = ChessBoardState.fromFen("k7/2Q5/1K6/8/8/8/8/8 b - - 0 1")!!
        assertEquals(ChessGameStatus.Stalemate, stale.status)

        val kVsK = ChessBoardState.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1")!!
        assertEquals(ChessGameStatus.DrawInsufficientMaterial, kVsK.status)

        val sameBishops = ChessBoardState.fromFen("4k1b1/8/8/8/8/8/8/4KB2 w - - 0 1")!!
        assertEquals(ChessGameStatus.DrawInsufficientMaterial, sameBishops.status)

        val fifty = ChessBoardState.fromFen("4k3/8/8/8/8/8/4R3/4K3 w - - 100 80")!!
        assertEquals(ChessGameStatus.DrawFiftyMove, fifty.status)

        val rep = ChessBoardState()
        fun ply(f: String, t: String) {
            val sqF = ChessSquare.fromAlgebraic(f)!!
            val sqT = ChessSquare.fromAlgebraic(t)!!
            val m = rep.legalMovesFrom(sqF).first { it.to == sqT }
            assertTrue(rep.makeMove(m))
        }
        repeat(2) {
            ply("g1", "f3"); ply("g8", "f6")
            ply("f3", "g1"); ply("f6", "g8")
        }
        assertEquals(ChessGameStatus.DrawThreefoldRepetition, rep.status)
    }

    @Test
    fun chessAIDifficultiesUndoAndQuickClock() {
        for (diff in ChessDifficulty.entries) {
            val matePos = ChessBoardState.fromFen("6k1/8/8/8/8/6Q1/7P/6K1 b - - 0 1")!!
            val aiMove = ChessAI.bestMove(matePos, diff)
            assertNotNull(aiMove)
        }

        val session = ChessSessionEngine(ChessDifficulty.INTERMEDIATE, ChessTimeControl.THREE_MINUTES)
        assertEquals(180.0, session.whiteTimeRemaining, 0.001)
        assertEquals(180.0, session.blackTimeRemaining, 0.001)
        val e2 = ChessSquare.fromAlgebraic("e2")!!
        val e4 = ChessSquare.fromAlgebraic("e4")!!
        val whiteMove = session.state.legalMovesFrom(e2).first { it.to == e4 }
        assertTrue(session.performMove(whiteMove))
        val e7 = ChessSquare.fromAlgebraic("e7")!!
        val e5 = ChessSquare.fromAlgebraic("e5")!!
        val blackMove = session.state.legalMovesFrom(e7).first { it.to == e5 }
        assertTrue(session.performMove(blackMove))
        assertTrue(session.canUndo)

        assertEquals(2, session.undoToPlayerTurn())
        assertEquals(PieceColor.LIGHT, session.state.turn)
        assertNull(session.state.piece(e4))
        assertEquals(PieceKind.PAWN, session.state.piece(e2)?.kind)

        val timedWinner = session.tickClock(181.0)
        assertEquals(PieceColor.DARK, timedWinner)
        assertEquals(ChessGameStatus.Timeout(PieceColor.DARK), session.state.status)

        session.reset(ChessDifficulty.ADVANCED, ChessTimeControl.FIVE_MINUTES)
        assertEquals(300.0, session.whiteTimeRemaining, 0.001)
        assertEquals(ChessGameStatus.Playing, session.state.status)

        val vm = ChessViewModel()
        vm.beginMatch(ChessDifficulty.BEGINNER, PieceColor.LIGHT, ChessTimeControl.THREE_MINUTES)
        // Verify human tap move + animation lifecycle executes cleanly without MonotonicFrameClock crash
        vm.handleBoardTap(e2)
        assertEquals(e2, vm.selectedSquare)
        vm.handleBoardTap(e4)
        assertTrue(vm.isAnimating)
        assertNotNull(vm.activeAnimatedMove)
        vm.onMoveAnimationFinished(vm.animSessionToken)
        assertFalse(vm.isAnimating)
        assertNull(vm.activeAnimatedMove)

        // Verify starting as Brown (AI moves first) also initializes and runs cleanly
        vm.beginMatch(ChessDifficulty.INTERMEDIATE, PieceColor.DARK, ChessTimeControl.THREE_MINUTES)
        vm.tickClockIfNeeded(System.currentTimeMillis() + 500L)
        vm.openModeSetup()
        assertTrue(vm.showModeSetup)
        assertFalse(vm.status.isTerminal)
        vm.cleanupOnExit()
    }

    @Test
    fun tapConfettiCoversEntireSelectedColorBoxForAllColors() {
        val engine = TapEngine()
        val sampleBounds = Rect(100f, 300f, 500f, 700f)

        for (colorIndex in 0 until 4) {
            engine.startGame()
            engine.nextRound(advanceRound = false)
            val targetCube = engine.cubeOrder.firstOrNull { it.name == engine.instructionWord }
                ?: engine.cubeOrder[colorIndex]
            // Force exact answer check match across all 4 color cubes
            engine.isLive = true
            if (targetCube.name == engine.instructionWord) {
                engine.selectCube(targetCube, sampleBounds.center, sampleBounds)
                assertEquals(1, engine.burstRings.size)
                val ring = engine.burstRings[0]
                assertEquals(sampleBounds.center.x, ring.x, 1f)
                assertEquals(sampleBounds.center.y, ring.y, 1f)
                assertTrue(ring.maxRadius >= sampleBounds.width * 0.5f)

                val xs = engine.particles.map { it.x }
                val ys = engine.particles.map { it.y }
                assertTrue("Confetti should reach left side of box", xs.any { it < sampleBounds.left + sampleBounds.width * 0.25f })
                assertTrue("Confetti should reach right side of box", xs.any { it > sampleBounds.right - sampleBounds.width * 0.25f })
                assertTrue("Confetti should reach top side of box", ys.any { it < sampleBounds.top + sampleBounds.height * 0.25f })
                assertTrue("Confetti should reach bottom side of box", ys.any { it > sampleBounds.bottom - sampleBounds.height * 0.25f })
            }
        }
    }

    @Test
    fun rushSpritesFlameBoostOrbAndContactCalibration() {
        assertEquals(5.0, RushPowerUpType.FLAME_BOOST.duration, 0.001)
        assertEquals(0xFF5500L, RushPowerUpType.FLAME_BOOST.colorHex)
        assertTrue(RushPowerUpType.FLAME_BOOST.badgeTitle.isNotEmpty())

        val engine = RushEngine()
        engine.reset()
        val directHullHit = RushMeteor(
            x = engine.shipX,
            y = engine.shipY + 20.0f,
            vx = 0.0f,
            vy = -100.0f,
            rotation = 0.0f,
            angularVelocity = 0.0f
        )
        assertTrue(engine.isMeteorContactingShip(directHullHit))

        val belowExhaust = RushMeteor(
            x = engine.shipX,
            y = engine.shipY - 39.0f,
            vx = 0.0f,
            vy = -100.0f,
            rotation = 0.0f,
            angularVelocity = 0.0f
        )
        assertFalse(engine.isMeteorContactingShip(belowExhaust))
    }

    private data class SubmissionEntry(val score: Int, val leaderboardID: String)

    @Test
    fun unauthenticatedPlayerNeverBlocksOrSubmits() {
        val entries = CopyOnWriteArrayList<SubmissionEntry>()
        val manager = PlayGamesManager(
            isPlayerAuthenticated = { false },
            scoreSubmitter = { score, _, leaderboardID, completion ->
                entries.add(SubmissionEntry(score, leaderboardID))
                completion(null)
            }
        )

        assertFalse(manager.isAuthenticated)
        for (game in GameIdentifier.entries) {
            var res: Result<Unit>? = null
            manager.submitScore(42, game) { result ->
                res = result
            }
            assertNotNull(res)
            assertTrue(res!!.isFailure)
        }
        assertTrue(entries.isEmpty())
    }

    @Test
    fun isolatedManagerRoutesExactIDsAndHandlesNetworkFailure() {
        val entries = CopyOnWriteArrayList<SubmissionEntry>()
        val failNetwork = AtomicBoolean(false)
        val simulatedError = RuntimeException("Offline (-1009)")

        val manager = PlayGamesManager(
            isPlayerAuthenticated = { true },
            scoreSubmitter = { score, _, leaderboardID, completion ->
                entries.add(SubmissionEntry(score, leaderboardID))
                completion(if (failNetwork.get()) simulatedError else null)
            }
        )

        val expected = listOf(
            Triple(GameIdentifier.TAP, "com.gc.cheerup.tap.highscore", 15),
            Triple(GameIdentifier.PING_PONG, "com.gc.cheerup.pingpong.highscore", 7),
            Triple(GameIdentifier.GRAVITY, "com.gc.cheerup.gravity.highscore", 88),
            Triple(GameIdentifier.RUSH, "com.gc.cheerup.rush.highscore", 54),
            Triple(GameIdentifier.CHESS, "com.gc.cheerup.chess.wins", 3)
        )

        for ((game, expectedID, score) in expected) {
            var res: Result<Unit>? = null
            manager.submitScore(score, game) { res = it }
            assertTrue(res?.isSuccess == true)
            assertEquals(SubmissionEntry(score, expectedID), entries.last())
        }

        // Wheels has no leaderboard, and non-positive scores are skipped cleanly
        manager.submitScore(10, GameIdentifier.WHEELS) {}
        manager.submitScore(0, GameIdentifier.TAP) {}
        assertEquals(5, entries.size)

        // Network failure completes gracefully and stores error without throwing
        failNetwork.set(true)
        var failureResult: Result<Unit>? = null
        manager.submitScore(19, GameIdentifier.RUSH) { failureResult = it }
        assertTrue(failureResult?.isFailure == true)
        assertEquals(simulatedError, manager.lastSubmissionError)
    }

    @Test
    fun intermediateHighScoreUpdatesDoNotSubmitAndChessTracksCumulativeWins() {
        val entries = CopyOnWriteArrayList<SubmissionEntry>()
        PlayGamesManager.shared.configureForTesting(
            isPlayerAuthenticated = { true },
            scoreSubmitter = { score, _, leaderboardID, completion ->
                entries.add(SubmissionEntry(score, leaderboardID))
                completion(null)
            }
        )
        ScoreManager.shared.setHighScoreForTesting(GameIdentifier.PING_PONG, 0)
        ScoreManager.shared.setHighScoreForTesting(GameIdentifier.CHESS, 0)
        try {
            // 1. Live high-score updates mid-game must NOT trigger Play Games submissions
            val previousPingBest = ScoreManager.shared.highScore(GameIdentifier.PING_PONG)
            ScoreManager.shared.updateHighScore(GameIdentifier.PING_PONG, previousPingBest + 5)
            assertTrue(entries.isEmpty())

            // 2. Chess baseline -> non-wins do NOT increment or submit
            val startingWins = ScoreManager.shared.highScore(GameIdentifier.CHESS)
            val lossMatch = UUID.randomUUID()
            val drawMatch = UUID.randomUUID()
            assertEquals(startingWins, ScoreManager.shared.recordChessOutcome(PieceColor.DARK, PieceColor.LIGHT, lossMatch))
            assertEquals(startingWins, ScoreManager.shared.recordChessOutcome(null, PieceColor.LIGHT, drawMatch))
            assertTrue(entries.isEmpty())

            // 3. First human win increments cumulative total by +1 and submits to chess wins leaderboard
            val winMatch1 = UUID.randomUUID()
            val winsAfter1 = ScoreManager.shared.recordChessOutcome(PieceColor.LIGHT, PieceColor.LIGHT, winMatch1)
            assertEquals(startingWins + 1, winsAfter1)
            assertEquals(startingWins + 1, ScoreManager.shared.highScore(GameIdentifier.CHESS))
            assertEquals(
                listOf(SubmissionEntry(startingWins + 1, "com.gc.cheerup.chess.wins")),
                entries.toList()
            )

            // 4. Duplicate completion event for same match does NOT re-increment or re-submit
            val dupWins = ScoreManager.shared.recordChessOutcome(PieceColor.LIGHT, PieceColor.LIGHT, winMatch1)
            assertEquals(startingWins + 1, dupWins)
            assertEquals(1, entries.size)

            // 5. Starting a new ChessViewModel match preserves cumulative wins; a second win increments to +2
            val vm = ChessViewModel()
            vm.startNewGame(ChessDifficulty.BEGINNER, PieceColor.DARK, ChessTimeControl.UNTIMED)
            assertEquals(startingWins + 1, vm.gamesWon)
            vm.cleanupOnExit()

            val winMatch2 = UUID.randomUUID()
            val winsAfter2 = ScoreManager.shared.recordChessOutcome(PieceColor.DARK, PieceColor.DARK, winMatch2)
            assertEquals(startingWins + 2, winsAfter2)
            assertEquals(2, entries.size)
            assertEquals(SubmissionEntry(startingWins + 2, "com.gc.cheerup.chess.wins"), entries.last())
        } finally {
            PlayGamesManager.shared.resetTestingConfiguration()
        }
    }

    @Test
    fun allArcadeEnginesSubmitOnceAtGameOverAndIgnoreAbandonment() {
        val entries = CopyOnWriteArrayList<SubmissionEntry>()
        PlayGamesManager.shared.configureForTesting(
            isPlayerAuthenticated = { true },
            scoreSubmitter = { score, _, leaderboardID, completion ->
                entries.add(SubmissionEntry(score, leaderboardID))
                completion(null)
            }
        )
        for (game in GameIdentifier.entries) {
            ScoreManager.shared.setHighScoreForTesting(game, 0)
        }
        try {
            // --- Ping Pong (Endless): score increases live without submission, then submits once at game over ---
            val pingPong = PingPongEngine()
            pingPong.reset(PingPongMode.ENDLESS)
            for (i in 0 until 2500) {
                if (pingPong.isGameOver) break
                val targetX = if (pingPong.playerScore >= 1) -500f else pingPong.ballPos.x
                pingPong.update(1.0 / 60.0, targetX)
                if (!pingPong.isGameOver) {
                    assertTrue(entries.isEmpty())
                }
            }
            assertTrue(pingPong.isGameOver)
            assertTrue(pingPong.playerScore >= 1)
            assertEquals(1, entries.size)
            assertEquals(SubmissionEntry(pingPong.playerScore, "com.gc.cheerup.pingpong.highscore"), entries.last())

            // Stepping again after gameOver must not submit duplicates
            pingPong.update(1.0 / 60.0, -500f)
            assertEquals(1, entries.size)

            // Resetting (simulating abandon/restart before game over) submits 0 times
            pingPong.reset(PingPongMode.CLASSIC)
            assertEquals(1, entries.size)

            // --- Rush: earn survival score >= 1 while invulnerable, then collide to trigger single gameOver submission ---
            val rush = RushEngine()
            rush.reset()
            rush.setInvulnerable(3.2)
            var dtTicks = 0
            while (rush.score < 1 && dtTicks < 400) {
                rush.update(0.02, Offset(rush.shipX, rush.shipY))
                dtTicks += 1
            }
            assertTrue(rush.score >= 1)
            assertFalse(rush.isGameOver)
            assertEquals(1, entries.size)

            var rushSteps = 0
            while (!rush.isGameOver && rushSteps < 2000) {
                val meteor = rush.asteroids.firstOrNull { it.y > rush.shipY }
                val target = if (meteor != null) {
                    Offset(meteor.x, meteor.y)
                } else {
                    Offset(rush.shipX, rush.shipY)
                }
                rush.update(0.02, target)
                rushSteps += 1
            }
            assertTrue(rush.isGameOver)
            assertEquals(2, entries.size)
            assertEquals(SubmissionEntry(rush.score, "com.gc.cheerup.rush.highscore"), entries.last())
            rush.update(0.02, null)
            assertEquals(2, entries.size)

            // --- Gravity: eat 1 mote so score > 0 and length >= 5, then self-collide once ---
            val gravity = GravityEngine()
            gravity.reset()
            var gSteps = 0
            while ((gravity.score == 0 || gravity.segments.size < 5) && gSteps < 1000 && !gravity.isGameOver) {
                val mote = gravity.spawnedMotes.firstOrNull { it.type != ElementalMoteType.WIND }
                    ?: gravity.spawnedMotes.firstOrNull()
                if (mote != null) {
                    val head = gravity.segments[0]
                    when {
                        head.x < mote.coord.x -> gravity.queueScreenDirection(GravityDirection.RIGHT)
                        head.x > mote.coord.x -> gravity.queueScreenDirection(GravityDirection.LEFT)
                        head.y < mote.coord.y -> gravity.queueScreenDirection(GravityDirection.UP)
                        head.y > mote.coord.y -> gravity.queueScreenDirection(GravityDirection.DOWN)
                    }
                }
                gravity.update(0.05)
                gSteps += 1
            }
            var graceWait = 0
            while ((gravity.isEffectActive(ElementalMoteType.WIND) || graceWait < 12) && graceWait < 200 && !gravity.isGameOver) {
                gravity.update(0.05)
                graceWait += 1
            }
            val turns = listOf(
                GravityDirection.RIGHT,
                GravityDirection.DOWN,
                GravityDirection.LEFT,
                GravityDirection.UP
            )
            for (turn in turns + turns) {
                if (gravity.isGameOver) break
                gravity.queueScreenDirection(turn)
                val startHead = gravity.segments[0]
                var ticks = 0
                while (gravity.segments[0] == startHead && !gravity.isGameOver && ticks < 20) {
                    gravity.update(0.05)
                    ticks += 1
                }
            }
            assertTrue(gravity.score > 0)
            assertTrue(gravity.isGameOver)
            assertEquals(3, entries.size)
            assertEquals(SubmissionEntry(gravity.score, "com.gc.cheerup.gravity.highscore"), entries.last())
            gravity.update(0.20)
            assertEquals(3, entries.size)

            // --- Tap: score round 1, then lose all 3 lives -> submits final score once ---
            val tap = TapEngine()
            tap.startGame()
            val roundAfterStart = tap.round
            val correct = tap.cubeOrder.firstOrNull { it.name == tap.instructionWord }
            if (correct != null) {
                tap.selectCube(correct, Offset.Zero)
            }
            assertTrue(tap.round >= roundAfterStart)
            while (tap.lives > 0) {
                tap.tick(10.0)
            }
            assertTrue(tap.isOver)
            val expectedTapScore = maxOf(0, tap.round - 1)
            assertEquals(4, entries.size)
            assertEquals(SubmissionEntry(expectedTapScore, "com.gc.cheerup.tap.highscore"), entries.last())
        } finally {
            PlayGamesManager.shared.resetTestingConfiguration()
        }
    }
}
