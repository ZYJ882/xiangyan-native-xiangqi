package com.xiangyan.nativeapp.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardRulesTest {
    @Test
    fun initialPositionHasExpectedLegalMoves() {
        val state = GameState.initial()
        assertTrue(BoardRules.isLegalMove(state, state.pieceAt(Square(6, 0))!!, Square(5, 0)))
        assertFalse(BoardRules.isLegalMove(state, state.pieceAt(Square(6, 0))!!, Square(6, 1)))
        assertTrue(BoardRules.isLegalMove(state, state.pieceAt(Square(9, 1))!!, Square(7, 2)))
        assertFalse(BoardRules.isLegalMove(state, state.pieceAt(Square(9, 3))!!, Square(8, 2)))
    }

    @Test
    fun flyingGeneralsAreMutuallyInCheckAndDirectCaptureIsRejected() {
        val state = position(
            turn = Side.Red,
            Piece(1, Side.Red, PieceType.General, Square(9, 4)),
            Piece(2, Side.Black, PieceType.General, Square(0, 4)),
        )
        assertTrue(BoardRules.isInCheck(state, Side.Red))
        assertTrue(BoardRules.isInCheck(state, Side.Black))
        assertFalse(BoardRules.isLegalMove(state, state.pieceAt(Square(9, 4))!!, Square(0, 4)))
    }

    @Test
    fun cannonNeedsExactlyOneScreenToCapture() {
        val state = position(
            turn = Side.Red,
            Piece(1, Side.Red, PieceType.General, Square(9, 4)),
            Piece(2, Side.Black, PieceType.General, Square(0, 4)),
            Piece(3, Side.Red, PieceType.Cannon, Square(5, 4)),
            Piece(4, Side.Red, PieceType.Soldier, Square(3, 4)),
        )
        assertTrue(BoardRules.isInCheck(state, Side.Black))
        val withTwoScreens = state.copy(pieces = state.pieces + Piece(5, Side.Red, PieceType.Soldier, Square(4, 4)))
        assertFalse(BoardRules.isInCheck(withTwoScreens, Side.Black))
    }

    @Test
    fun checkmateIsAdjudicatedBeforeAnyKingCapture() {
        val state = position(
            turn = Side.Red,
            Piece(1, Side.Red, PieceType.General, Square(9, 4)),
            Piece(2, Side.Black, PieceType.General, Square(0, 3)),
            Piece(3, Side.Black, PieceType.Chariot, Square(8, 4)),
            Piece(4, Side.Black, PieceType.Chariot, Square(9, 0)),
            Piece(5, Side.Black, PieceType.Chariot, Square(9, 8)),
            Piece(6, Side.Black, PieceType.Soldier, Square(7, 4)),
        )
        val result = BoardRules.adjudicate(state)
        assertNotNull(result)
        assertEquals(BoardRules.TerminalReason.Checkmate, result!!.reason)
        assertEquals(Side.Black, result.winner)
    }

    @Test
    fun threefoldRepetitionIsDrawn() {
        var state = position(
            turn = Side.Red,
            Piece(1, Side.Red, PieceType.General, Square(9, 5)),
            Piece(2, Side.Black, PieceType.General, Square(0, 3)),
            Piece(3, Side.Red, PieceType.Chariot, Square(8, 0)),
            Piece(4, Side.Black, PieceType.Chariot, Square(1, 8)),
        ).let { it.copy(positionHistory = listOf(it.fingerprint()), checkHistory = listOf(null)) }
        val moves = listOf(
            Move(Square(8, 0), Square(8, 1)), Move(Square(1, 8), Square(1, 7)),
            Move(Square(8, 1), Square(8, 0)), Move(Square(1, 7), Square(1, 8)),
            Move(Square(8, 0), Square(8, 1)), Move(Square(1, 8), Square(1, 7)),
            Move(Square(8, 1), Square(8, 0)), Move(Square(1, 7), Square(1, 8)),
        )
        moves.forEach { state = BoardRules.applyLegalMove(state, it)!! }
        val result = BoardRules.adjudicate(state)
        assertNotNull(result)
        assertEquals(BoardRules.TerminalReason.ThreefoldRepetition, result!!.reason)
        assertNull(result.winner)
    }

    @Test
    fun repeatedCheckTargetIsPerpetualCheckLoss() {
        val base = position(
            turn = Side.Red,
            Piece(1, Side.Red, PieceType.General, Square(9, 5)),
            Piece(2, Side.Black, PieceType.General, Square(0, 3)),
            Piece(3, Side.Red, PieceType.Chariot, Square(0, 0)),
        )
        val state = base.copy(
            positionHistory = listOf(base.fingerprint(), base.fingerprint(), base.fingerprint()),
            checkHistory = listOf(Side.Black, Side.Black, Side.Black),
        )
        val result = BoardRules.adjudicate(state)
        assertNotNull(result)
        assertEquals(BoardRules.TerminalReason.PerpetualCheck, result!!.reason)
        assertEquals(Side.Black, result.winner)
    }

    private fun position(turn: Side, vararg pieces: Piece) = GameState(pieces = pieces.toList(), turn = turn, humanSide = Side.Red, phase = GamePhase.HumanTurn)
}
