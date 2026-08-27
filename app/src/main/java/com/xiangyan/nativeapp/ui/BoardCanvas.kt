package com.xiangyan.nativeapp.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import com.xiangyan.nativeapp.game.GameState
import com.xiangyan.nativeapp.game.Side
import com.xiangyan.nativeapp.game.Square
import kotlin.math.roundToInt

@Composable
fun BoardCanvas(state: GameState, onSquareTap: (Square) -> Unit, modifier: Modifier = Modifier) {
    var canvasSize = remember { IntSize.Zero }
    Canvas(
        modifier = modifier.fillMaxWidth().aspectRatio(0.9f).onSizeChanged { canvasSize = it }
            .semantics { role = Role.Button; contentDescription = "中国象棋棋盘，点击棋子和目标位置走棋" }
            .pointerInput(state.pieces, canvasSize) {
                detectTapGestures { offset -> squareAt(offset, canvasSize)?.let(onSquareTap) }
            }
    ) { drawBoard(state) }
}

private fun squareAt(offset: Offset, size: IntSize): Square? {
    if (size.width <= 0 || size.height <= 0) return null
    val padding = size.width * .058f
    val cellX = (size.width - padding * 2) / 8f
    val cellY = (size.height - padding * 2) / 9f
    val col = ((offset.x - padding) / cellX).roundToInt()
    val row = ((offset.y - padding) / cellY).roundToInt()
    return Square(row, col).takeIf { it.isInside() }
}

private fun DrawScope.drawBoard(state: GameState) {
    val ink = Color(0xFF263229); val grid = Color(0xFF765A3A); val paper = Color(0xFFF1D7A4); val red = Color(0xFFB7362C); val jade = Color(0xFF547A5C)
    drawRoundRect(paper, size = Size(size.width, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
    val p = size.width * .058f; val dx = (size.width - 2 * p) / 8f; val dy = (size.height - 2 * p) / 9f
    for (row in 0..9) drawLine(grid, Offset(p, p + row * dy), Offset(size.width - p, p + row * dy), strokeWidth = 1.5f)
    for (col in 0..8) { drawLine(grid, Offset(p + col * dx, p), Offset(p + col * dx, p + 4 * dy), strokeWidth = 1.5f); drawLine(grid, Offset(p + col * dx, p + 5 * dy), Offset(p + col * dx, size.height - p), strokeWidth = 1.5f) }
    drawLine(grid, Offset(p, p), Offset(p + 2 * dx, p + 2 * dy), strokeWidth = 1.5f); drawLine(grid, Offset(p + 2 * dx, p), Offset(p, p + 2 * dy), strokeWidth = 1.5f)
    drawLine(grid, Offset(p + 6 * dx, p), Offset(p + 8 * dx, p + 2 * dy), strokeWidth = 1.5f); drawLine(grid, Offset(p + 8 * dx, p), Offset(p + 6 * dx, p + 2 * dy), strokeWidth = 1.5f)
    drawLine(grid, Offset(p, p + 7 * dy), Offset(p + 2 * dx, p + 9 * dy), strokeWidth = 1.5f); drawLine(grid, Offset(p + 2 * dx, p + 7 * dy), Offset(p, p + 9 * dy), strokeWidth = 1.5f)
    drawLine(grid, Offset(p + 6 * dx, p + 7 * dy), Offset(p + 8 * dx, p + 9 * dy), strokeWidth = 1.5f); drawLine(grid, Offset(p + 8 * dx, p + 7 * dy), Offset(p + 6 * dx, p + 9 * dy), strokeWidth = 1.5f)
    drawContext.canvas.nativeCanvas.drawText("楚 河", p + 1.25f * dx, p + 4.62f * dy, Paint().apply { color = 0xFF765A3A.toInt(); textSize = dy * .42f; typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD); letterSpacing = .16f })
    drawContext.canvas.nativeCanvas.drawText("漢 界", p + 5.25f * dx, p + 4.62f * dy, Paint().apply { color = 0xFF765A3A.toInt(); textSize = dy * .42f; typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD); letterSpacing = .16f })
    // 最近一步：玉绿色空心圈表示起点，朱砂色实心底与描边表示当前落点。
    state.moves.lastOrNull()?.let { lastMove ->
        val start = Offset(p + lastMove.from.col * dx, p + lastMove.from.row * dy)
        val end = Offset(p + lastMove.to.col * dx, p + lastMove.to.row * dy)
        val radius = minOf(dx, dy)
        drawCircle(jade.copy(alpha = .16f), radius = radius * .48f, center = start)
        drawCircle(jade, radius = radius * .48f, center = start, style = androidx.compose.ui.graphics.drawscope.Stroke(2.4f))
        drawCircle(red.copy(alpha = .20f), radius = radius * .54f, center = end)
        drawCircle(red, radius = radius * .54f, center = end, style = androidx.compose.ui.graphics.drawscope.Stroke(2.8f))
    }
    state.selected?.let { selection -> drawCircle(red.copy(alpha = .22f), radius = minOf(dx, dy) * .60f, center = Offset(p + selection.col * dx, p + selection.row * dy)) }
    state.pieces.forEach { piece ->
        val center = Offset(p + piece.square.col * dx, p + piece.square.row * dy); val pieceColor = if (piece.side == Side.Red) red else ink
        drawCircle(Color(0xFFFFF6E6), radius = minOf(dx, dy) * .405f, center = center); drawCircle(pieceColor, radius = minOf(dx, dy) * .405f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(2.2f))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (piece.side == Side.Red) 0xFFB7362C.toInt() else 0xFF263229.toInt(); textSize = minOf(dx, dy) * .48f; textAlign = Paint.Align.CENTER; typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD) }
        val baseline = center.y - (paint.ascent() + paint.descent()) / 2f; drawContext.canvas.nativeCanvas.drawText(piece.glyph, center.x, baseline, paint)
    }
}
