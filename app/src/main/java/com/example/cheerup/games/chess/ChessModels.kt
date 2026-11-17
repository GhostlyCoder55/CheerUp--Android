package com.example.cheerup.games.chess

import com.example.cheerup.core.localization.LanguageManager
import java.util.UUID

enum class PieceKind(val rawValue: String) {
    PAWN("pawn"),
    KNIGHT("knight"),
    BISHOP("bishop"),
    ROOK("rook"),
    QUEEN("queen"),
    KING("king");

    val impact: Float
        get() = when (this) {
            PAWN -> 0.65f
            KNIGHT -> 0.90f
            BISHOP -> 0.90f
            ROOK -> 1.15f
            QUEEN -> 1.45f
            KING -> 1.60f
        }

    val isSlider: Boolean
        get() = this == BISHOP || this == ROOK || this == QUEEN

    val value: Int
        get() = when (this) {
            PAWN -> 1
            KNIGHT -> 3
            BISHOP -> 3
            ROOK -> 5
            QUEEN -> 9
            KING -> 0
        }

    val fenSymbol: Char
        get() = when (this) {
            PAWN -> 'P'
            KNIGHT -> 'N'
            BISHOP -> 'B'
            ROOK -> 'R'
            QUEEN -> 'Q'
            KING -> 'K'
        }

    companion object {
        val promotableKinds: List<PieceKind> = listOf(QUEEN, ROOK, BISHOP, KNIGHT)
    }
}

enum class PieceColor(val rawValue: String) {
    LIGHT("light"),
    DARK("dark");

    val opponent: PieceColor
        get() = if (this == LIGHT) DARK else LIGHT
}

data class ChessSquare(
    val file: Int,
    val rank: Int
) {
    val isValid: Boolean
        get() = file in 0..7 && rank in 0..7

    /** Standard chess square coloring (a1 is dark, h1 is light, a8 is light). */
    val isLightSquare: Boolean
        get() = (file + rank) % 2 == 1

    val algebraic: String
        get() {
            if (!isValid) return "??"
            val fileChar = ('a'.code + file).toChar()
            return "$fileChar${rank + 1}"
        }

    override fun toString(): String = algebraic

    fun offset(files: Int, ranks: Int): ChessSquare? {
        val sq = ChessSquare(file + files, rank + ranks)
        return if (sq.isValid) sq else null
    }

    companion object {
        fun fromAlgebraic(algebraic: String): ChessSquare? {
            val lower = algebraic.lowercase()
            if (lower.length != 2) return null
            val f = lower[0].code
            val r = lower[1].code
            if (f !in 97..104 || r !in 49..56) return null
            return ChessSquare(file = f - 97, rank = r - 49)
        }
    }
}

data class ChessPiece(
    val id: UUID = UUID.randomUUID(),
    val kind: PieceKind,
    val color: PieceColor,
    val hasMoved: Boolean = false
) {
    val fenCharacter: Char
        get() {
            val ch = kind.fenSymbol
            return if (color == PieceColor.LIGHT) ch else ch.lowercaseChar()
        }
}

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
) {
    val fenString: String
        get() {
            val out = StringBuilder()
            if (whiteKingside) out.append('K')
            if (whiteQueenside) out.append('Q')
            if (blackKingside) out.append('k')
            if (blackQueenside) out.append('q')
            return if (out.isEmpty()) "-" else out.toString()
        }

    companion object {
        val all = CastlingRights()
        val none = CastlingRights(
            whiteKingside = false,
            whiteQueenside = false,
            blackKingside = false,
            blackQueenside = false
        )
    }
}

data class ChessMove(
    val from: ChessSquare,
    val to: ChessSquare,
    val movedPiece: ChessPiece,
    val capturedPiece: ChessPiece?,
    val capturedSquare: ChessSquare?,
    val promotion: PieceKind?,
    val isCastlingKingside: Boolean,
    val isCastlingQueenside: Boolean,
    val isEnPassant: Boolean
) {
    val isCastling: Boolean get() = isCastlingKingside || isCastlingQueenside
    val isCapture: Boolean get() = capturedPiece != null

    val notation: String
        get() {
            if (isCastlingKingside) return "O-O"
            if (isCastlingQueenside) return "O-O-O"
            val sb = StringBuilder()
            if (movedPiece.kind != PieceKind.PAWN) {
                sb.append(movedPiece.kind.fenSymbol)
            } else if (isCapture) {
                sb.append(from.algebraic.first())
            }
            if (isCapture) sb.append('x')
            sb.append(to.algebraic)
            if (promotion != null) {
                sb.append("=${promotion.fenSymbol}")
            }
            return sb.toString()
        }
}

sealed class ChessGameStatus {
    data object Playing : ChessGameStatus()
    data class Check(val color: PieceColor) : ChessGameStatus()
    data class Checkmate(val winnerColor: PieceColor) : ChessGameStatus()
    data object Stalemate : ChessGameStatus()
    data object DrawFiftyMove : ChessGameStatus()
    data object DrawThreefoldRepetition : ChessGameStatus()
    data object DrawInsufficientMaterial : ChessGameStatus()
    data class Timeout(val winnerColor: PieceColor) : ChessGameStatus()

    val isTerminal: Boolean
        get() = when (this) {
            Playing, is Check -> false
            is Checkmate, Stalemate, DrawFiftyMove, DrawThreefoldRepetition, DrawInsufficientMaterial, is Timeout -> true
        }

    val isDraw: Boolean
        get() = when (this) {
            Stalemate, DrawFiftyMove, DrawThreefoldRepetition, DrawInsufficientMaterial -> true
            else -> false
        }

    val winner: PieceColor?
        get() = when (this) {
            is Checkmate -> winnerColor
            is Timeout -> winnerColor
            else -> null
        }

    val inCheckColor: PieceColor?
        get() = (this as? Check)?.color
}

enum class ChessDifficulty(val rawValue: String) {
    BEGINNER("beginner"),
    INTERMEDIATE("intermediate"),
    ADVANCED("advanced");

    val id: String get() = rawValue

    val localizedTitle: String
        get() = when (this) {
            BEGINNER -> LanguageManager.shared.tr("Beginner", "Débutant", "Principiante")
            INTERMEDIATE -> LanguageManager.shared.tr("Intermediate", "Intermédiaire", "Intermedio")
            ADVANCED -> LanguageManager.shared.tr("Advanced", "Avancé", "Avanzado")
        }

    val shortLabel: String
        get() = when (this) {
            BEGINNER -> LanguageManager.shared.tr("EASY", "FACILE", "FÁCIL")
            INTERMEDIATE -> LanguageManager.shared.tr("MED", "MOYEN", "MEDIO")
            ADVANCED -> LanguageManager.shared.tr("HARD", "FORT", "DIFÍCIL")
        }
}

enum class ChessTimeControl(val rawValue: String) {
    UNTIMED("untimed"),
    THREE_MINUTES("threeMinutes"),
    FIVE_MINUTES("fiveMinutes");

    val id: String get() = rawValue

    val initialSeconds: Double?
        get() = when (this) {
            UNTIMED -> null
            THREE_MINUTES -> 180.0
            FIVE_MINUTES -> 300.0
        }

    val shortTitle: String
        get() = when (this) {
            UNTIMED -> LanguageManager.shared.tr("Untimed", "Libre", "Sin reloj")
            THREE_MINUTES -> "3 Min"
            FIVE_MINUTES -> "5 Min"
        }
}

data class PendingPromotion(
    val id: UUID = UUID.randomUUID(),
    val from: ChessSquare,
    val to: ChessSquare,
    val color: PieceColor,
    val options: List<ChessMove>
)
