package com.example.cheerup.games.chess

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ChessBoardState {
    var squares: Array<ChessPiece?> = arrayOfNulls(64)
        private set
    var turn: PieceColor = PieceColor.LIGHT
        private set
    var castlingRights: CastlingRights = CastlingRights.all
        private set
    var enPassantTarget: ChessSquare? = null
        private set
    var halfMoveClock: Int = 0
        private set
    var fullMoveNumber: Int = 1
        private set
    var capturedByLight: MutableList<PieceKind> = mutableListOf()
        private set
    var capturedByDark: MutableList<PieceKind> = mutableListOf()
        private set
    var lastMove: ChessMove? = null
        private set
    var status: ChessGameStatus = ChessGameStatus.Playing
        private set
    var positionCounts: MutableMap<String, Int> = mutableMapOf()
        private set

    constructor() {
        setupStandardBoard()
        val key = repetitionKey()
        positionCounts[key] = 1
        refreshStatus()
    }

    private constructor(other: ChessBoardState, lightCopy: Boolean) {
        this.squares = other.squares.copyOf()
        this.turn = other.turn
        this.castlingRights = other.castlingRights
        this.enPassantTarget = other.enPassantTarget
        this.halfMoveClock = other.halfMoveClock
        this.fullMoveNumber = other.fullMoveNumber
        this.lastMove = other.lastMove
        this.status = other.status
        if (lightCopy) {
            this.capturedByLight = mutableListOf()
            this.capturedByDark = mutableListOf()
            this.positionCounts = mutableMapOf()
        } else {
            this.capturedByLight = other.capturedByLight.toMutableList()
            this.capturedByDark = other.capturedByDark.toMutableList()
            this.positionCounts = other.positionCounts.toMutableMap()
        }
    }

    fun copy(): ChessBoardState = ChessBoardState(this, lightCopy = false)

    internal fun copyForSearch(): ChessBoardState = ChessBoardState(this, lightCopy = true)

    private fun setupStandardBoard() {
        squares = arrayOfNulls(64)
        val backRank = listOf(
            PieceKind.ROOK, PieceKind.KNIGHT, PieceKind.BISHOP, PieceKind.QUEEN,
            PieceKind.KING, PieceKind.BISHOP, PieceKind.KNIGHT, PieceKind.ROOK
        )
        for (file in 0 until 8) {
            setPiece(ChessPiece(kind = backRank[file], color = PieceColor.LIGHT), ChessSquare(file, 0))
            setPiece(ChessPiece(kind = PieceKind.PAWN, color = PieceColor.LIGHT), ChessSquare(file, 1))
            setPiece(ChessPiece(kind = PieceKind.PAWN, color = PieceColor.DARK), ChessSquare(file, 6))
            setPiece(ChessPiece(kind = backRank[file], color = PieceColor.DARK), ChessSquare(file, 7))
        }
    }

    fun piece(at: ChessSquare): ChessPiece? {
        if (!at.isValid) return null
        return squares[at.rank * 8 + at.file]
    }

    private fun setPiece(piece: ChessPiece?, at: ChessSquare) {
        if (!at.isValid) return
        squares[at.rank * 8 + at.file] = piece
    }

    fun kingSquare(forColor: PieceColor): ChessSquare? {
        for (idx in 0 until 64) {
            val p = squares[idx]
            if (p != null && p.kind == PieceKind.KING && p.color == forColor) {
                return ChessSquare(idx % 8, idx / 8)
            }
        }
        return null
    }

    fun isKingInCheck(forColor: PieceColor): Boolean {
        val kingSq = kingSquare(forColor) ?: return false
        return isSquareAttacked(kingSq, by = forColor.opponent)
    }

    fun isSquareAttacked(square: ChessSquare, by: PieceColor): Boolean {
        // 1. Pawn attacks
        val pawnRank = square.rank + if (by == PieceColor.LIGHT) -1 else 1
        if (pawnRank in 0..7) {
            for (df in intArrayOf(-1, 1)) {
                val pf = square.file + df
                if (pf in 0..7) {
                    val p = piece(ChessSquare(pf, pawnRank))
                    if (p != null && p.color == by && p.kind == PieceKind.PAWN) return true
                }
            }
        }

        // 2. Knight attacks
        val knightDeltas = arrayOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        for ((df, dr) in knightDeltas) {
            val sq = square.offset(df, dr) ?: continue
            val p = piece(sq)
            if (p != null && p.color == by && p.kind == PieceKind.KNIGHT) return true
        }

        // 3. King attacks
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val sq = square.offset(df, dr) ?: continue
                val p = piece(sq)
                if (p != null && p.color == by && p.kind == PieceKind.KING) return true
            }
        }

        // 4. Orthogonal rays (Rook / Queen)
        val orthoDirs = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        for ((df, dr) in orthoDirs) {
            var cur = square.offset(df, dr)
            while (cur != null) {
                val p = piece(cur)
                if (p != null) {
                    if (p.color == by && (p.kind == PieceKind.ROOK || p.kind == PieceKind.QUEEN)) return true
                    break
                }
                cur = cur.offset(df, dr)
            }
        }

        // 5. Diagonal rays (Bishop / Queen)
        val diagDirs = arrayOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
        for ((df, dr) in diagDirs) {
            var cur = square.offset(df, dr)
            while (cur != null) {
                val p = piece(cur)
                if (p != null) {
                    if (p.color == by && (p.kind == PieceKind.BISHOP || p.kind == PieceKind.QUEEN)) return true
                    break
                }
                cur = cur.offset(df, dr)
            }
        }

        return false
    }

    fun legalMoves(forColor: PieceColor? = null): List<ChessMove> {
        val mover = forColor ?: turn
        if (status.isTerminal) return emptyList()
        val pseudo = ArrayList<ChessMove>(40)

        for (idx in 0 until 64) {
            val p = squares[idx] ?: continue
            if (p.color != mover) continue
            val sq = ChessSquare(idx % 8, idx / 8)
            appendPseudoLegalMoves(sq, p, pseudo)
        }

        return pseudo.filter { move ->
            val copy = this.copyForSearch()
            copy.applyUnchecked(move)
            !copy.isKingInCheck(mover)
        }
    }

    fun legalMovesFrom(square: ChessSquare): List<ChessMove> {
        if (status.isTerminal) return emptyList()
        val p = piece(square) ?: return emptyList()
        if (p.color != turn) return emptyList()
        val pseudo = ArrayList<ChessMove>(16)
        appendPseudoLegalMoves(square, p, pseudo)
        return pseudo.filter { move ->
            val copy = this.copyForSearch()
            copy.applyUnchecked(move)
            !copy.isKingInCheck(turn)
        }
    }

    private fun appendPseudoLegalMoves(sq: ChessSquare, piece: ChessPiece, out: MutableList<ChessMove>) {
        when (piece.kind) {
            PieceKind.PAWN -> {
                val dir = if (piece.color == PieceColor.LIGHT) 1 else -1
                val startRank = if (piece.color == PieceColor.LIGHT) 1 else 6
                val promoRank = if (piece.color == PieceColor.LIGHT) 7 else 0

                // Single push
                val oneAhead = sq.offset(0, dir)
                if (oneAhead != null && this.piece(oneAhead) == null) {
                    if (oneAhead.rank == promoRank) {
                        for (kind in PieceKind.promotableKinds) {
                            out.add(
                                ChessMove(
                                    from = sq, to = oneAhead, movedPiece = piece,
                                    capturedPiece = null, capturedSquare = null, promotion = kind,
                                    isCastlingKingside = false, isCastlingQueenside = false, isEnPassant = false
                                )
                            )
                        }
                    } else {
                        out.add(
                            ChessMove(
                                from = sq, to = oneAhead, movedPiece = piece,
                                capturedPiece = null, capturedSquare = null, promotion = null,
                                isCastlingKingside = false, isCastlingQueenside = false, isEnPassant = false
                            )
                        )
                        if (sq.rank == startRank) {
                            val twoAhead = sq.offset(0, dir * 2)
                            if (twoAhead != null && this.piece(twoAhead) == null) {
                                out.add(
                                    ChessMove(
                                        from = sq, to = twoAhead, movedPiece = piece,
                                        capturedPiece = null, capturedSquare = null, promotion = null,
                                        isCastlingKingside = false, isCastlingQueenside = false, isEnPassant = false
                                    )
                                )
                            }
                        }
                    }
                }

                // Diagonal captures & en passant
                for (df in intArrayOf(-1, 1)) {
                    val targetSq = sq.offset(df, dir) ?: continue
                    val victim = this.piece(targetSq)
                    if (victim != null && victim.color != piece.color) {
                        if (targetSq.rank == promoRank) {
                            for (kind in PieceKind.promotableKinds) {
                                out.add(
                                    ChessMove(
                                        from = sq, to = targetSq, movedPiece = piece,
                                        capturedPiece = victim, capturedSquare = targetSq, promotion = kind,
                                        isCastlingKingside = false, isCastlingQueenside = false, isEnPassant = false
                                    )
                                )
                            }
                        } else {
                            out.add(
                                ChessMove(
                                    from = sq, to = targetSq, movedPiece = piece,
                                    capturedPiece = victim, capturedSquare = targetSq, promotion = null,
                                    isCastlingKingside = false, isCastlingQueenside = false, isEnPassant = false
                                )
                            )
                        }
                    } else if (enPassantTarget != null && enPassantTarget == targetSq) {
                        val capturedSq = ChessSquare(targetSq.file, sq.rank)
                        val epVictim = this.piece(capturedSq)
                        if (epVictim != null && epVictim.color != piece.color && epVictim.kind == PieceKind.PAWN) {
                            out.add(
                                ChessMove(
                                    from = sq, to = targetSq, movedPiece = piece,
                                    capturedPiece = epVictim, capturedSquare = capturedSq, promotion = null,
                                    isCastlingKingside = false, isCastlingQueenside = false, isEnPassant = true
                                )
                            )
                        }
                    }
                }
            }

            PieceKind.KNIGHT -> {
                val deltas = arrayOf(
                    -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1
                )
                for ((df, dr) in deltas) {
                    val dst = sq.offset(df, dr) ?: continue
                    val occupant = this.piece(dst)
                    if (occupant != null) {
                        if (occupant.color != piece.color) {
                            out.add(
                                ChessMove(
                                    from = sq, to = dst, movedPiece = piece,
                                    capturedPiece = occupant, capturedSquare = dst, promotion = null,
                                    isCastlingKingside = false, isCastlingQueenside = false, isEnPassant = false
                                )
                            )
                        }
                    } else {
                        out.add(
                            ChessMove(
                                from = sq, to = dst, movedPiece = piece,
                                capturedPiece = null, capturedSquare = null, promotion = null,
                                isCastlingKingside = false, isCastlingQueenside = false, isEnPassant = false
                            )
                        )
                    }
                }
            }

            PieceKind.BISHOP -> {
                appendSlidingMoves(sq, piece, arrayOf(1 to 1, 1 to -1, -1 to 1, -1 to -1), out)
            }

            PieceKind.ROOK -> {
                appendSlidingMoves(sq, piece, arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1), out)
            }

            PieceKind.QUEEN -> {
                appendSlidingMoves(
                    sq, piece,
                    arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1),
                    out
                )
            }

            PieceKind.KING -> {
                for (df in -1..1) {
                    for (dr in -1..1) {
                        if (df == 0 && dr == 0) continue
                        val dst = sq.offset(df, dr) ?: continue
                        val occupant = this.piece(dst)
                        if (occupant != null) {
                            if (occupant.color != piece.color) {
                                out.add(
                                    ChessMove(
                                        from = sq, to = dst, movedPiece = piece,
                                        capturedPiece = occupant, capturedSquare = dst, promotion = null,
                                        isCastlingKingside = false, isCastlingQueenside = false, isEnPassant = false
                                    )
                                )
                            }
                        } else {
                            out.add(
                                ChessMove(
                                    from = sq, to = dst, movedPiece = piece,
                                    capturedPiece = null, capturedSquare = null, promotion = null,
                                    isCastlingKingside = false, isCastlingQueenside = false, isEnPassant = false
                                )
                            )
                        }
                    }
                }
                appendCastlingMoves(sq, piece, out)
            }
        }
    }

    private fun appendSlidingMoves(
        sq: ChessSquare,
        piece: ChessPiece,
        directions: Array<Pair<Int, Int>>,
        out: MutableList<ChessMove>
    ) {
        for ((df, dr) in directions) {
            var cur = sq.offset(df, dr)
            while (cur != null) {
                val occupant = this.piece(cur)
                if (occupant != null) {
                    if (occupant.color != piece.color) {
                        out.add(
                            ChessMove(
                                from = sq, to = cur, movedPiece = piece,
                                capturedPiece = occupant, capturedSquare = cur, promotion = null,
                                isCastlingKingside = false, isCastlingQueenside = false, isEnPassant = false
                            )
                        )
                    }
                    break
                } else {
                    out.add(
                        ChessMove(
                            from = sq, to = cur, movedPiece = piece,
                            capturedPiece = null, capturedSquare = null, promotion = null,
                            isCastlingKingside = false, isCastlingQueenside = false, isEnPassant = false
                        )
                    )
                }
                cur = cur.offset(df, dr)
            }
        }
    }

    private fun appendCastlingMoves(sq: ChessSquare, king: ChessPiece, out: MutableList<ChessMove>) {
        val homeRank = if (king.color == PieceColor.LIGHT) 0 else 7
        if (sq.file != 4 || sq.rank != homeRank || king.hasMoved) return
        val enemy = king.color.opponent
        if (isSquareAttacked(sq, by = enemy)) return

        val canKingside = if (king.color == PieceColor.LIGHT) castlingRights.whiteKingside else castlingRights.blackKingside
        if (canKingside) {
            val fSq = ChessSquare(5, homeRank)
            val gSq = ChessSquare(6, homeRank)
            val rookSq = ChessSquare(7, homeRank)
            val rook = this.piece(rookSq)
            if (this.piece(fSq) == null &&
                this.piece(gSq) == null &&
                rook != null && rook.kind == PieceKind.ROOK && rook.color == king.color && !rook.hasMoved &&
                !isSquareAttacked(fSq, by = enemy) &&
                !isSquareAttacked(gSq, by = enemy)
            ) {
                out.add(
                    ChessMove(
                        from = sq, to = gSq, movedPiece = king,
                        capturedPiece = null, capturedSquare = null, promotion = null,
                        isCastlingKingside = true, isCastlingQueenside = false, isEnPassant = false
                    )
                )
            }
        }

        val canQueenside = if (king.color == PieceColor.LIGHT) castlingRights.whiteQueenside else castlingRights.blackQueenside
        if (canQueenside) {
            val dSq = ChessSquare(3, homeRank)
            val cSq = ChessSquare(2, homeRank)
            val bSq = ChessSquare(1, homeRank)
            val rookSq = ChessSquare(0, homeRank)
            val rook = this.piece(rookSq)
            if (this.piece(dSq) == null &&
                this.piece(cSq) == null &&
                this.piece(bSq) == null &&
                rook != null && rook.kind == PieceKind.ROOK && rook.color == king.color && !rook.hasMoved &&
                !isSquareAttacked(dSq, by = enemy) &&
                !isSquareAttacked(cSq, by = enemy)
            ) {
                out.add(
                    ChessMove(
                        from = sq, to = cSq, movedPiece = king,
                        capturedPiece = null, capturedSquare = null, promotion = null,
                        isCastlingKingside = false, isCastlingQueenside = true, isEnPassant = false
                    )
                )
            }
        }
    }

    fun makeMove(move: ChessMove): Boolean {
        val validMoves = legalMovesFrom(move.from)
        val legal = validMoves.firstOrNull { it == move } ?: return false
        applyMoveAndUpdateState(legal)
        return true
    }

    internal fun applyUnchecked(move: ChessMove) {
        setPiece(null, move.from)

        if (move.isEnPassant && move.capturedSquare != null) {
            setPiece(null, move.capturedSquare)
        }

        var updatedPiece = move.movedPiece.copy(hasMoved = true)
        if (move.promotion != null) {
            updatedPiece = updatedPiece.copy(kind = move.promotion)
        }
        setPiece(updatedPiece, move.to)

        if (move.isCastlingKingside) {
            val rank = move.from.rank
            val rook = piece(ChessSquare(7, rank))
            if (rook != null) {
                setPiece(null, ChessSquare(7, rank))
                setPiece(rook.copy(hasMoved = true), ChessSquare(5, rank))
            }
        } else if (move.isCastlingQueenside) {
            val rank = move.from.rank
            val rook = piece(ChessSquare(0, rank))
            if (rook != null) {
                setPiece(null, ChessSquare(0, rank))
                setPiece(rook.copy(hasMoved = true), ChessSquare(3, rank))
            }
        }

        revokeCastlingRights(move)

        enPassantTarget = if (move.movedPiece.kind == PieceKind.PAWN && abs(move.to.rank - move.from.rank) == 2) {
            val skippedRank = (move.from.rank + move.to.rank) / 2
            ChessSquare(move.from.file, skippedRank)
        } else {
            null
        }

        turn = turn.opponent
    }

    private fun applyMoveAndUpdateState(move: ChessMove) {
        applyUnchecked(move)

        if (move.capturedPiece != null) {
            if (move.movedPiece.color == PieceColor.LIGHT) {
                capturedByLight.add(move.capturedPiece.kind)
            } else {
                capturedByDark.add(move.capturedPiece.kind)
            }
        }

        if (move.movedPiece.kind == PieceKind.PAWN || move.isCapture) {
            halfMoveClock = 0
        } else {
            halfMoveClock += 1
        }

        if (move.movedPiece.color == PieceColor.DARK) {
            fullMoveNumber += 1
        }

        lastMove = move
        val key = repetitionKey()
        positionCounts[key] = (positionCounts[key] ?: 0) + 1
        refreshStatus()
    }

    private fun revokeCastlingRights(move: ChessMove) {
        var wk = castlingRights.whiteKingside
        var wq = castlingRights.whiteQueenside
        var bk = castlingRights.blackKingside
        var bq = castlingRights.blackQueenside

        if (move.movedPiece.kind == PieceKind.KING) {
            if (move.movedPiece.color == PieceColor.LIGHT) {
                wk = false
                wq = false
            } else {
                bk = false
                bq = false
            }
        }
        fun checkCorner(sq: ChessSquare) {
            if (sq.file == 0 && sq.rank == 0) wq = false
            if (sq.file == 7 && sq.rank == 0) wk = false
            if (sq.file == 0 && sq.rank == 7) bq = false
            if (sq.file == 7 && sq.rank == 7) bk = false
        }
        checkCorner(move.from)
        checkCorner(move.to)
        castlingRights = CastlingRights(wk, wq, bk, bq)
    }

    fun forceTimeout(winner: PieceColor) {
        status = ChessGameStatus.Timeout(winner)
    }

    private fun refreshStatus() {
        status = ChessGameStatus.Playing
        val nextMoves = legalMoves(turn)
        val inCheck = isKingInCheck(turn)

        if (nextMoves.isEmpty()) {
            status = if (inCheck) ChessGameStatus.Checkmate(turn.opponent) else ChessGameStatus.Stalemate
            return
        }

        if (isInsufficientMaterial()) {
            status = ChessGameStatus.DrawInsufficientMaterial
            return
        }

        val key = repetitionKey()
        if ((positionCounts[key] ?: 0) >= 3) {
            status = ChessGameStatus.DrawThreefoldRepetition
            return
        }

        if (halfMoveClock >= 100) {
            status = ChessGameStatus.DrawFiftyMove
            return
        }

        status = if (inCheck) ChessGameStatus.Check(turn) else ChessGameStatus.Playing
    }

    fun isInsufficientMaterial(): Boolean {
        val lightMinors = mutableListOf<Pair<PieceKind, ChessSquare>>()
        val darkMinors = mutableListOf<Pair<PieceKind, ChessSquare>>()

        for (idx in 0 until 64) {
            val p = squares[idx] ?: continue
            when (p.kind) {
                PieceKind.PAWN, PieceKind.ROOK, PieceKind.QUEEN -> return false
                PieceKind.KING -> continue
                PieceKind.BISHOP, PieceKind.KNIGHT -> {
                    val sq = ChessSquare(idx % 8, idx / 8)
                    if (p.color == PieceColor.LIGHT) {
                        lightMinors.add(p.kind to sq)
                    } else {
                        darkMinors.add(p.kind to sq)
                    }
                }
            }
        }

        val totalMinors = lightMinors.size + darkMinors.size
        if (totalMinors <= 1) return true

        if (lightMinors.size == 1 && darkMinors.size == 1 &&
            lightMinors[0].first == PieceKind.BISHOP && darkMinors[0].first == PieceKind.BISHOP
        ) {
            return lightMinors[0].second.isLightSquare == darkMinors[0].second.isLightSquare
        }

        return false
    }

    fun repetitionKey(): String {
        val sb = StringBuilder()
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0 until 8) {
                val p = squares[rank * 8 + file]
                if (p != null) {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(p.fenCharacter)
                } else {
                    empty += 1
                }
            }
            if (empty > 0) sb.append(empty)
            if (rank > 0) sb.append('/')
        }
        sb.append(" ${if (turn == PieceColor.LIGHT) "w" else "b"} ${castlingRights.fenString}")

        val ep = enPassantTarget
        if (ep != null && hasLegalEnPassantCapture(ep)) {
            sb.append(" ${ep.algebraic}")
        } else {
            sb.append(" -")
        }
        return sb.toString()
    }

    private fun hasLegalEnPassantCapture(ep: ChessSquare): Boolean {
        val pawnRank = if (turn == PieceColor.LIGHT) ep.rank - 1 else ep.rank + 1
        if (pawnRank !in 0..7) return false
        for (df in intArrayOf(-1, 1)) {
            val pf = ep.file + df
            if (pf !in 0..7) continue
            val fromSq = ChessSquare(pf, pawnRank)
            val p = piece(fromSq)
            if (p != null && p.color == turn && p.kind == PieceKind.PAWN) {
                val capturedSq = ChessSquare(ep.file, pawnRank)
                val victim = piece(capturedSq)
                val move = ChessMove(
                    from = fromSq, to = ep, movedPiece = p,
                    capturedPiece = victim, capturedSquare = capturedSq, promotion = null,
                    isCastlingKingside = false, isCastlingQueenside = false, isEnPassant = true
                )
                val copy = this.copyForSearch()
                copy.applyUnchecked(move)
                if (!copy.isKingInCheck(turn)) return true
            }
        }
        return false
    }

    companion object {
        fun fromFen(fen: String): ChessBoardState? {
            val parts = fen.trim().split(Regex("\\s+"))
            if (parts.size < 4) return null

            val board = arrayOfNulls<ChessPiece>(64)
            val ranks = parts[0].split("/")
            if (ranks.size != 8) return null

            for ((fenRowIdx, rankStr) in ranks.withIndex()) {
                val rank = 7 - fenRowIdx
                var file = 0
                for (ch in rankStr) {
                    val digit = ch.digitToIntOrNull()
                    if (digit != null) {
                        file += digit
                    } else {
                        if (file >= 8) return null
                        val color = if (ch.isUpperCase()) PieceColor.LIGHT else PieceColor.DARK
                        val kind = when (ch.lowercaseChar()) {
                            'p' -> PieceKind.PAWN
                            'n' -> PieceKind.KNIGHT
                            'b' -> PieceKind.BISHOP
                            'r' -> PieceKind.ROOK
                            'q' -> PieceKind.QUEEN
                            'k' -> PieceKind.KING
                            else -> return null
                        }
                        val hasMoved = !(kind == PieceKind.PAWN && ((color == PieceColor.LIGHT && rank == 1) || (color == PieceColor.DARK && rank == 6)))
                        board[rank * 8 + file] = ChessPiece(kind = kind, color = color, hasMoved = hasMoved)
                        file += 1
                    }
                }
                if (file != 8) return null
            }

            val sideToMove = if (parts[1] == "b") PieceColor.DARK else PieceColor.LIGHT
            val castleStr = parts[2]
            val rights = CastlingRights(
                whiteKingside = castleStr.contains('K'),
                whiteQueenside = castleStr.contains('Q'),
                blackKingside = castleStr.contains('k'),
                blackQueenside = castleStr.contains('q')
            )

            fun clearHasMovedIfAt(sq: ChessSquare, kind: PieceKind, color: PieceColor) {
                val idx = sq.rank * 8 + sq.file
                val p = board[idx]
                if (p != null && p.kind == kind && p.color == color) {
                    board[idx] = p.copy(hasMoved = false)
                }
            }
            if (rights.whiteKingside || rights.whiteQueenside) clearHasMovedIfAt(ChessSquare(4, 0), PieceKind.KING, PieceColor.LIGHT)
            if (rights.whiteKingside) clearHasMovedIfAt(ChessSquare(7, 0), PieceKind.ROOK, PieceColor.LIGHT)
            if (rights.whiteQueenside) clearHasMovedIfAt(ChessSquare(0, 0), PieceKind.ROOK, PieceColor.LIGHT)
            if (rights.blackKingside || rights.blackQueenside) clearHasMovedIfAt(ChessSquare(4, 7), PieceKind.KING, PieceColor.DARK)
            if (rights.blackKingside) clearHasMovedIfAt(ChessSquare(7, 7), PieceKind.ROOK, PieceColor.DARK)
            if (rights.blackQueenside) clearHasMovedIfAt(ChessSquare(0, 7), PieceKind.ROOK, PieceColor.DARK)

            val epSquare = if (parts[3] == "-") null else ChessSquare.fromAlgebraic(parts[3])
            val half = if (parts.size > 4) (parts[4].toIntOrNull() ?: 0) else 0
            val full = if (parts.size > 5) max(1, parts[5].toIntOrNull() ?: 1) else 1

            val state = ChessBoardState()
            state.squares = board
            state.turn = sideToMove
            state.castlingRights = rights
            state.enPassantTarget = epSquare
            state.halfMoveClock = half
            state.fullMoveNumber = full
            state.capturedByLight.clear()
            state.capturedByDark.clear()
            state.lastMove = null
            state.status = ChessGameStatus.Playing
            state.positionCounts.clear()
            val key = state.repetitionKey()
            state.positionCounts[key] = 1
            state.refreshStatus()
            return state
        }
    }
}

data class ChessSnapshot(
    val boardState: ChessBoardState,
    val whiteTimeRemaining: Double,
    val blackTimeRemaining: Double
)

class ChessSessionEngine(
    var difficulty: ChessDifficulty = ChessDifficulty.INTERMEDIATE,
    var timeControl: ChessTimeControl = ChessTimeControl.UNTIMED
) {
    var state: ChessBoardState = ChessBoardState()
        private set
    var whiteTimeRemaining: Double = timeControl.initialSeconds ?: 0.0
        private set
    var blackTimeRemaining: Double = timeControl.initialSeconds ?: 0.0
        private set
    val moveHistory = mutableListOf<ChessMove>()
    private val undoStack = mutableListOf<ChessSnapshot>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    fun reset(difficulty: ChessDifficulty? = null, timeControl: ChessTimeControl? = null) {
        if (difficulty != null) this.difficulty = difficulty
        if (timeControl != null) this.timeControl = timeControl
        val initialSecs = this.timeControl.initialSeconds ?: 0.0
        whiteTimeRemaining = initialSecs
        blackTimeRemaining = initialSecs
        state = ChessBoardState()
        moveHistory.clear()
        undoStack.clear()
    }

    fun performMove(move: ChessMove): Boolean {
        val snapshot = ChessSnapshot(
            boardState = state.copy(),
            whiteTimeRemaining = whiteTimeRemaining,
            blackTimeRemaining = blackTimeRemaining
        )
        if (!state.makeMove(move)) return false
        undoStack.add(snapshot)
        moveHistory.add(move)
        return true
    }

    fun undoSinglePly(): Boolean {
        if (undoStack.isEmpty()) return false
        val last = undoStack.removeAt(undoStack.lastIndex)
        state = last.boardState
        whiteTimeRemaining = last.whiteTimeRemaining
        blackTimeRemaining = last.blackTimeRemaining
        if (moveHistory.isNotEmpty()) {
            moveHistory.removeAt(moveHistory.lastIndex)
        }
        return true
    }

    fun undoToPlayerTurn(): Int {
        if (undoStack.isEmpty()) return 0
        var pliesUndone = 0
        if (undoSinglePly()) {
            pliesUndone += 1
        }
        while (state.turn != PieceColor.LIGHT && undoSinglePly()) {
            pliesUndone += 1
        }
        return pliesUndone
    }

    fun tickClock(elapsed: Double): PieceColor? {
        if (timeControl.initialSeconds == null || state.status.isTerminal || elapsed <= 0.0) return null
        return if (state.turn == PieceColor.LIGHT) {
            whiteTimeRemaining = max(0.0, whiteTimeRemaining - elapsed)
            if (whiteTimeRemaining <= 0.0) {
                state.forceTimeout(PieceColor.DARK)
                PieceColor.DARK
            } else null
        } else {
            blackTimeRemaining = max(0.0, blackTimeRemaining - elapsed)
            if (blackTimeRemaining <= 0.0) {
                state.forceTimeout(PieceColor.LIGHT)
                PieceColor.LIGHT
            } else null
        }
    }
}

object ChessAI {
    private const val MATE_SCORE = 100_000

    fun bestMove(state: ChessBoardState, difficulty: ChessDifficulty): ChessMove? {
        val color = state.turn
        val moves = orderedMoves(state.legalMoves(color))
        if (moves.isEmpty()) return null
        if (moves.size == 1) return moves[0]

        return when (difficulty) {
            ChessDifficulty.BEGINNER -> chooseBeginnerMove(state, moves, color)
            ChessDifficulty.INTERMEDIATE -> searchBestMove(state, moves, depth = 2, color = color, topPoolSize = 2, thresholdCp = 25)
            ChessDifficulty.ADVANCED -> searchBestMove(state, moves, depth = 3, color = color, topPoolSize = 1, thresholdCp = 0)
        }
    }

    private fun chooseBeginnerMove(
        state: ChessBoardState,
        moves: List<ChessMove>,
        color: PieceColor
    ): ChessMove? {
        val scored = moves.map { move ->
            val copy = state.copyForSearch()
            copy.applyUnchecked(move)
            if (copy.isKingInCheck(color.opponent) && copy.legalMoves(color.opponent).isEmpty()) {
                move to MATE_SCORE
            } else {
                val base = evaluate(copy, color)
                val jitter = Random.nextInt(-85, 86)
                move to (base + jitter)
            }
        }.sortedByDescending { it.second }

        val mate = scored.firstOrNull()
        if (mate != null && mate.second >= MATE_SCORE - 100) {
            return mate.first
        }
        val poolCount = min(3, scored.size)
        return scored[Random.nextInt(0, poolCount)].first
    }

    private fun searchBestMove(
        state: ChessBoardState,
        moves: List<ChessMove>,
        depth: Int,
        color: PieceColor,
        topPoolSize: Int,
        thresholdCp: Int
    ): ChessMove? {
        val scored = ArrayList<Pair<ChessMove, Int>>(moves.size)
        var alpha = -MATE_SCORE - 500
        val beta = MATE_SCORE + 500
        val nodeBudget = intArrayOf(if (depth >= 3) 28_000 else 8_000)

        for (move in moves) {
            val copy = state.copyForSearch()
            copy.applyUnchecked(move)
            val value = -negamax(copy, depth - 1, 1, -beta, -alpha, color.opponent, nodeBudget)
            scored.add(move to value)
            if (value > alpha) alpha = value
        }

        scored.sortByDescending { it.second }
        val best = scored.firstOrNull() ?: return null
        if (topPoolSize <= 1 || best.second >= MATE_SCORE - 1_000) {
            return best.first
        }
        val candidates = scored.take(topPoolSize).filter { best.second - it.second <= thresholdCp }
        return (candidates.randomOrNull() ?: best).first
    }

    private fun negamax(
        state: ChessBoardState,
        depth: Int,
        ply: Int,
        initialAlpha: Int,
        beta: Int,
        perspective: PieceColor,
        nodeBudget: IntArray
    ): Int {
        nodeBudget[0] -= 1
        if (nodeBudget[0] <= 0) {
            return evaluate(state, perspective)
        }

        val moves = orderedMoves(state.legalMoves(perspective))
        if (moves.isEmpty()) {
            return if (state.isKingInCheck(perspective)) {
                -(MATE_SCORE - ply)
            } else {
                0
            }
        }

        if (state.isInsufficientMaterial()) return 0

        if (depth <= 0) {
            return quiescence(state, initialAlpha, beta, perspective, 2, nodeBudget, precomputedOrderedMoves = moves)
        }

        var alpha = initialAlpha
        var best = -MATE_SCORE - 500

        for (move in moves) {
            val copy = state.copyForSearch()
            copy.applyUnchecked(move)
            val score = -negamax(copy, depth - 1, ply + 1, -beta, -alpha, perspective.opponent, nodeBudget)
            if (score > best) best = score
            if (best > alpha) alpha = best
            if (alpha >= beta) break
        }
        return best
    }

    private fun quiescence(
        state: ChessBoardState,
        initialAlpha: Int,
        beta: Int,
        perspective: PieceColor,
        qDepth: Int,
        nodeBudget: IntArray,
        precomputedOrderedMoves: List<ChessMove>? = null
    ): Int {
        nodeBudget[0] -= 1
        val standPat = evaluate(state, perspective)
        if (qDepth <= 0 || nodeBudget[0] <= 0) return standPat
        var alpha = initialAlpha
        if (standPat >= beta) return beta
        if (standPat > alpha) alpha = standPat

        val baseMoves = precomputedOrderedMoves ?: orderedMoves(state.legalMoves(perspective))
        val forcing = baseMoves.filter { it.isCapture || it.promotion != null }
        for (move in forcing) {
            val copy = state.copyForSearch()
            copy.applyUnchecked(move)
            val score = -quiescence(copy, -beta, -alpha, perspective.opponent, qDepth - 1, nodeBudget)
            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }
        return alpha
    }

    private fun orderedMoves(moves: List<ChessMove>): List<ChessMove> =
        moves.sortedByDescending { movePriority(it) }

    private fun movePriority(move: ChessMove): Int {
        var score = 0
        if (move.capturedPiece != null) {
            score += 1_000 + move.capturedPiece.kind.value * 100 - move.movedPiece.kind.value * 10
        }
        if (move.promotion != null) {
            score += 800 + move.promotion.value * 80
        }
        if (move.isCastling) {
            score += 90
        }
        val fileDist = abs(move.to.file * 2 - 7)
        val rankDist = abs(move.to.rank * 2 - 7)
        score += 14 - (fileDist + rankDist)
        return score
    }

    private fun evaluate(state: ChessBoardState, perspective: PieceColor): Int {
        var total = 0
        var lightBishops = 0
        var darkBishops = 0

        for (idx in 0 until 64) {
            val p = state.squares[idx] ?: continue
            val file = idx % 8
            val rank = idx / 8
            val pstRank = if (p.color == PieceColor.LIGHT) rank else (7 - rank)
            val squareVal = pieceCentipawns(p.kind) + pstBonus(p.kind, file, pstRank)

            if (p.color == PieceColor.LIGHT) {
                total += squareVal
                if (p.kind == PieceKind.BISHOP) lightBishops += 1
            } else {
                total -= squareVal
                if (p.kind == PieceKind.BISHOP) darkBishops += 1
            }
        }

        if (lightBishops >= 2) total += 30
        if (darkBishops >= 2) total -= 30

        return if (perspective == PieceColor.LIGHT) total else -total
    }

    private fun pieceCentipawns(kind: PieceKind): Int = when (kind) {
        PieceKind.PAWN -> 100
        PieceKind.KNIGHT -> 320
        PieceKind.BISHOP -> 330
        PieceKind.ROOK -> 500
        PieceKind.QUEEN -> 900
        PieceKind.KING -> 0
    }

    private fun pstBonus(kind: PieceKind, file: Int, rankFromBack: Int): Int {
        val row = rankFromBack.coerceIn(0, 7)
        val col = file.coerceIn(0, 7)
        val idx = (7 - row) * 8 + col
        return when (kind) {
            PieceKind.PAWN -> pawnPST[idx]
            PieceKind.KNIGHT -> knightPST[idx]
            PieceKind.BISHOP -> bishopPST[idx]
            PieceKind.ROOK -> rookPST[idx]
            PieceKind.QUEEN -> queenPST[idx]
            PieceKind.KING -> kingMidPST[idx]
        }
    }

    private val pawnPST = intArrayOf(
         0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 27, 27, 10,  5,  5,
         0,  0,  0, 24, 24,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-22,-22, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    )

    private val knightPST = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val bishopPST = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val rookPST = intArrayOf(
          0,  0,  0,  0,  0,  0,  0,  0,
          5, 10, 10, 10, 10, 10, 10,  5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
          0,  0,  0,  5,  5,  0,  0,  0
    )

    private val queenPST = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    )

    private val kingMidPST = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    )
}
