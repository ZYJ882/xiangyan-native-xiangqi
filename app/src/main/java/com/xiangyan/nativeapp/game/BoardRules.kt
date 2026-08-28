package com.xiangyan.nativeapp.game

/**
 * 应用层完整约束棋子走法、将帅安全和局面裁判。
 * Pikafish 仍负责搜索，但所有进入 UI 状态的走法都必须先经过这里复核。
 */
object BoardRules {
    enum class TerminalReason { Checkmate, Stalemate, ThreefoldRepetition, PerpetualCheck }

    data class Adjudication(val reason: TerminalReason, val winner: Side? = null)

    fun legalMoves(state: GameState, side: Side): List<Move> {
        val position = if (state.turn == side) state else state.copy(turn = side)
        return position.pieces.filter { it.side == side }.flatMap { piece ->
            (0..9).flatMap { row -> (0..8).mapNotNull { col ->
                val to = Square(row, col)
                if (isLegalMove(position, piece, to)) Move(piece.square, to) else null
            } }
        }
    }

    fun isLegalMove(state: GameState, piece: Piece, to: Square): Boolean {
        if (piece.side != state.turn) return false
        // 中国象棋以“将死”结束，不允许把将/帅当作普通棋子直接吃掉。
        if (state.pieceAt(to)?.type == PieceType.General) return false
        if (!isPseudoLegalMove(state, piece, to)) return false
        return !isInCheck(simulateMove(state, Move(piece.square, to)), piece.side)
    }

    /** 应用一着已经通过本地校验的走法，并更新可重复式裁判所需的历史。 */
    fun applyLegalMove(state: GameState, move: Move): GameState? {
        val moving = state.pieceAt(move.from) ?: return null
        if (!isLegalMove(state, moving, move.to)) return null
        val target = state.pieceAt(move.to)
        if (target?.type == PieceType.General) return null
        val pieces = state.pieces.filterNot { it.id == target?.id }
            .map { if (it.id == moving.id) it.copy(square = move.to) else it }
        val next = state.copy(
            pieces = pieces,
            turn = moving.side.opposite(),
            selected = null,
            moves = state.moves + move,
        )
        val positions = state.historyIncludingCurrent() + next.fingerprint()
        val checks = state.checksIncludingCurrent() + checkTarget(next)
        return next.copy(positionHistory = positions, checkHistory = checks)
    }

    fun isInCheck(state: GameState, side: Side): Boolean {
        val general = state.pieces.firstOrNull { it.side == side && it.type == PieceType.General } ?: return true
        return state.pieces.any { attacker -> attacker.side != side && attacksSquare(state, attacker, general.square) }
    }

    fun hasAnyLegalMove(state: GameState, side: Side): Boolean = legalMoves(state, side).isNotEmpty()

    /**
     * 应用级确定性裁判：无合法着法时判将死/困毙；同一轮到方局面三次出现判和，
     * 若三次重复均为同一方被将，则判连续将军方负。长捉仍需赛事规则定义，应用不擅自推断。
     */
    fun adjudicate(state: GameState): Adjudication? {
        val sideToMove = state.turn
        if (!hasAnyLegalMove(state, sideToMove)) {
            return if (isInCheck(state, sideToMove)) {
                Adjudication(TerminalReason.Checkmate, sideToMove.opposite())
            } else {
                Adjudication(TerminalReason.Stalemate, sideToMove.opposite())
            }
        }

        val current = state.fingerprint()
        val positions = state.historyIncludingCurrent()
        if (positions.count { it == current } < 3) return null
        val checkTargets = state.checksIncludingCurrent()
        val repeatedIndexes = positions.mapIndexedNotNull { index, fingerprint ->
            if (fingerprint == current) index else null
        }
        val repeatedChecks = repeatedIndexes.mapNotNull { checkTargets.getOrNull(it) }.distinct()
        return if (repeatedChecks.size == 1) {
            val checkedSide = repeatedChecks.single()
            Adjudication(TerminalReason.PerpetualCheck, checkedSide)
        } else {
            Adjudication(TerminalReason.ThreefoldRepetition, null)
        }
    }

    private fun checkTarget(state: GameState): Side? = state.turn.takeIf { isInCheck(state, it) }

    private fun attacksSquare(state: GameState, attacker: Piece, target: Square): Boolean {
        if (attacker.type == PieceType.General && attacker.square.col == target.col && piecesBetween(state, attacker.square, target) == 0) return true
        return isPseudoLegalMove(state, attacker, target)
    }

    private fun isPseudoLegalMove(state: GameState, piece: Piece, to: Square): Boolean {
        if (!to.isInside() || to == piece.square || state.pieceAt(to)?.side == piece.side) return false
        val dr = to.row - piece.square.row
        val dc = to.col - piece.square.col
        return when (piece.type) {
            PieceType.General -> insidePalace(piece.side, to) && kotlin.math.abs(dr) + kotlin.math.abs(dc) == 1
            PieceType.Advisor -> insidePalace(piece.side, to) && kotlin.math.abs(dr) == 1 && kotlin.math.abs(dc) == 1
            PieceType.Elephant -> kotlin.math.abs(dr) == 2 && kotlin.math.abs(dc) == 2 && onOwnSide(piece.side, to) && state.pieceAt(Square(piece.square.row + dr / 2, piece.square.col + dc / 2)) == null
            PieceType.Horse -> (kotlin.math.abs(dr) == 2 && kotlin.math.abs(dc) == 1 && state.pieceAt(Square(piece.square.row + dr / 2, piece.square.col)) == null) || (kotlin.math.abs(dr) == 1 && kotlin.math.abs(dc) == 2 && state.pieceAt(Square(piece.square.row, piece.square.col + dc / 2)) == null)
            PieceType.Chariot -> straight(dr, dc) && piecesBetween(state, piece.square, to) == 0
            PieceType.Cannon -> straight(dr, dc) && if (state.pieceAt(to) == null) piecesBetween(state, piece.square, to) == 0 else piecesBetween(state, piece.square, to) == 1
            PieceType.Soldier -> soldierMove(piece.side, piece.square, to)
        }
    }

    private fun simulateMove(before: GameState, move: Move): GameState {
        val moved = before.pieceAt(move.from) ?: return before
        val pieces = before.pieces.filterNot { it.square == move.to }.map { if (it.id == moved.id) it.copy(square = move.to) else it }
        return before.copy(pieces = pieces, turn = before.turn.opposite(), selected = null, moves = before.moves + move)
    }

    private fun insidePalace(side: Side, square: Square) = square.col in 3..5 && if (side == Side.Red) square.row in 7..9 else square.row in 0..2
    private fun onOwnSide(side: Side, square: Square) = if (side == Side.Red) square.row >= 5 else square.row <= 4
    private fun straight(dr: Int, dc: Int) = (dr == 0) != (dc == 0)
    private fun soldierMove(side: Side, from: Square, to: Square): Boolean {
        val dr = to.row - from.row; val dc = to.col - from.col
        val forward = if (side == Side.Red) -1 else 1
        val crossed = if (side == Side.Red) from.row <= 4 else from.row >= 5
        return (dr == forward && dc == 0) || (crossed && dr == 0 && kotlin.math.abs(dc) == 1)
    }
    private fun piecesBetween(state: GameState, from: Square, to: Square): Int {
        val rowStep = (to.row - from.row).compareTo(0); val colStep = (to.col - from.col).compareTo(0)
        var row = from.row + rowStep; var col = from.col + colStep; var count = 0
        while (row != to.row || col != to.col) { if (state.pieceAt(Square(row, col)) != null) count++; row += rowStep; col += colStep }
        return count
    }
}
