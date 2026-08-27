package com.xiangyan.nativeapp.game

/** 规则层只处理合法落点；将军、重复局面及残局规则将作为完整引擎接入后的扩展点。 */
object BoardRules {
    fun legalMoves(state: GameState, side: Side): List<Move> = state.pieces.filter { it.side == side }.flatMap { piece ->
        (0..9).flatMap { row -> (0..8).mapNotNull { col ->
            val to = Square(row, col)
            if (isLegalMove(state, piece, to)) Move(piece.square, to) else null
        } }
    }

    fun isLegalMove(state: GameState, piece: Piece, to: Square): Boolean {
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
