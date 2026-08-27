package com.xiangyan.nativeapp.game

enum class Side { Red, Black; fun opposite() = if (this == Red) Black else Red }

enum class PieceType(val glyph: String, val value: Int) {
    General("将", 10_000), Advisor("士", 110), Elephant("象", 110), Horse("马", 300),
    Chariot("车", 600), Cannon("炮", 350), Soldier("卒", 90)
}

data class Square(val row: Int, val col: Int) {
    fun isInside() = row in 0..9 && col in 0..8
}

data class Piece(val id: Int, val side: Side, val type: PieceType, val square: Square) {
    val glyph: String get() = if (side == Side.Red) when (type) {
        PieceType.General -> "帅"; PieceType.Advisor -> "仕"; PieceType.Elephant -> "相"; PieceType.Horse -> "马"; PieceType.Chariot -> "车"; PieceType.Cannon -> "炮"; PieceType.Soldier -> "兵"
    } else type.glyph
}

data class Move(val from: Square, val to: Square)

/** 准备态确保首屏和设置页不触发原生搜索；暂停与停止态均不可走子。 */
enum class GamePhase { Ready, HumanTurn, Thinking, Paused, Stopped, Finished }

data class GameState(
    val pieces: List<Piece>,
    val turn: Side = Side.Red,
    val humanSide: Side = Side.Red,
    val selected: Square? = null,
    val phase: GamePhase = GamePhase.Ready,
    val message: String = "点击「开始对局」随机决定先手",
    val moves: List<Move> = emptyList(),
    val searchToken: Long = 0L,
) {
    fun pieceAt(square: Square) = pieces.firstOrNull { it.square == square }
    fun fingerprint(): Long = pieces.sortedBy { it.id }.fold(0x4F1BBCDCBFA54017L xor turn.ordinal.toLong()) { acc, piece ->
        val v = (piece.id * 131 + piece.square.row * 11 + piece.square.col + piece.type.ordinal * 17 + piece.side.ordinal * 29).toLong()
        (acc xor v) * 0x100000001B3L
    }

    companion object {
        fun initial() = GameState(pieces = listOf(
            Piece(0, Side.Black, PieceType.Chariot, Square(0, 0)), Piece(1, Side.Black, PieceType.Horse, Square(0, 1)), Piece(2, Side.Black, PieceType.Elephant, Square(0, 2)), Piece(3, Side.Black, PieceType.Advisor, Square(0, 3)), Piece(4, Side.Black, PieceType.General, Square(0, 4)), Piece(5, Side.Black, PieceType.Advisor, Square(0, 5)), Piece(6, Side.Black, PieceType.Elephant, Square(0, 6)), Piece(7, Side.Black, PieceType.Horse, Square(0, 7)), Piece(8, Side.Black, PieceType.Chariot, Square(0, 8)),
            Piece(9, Side.Black, PieceType.Cannon, Square(2, 1)), Piece(10, Side.Black, PieceType.Cannon, Square(2, 7)),
            Piece(11, Side.Black, PieceType.Soldier, Square(3, 0)), Piece(12, Side.Black, PieceType.Soldier, Square(3, 2)), Piece(13, Side.Black, PieceType.Soldier, Square(3, 4)), Piece(14, Side.Black, PieceType.Soldier, Square(3, 6)), Piece(15, Side.Black, PieceType.Soldier, Square(3, 8)),
            Piece(16, Side.Red, PieceType.Chariot, Square(9, 0)), Piece(17, Side.Red, PieceType.Horse, Square(9, 1)), Piece(18, Side.Red, PieceType.Elephant, Square(9, 2)), Piece(19, Side.Red, PieceType.Advisor, Square(9, 3)), Piece(20, Side.Red, PieceType.General, Square(9, 4)), Piece(21, Side.Red, PieceType.Advisor, Square(9, 5)), Piece(22, Side.Red, PieceType.Elephant, Square(9, 6)), Piece(23, Side.Red, PieceType.Horse, Square(9, 7)), Piece(24, Side.Red, PieceType.Chariot, Square(9, 8)),
            Piece(25, Side.Red, PieceType.Cannon, Square(7, 1)), Piece(26, Side.Red, PieceType.Cannon, Square(7, 7)),
            Piece(27, Side.Red, PieceType.Soldier, Square(6, 0)), Piece(28, Side.Red, PieceType.Soldier, Square(6, 2)), Piece(29, Side.Red, PieceType.Soldier, Square(6, 4)), Piece(30, Side.Red, PieceType.Soldier, Square(6, 6)), Piece(31, Side.Red, PieceType.Soldier, Square(6, 8)),
        ))
    }
}
