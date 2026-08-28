package com.xiangyan.nativeapp.ui

import android.app.Application
import android.content.Context
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
    private val settings = application.getSharedPreferences("xiangyan_settings", Context.MODE_PRIVATE)
    var state by mutableStateOf(GameState.initial()); private set
    var profile by mutableStateOf(restoreProfile()); private set
    private val main = Handler(Looper.getMainLooper())
    private val engine = EngineController(application.applicationContext)

    fun selectProfile(newProfile: EngineProfile) {
        profile = newProfile
        settings.edit().putString(PREF_ENGINE_PROFILE, newProfile.name).apply()
        if (state.phase == GamePhase.Thinking) {
            engine.cancel()
            state = state.copy(phase = GamePhase.Paused, selected = null, message = "已保存 ${newProfile.label} 强度 · 点击继续后按新强度思考")
        }
    }

    /** 每局随机分配用户阵营；红方依照中国象棋正式规则先行。 */
    fun startRandomGame() {
        engine.cancel(); engine.newGame()
        val humanSide = if (Random.nextBoolean()) Side.Red else Side.Black
        val initial = GameState.initial().copy(
            humanSide = humanSide, turn = Side.Red,
            phase = if (humanSide == Side.Red) GamePhase.HumanTurn else GamePhase.Thinking,
            message = if (humanSide == Side.Red) "你执红先行 · 请走第一步" else "你执黑后手 · AI 正在走红方第一步…",
        )
        state = initial
        if (humanSide == Side.Black) requestAiMove(initial)
    }

    fun pause() {
        if (state.phase !in setOf(GamePhase.HumanTurn, GamePhase.Thinking)) return
        engine.cancel(); state = state.copy(phase = GamePhase.Paused, selected = null, message = "对局已暂停 · 引擎搜索已取消")
    }

    fun resume() {
        if (state.phase != GamePhase.Paused) return
        val base = state.copy(selected = null)
        if (base.turn == base.humanSide) state = base.copy(phase = GamePhase.HumanTurn, message = playerTurnMessage(base))
        else requestAiMove(base.copy(phase = GamePhase.Thinking, message = "继续对局 · AI 正在思考…"))
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
        if (square == selected) {
            state = current.copy(selected = null, message = playerTurnMessage(current))
            return
        }
        if (current.pieceAt(square)?.side == current.humanSide) {
            state = current.copy(selected = square, message = "已切换选中棋子")
            return
        }
        val afterHuman = BoardRules.applyLegalMove(current, Move(selected, square))
        if (afterHuman == null) {
            state = current.copy(message = "该走法不合法：不能自将、照面或直接吃将")
            return
        }
        val result = BoardRules.adjudicate(afterHuman)
        if (result != null) {
            state = finish(afterHuman, result)
            return
        }
        requestAiMove(afterHuman.copy(phase = GamePhase.Thinking, selected = null, message = if (BoardRules.isInCheck(afterHuman, afterHuman.turn)) "将军！AI 正在应将…" else "AI 正在按 ${profile.label} 强度思考…"))
    }

    private fun requestAiMove(base: GameState) {
        val thinking = base.copy(phase = GamePhase.Thinking, selected = null); state = thinking
        val requestToken = engine.request(FenCodec.encode(thinking), profile) { token, uci -> main.post {
            if (state.phase != GamePhase.Thinking || state.searchToken != token) return@post
            val move = uci?.let(FenCodec::decodeMove)
            state = if (move == null) {
                thinking.copy(phase = GamePhase.Paused, message = "Pikafish 未返回走法 · 请停止后重新开始")
            } else {
                val afterAi = BoardRules.applyLegalMove(thinking, move)
                if (afterAi == null) {
                    thinking.copy(phase = GamePhase.Paused, message = "引擎走法未通过本地规则校验 · 请停止后重新开始")
                } else {
                    val result = BoardRules.adjudicate(afterAi)
                    if (result != null) finish(afterAi, result)
                    else afterAi.copy(phase = GamePhase.HumanTurn, message = playerTurnMessage(afterAi))
                }
            }
        } }
        state = state.copy(searchToken = requestToken)
    }

    private fun finish(position: GameState, result: BoardRules.Adjudication): GameState {
        val winner = result.winner?.let { if (it == position.humanSide) "你" else "AI" }
        val message = when (result.reason) {
            BoardRules.TerminalReason.Checkmate -> "将死！$winner 获胜"
            BoardRules.TerminalReason.Stalemate -> "困毙！$winner 获胜"
            BoardRules.TerminalReason.ThreefoldRepetition -> "三次重复局面 · 和棋"
            BoardRules.TerminalReason.PerpetualCheck -> "长将判负 · ${if (result.winner == position.humanSide) "你" else "AI"} 获胜"
        }
        return position.copy(phase = GamePhase.Finished, selected = null, message = message)
    }

    private fun playerTurnMessage(position: GameState) = if (BoardRules.isInCheck(position, position.humanSide)) "将军！轮到你走 · 必须应将" else "轮到你走 · ${sideName(position.humanSide)}方"
    private fun sideName(side: Side) = if (side == Side.Red) "红" else "黑"
    private fun restoreProfile(): EngineProfile = settings.getString(PREF_ENGINE_PROFILE, EngineProfile.Standard.name).let { saved -> EngineProfile.entries.firstOrNull { it.name == saved } ?: EngineProfile.Standard }
    override fun onCleared() { engine.close(); super.onCleared() }

    companion object {
        private const val PREF_ENGINE_PROFILE = "engine_profile"
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = GameViewModel(application) as T
        }
    }
}
