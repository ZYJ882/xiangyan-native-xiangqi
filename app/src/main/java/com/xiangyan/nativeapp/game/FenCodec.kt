package com.xiangyan.nativeapp.game

/** Pikafish 的 FEN 从黑方顶线（应用 row 0）到红方底线（row 9）排列。 */
object FenCodec {
    fun encode(state: GameState): String = buildString {
        for (row in 0..9) {
            var empty = 0
            for (col in 0..8) {
                val piece = state.pieceAt(Square(row, col))
                if (piece == null) empty++ else {
                    if (empty > 0) { append(empty); empty = 0 }
                    append(pieceSymbol(piece))
                }
            }
            if (empty > 0) append(empty)
            if (row != 9) append('/')
        }
        append(if (state.turn == Side.Red) " w - - 0 1" else " b - - 0 1")
    }

    fun decodeMove(uci: String): Move? {
        if (!Regex("^[a-i][0-9][a-i][0-9]$").matches(uci)) return null
        fun squareAt(index: Int): Square {
            val col = uci[index] - 'a'
            val row = 9 - (uci[index + 1] - '0')
            return Square(row, col)
        }
        return Move(squareAt(0), squareAt(2))
    }

    private fun pieceSymbol(piece: Piece): Char {
        val base = when (piece.type) {
            PieceType.General -> 'k'; PieceType.Advisor -> 'a'; PieceType.Elephant -> 'b'
            PieceType.Horse -> 'n'; PieceType.Chariot -> 'r'; PieceType.Cannon -> 'c'; PieceType.Soldier -> 'p'
        }
        return if (piece.side == Side.Red) base.uppercaseChar() else base
    }
}
