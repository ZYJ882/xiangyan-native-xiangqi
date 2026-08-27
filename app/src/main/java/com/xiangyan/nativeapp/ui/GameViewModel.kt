package com.xiangyan.nativeapp.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import com.xiangyan.nativeapp.engine.EngineController
import com.xiangyan.nativeapp.engine.EngineProfile
import com.xiangyan.nativeapp.game.BoardRules
import com.xiangyan.nativeapp.game.FenCodec
import com.xiangyan.nativeapp.game.GamePhase
import com.xiangyan.nativeapp.game.GameState
import com.xiangyan.nativeapp.game.Move
import com.xiangyan.nativeapp.game.Side
import com.xiangyan.nativeapp.game.Square
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {
    var state by mutableStateOf(GameState.initial()); private set
    var profile by mutableStateOf(EngineProfile.Standard); private set
    private val main = Handler(Looper.getMainLooper())
    private val engine = EngineController(application.applicationContext)

    fun selectProfile(newProfile: EngineProfile) {
        profile = newProfile
        if (state.phase == GamePhase.Thinking) {
            engine.cancel()
            state = state.copy(phase = GamePhase.Paused, selected = null, message = "已切换 ${newProfile.label} 强度 · 点击继续后按新强度思考")
        }
    }

    /** 每局随机分配用户阵营；红方依照中国象棋正式规则先行。 */
    fun startRandomGame() {
        engine.cancel()
        engine.newGame()
        val humanSide = if (Random.nextBoolean()) Side.Red else Side.Black
        val initial = GameState.initial().copy(
            humanSide = humanSide,
            turn = Side.Red,
            phase = if (humanSide == Side.Red) GamePhase.HumanTurn else GamePhase.Thinking,
            message = if (humanSide == Side.Red) "你执红先行 · 请走第一步" else "你执黑后手 · AI 正在走红方第一步…",
        )
        state = initial
        if (humanSide == Side.Black) requestAiMove(initial)
    }

    fun pause() {
        if (state.phase !in setOf(GamePhase.HumanTurn, GamePhase.Thinking)) return
        engine.cancel()
        state = state.copy(phase = GamePhase.Paused, selected = null, message = "对局已暂停 · 引擎搜索已取消")
    }

    fun resume() {
        if (state.phase != GamePhase.Paused) return
        val base = state.copy(selected = null)
        if (base.turn == base.humanSide) {
            state = base.copy(phase = GamePhase.HumanTurn, message = "继续对局 · 轮到你走")
        } else {
            requestAiMove(base.copy(phase = GamePhase.Thinking, message = "继续对局 · AI 正在思考…"))
        }
    }

    fun stop() {
        engine.cancel()
        state = state.copy(phase = GamePhase.Stopped, selected = null, message = "对局已停止 · 点击开始可重新随机先手")
    }

    fun onSquareTap(square: Square) {
        if (state.phase != GamePhase.HumanTurn || state.turn != state.humanSide) return
        val current = state
        val selected = current.selected
        if (selected == null) {
            if (current.pieceAt(square)?.side == current.humanSide) state = current.copy(selected = square, message = "已选中棋子 · 请选择落点")
            return
        }
        if (square == selected) { state = current.copy(selected = null, message = "继续对局 · 轮到你走"); return }
        if (current.pieceAt(square)?.side == current.humanSide) { state = current.copy(selected = square, message = "已切换选中棋子"); return }
        val piece = current.pieceAt(selected) ?: return
        if (!BoardRules.isLegalMove(current, piece, square)) { state = current.copy(message = "该落点不符合当前基础规则"); return }
        val afterHuman = applyMove(current, Move(selected, square), current.humanSide.opposite())
        if (afterHuman.phase == GamePhase.Finished) { state = afterHuman.copy(message = "你获胜 · 将已被吃掉"); return }
        requestAiMove(afterHuman.copy(phase = GamePhase.Thinking, selected = null, message = "AI 正在按 ${profile.label} 强度思考…"))
    }

    private fun requestAiMove(base: GameState) {
        val thinking = base.copy(phase = GamePhase.Thinking, selected = null)
        state = thinking
        val requestToken = engine.request(FenCodec.encode(thinking), profile) { token, uci -> main.post {
            if (state.phase != GamePhase.Thinking || state.searchToken != token) return@post
            val move = uci?.let(FenCodec::decodeMove)
            state = if (move == null) thinking.copy(phase = GamePhase.Paused, message = "Pikafish 未返回走法 · 请停止后重新开始") else {
                val afterAi = applyMove(thinking, move, thinking.humanSide)
                if (afterAi.phase == GamePhase.Finished) afterAi.copy(message = "AI 获胜 · 将已被吃掉") else afterAi.copy(phase = GamePhase.HumanTurn, message = "轮到你走 · ${sideName(thinking.humanSide)}方")
            }
        } }
        state = state.copy(searchToken = requestToken)
    }

    private fun applyMove(before: GameState, move: Move, next: Side): GameState {
        val moved = before.pieceAt(move.from) ?: return before
        val target = before.pieceAt(move.to)
        val pieces = before.pieces.filterNot { it.id == target?.id }.map { if (it.id == moved.id) it.copy(square = move.to) else it }
        val isFinished = target?.type == com.xiangyan.nativeapp.game.PieceType.General
        return before.copy(pieces = pieces, turn = next, selected = null, phase = if (isFinished) GamePhase.Finished else before.phase, moves = before.moves + move)
    }

    private fun sideName(side: Side) = if (side == Side.Red) "红" else "黑"
    override fun onCleared() { engine.close(); super.onCleared() }

    companion object {
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = GameViewModel(application) as T
        }
    }
}
