package com.example.cheerup.games.chess

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cheerup.R
import com.example.cheerup.ads.RewardedAdManager
import com.example.cheerup.core.audio.AudioManager
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.core.score.ScoreManager
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.domain.model.MascotMood
import com.example.cheerup.presentation.components.CheerUpCircleIconButton
import com.example.cheerup.presentation.components.CheerUpMascotView
import com.example.cheerup.presentation.components.CheerUpPrimaryButton
import com.example.cheerup.presentation.components.CheerUpSecondaryButton
import com.example.cheerup.presentation.components.CheerUpSymbolIcon
import com.example.cheerup.presentation.components.SpeechBubbleShape
import com.example.cheerup.presentation.theme.CheerUpColors
import com.example.cheerup.presentation.theme.CheerUpFont
import com.example.cheerup.presentation.theme.Spacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

class ChessViewModel {
    val engine = ChessSessionEngine(difficulty = ChessDifficulty.INTERMEDIATE, timeControl = ChessTimeControl.UNTIMED)

    var difficulty: ChessDifficulty by mutableStateOf(ChessDifficulty.INTERMEDIATE)
    var humanColor: PieceColor by mutableStateOf(PieceColor.LIGHT)
    var timeControl: ChessTimeControl by mutableStateOf(ChessTimeControl.UNTIMED)
    var showModeSetup: Boolean by mutableStateOf(true)
    var selectedSquare: ChessSquare? by mutableStateOf(null)
    var highlightedMoves: List<ChessMove> by mutableStateOf(emptyList())
    var pendingPromotion: PendingPromotion? by mutableStateOf(null)

    var isPaused: Boolean by mutableStateOf(false)
    var isThinking: Boolean by mutableStateOf(false)
    var isAnimating: Boolean by mutableStateOf(false)
    var status: ChessGameStatus by mutableStateOf(ChessGameStatus.Playing)
    var turn: PieceColor by mutableStateOf(PieceColor.LIGHT)
    var lastMoveNotation: String by mutableStateOf("")
    var fullMoveNumber: Int by mutableIntStateOf(1)

    var whiteTimeRemaining: Double by mutableDoubleStateOf(0.0)
    var blackTimeRemaining: Double by mutableDoubleStateOf(0.0)

    var mascotMood: MascotMood by mutableStateOf(MascotMood.HAPPY)
    var speechText: String by mutableStateOf(
        LanguageManager.shared.tr(
            "Pick your challenge to begin the match!",
            "Choisissez votre niveau pour commencer !",
            "¡Elige la dificultad para empezar!"
        )
    )

    var calloutText: String? by mutableStateOf(null)
    var calloutColor: Color by mutableStateOf(Color(0xFFF3DDB0))

    var gamesWon: Int by mutableIntStateOf(ScoreManager.shared.highScore(GameIdentifier.CHESS))
    var boardVersion: Int by mutableIntStateOf(0)

    // Animation state for moving piece (driven safely on Compose's MonotonicFrameClock)
    var activeAnimatedMove: ChessMove? by mutableStateOf(null)
    var animProgress: Float by mutableFloatStateOf(1f)
    var moveAnimationTrigger: Int by mutableIntStateOf(0)
    var animSessionToken: UUID by mutableStateOf(UUID.randomUUID())
        private set
    var nudgedPieceId: UUID? by mutableStateOf(null)

    private val vmScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var aiJob: Job? = null
    private var calloutJob: Job? = null
    private var sessionToken: UUID = UUID.randomUUID()
    private var isActiveInForeground = true
    private var lastTickMillis = System.currentTimeMillis()
    private var hasPresentedEndOfSessionAd = false
    private var hasRecordedOutcomeForMatch = false

    init {
        syncPublishedState()
        RewardedAdManager.shared.preloadWheelsAdIfNeeded()
    }

    fun pauseGame() {
        if (showModeSetup || status.isTerminal || isPaused) return
        isPaused = true
        lastTickMillis = System.currentTimeMillis()
    }

    fun resumeGame() {
        if (!isPaused) return
        isPaused = false
        lastTickMillis = System.currentTimeMillis()
    }

    fun handleForegroundChange(isForeground: Boolean) {
        isActiveInForeground = isForeground
        lastTickMillis = System.currentTimeMillis()
        if (!isForeground) {
            pauseGame()
        }
    }

    fun tickClockIfNeeded(nowMillis: Long = System.currentTimeMillis()) {
        val elapsed = (nowMillis - lastTickMillis) / 1000.0
        lastTickMillis = nowMillis
        if (!isActiveInForeground ||
            showModeSetup ||
            isPaused ||
            pendingPromotion != null ||
            status.isTerminal ||
            timeControl.initialSeconds == null ||
            elapsed <= 0.0 ||
            elapsed >= 2.0
        ) {
            return
        }

        val winner = engine.tickClock(elapsed)
        if (winner != null) {
            cancelAI()
            selectedSquare = null
            highlightedMoves = emptyList()
            syncPublishedState()
            recordOutcomeIfNeeded(winner)
            AudioManager.shared.play(if (winner == humanColor) AudioManager.SoundEffect.WIN else AudioManager.SoundEffect.ERROR)
            mascotMood = if (winner == humanColor) MascotMood.PARTY else MascotMood.SAD
            speechText = if (winner == humanColor) {
                LanguageManager.shared.tr("Opponent's clock ran out! Victory!", "Temps adverse écoulé ! Victoire !", "¡Tiempo rival agotado! ¡Victoria!")
            } else {
                LanguageManager.shared.tr("Out of time! Better luck next game.", "Temps écoulé ! Retentez votre chance.", "¡Sin tiempo! Inténtalo de nuevo.")
            }
            presentEndOfSessionAdIfNeeded()
        } else {
            whiteTimeRemaining = engine.whiteTimeRemaining
            blackTimeRemaining = engine.blackTimeRemaining
        }
    }

    fun beginMatch(difficulty: ChessDifficulty, playerColor: PieceColor, timeControl: ChessTimeControl) {
        this.difficulty = difficulty
        this.humanColor = playerColor
        this.timeControl = timeControl
        this.showModeSetup = false
        startNewGame(difficulty, playerColor, timeControl)
    }

    fun openModeSetup() {
        cancelAI()
        sessionToken = UUID.randomUUID()
        isPaused = false
        isAnimating = false
        activeAnimatedMove = null
        animProgress = 1f
        pendingPromotion = null
        selectedSquare = null
        highlightedMoves = emptyList()
        calloutText = null
        status = ChessGameStatus.Playing
        showModeSetup = true
    }

    fun startNewGame(
        difficulty: ChessDifficulty? = null,
        playerColor: PieceColor? = null,
        timeControl: ChessTimeControl? = null
    ) {
        cancelAI()
        sessionToken = UUID.randomUUID()
        if (difficulty != null) this.difficulty = difficulty
        if (playerColor != null) this.humanColor = playerColor
        if (timeControl != null) this.timeControl = timeControl

        isPaused = false
        hasPresentedEndOfSessionAd = false
        hasRecordedOutcomeForMatch = false
        gamesWon = ScoreManager.shared.highScore(GameIdentifier.CHESS)
        RewardedAdManager.shared.preloadWheelsAdIfNeeded()
        engine.reset(this.difficulty, this.timeControl)
        selectedSquare = null
        highlightedMoves = emptyList()
        pendingPromotion = null
        calloutText = null
        isAnimating = false
        activeAnimatedMove = null
        animProgress = 1f
        lastTickMillis = System.currentTimeMillis()
        syncPublishedState()

        mascotMood = MascotMood.HAPPY
        if (this.humanColor == PieceColor.LIGHT) {
            speechText = LanguageManager.shared.tr(
                "Tap or drag any cream piece to make your move!",
                "Touchez ou glissez une pièce crème pour jouer !",
                "¡Toca o arrastra una pieza crema para mover!"
            )
        } else {
            speechText = LanguageManager.shared.tr(
                "Cream moves first! AI opening...",
                "Les crèmes jouent d'abord ! Ouverture de l'IA...",
                "¡Mueven las crema primero! Apertura de la IA..."
            )
            scheduleAIMove()
        }
        HapticsManager.shared.selection()
    }

    fun selectPromotion(move: ChessMove) {
        pendingPromotion = null
        executeMoveWithAnimation(move, byHuman = true)
    }

    fun cleanupOnExit() {
        cancelAI()
        sessionToken = UUID.randomUUID()
    }

    private fun matchesMoveDestination(move: ChessMove, targetSquare: ChessSquare): Boolean {
        if (move.to == targetSquare) return true
        if (move.isCastlingKingside && targetSquare == ChessSquare(7, move.from.rank)) return true
        if (move.isCastlingQueenside && targetSquare == ChessSquare(0, move.from.rank)) return true
        return false
    }

    fun handleDragPiece(from: ChessSquare, to: ChessSquare): Boolean {
        if (showModeSetup || isAnimating || isThinking || isPaused || pendingPromotion != null) return false
        if (status.isTerminal || turn != humanColor) return false

        val legal = engine.state.legalMovesFrom(from).filter { matchesMoveDestination(it, to) }
        if (legal.isEmpty()) return false

        selectedSquare = null
        highlightedMoves = emptyList()

        if (legal.size > 1 && legal.all { it.promotion != null }) {
            pendingPromotion = PendingPromotion(from = from, to = to, color = humanColor, options = legal)
            AudioManager.shared.play(AudioManager.SoundEffect.TAP)
            return true
        } else {
            val move = legal.firstOrNull() ?: return false
            executeMoveWithAnimation(move, byHuman = true)
            return true
        }
    }

    fun handleBoardTap(square: ChessSquare) {
        if (showModeSetup || isAnimating || isThinking || isPaused || pendingPromotion != null) return
        if (status.isTerminal || turn != humanColor) return

        if (selectedSquare != null) {
            val matchingMoves = highlightedMoves.filter { matchesMoveDestination(it, square) }
            if (matchingMoves.isNotEmpty()) {
                if (matchingMoves.size > 1 && matchingMoves.all { it.promotion != null }) {
                    selectedSquare = null
                    highlightedMoves = emptyList()
                    pendingPromotion = PendingPromotion(
                        from = matchingMoves[0].from,
                        to = square,
                        color = humanColor,
                        options = matchingMoves
                    )
                    AudioManager.shared.play(AudioManager.SoundEffect.TAP)
                    return
                } else {
                    val move = matchingMoves.first()
                    selectedSquare = null
                    highlightedMoves = emptyList()
                    executeMoveWithAnimation(move, byHuman = true)
                    return
                }
            }
        }

        val tappedPiece = engine.state.piece(square)
        if (tappedPiece != null && tappedPiece.color == humanColor) {
            val legal = engine.state.legalMovesFrom(square)
            if (legal.isEmpty()) {
                selectedSquare = null
                highlightedMoves = emptyList()
                val inCheck = engine.state.isKingInCheck(humanColor)
                nudgedPieceId = tappedPiece.id
                HapticsManager.shared.warning()
                mascotMood = MascotMood.WOW
                speechText = if (inCheck) {
                    LanguageManager.shared.tr(
                        "Your King is in check! Move to protect the King.",
                        "Votre Roi est en échec ! Protégez le Roi.",
                        "¡Tu Rey está en jaque! Protege al Rey."
                    )
                } else {
                    LanguageManager.shared.tr(
                        "No legal moves for that piece right now!",
                        "Aucun coup légal pour cette pièce !",
                        "¡Esa pieza no tiene jugadas legales ahora!"
                    )
                }
                return
            }

            selectedSquare = square
            highlightedMoves = legal
            AudioManager.shared.play(AudioManager.SoundEffect.TAP)
            HapticsManager.shared.selection()
        } else {
            selectedSquare = null
            highlightedMoves = emptyList()
        }
    }

    private fun recordOutcomeIfNeeded(winner: PieceColor?) {
        if (hasRecordedOutcomeForMatch) return
        hasRecordedOutcomeForMatch = true
        gamesWon = ScoreManager.shared.recordChessOutcome(winner = winner, humanColor = humanColor)
    }

    private fun executeMoveWithAnimation(move: ChessMove, byHuman: Boolean) {
        val activeToken = sessionToken
        if (!engine.performMove(move)) return
        val postState = engine.state
        syncPublishedState()
        updateMascotAndCallout(move, postState.status, byHuman)

        if (move.isCapture) {
            AudioManager.shared.play(AudioManager.SoundEffect.BOUNCE)
            HapticsManager.shared.mediumImpact()
        } else {
            AudioManager.shared.play(AudioManager.SoundEffect.POP)
            HapticsManager.shared.lightTap()
        }

        if (postState.status.isTerminal) {
            recordOutcomeIfNeeded(postState.status.winner)
            when (val st = postState.status) {
                is ChessGameStatus.Checkmate -> {
                    AudioManager.shared.play(if (st.winnerColor == humanColor) AudioManager.SoundEffect.WIN else AudioManager.SoundEffect.ERROR)
                }
                ChessGameStatus.Stalemate,
                ChessGameStatus.DrawFiftyMove,
                ChessGameStatus.DrawThreefoldRepetition,
                ChessGameStatus.DrawInsufficientMaterial -> {
                    AudioManager.shared.play(AudioManager.SoundEffect.DING)
                }
                else -> {}
            }
        } else if (postState.status is ChessGameStatus.Check) {
            AudioManager.shared.play(AudioManager.SoundEffect.SWITCH_OFF)
        }

        animProgress = 0f
        isAnimating = true
        activeAnimatedMove = move
        animSessionToken = activeToken
        moveAnimationTrigger += 1
    }

    fun onMoveAnimationFinished(completedToken: UUID) {
        if (sessionToken != completedToken) return
        activeAnimatedMove = null
        animProgress = 1f
        isAnimating = false
        boardVersion += 1

        val currentState = engine.state
        if (currentState.status.isTerminal) {
            recordOutcomeIfNeeded(currentState.status.winner)
            presentEndOfSessionAdIfNeeded()
        } else if (currentState.turn != humanColor) {
            scheduleAIMove()
        }
    }

    private fun presentEndOfSessionAdIfNeeded() {
        if (hasPresentedEndOfSessionAd) return
        hasPresentedEndOfSessionAd = true
        val activeToken = sessionToken

        vmScope.launch {
            delay(850L)
            if (sessionToken != activeToken || !status.isTerminal) return@launch
            RewardedAdManager.shared.presentWheelsAd(
                onRewardEarned = {
                    RewardedAdManager.shared.preloadWheelsAdIfNeeded()
                },
                onDismissedWithoutReward = {
                    RewardedAdManager.shared.preloadWheelsAdIfNeeded()
                }
            )
        }
    }

    private fun scheduleAIMove() {
        cancelAI()
        val activeToken = sessionToken
        val boardSnapshot = engine.state.copy()
        val chosenDifficulty = difficulty
        isThinking = true

        aiJob = vmScope.launch {
            val thinkStart = System.currentTimeMillis()
            val chosenMove = withContext(Dispatchers.Default) {
                ChessAI.bestMove(boardSnapshot, chosenDifficulty)
            }
            val elapsed = System.currentTimeMillis() - thinkStart
            if (elapsed < 200L) {
                delay(200L - elapsed)
            }
            if (sessionToken != activeToken) return@launch
            isThinking = false
            if (chosenMove != null && !engine.state.status.isTerminal) {
                executeMoveWithAnimation(chosenMove, byHuman = false)
            }
        }
    }

    private fun cancelAI() {
        aiJob?.cancel()
        aiJob = null
        isThinking = false
    }

    private fun syncPublishedState() {
        status = engine.state.status
        turn = engine.state.turn
        fullMoveNumber = engine.state.fullMoveNumber
        lastMoveNotation = engine.state.lastMove?.notation ?: ""
        whiteTimeRemaining = engine.whiteTimeRemaining
        blackTimeRemaining = engine.blackTimeRemaining
        boardVersion += 1
    }

    private fun triggerCallout(text: String, color: Color) {
        calloutJob?.cancel()
        calloutColor = color
        calloutText = text
        calloutJob = vmScope.launch {
            delay(1700L)
            calloutText = null
        }
    }

    private fun updateMascotAndCallout(move: ChessMove, status: ChessGameStatus, byHuman: Boolean) {
        when (status) {
            is ChessGameStatus.Checkmate -> {
                triggerCallout(
                    LanguageManager.shared.tr("CHECKMATE!", "ÉCHEC ET MAT !", "¡JAQUE MATE!"),
                    color = if (status.winnerColor == humanColor) Color(0xFFF3DDB0) else CheerUpColors.Tap.red
                )
                if (status.winnerColor == humanColor) {
                    mascotMood = MascotMood.PARTY
                    speechText = LanguageManager.shared.tr("CHECKMATE! Brilliant victory!", "ÉCHEC ET MAT ! Magnifique victoire !", "¡JAQUE MATE! ¡Victoria brillante!")
                } else {
                    mascotMood = MascotMood.SAD
                    speechText = LanguageManager.shared.tr("Checkmate! Rematch anytime!", "Échec et mat ! On prend la revanche ?", "¡Jaque mate! ¿Revancha cuando quieras?")
                }
            }
            is ChessGameStatus.Check -> {
                val aiInCheck = status.color != humanColor
                triggerCallout(
                    LanguageManager.shared.tr("CHECK!", "ÉCHEC !", "¡JAQUE!"),
                    color = if (aiInCheck) Color(0xFF2EDB8A) else CheerUpColors.Tap.red
                )
                mascotMood = if (aiInCheck) MascotMood.PARTY else MascotMood.WOW
                speechText = if (aiInCheck) {
                    LanguageManager.shared.tr("Nice check! Keep up the pressure!", "Joli échec ! Maintenez la pression !", "¡Buen jaque! ¡Sigue presionando!")
                } else {
                    LanguageManager.shared.tr("Watch out — your King is in check!", "Attention — votre Roi est en échec !", "¡Cuidado! ¡Tu Rey está en jaque!")
                }
            }
            ChessGameStatus.Stalemate -> {
                triggerCallout(LanguageManager.shared.tr("STALEMATE", "PAT", "AHOGADO"), CheerUpColors.lightGrey)
                mascotMood = MascotMood.SUS
                speechText = LanguageManager.shared.tr("Stalemate! A hard-fought draw.", "Pat ! Match nul très disputé.", "¡Ahogado! Empate muy reñido.")
            }
            ChessGameStatus.DrawFiftyMove,
            ChessGameStatus.DrawThreefoldRepetition,
            ChessGameStatus.DrawInsufficientMaterial -> {
                triggerCallout(LanguageManager.shared.tr("DRAW GAME", "MATCH NUL", "TABLAS"), CheerUpColors.lightGrey)
                mascotMood = MascotMood.SUS
                speechText = LanguageManager.shared.tr("Draw game! Well balanced.", "Partie nulle ! Bien équilibré.", "¡Tablas! Partida muy equilibrada.")
            }
            else -> {
                when {
                    move.promotion != null -> {
                        triggerCallout(LanguageManager.shared.tr("PROMOTION!", "PROMOTION !", "¡CORONACIÓN!"), Color(0xFFF3DDB0))
                        mascotMood = if (byHuman) MascotMood.PARTY else MascotMood.WOW
                        speechText = if (byHuman) {
                            LanguageManager.shared.tr("Pawn promoted! Royal power unlocked!", "Pion promu ! Puissance royale !", "¡Peón coronado! ¡Nuevo poder real!")
                        } else {
                            LanguageManager.shared.tr("Whoa! Opponent promoted a pawn!", "Oh ! L'adversaire a promu un pion !", "¡Vaya! ¡El rival coronó un peón!")
                        }
                    }
                    move.isCastling -> {
                        triggerCallout(LanguageManager.shared.tr("CASTLED!", "ROQUE !", "¡ENROQUE!"), Color(0xFF22D3EE))
                        mascotMood = MascotMood.HAPPY
                        speechText = LanguageManager.shared.tr("Crisp castling! King safely tucked.", "Joli roque ! Le Roi est à l'abri.", "¡Buen enroque! El Rey está seguro.")
                    }
                    move.isEnPassant -> {
                        triggerCallout("EN PASSANT!", Color(0xFFF3DDB0))
                        mascotMood = MascotMood.PARTY
                        speechText = LanguageManager.shared.tr("En Passant! Classic master tactic!", "Prise en passant ! Coup de maître !", "¡Al paso! ¡Jugada maestra!")
                    }
                    move.capturedPiece != null -> {
                        val cap = move.capturedPiece
                        if (cap.kind.value >= 5) {
                            triggerCallout(
                                if (byHuman) LanguageManager.shared.tr("BIG CAPTURE!", "GROSSE PRISE !", "¡GRAN CAPTURA!")
                                else LanguageManager.shared.tr("WATCH OUT!", "ATTENTION !", "¡CUIDADO!"),
                                color = if (byHuman) Color(0xFFF3DDB0) else CheerUpColors.Tap.red
                            )
                        }
                        mascotMood = if (byHuman) MascotMood.PARTY else MascotMood.DIZZY
                        speechText = if (byHuman) {
                            if (cap.kind.value >= 5) LanguageManager.shared.tr("Huge capture! What a strike!", "Prise énorme ! Quel coup !", "¡Gran captura! ¡Vaya golpe!")
                            else LanguageManager.shared.tr("Clean capture! Material up!", "Jolie prise !", "¡Buena captura!")
                        } else {
                            LanguageManager.shared.tr("Ouch! Careful on the next move!", "Aïe ! Attention au prochain coup !", "¡Ay! ¡Ojo en la siguiente jugada!")
                        }
                    }
                    byHuman -> {
                        mascotMood = MascotMood.HAPPY
                        speechText = LanguageManager.shared.tr("Thinking of a reply...", "Réflexion en cours...", "Pensando respuesta...")
                    }
                    else -> {
                        mascotMood = MascotMood.HAPPY
                        speechText = LanguageManager.shared.tr("Your turn! Spot the best tactic.", "À vous ! Trouvez le meilleur coup.", "¡Tu turno! Busca la mejor táctica.")
                    }
                }
            }
        }
    }

    fun formattedClock(seconds: Double): String {
        val clamped = max(0, ceil(seconds).toInt())
        val m = clamped / 60
        val s = clamped % 60
        return "%d:%02d".format(m, s)
    }
}

private data class ChessShatterParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    var age: Float = 0f,
    val duration: Float = 0.36f
)

private class ChessBoardMetrics {
    var leftPx: Float = 0f
    var topPx: Float = 0f
    var tileSizePx: Float = 44f
}

@Composable
fun ChessScreen(
    onExit: () -> Unit
) {
    val viewModel = remember { ChessViewModel() }

    fun handleExit() {
        viewModel.cleanupOnExit()
        onExit()
    }

    BackHandler(onBack = ::handleExit)

    val pawnLight = ImageBitmap.imageResource(R.drawable.chess_pawn_light)
    val pawnDark = ImageBitmap.imageResource(R.drawable.chess_pawn_dark)
    val knightLight = ImageBitmap.imageResource(R.drawable.chess_knight_light)
    val knightDark = ImageBitmap.imageResource(R.drawable.chess_knight_dark)
    val bishopLight = ImageBitmap.imageResource(R.drawable.chess_bishop_light)
    val bishopDark = ImageBitmap.imageResource(R.drawable.chess_bishop_dark)
    val rookLight = ImageBitmap.imageResource(R.drawable.chess_rook_light)
    val rookDark = ImageBitmap.imageResource(R.drawable.chess_rook_dark)
    val queenLight = ImageBitmap.imageResource(R.drawable.chess_queen_light)
    val queenDark = ImageBitmap.imageResource(R.drawable.chess_queen_dark)
    val kingLight = ImageBitmap.imageResource(R.drawable.chess_king_light)
    val kingDark = ImageBitmap.imageResource(R.drawable.chess_king_dark)

    val pieceBitmaps = remember {
        mapOf(
            (PieceKind.PAWN to PieceColor.LIGHT) to pawnLight,
            (PieceKind.PAWN to PieceColor.DARK) to pawnDark,
            (PieceKind.KNIGHT to PieceColor.LIGHT) to knightLight,
            (PieceKind.KNIGHT to PieceColor.DARK) to knightDark,
            (PieceKind.BISHOP to PieceColor.LIGHT) to bishopLight,
            (PieceKind.BISHOP to PieceColor.DARK) to bishopDark,
            (PieceKind.ROOK to PieceColor.LIGHT) to rookLight,
            (PieceKind.ROOK to PieceColor.DARK) to rookDark,
            (PieceKind.QUEEN to PieceColor.LIGHT) to queenLight,
            (PieceKind.QUEEN to PieceColor.DARK) to queenDark,
            (PieceKind.KING to PieceColor.LIGHT) to kingLight,
            (PieceKind.KING to PieceColor.DARK) to kingDark
        )
    }

    var selectedDifficulty by remember { mutableStateOf(ChessDifficulty.INTERMEDIATE) }
    var selectedPlayerColor by remember { mutableStateOf(PieceColor.LIGHT) }
    var selectedTimeControl by remember { mutableStateOf(ChessTimeControl.UNTIMED) }
    val textMeasurer = rememberTextMeasurer(cacheSize = 32)

    var draggingFromSquare by remember { mutableStateOf<ChessSquare?>(null) }
    var draggingPos by remember { mutableStateOf<Offset?>(null) }
    val shards = remember { mutableStateListOf<ChessShatterParticle>() }

    // Drive piece move animation cleanly inside Compose's MonotonicFrameClock scope
    LaunchedEffect(viewModel.moveAnimationTrigger) {
        if (viewModel.moveAnimationTrigger == 0) return@LaunchedEffect
        val token = viewModel.animSessionToken
        val anim = Animatable(0f)
        anim.animateTo(1f, tween(durationMillis = 210)) {
            viewModel.animProgress = value
        }
        viewModel.onMoveAnimationFinished(token)
    }

    // Clear active drag state if modal/pause/terminal overlay opens mid-drag
    LaunchedEffect(viewModel.showModeSetup, viewModel.isPaused, viewModel.status.isTerminal) {
        if (viewModel.showModeSetup || viewModel.isPaused || viewModel.status.isTerminal) {
            draggingFromSquare = null
            draggingPos = null
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    viewModel.handleForegroundChange(false)
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.handleForegroundChange(true)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!RewardedAdManager.shared.isPresentingAd) {
                viewModel.cleanupOnExit()
            }
        }
    }

    // 250ms clock timer (runs only when timed clock is active)
    val shouldRunClock = viewModel.timeControl != ChessTimeControl.UNTIMED &&
        !viewModel.showModeSetup &&
        !viewModel.isPaused &&
        !viewModel.status.isTerminal
    LaunchedEffect(shouldRunClock) {
        if (shouldRunClock) {
            while (true) {
                delay(250L)
                viewModel.tickClockIfNeeded()
            }
        }
    }

    // Spawn pixel shatter shards on captures
    LaunchedEffect(viewModel.boardVersion) {
        val last = viewModel.engine.state.lastMove
        if (last != null && last.capturedPiece != null) {
            val victimColor = last.capturedPiece.color
            val palette = if (victimColor == PieceColor.LIGHT) {
                listOf(Color(0xFFF7E3B8), Color(0xFFE3C491), Color(0xFFB88F5C))
            } else {
                listOf(Color(0xFF8F593D), Color(0xFF6E422E), Color(0xFF422417))
            }
            val isFlipped = viewModel.humanColor == PieceColor.DARK
            val vf = if (isFlipped) (7 - last.to.file) else last.to.file
            val vr = if (isFlipped) last.to.rank else (7 - last.to.rank)
            for (i in 0 until 14) {
                val angle = Random.nextDouble(0.0, 2.0 * PI)
                val speed = Random.nextDouble(1.2, 3.4).toFloat()
                shards.add(
                    ChessShatterParticle(
                        x = vf + 0.5f,
                        y = vr + 0.5f,
                        vx = cos(angle).toFloat() * speed,
                        vy = sin(angle).toFloat() * speed,
                        size = Random.nextDouble(0.08, 0.14).toFloat(),
                        color = palette[i % palette.size]
                    )
                )
            }
        }
    }

    LaunchedEffect(shards.isNotEmpty()) {
        if (shards.isNotEmpty()) {
            var lastNanos = 0L
            while (shards.isNotEmpty()) {
                withFrameNanos { nanos ->
                    val dt = if (lastNanos == 0L) 1f / 60f else ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    lastNanos = nanos
                    for (i in shards.indices.reversed()) {
                        val s = shards[i]
                        s.age += dt
                        s.x += s.vx * dt
                        s.y += s.vy * dt
                        if (s.age >= s.duration) shards.removeAt(i)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(CheerUpColors.backgroundGradient))
            .systemBarsPadding()
    ) {
        // Subtle warm amber ambient glow behind the wooden pixel chessboard
        Canvas(modifier = Modifier.fillMaxSize()) {
            val glowRadius = size.minDimension * 0.75f
            if (glowRadius > 1f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFB87A44).copy(alpha = 0.18f), Color.Transparent),
                        center = center,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = center
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 2.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheerUpCircleIconButton(
                    symbol = "xmark",
                    size = 34.dp,
                    iconSize = 14.dp,
                    onClick = {
                        HapticsManager.shared.selection()
                        handleExit()
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Active Match Mode Pill
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CheerUpColors.darkBlue.copy(alpha = 0.9f))
                        .border(1.dp, CheerUpColors.blueGrey.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheerUpSymbolIcon(
                        symbol = "crown.fill",
                        tint = Color(0xFFF3DDB0),
                        size = 11.dp
                    )
                    Text(
                        text = "CHESS • ${viewModel.difficulty.localizedTitle}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = CheerUpColors.lightGrey
                    )
                    if (viewModel.timeControl != ChessTimeControl.UNTIMED) {
                        Text(
                            text = "• ${viewModel.timeControl.shortTitle}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF3DDB0)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                CheerUpCircleIconButton(
                    symbol = "pause.fill",
                    size = 34.dp,
                    iconSize = 14.dp,
                    onClick = {
                        HapticsManager.shared.selection()
                        viewModel.pauseGame()
                    }
                )
            }

            // Top-Left MiniCheerUp! Mascot Stage + Optional Clocks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheerUpMascotView(mood = viewModel.mascotMood, size = 48.dp)

                val bubbleShape = remember { SpeechBubbleShape() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .shadow(5.dp, bubbleShape)
                        .clip(bubbleShape)
                        .background(CheerUpColors.lightGrey)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = viewModel.speechText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CheerUpColors.deepNavy,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (viewModel.timeControl.initialSeconds != null) {
                    val opponentColor = viewModel.humanColor.opponent
                    val playerColor = viewModel.humanColor
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        ChessClockPill(
                            time = viewModel.formattedClock(
                                if (opponentColor == PieceColor.DARK) viewModel.blackTimeRemaining else viewModel.whiteTimeRemaining
                            ),
                            active = viewModel.turn == opponentColor && !viewModel.status.isTerminal,
                            isDarkPiece = opponentColor == PieceColor.DARK
                        )
                        ChessClockPill(
                            time = viewModel.formattedClock(
                                if (playerColor == PieceColor.DARK) viewModel.blackTimeRemaining else viewModel.whiteTimeRemaining
                            ),
                            active = viewModel.turn == playerColor && !viewModel.status.isTerminal,
                            isDarkPiece = playerColor == PieceColor.DARK
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Maximized 2D Brown-and-Cream Pixel Chessboard + Captured Trays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(384f / 452f),
                contentAlignment = Alignment.TopCenter
            ) {
                val _ver = viewModel.boardVersion
                val isBoardFlipped = viewModel.humanColor == PieceColor.DARK

                val boardMetrics = remember { ChessBoardMetrics() }

                fun screenToSquare(pt: Offset): ChessSquare? {
                    val localX = pt.x - boardMetrics.leftPx
                    val localY = pt.y - boardMetrics.topPx
                    if (localX < 0f || localY < 0f || boardMetrics.tileSizePx <= 1f) return null
                    val dispFile = floor(localX / boardMetrics.tileSizePx).toInt()
                    val dispRowFromTop = floor(localY / boardMetrics.tileSizePx).toInt()
                    if (dispFile !in 0..7 || dispRowFromTop !in 0..7) return null
                    val file = if (isBoardFlipped) (7 - dispFile) else dispFile
                    val rank = if (isBoardFlipped) dispRowFromTop else (7 - dispRowFromTop)
                    val sq = ChessSquare(file, rank)
                    return if (sq.isValid) sq else null
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(viewModel.showModeSetup, viewModel.isPaused, viewModel.status, isBoardFlipped) {
                            if (viewModel.showModeSetup || viewModel.isPaused || viewModel.status.isTerminal) return@pointerInput
                            awaitEachGesture {
                                val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                                val startSq = screenToSquare(down.position) ?: return@awaitEachGesture
                                val startPt = down.position
                                val friendlyPiece = viewModel.engine.state.piece(startSq)
                                val isFriendly = friendlyPiece != null && friendlyPiece.color == viewModel.humanColor

                                if (isFriendly) {
                                    viewModel.handleBoardTap(startSq)
                                }

                                var isDragging = false
                                var lastPt = startPt

                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    lastPt = change.position
                                    if (change.pressed && isFriendly) {
                                        if (!isDragging && hypot(lastPt.x - startPt.x, lastPt.y - startPt.y) > 12f) {
                                            isDragging = true
                                            draggingFromSquare = startSq
                                        }
                                        if (isDragging) {
                                            draggingPos = lastPt
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                val endSq = screenToSquare(lastPt)
                                if (isDragging) {
                                    draggingFromSquare = null
                                    draggingPos = null
                                    if (endSq != null && endSq != startSq) {
                                        viewModel.handleDragPiece(startSq, endSq)
                                    }
                                } else if (!isFriendly && endSq != null) {
                                    viewModel.handleBoardTap(endSq)
                                }
                            }
                        }
                ) {
                    if (size.width < 64f || size.height < 64f) return@Canvas
                    val scale = size.width / 384f
                    val tile = 44f * scale
                    boardMetrics.tileSizePx = tile
                    val leftRail = 18f * scale
                    val bottomRail = 18f * scale
                    val outerTrim = 8f * scale
                    val gridSpan = tile * 8f
                    val frameW = leftRail + gridSpan + outerTrim
                    val frameH = bottomRail + gridSpan + outerTrim

                    val frameLeft = (size.width - frameW) * 0.5f
                    val frameTop = (size.height - frameH) * 0.5f
                    val gridLeft = frameLeft + leftRail
                    val gridTop = frameTop + outerTrim
                    boardMetrics.leftPx = gridLeft
                    boardMetrics.topPx = gridTop

                    // Top & Bottom Captured Piece Trays
                    val trayH = 28f * scale
                    val topTrayCenterY = frameTop - 19f * scale
                    val bottomTrayCenterY = frameTop + frameH + 19f * scale

                    for (ty in listOf(topTrayCenterY, bottomTrayCenterY)) {
                        drawRoundRect(
                            color = Color(0xFF12212E).copy(alpha = 0.78f),
                            topLeft = Offset(frameLeft, ty - trayH * 0.5f),
                            size = Size(frameW, trayH),
                            cornerRadius = CornerRadius(6f * scale, 6f * scale)
                        )
                        drawRoundRect(
                            color = Color(0xFF6E4229).copy(alpha = 0.55f),
                            topLeft = Offset(frameLeft, ty - trayH * 0.5f),
                            size = Size(frameW, trayH),
                            cornerRadius = CornerRadius(6f * scale, 6f * scale),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    fun sortOrder(k: PieceKind): Int = when (k) {
                        PieceKind.QUEEN -> 0
                        PieceKind.ROOK -> 1
                        PieceKind.BISHOP -> 2
                        PieceKind.KNIGHT -> 3
                        PieceKind.PAWN -> 4
                        PieceKind.KING -> 5
                    }

                    val lightCaptured = viewModel.engine.state.capturedByDark.sortedBy { sortOrder(it) }
                    val darkCaptured = viewModel.engine.state.capturedByLight.sortedBy { sortOrder(it) }
                    val topKinds = if (isBoardFlipped) darkCaptured else lightCaptured
                    val topTrayPieceColor = if (isBoardFlipped) PieceColor.DARK else PieceColor.LIGHT
                    val bottomKinds = if (isBoardFlipped) lightCaptured else darkCaptured
                    val bottomTrayPieceColor = if (isBoardFlipped) PieceColor.LIGHT else PieceColor.DARK

                    val miniW = (17f * scale).toInt()
                    val miniH = (28f * scale).toInt()
                    val miniStep = 11.5f * scale

                    topKinds.forEachIndexed { idx, kind ->
                        val bmp = pieceBitmaps[kind to topTrayPieceColor] ?: return@forEachIndexed
                        val ix = (frameLeft + 12f * scale + idx * miniStep).toInt()
                        val iy = (topTrayCenterY - miniH * 0.5f).toInt()
                        drawImage(
                            image = bmp,
                            dstOffset = IntOffset(ix, iy),
                            dstSize = IntSize(miniW, miniH),
                            filterQuality = FilterQuality.None
                        )
                    }

                    bottomKinds.forEachIndexed { idx, kind ->
                        val bmp = pieceBitmaps[kind to bottomTrayPieceColor] ?: return@forEachIndexed
                        val ix = (frameLeft + 12f * scale + idx * miniStep).toInt()
                        val iy = (bottomTrayCenterY - miniH * 0.5f).toInt()
                        drawImage(
                            image = bmp,
                            dstOffset = IntOffset(ix, iy),
                            dstSize = IntSize(miniW, miniH),
                            filterQuality = FilterQuality.None
                        )
                    }

                    // Material Advantage Labels (+X)
                    val playerMat = bottomKinds.sumOf { it.value }
                    val aiMat = topKinds.sumOf { it.value }
                    val diff = playerMat - aiMat
                    if (diff > 0) {
                        val advLayout = textMeasurer.measure(
                            text = "+$diff",
                            style = TextStyle(color = Color(0xFFFFF09C), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        )
                        drawText(
                            textLayoutResult = advLayout,
                            topLeft = Offset(frameLeft + frameW - advLayout.size.width - 10f * scale, bottomTrayCenterY - advLayout.size.height * 0.5f)
                        )
                    } else if (diff < 0) {
                        val advLayout = textMeasurer.measure(
                            text = "+${-diff}",
                            style = TextStyle(color = Color(0xFFF7E3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        )
                        drawText(
                            textLayoutResult = advLayout,
                            topLeft = Offset(frameLeft + frameW - advLayout.size.width - 10f * scale, topTrayCenterY - advLayout.size.height * 0.5f)
                        )
                    }

                    // Drop Shadow & Outer Walnut Wood Frame (#45291B)
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.42f),
                        topLeft = Offset(frameLeft, frameTop + 5f * scale),
                        size = Size(frameW, frameH),
                        cornerRadius = CornerRadius(6f * scale, 6f * scale)
                    )
                    drawRoundRect(
                        color = Color(0xFF45291B),
                        topLeft = Offset(frameLeft, frameTop),
                        size = Size(frameW, frameH),
                        cornerRadius = CornerRadius(5f * scale, 5f * scale)
                    )
                    drawRoundRect(
                        color = Color(0xFF24120A),
                        topLeft = Offset(frameLeft, frameTop),
                        size = Size(frameW, frameH),
                        cornerRadius = CornerRadius(5f * scale, 5f * scale),
                        style = Stroke(width = 2f * scale)
                    )
                    // Inner wood bevel
                    drawRoundRect(
                        color = Color(0xFF6E4229),
                        topLeft = Offset(frameLeft + 4f * scale, frameTop + 4f * scale),
                        size = Size(frameW - 8f * scale, frameH - 8f * scale),
                        cornerRadius = CornerRadius(3f * scale, 3f * scale),
                        style = Stroke(width = 2f * scale)
                    )

                    // 8x8 Chessboard Grid Tiles (#EDD3A8 light, #7B4A2E dark)
                    fun squareTopLeft(sq: ChessSquare): Offset {
                        val vf = if (isBoardFlipped) (7 - sq.file) else sq.file
                        val vr = if (isBoardFlipped) sq.rank else (7 - sq.rank)
                        return Offset(gridLeft + vf * tile, gridTop + vr * tile)
                    }

                    for (rank in 0 until 8) {
                        for (file in 0 until 8) {
                            val sq = ChessSquare(file, rank)
                            val tl = squareTopLeft(sq)
                            drawRect(
                                color = if (sq.isLightSquare) Color(0xFFEDD3A8) else Color(0xFF7B4A2E),
                                topLeft = tl,
                                size = Size(tile, tile)
                            )
                        }
                    }

                    // Cream Hairline around 8x8 tiles
                    drawRect(
                        color = Color(0xFFF7E3B8),
                        topLeft = Offset(gridLeft - scale, gridTop - scale),
                        size = Size(gridSpan + 2f * scale, gridSpan + 2f * scale),
                        style = Stroke(width = 2f * scale)
                    )

                    // File labels a-h on bottom wood rail & Rank labels 1-8 on left wood rail
                    val files = if (isBoardFlipped) listOf("h", "g", "f", "e", "d", "c", "b", "a") else listOf("a", "b", "c", "d", "e", "f", "g", "h")
                    files.forEachIndexed { idx, letter ->
                        val layout = textMeasurer.measure(
                            text = letter,
                            style = TextStyle(color = Color(0xFFF7E3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(
                                gridLeft + (idx + 0.5f) * tile - layout.size.width * 0.5f,
                                gridTop + gridSpan + (bottomRail - layout.size.height) * 0.5f
                            )
                        )
                    }
                    for (visRow in 0 until 8) {
                        val logicalRank = if (isBoardFlipped) (visRow + 1) else (8 - visRow)
                        val layout = textMeasurer.measure(
                            text = "$logicalRank",
                            style = TextStyle(color = Color(0xFFF7E3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(
                                frameLeft + (leftRail - layout.size.width) * 0.5f,
                                gridTop + (visRow + 0.5f) * tile - layout.size.height * 0.5f
                            )
                        )
                    }

                    // Last Move Highlights
                    viewModel.engine.state.lastMove?.let { last ->
                        for (sq in listOf(last.from, last.to)) {
                            val tl = squareTopLeft(sq)
                            drawRect(
                                color = Color(0xFFD19E4D).copy(alpha = if (sq.isLightSquare) 0.42f else 0.34f),
                                topLeft = tl,
                                size = Size(tile, tile)
                            )
                        }
                    }

                    // Checked King Red Alert Frame
                    viewModel.status.inCheckColor?.let { checkedColor ->
                        viewModel.engine.state.kingSquare(checkedColor)?.let { kingSq ->
                            val tl = squareTopLeft(kingSq)
                            drawRect(
                                color = Color(0xFFDB4238),
                                topLeft = tl,
                                size = Size(tile, tile),
                                style = Stroke(width = 3.5f * scale)
                            )
                        }
                    }

                    // Selected Square Gold Corner Frame & Legal Move Markers
                    viewModel.selectedSquare?.let { selSq ->
                        val tl = squareTopLeft(selSq)
                        drawRect(
                            color = Color(0xFFFFF09C),
                            topLeft = tl,
                            size = Size(tile, tile),
                            style = Stroke(width = 3f * scale)
                        )
                    }

                    for (mv in viewModel.highlightedMoves) {
                        val tl = squareTopLeft(mv.to)
                        val center = Offset(tl.x + tile * 0.5f, tl.y + tile * 0.5f)
                        if (mv.isCapture) {
                            drawRect(
                                color = Color(0xFFDB4238).copy(alpha = 0.9f),
                                topLeft = tl + Offset(2f * scale, 2f * scale),
                                size = Size(tile - 4f * scale, tile - 4f * scale),
                                style = Stroke(width = 3f * scale)
                            )
                        } else {
                            drawRoundRect(
                                color = Color(0xFFF7C447).copy(alpha = 0.88f),
                                topLeft = Offset(center.x - 6f * scale, center.y - 6f * scale),
                                size = Size(12f * scale, 12f * scale),
                                cornerRadius = CornerRadius(3f * scale, 3f * scale)
                            )
                        }
                    }

                    // Draw Pieces from Top Visual Rank down to Bottom Visual Rank for correct overlap
                    val pieceW = (30f * scale).toInt()
                    val pieceH = (49f * scale).toInt()
                    val animMove = viewModel.activeAnimatedMove
                    val animT = viewModel.animProgress

                    for (visRow in 0 until 8) {
                        for (visCol in 0 until 8) {
                            val file = if (isBoardFlipped) (7 - visCol) else visCol
                            val rank = if (isBoardFlipped) visRow else (7 - visRow)
                            val sq = ChessSquare(file, rank)
                            if (draggingFromSquare == sq) continue
                            if (animMove != null && animMove.to == sq && animT < 1f) continue

                            val p = viewModel.engine.state.piece(sq) ?: continue
                            val bmp = pieceBitmaps[p.kind to p.color] ?: continue
                            val tl = squareTopLeft(sq)
                            val bottomCenterX = tl.x + tile * 0.5f
                            val bottomY = tl.y + tile - 1f * scale
                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset((bottomCenterX - pieceW * 0.5f).toInt(), (bottomY - pieceH).toInt()),
                                dstSize = IntSize(pieceW, pieceH),
                                filterQuality = FilterQuality.None
                            )
                        }
                    }

                    // Animated Moving Piece (with Knight/Pawn hop arc)
                    if (animMove != null && animT < 1f) {
                        val fromTL = squareTopLeft(animMove.from)
                        val toTL = squareTopLeft(animMove.to)
                        val startX = fromTL.x + tile * 0.5f
                        val startY = fromTL.y + tile - 1f * scale
                        val endX = toTL.x + tile * 0.5f
                        val endY = toTL.y + tile - 1f * scale

                        val peak = when (animMove.movedPiece.kind) {
                            PieceKind.KNIGHT -> tile * 0.56f
                            PieceKind.PAWN -> tile * 0.16f
                            else -> 0f
                        }
                        val curX = startX + (endX - startX) * animT
                        val curY = startY + (endY - startY) * animT - 4f * peak * animT * (1f - animT)
                        val kindToDraw = animMove.promotion ?: animMove.movedPiece.kind
                        val bmp = pieceBitmaps[kindToDraw to animMove.movedPiece.color]
                        if (bmp != null) {
                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset((curX - pieceW * 0.5f).toInt(), (curY - pieceH).toInt()),
                                dstSize = IntSize(pieceW, pieceH),
                                filterQuality = FilterQuality.None
                            )
                        }
                    }

                    // Actively Dragged Piece
                    val dragSq = draggingFromSquare
                    val dragPt = draggingPos
                    if (dragSq != null && dragPt != null) {
                        val p = viewModel.engine.state.piece(dragSq)
                        if (p != null) {
                            val bmp = pieceBitmaps[p.kind to p.color]
                            if (bmp != null) {
                                val dw = (pieceW * 1.12f).toInt()
                                val dh = (pieceH * 1.12f).toInt()
                                drawImage(
                                    image = bmp,
                                    dstOffset = IntOffset((dragPt.x - dw * 0.5f).toInt(), (dragPt.y - dh * 0.85f).toInt()),
                                    dstSize = IntSize(dw, dh),
                                    filterQuality = FilterQuality.None
                                )
                            }
                        }
                    }

                    // Capture Shatter Shards
                    for (sh in shards) {
                        val alpha = (1f - sh.age / sh.duration).coerceIn(0f, 1f)
                        val sx = gridLeft + sh.x * tile
                        val sy = gridTop + sh.y * tile
                        val s = sh.size * tile
                        drawRect(
                            color = sh.color.copy(alpha = alpha),
                            topLeft = Offset(sx - s * 0.5f, sy - s * 0.5f),
                            size = Size(s, s)
                        )
                    }
                }

                // Dynamic Callout Pill ("CHECK!", "CASTLED!", "PROMOTION!")
                viewModel.calloutText?.let { callout ->
                    Row(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(CheerUpColors.darkBlue.copy(alpha = 0.95f))
                            .border(1.5.dp, viewModel.calloutColor.copy(alpha = 0.75f), CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheerUpSymbolIcon(
                            symbol = "crown.fill",
                            tint = viewModel.calloutColor,
                            size = 11.dp
                        )
                        Text(
                            text = callout,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = viewModel.calloutColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Status & New Match Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CheerUpColors.darkBlue.copy(alpha = 0.92f))
                        .border(1.dp, CheerUpColors.blueGrey.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.isThinking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = Color(0xFFF3DDB0),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(
                                    if (viewModel.turn == PieceColor.LIGHT) Color(0xFFF3DDB0) else Color(0xFF7A482F)
                                )
                                .border(1.5.dp, Color(0xFF24130B), CircleShape)
                        )
                    }

                    val turnText = when {
                        viewModel.isThinking -> LanguageManager.shared.tr("AI thinking...", "IA réfléchit...", "IA pensando...")
                        viewModel.lastMoveNotation.isNotEmpty() -> "${viewModel.fullMoveNumber}. ${viewModel.lastMoveNotation}"
                        viewModel.turn == PieceColor.LIGHT -> LanguageManager.shared.tr("Cream to move", "Trait aux Crèmes", "Mueven Crema")
                        else -> LanguageManager.shared.tr("Brown to move", "Trait aux Bruns", "Mueven Marrón")
                    }
                    Text(
                        text = turnText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CheerUpColors.lightGrey,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // New Game Button
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CheerUpColors.lightGrey)
                        .clickable {
                            HapticsManager.shared.selection()
                            selectedDifficulty = viewModel.difficulty
                            selectedPlayerColor = viewModel.humanColor
                            selectedTimeControl = viewModel.timeControl
                            viewModel.openModeSetup()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheerUpSymbolIcon(
                        symbol = "arrow.clockwise",
                        tint = CheerUpColors.deepNavy,
                        size = 13.dp
                    )
                    Text(
                        text = LanguageManager.shared.tr("New Game", "Nouvelle partie", "Nueva partida"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CheerUpColors.deepNavy
                    )
                }
            }
        }

        // Pre-Match Difficulty, Piece Color & Clock Selection Overlay
        if (viewModel.showModeSetup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.68f)),
                contentAlignment = Alignment.Center
            ) {
                val cardShape = RoundedCornerShape(Spacing.cardCornerRadius)
                Column(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenPadding)
                        .widthIn(max = 344.dp)
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(CheerUpColors.darkBlue)
                        .border(1.5.dp, CheerUpColors.blueGrey.copy(alpha = 0.6f), cardShape)
                        .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CHESS",
                            style = CheerUpFont.title,
                            color = CheerUpColors.lightGrey
                        )
                        Text(
                            text = LanguageManager.shared.tr("Choose AI Difficulty", "Choisissez le niveau de l'IA", "Elige la dificultad de la IA"),
                            style = CheerUpFont.callout,
                            color = CheerUpColors.mutedSilver
                        )
                    }

                    // Difficulty Cards
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChessDifficulty.entries.forEach { level ->
                            val isSelected = selectedDifficulty == level
                            val rowShape = RoundedCornerShape(14.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(rowShape)
                                    .background(if (isSelected) CheerUpColors.lightGrey else CheerUpColors.deepNavy.copy(alpha = 0.7f))
                                    .border(
                                        1.5.dp,
                                        if (isSelected) CheerUpColors.lightGrey else CheerUpColors.blueGrey.copy(alpha = 0.6f),
                                        rowShape
                                    )
                                    .clickable {
                                        HapticsManager.shared.selection()
                                        AudioManager.shared.play(AudioManager.SoundEffect.TAP)
                                        selectedDifficulty = level
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = when (level) {
                                            ChessDifficulty.BEGINNER -> LanguageManager.shared.tr("Easy", "Facile", "Fácil")
                                            ChessDifficulty.INTERMEDIATE -> LanguageManager.shared.tr("Medium", "Moyen", "Medio")
                                            ChessDifficulty.ADVANCED -> LanguageManager.shared.tr("Hard", "Difficile", "Difícil")
                                        },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) CheerUpColors.deepNavy else CheerUpColors.lightGrey
                                    )
                                    Text(
                                        text = when (level) {
                                            ChessDifficulty.BEGINNER -> LanguageManager.shared.tr("Relaxed pace for casual play", "Rythme détendu pour s'amuser", "Ritmo relajado para disfrutar")
                                            ChessDifficulty.INTERMEDIATE -> LanguageManager.shared.tr("Balanced tactics & solid defense", "Tactique équilibrée et défense solide", "Táctica equilibrada y defensa sólida")
                                            ChessDifficulty.ADVANCED -> LanguageManager.shared.tr("Deep search & sharp counterplay", "Calcul avancé et jeu incisif", "Cálculo profundo y juego preciso")
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) CheerUpColors.deepNavy.copy(alpha = 0.78f) else CheerUpColors.mutedSilver
                                    )
                                }
                                CheerUpSymbolIcon(
                                    symbol = if (isSelected) "checkmark.circle.fill" else "circle",
                                    tint = if (isSelected) CheerUpColors.deepNavy else CheerUpColors.mutedSilver.copy(alpha = 0.5f),
                                    size = 20.dp
                                )
                            }
                        }
                    }

                    // Piece Color Selector: Cream (.LIGHT) vs Brown (.DARK)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = LanguageManager.shared.tr("YOUR PIECES", "VOS PIÈCES", "TUS PIEZAS"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp,
                            color = CheerUpColors.mutedSilver
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(PieceColor.LIGHT, PieceColor.DARK).forEach { pieceColor ->
                                val isSelected = selectedPlayerColor == pieceColor
                                val isCream = pieceColor == PieceColor.LIGHT
                                val btnShape = RoundedCornerShape(12.dp)
                                val kingBmp = pieceBitmaps[PieceKind.KING to pieceColor]

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(btnShape)
                                        .background(if (isSelected) CheerUpColors.lightGrey else CheerUpColors.deepNavy.copy(alpha = 0.7f))
                                        .border(
                                            1.5.dp,
                                            if (isSelected) CheerUpColors.lightGrey else CheerUpColors.blueGrey.copy(alpha = 0.6f),
                                            btnShape
                                        )
                                        .clickable {
                                            HapticsManager.shared.selection()
                                            selectedPlayerColor = pieceColor
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (kingBmp != null) {
                                        Image(
                                            bitmap = kingBmp,
                                            contentDescription = null,
                                            modifier = Modifier.size(width = 20.dp, height = 30.dp),
                                            filterQuality = FilterQuality.None
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isCream) LanguageManager.shared.tr("Cream", "Crème", "Crema")
                                            else LanguageManager.shared.tr("Brown", "Brun", "Marrón"),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) CheerUpColors.deepNavy else CheerUpColors.lightGrey,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (isCream) LanguageManager.shared.tr("Moves 1st", "Joue 1er", "Sale 1.º")
                                            else LanguageManager.shared.tr("Moves 2nd", "Joue 2e", "Sale 2.º"),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) CheerUpColors.deepNavy.copy(alpha = 0.78f) else CheerUpColors.mutedSilver,
                                            maxLines = 1
                                        )
                                    }
                                    CheerUpSymbolIcon(
                                        symbol = if (isSelected) "checkmark.circle.fill" else "circle",
                                        tint = if (isSelected) CheerUpColors.deepNavy else CheerUpColors.mutedSilver.copy(alpha = 0.5f),
                                        size = 16.dp
                                    )
                                }
                            }
                        }
                    }

                    // Time Control Selector
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = LanguageManager.shared.tr("TIME CONTROL", "CADENCE", "RELOJ"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp,
                            color = CheerUpColors.mutedSilver
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ChessTimeControl.entries.forEach { control ->
                                val isSelected = selectedTimeControl == control
                                val tcShape = RoundedCornerShape(11.dp)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(tcShape)
                                        .background(if (isSelected) CheerUpColors.lightGrey else CheerUpColors.deepNavy.copy(alpha = 0.7f))
                                        .border(1.dp, CheerUpColors.blueGrey.copy(alpha = 0.6f), tcShape)
                                        .clickable {
                                            HapticsManager.shared.selection()
                                            selectedTimeControl = control
                                        }
                                        .padding(vertical = 9.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CheerUpSymbolIcon(
                                        symbol = if (control == ChessTimeControl.UNTIMED) "infinity" else "timer",
                                        tint = if (isSelected) CheerUpColors.deepNavy else CheerUpColors.lightGrey,
                                        size = 11.dp
                                    )
                                    Text(
                                        text = control.shortTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) CheerUpColors.deepNavy else CheerUpColors.lightGrey
                                    )
                                }
                            }
                        }
                    }

                    CheerUpPrimaryButton(
                        title = LanguageManager.shared.tr("Start Match", "Lancer la partie", "Empezar partida"),
                        icon = "play.fill",
                        action = {
                            viewModel.beginMatch(
                                difficulty = selectedDifficulty,
                                playerColor = selectedPlayerColor,
                                timeControl = selectedTimeControl
                            )
                        }
                    )
                }
            }
        }

        // Pawn Promotion Modal
        viewModel.pendingPromotion?.let { promo ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                val cardShape = RoundedCornerShape(Spacing.cardCornerRadius)
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .shadow(18.dp, cardShape)
                        .clip(cardShape)
                        .background(CheerUpColors.darkBlue)
                        .border(1.5.dp, Color(0xFFF3DDB0).copy(alpha = 0.75f), cardShape)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = LanguageManager.shared.tr("CHOOSE PROMOTION", "PROMOTION DU PION", "ELIGE PROMOCIÓN"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFFF3DDB0)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        promo.options.forEach { move ->
                            val kind = move.promotion ?: return@forEach
                            val bmp = pieceBitmaps[kind to promo.color]
                            val optShape = RoundedCornerShape(12.dp)
                            Column(
                                modifier = Modifier
                                    .clip(optShape)
                                    .background(Color(0xFF45291B))
                                    .border(1.5.dp, Color(0xFFF3DDB0), optShape)
                                    .clickable { viewModel.selectPromotion(move) }
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = kind.rawValue,
                                        modifier = Modifier.size(width = 44.dp, height = 54.dp),
                                        filterQuality = FilterQuality.None
                                    )
                                }
                                Text(
                                    text = when (kind) {
                                        PieceKind.QUEEN -> LanguageManager.shared.tr("Queen", "Dame", "Dama")
                                        PieceKind.ROOK -> LanguageManager.shared.tr("Rook", "Tour", "Torre")
                                        PieceKind.BISHOP -> LanguageManager.shared.tr("Bishop", "Fou", "Alfil")
                                        PieceKind.KNIGHT -> LanguageManager.shared.tr("Knight", "Cavalier", "Caballo")
                                        else -> ""
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF3DDB0)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pause Modal Overlay
        if (viewModel.isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                val cardShape = RoundedCornerShape(Spacing.cardCornerRadius)
                Column(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenPadding)
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(CheerUpColors.darkBlue)
                        .border(1.dp, CheerUpColors.blueGrey.copy(alpha = 0.5f), cardShape)
                        .padding(Spacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xl)
                ) {
                    Text(
                        text = LanguageManager.shared.tr("PAUSED", "PAUSE", "PAUSA"),
                        style = CheerUpFont.title,
                        color = CheerUpColors.lightGrey
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        CheerUpPrimaryButton(
                            title = LanguageManager.shared.tr("Resume", "Reprendre", "Continuar"),
                            icon = "play.fill",
                            action = {
                                HapticsManager.shared.selection()
                                viewModel.resumeGame()
                            }
                        )
                        CheerUpSecondaryButton(
                            title = LanguageManager.shared.tr("New Game", "Nouvelle partie", "Nueva partida"),
                            icon = "arrow.clockwise",
                            action = {
                                HapticsManager.shared.selection()
                                selectedDifficulty = viewModel.difficulty
                                selectedPlayerColor = viewModel.humanColor
                                selectedTimeControl = viewModel.timeControl
                                viewModel.openModeSetup()
                            }
                        )
                        CheerUpSecondaryButton(
                            title = LanguageManager.shared.tr("Arcade", "Arcade", "Arcade"),
                            icon = "square.grid.2x2.fill",
                            action = {
                                HapticsManager.shared.selection()
                                handleExit()
                            }
                        )
                    }
                }
            }
        }

        // End-of-Game Modal
        if (viewModel.status.isTerminal && !viewModel.isAnimating && !viewModel.showModeSetup) {
            val outcomeHeadline = when (val st = viewModel.status) {
                is ChessGameStatus.Checkmate -> if (st.winnerColor == viewModel.humanColor) {
                    LanguageManager.shared.tr("VICTORY!", "VICTOIRE !", "¡VICTORIA!")
                } else {
                    LanguageManager.shared.tr("CHECKMATE", "ÉCHEC ET MAT", "JAQUE MATE")
                }
                is ChessGameStatus.Timeout -> if (st.winnerColor == viewModel.humanColor) {
                    LanguageManager.shared.tr("TIME WIN!", "VICTOIRE AU TEMPS !", "¡VICTORIA POR TIEMPO!")
                } else {
                    LanguageManager.shared.tr("TIME OUT", "TEMPS ÉCOULÉ", "TIEMPO AGOTADO")
                }
                ChessGameStatus.Stalemate -> LanguageManager.shared.tr("STALEMATE", "PAT", "AHOGADO")
                ChessGameStatus.DrawFiftyMove,
                ChessGameStatus.DrawThreefoldRepetition,
                ChessGameStatus.DrawInsufficientMaterial -> LanguageManager.shared.tr("DRAW", "MATCH NUL", "TABLAS")
                else -> ""
            }

            val outcomeDetail = when (val st = viewModel.status) {
                is ChessGameStatus.Checkmate -> if (st.winnerColor == viewModel.humanColor) {
                    LanguageManager.shared.tr("You checkmated the AI!", "Vous avez maté l'IA !", "¡Diste jaque mate a la IA!")
                } else {
                    LanguageManager.shared.tr("The AI checkmated your King.", "L'IA a maté votre Roi.", "La IA dio jaque mate a tu Rey.")
                }
                is ChessGameStatus.Timeout -> if (st.winnerColor == viewModel.humanColor) {
                    LanguageManager.shared.tr("Opponent ran out of time.", "Le temps de l'adversaire est écoulé.", "El rival se quedó sin tiempo.")
                } else {
                    LanguageManager.shared.tr("Your clock reached 0:00.", "Votre horloge a atteint 0:00.", "Tu reloj llegó a 0:00.")
                }
                ChessGameStatus.Stalemate -> LanguageManager.shared.tr("No legal moves remaining.", "Aucun coup légal restant.", "Sin jugadas legales posibles.")
                ChessGameStatus.DrawFiftyMove -> LanguageManager.shared.tr("50 moves without capture or pawn push.", "50 coups sans prise ni avancée de pion.", "50 jugadas sin captura ni avance de peón.")
                ChessGameStatus.DrawThreefoldRepetition -> LanguageManager.shared.tr("Same position repeated three times.", "Position répétée trois fois.", "Misma posición repetida tres veces.")
                ChessGameStatus.DrawInsufficientMaterial -> LanguageManager.shared.tr("Neither side has enough material to mate.", "Matériel insuffisant pour mater.", "Material insuficiente para dar mate.")
                else -> ""
            }

            val outcomeColor = viewModel.status.winner?.let { w ->
                if (w == viewModel.humanColor) Color(0xFFF3DDB0) else CheerUpColors.Tap.red
            } ?: CheerUpColors.lightGrey

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                val cardShape = RoundedCornerShape(Spacing.cardCornerRadius)
                Column(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenPadding)
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(CheerUpColors.darkBlue)
                        .border(1.dp, CheerUpColors.blueGrey.copy(alpha = 0.5f), cardShape)
                        .padding(Spacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    Text(
                        text = "CHESS",
                        style = CheerUpFont.title2,
                        color = CheerUpColors.lightGrey
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = outcomeHeadline,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = outcomeColor,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = outcomeDetail,
                            style = CheerUpFont.callout,
                            color = CheerUpColors.mutedSilver,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CheerUpColors.deepNavy.copy(alpha = 0.65f))
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = LanguageManager.shared.tr("BEST", "RECORD", "RÉCORD"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CheerUpColors.mutedSilver
                            )
                            Text(
                                text = "${viewModel.gamesWon}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = if (viewModel.status.winner == viewModel.humanColor) Color(0xFFF3DDB0) else CheerUpColors.lightGrey
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CheerUpColors.deepNavy.copy(alpha = 0.65f))
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = LanguageManager.shared.tr("MOVES", "COUPS", "JUGADAS"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CheerUpColors.mutedSilver
                            )
                            Text(
                                text = "${viewModel.fullMoveNumber}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = CheerUpColors.lightGrey
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        CheerUpPrimaryButton(
                            title = LanguageManager.shared.tr("Play Again", "Rejouer", "Jugar otra vez"),
                            icon = "arrow.clockwise",
                            action = {
                                HapticsManager.shared.selection()
                                selectedDifficulty = viewModel.difficulty
                                selectedPlayerColor = viewModel.humanColor
                                selectedTimeControl = viewModel.timeControl
                                viewModel.openModeSetup()
                            }
                        )
                        CheerUpSecondaryButton(
                            title = LanguageManager.shared.tr("Arcade", "Arcade", "Arcade"),
                            icon = "square.grid.2x2.fill",
                            action = {
                                HapticsManager.shared.selection()
                                handleExit()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChessClockPill(
    time: String,
    active: Boolean,
    isDarkPiece: Boolean
) {
    val shape = RoundedCornerShape(7.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (active) Color(0xFFF3DDB0) else CheerUpColors.darkBlue.copy(alpha = 0.92f))
            .border(
                1.dp,
                if (active) Color(0xFFFBF0D9) else CheerUpColors.blueGrey.copy(alpha = 0.6f),
                shape
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isDarkPiece) Color(0xFF7A482F) else Color(0xFFF3DDB0))
                .border(1.dp, Color(0xFF24130B), CircleShape)
        )
        Text(
            text = time,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = if (active) CheerUpColors.deepNavy else CheerUpColors.lightGrey
        )
    }
}
