package com.xiangyan.nativeapp.ui

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiangyan.nativeapp.engine.EngineProfile
import com.xiangyan.nativeapp.game.GamePhase
import com.xiangyan.nativeapp.game.GameState
import kotlin.math.cos
import kotlin.math.sin

private val Paper = Color(0xFFF7F1E4); private val Ink = Color(0xFF1E2620); private val Vermilion = Color(0xFFB7362C); private val Jade = Color(0xFF547A5C); private val Sand = Color(0xFFE9E1D2)

@Composable
fun XiangqiApp() {
    val application = LocalContext.current.applicationContext as Application
    val vm: GameViewModel = viewModel(factory = remember(application) { GameViewModel.factory(application) })
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Vermilion, onPrimary = Color.White, background = Paper, surface = Paper, onSurface = Ink)) {
        if (settingsOpen) SettingsScreen(vm.profile, vm::selectProfile) { settingsOpen = false }
        else GameScreen(vm.state, vm.profile, vm::onSquareTap, vm::startRandomGame, vm::pause, vm::resume, vm::stop) { settingsOpen = true }
    }
}

@Composable
private fun GameScreen(
    state: GameState,
    profile: EngineProfile,
    onSquareTap: (com.xiangyan.nativeapp.game.Square) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
) = Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
    Column(modifier = Modifier.statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp)) {
        Header(onOpenSettings)
        Spacer(Modifier.height(19.dp))
        StatusStrip(state.message, state.phase)
        Spacer(Modifier.height(16.dp))
        BoardCanvas(state, onSquareTap)
        Spacer(Modifier.height(17.dp))
        GameControls(state.phase, onStart, onPause, onResume, onStop)
        Spacer(Modifier.height(16.dp))
        EnginePanel(profile, state)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
        FlatMark(Modifier.size(34.dp)); Spacer(Modifier.width(9.dp))
        Column { Text("象研局", fontFamily = FontFamily.Serif, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink); Text("NATIVE XIANGQI / OFFLINE", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, color = Jade) }
    }
    IconButton(onClick = onOpenSettings) { GearGlyph(Modifier.size(25.dp)) }
}

@Composable
private fun FlatMark(modifier: Modifier = Modifier) = Canvas(modifier) {
    val c = center; val r = size.minDimension * .46f
    drawCircle(Paper, r, c); drawCircle(Vermilion, r, c, style = Stroke(width = size.minDimension * .10f))
    val step = size.minDimension * .18f
    for (index in 0..2) { drawLine(Vermilion, Offset(c.x - step, c.y + (index - 1) * step), Offset(c.x + step, c.y + (index - 1) * step), 2.2f); drawLine(Vermilion, Offset(c.x + (index - 1) * step, c.y - step), Offset(c.x + (index - 1) * step, c.y + step), 2.2f) }
    drawCircle(Vermilion, size.minDimension * .12f, c)
}

@Composable
private fun GearGlyph(modifier: Modifier = Modifier) = Canvas(modifier) {
    val c = center; val r = size.minDimension * .25f
    for (i in 0 until 8) { val a = i * Math.PI / 4; val from = Offset(c.x + cos(a).toFloat() * r, c.y + sin(a).toFloat() * r); val to = Offset(c.x + cos(a).toFloat() * r * 1.45f, c.y + sin(a).toFloat() * r * 1.45f); drawLine(Ink, from, to, 3.2f) }
    drawCircle(Ink, r, c, style = Stroke(3.2f)); drawCircle(Ink, r * .32f, c)
}

@Composable private fun StatusStrip(message: String, phase: GamePhase) = Surface(color = if (phase == GamePhase.Thinking) Ink else Sand, shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxWidth()) {
    Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (phase == GamePhase.Thinking) "●" else "◉", color = if (phase == GamePhase.Thinking) Color(0xFFE8B3A5) else Vermilion, fontSize = 11.sp); Spacer(Modifier.width(9.dp)); Text(message, color = if (phase == GamePhase.Thinking) Color(0xFFFFF6E6) else Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
}

@Composable
private fun GameControls(phase: GamePhase, onStart: () -> Unit, onPause: () -> Unit, onResume: () -> Unit, onStop: () -> Unit) = Column(Modifier.fillMaxWidth()) {
    when (phase) {
        GamePhase.Ready, GamePhase.Stopped, GamePhase.Finished -> {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(3.dp), contentPadding = PaddingValues(vertical = 14.dp), colors = ButtonDefaults.buttonColors(containerColor = Vermilion)) { Text("开始对局 · 随机先手", fontWeight = FontWeight.Bold) }
            Text("随机决定你执红或执黑；红方按正式棋规先行。", color = Jade, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
        }
        GamePhase.Paused -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onResume, modifier = Modifier.weight(1f), shape = RoundedCornerShape(3.dp), colors = ButtonDefaults.buttonColors(containerColor = Jade)) { Text("继续对局", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f), shape = RoundedCornerShape(3.dp)) { Text("停止对局", color = Vermilion) }
        }
        GamePhase.HumanTurn, GamePhase.Thinking -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onPause, modifier = Modifier.weight(1f), shape = RoundedCornerShape(3.dp), colors = ButtonDefaults.buttonColors(containerColor = Jade)) { Text("暂停", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f), shape = RoundedCornerShape(3.dp)) { Text("停止", color = Vermilion) }
        }
    }
}

@Composable private fun EnginePanel(profile: EngineProfile, state: GameState) = Surface(color = Ink, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("本地引擎预算", color = Color(0xFFE8B3A5), fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold); Text(profile.label, color = Color.White, fontFamily = FontFamily.Serif, fontSize = 27.sp, fontWeight = FontWeight.Bold) }; Text(if (state.phase == GamePhase.Ready) "尚未加载" else "JNI · C++", color = Color(0xFFBBD2BD), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth()) { Metric("时限", "${profile.timeBudgetMs}ms", Modifier.weight(1f)); Metric("深度", "${profile.depthCap}", Modifier.weight(1f)); Metric("线程", "${profile.threads}", Modifier.weight(1f)); Metric("Hash", "${profile.hashMb}M", Modifier.weight(1f)) }
        Spacer(Modifier.height(13.dp)); Text("AI 强度和完整规则请在右上角设置中查看。", color = Color(0xFFBEC9BE), fontSize = 10.sp)
    }
}

@Composable private fun Metric(label: String, value: String, modifier: Modifier) = Column(modifier) { Text(label, color = Color(0xFF9FAE9E), fontSize = 9.sp); Text(value, color = Color(0xFFFFF6E6), fontFamily = FontFamily.Serif, fontSize = 17.sp, fontWeight = FontWeight.Bold) }

@Composable
private fun SettingsScreen(profile: EngineProfile, onProfile: (EngineProfile) -> Unit, onBack: () -> Unit) = Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.statusBarsPadding().fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("‹ 返回", color = Vermilion, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f)); Text("设置", color = Ink, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp); Spacer(Modifier.weight(1f)); Spacer(Modifier.width(52.dp))
        }
        TabRow(selectedTabIndex = tab, containerColor = Paper, contentColor = Vermilion) { listOf("AI 强度", "象棋规则", "软件规则").forEachIndexed { index, label -> Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label, fontSize = 12.sp, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium) }) } }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp)) {
            when (tab) { 0 -> StrengthSettings(profile, onProfile); 1 -> XiangqiRules(); else -> AppRules() }
        }
    }
}

@Composable private fun StrengthSettings(selected: EngineProfile, onSelected: (EngineProfile) -> Unit) = Column {
    SettingIntro("以时间预算定义强度", "强度影响每步思考时限、搜索深度、线程与置换表目标。切换强度不会自动开局；AI 正在思考时切换将使对局暂停。")
    EngineProfile.entries.forEach { item -> Surface(color = if (selected == item) Color(0xFFFFE8E2) else Color(0xFFFCF8F0), shape = RoundedCornerShape(3.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { FilterChip(selected = selected == item, onClick = { onSelected(item) }, label = { Text(item.label, fontWeight = FontWeight.Bold) }); Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(profileDescription(item), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text("${item.timeBudgetMs}ms · 深度 ${item.depthCap} · ${item.threads} 线程 · ${item.hashMb}MB", color = Jade, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp)) } }
    } }
}

private fun profileDescription(profile: EngineProfile) = when (profile) { EngineProfile.Starter -> "即时反馈，适合教学"; EngineProfile.Casual -> "可利用但有计划感"; EngineProfile.Standard -> "棋力、发热与节奏的默认平衡"; EngineProfile.Advanced -> "性能模式下的强棋力对弈"; EngineProfile.Analysis -> "用户主动触发的长时拆棋" }

@Composable private fun XiangqiRules() = Column {
    SettingIntro("中国象棋规则", "棋盘为九路十线，中间的楚河汉界将双方分开。红方按正式规则先行；目标是通过将军与将死迫使对方无法合法应对。")
    RuleSection("行棋与胜负", listOf("双方轮流走一着，己方棋子不能落在己方棋子所在位置。", "将军时必须立即应将；无法应将为将死。未被将军但无任何合法着法为困毙，亦判负。", "将（帅）不可走出九宫，且两将（帅）不能在同一路直线无子相隔时照面。", "长将、长捉与三次重复局面属于常见竞赛裁判规则；实际赛事应以赛事规程为准。"))
    RuleSection("将 / 帅", listOf("每次横走或竖走一格，只能在本方九宫内活动。", "红方称帅，黑方称将；被对方攻击时称为将军。"))
    RuleSection("士 / 仕", listOf("每次斜走一格，只能在本方九宫内活动。"))
    RuleSection("象 / 相", listOf("每次斜走两格，活动范围不能过河。", "斜线中点有棋子时称塞象眼，不能走。"))
    RuleSection("马", listOf("走日字：先横或竖一格，再斜走一格。", "先走方向紧邻的一格有棋子时称蹩马腿，不能走。"))
    RuleSection("车", listOf("横走或竖走任意格，路径上不能有其他棋子阻挡。"))
    RuleSection("炮", listOf("不吃子时走法与车相同，路径不得有阻挡。", "吃子时，炮与目标之间必须恰好隔着一枚任意阵营棋子，称炮架。"))
    RuleSection("兵 / 卒", listOf("未过河时只能向前走一格；红兵向上，黑卒向下。", "过河后仍不可后退，但可向前或横走一格。"))
}

@Composable private fun AppRules() = Column {
    SettingIntro("本软件的对局规则", "本应用为离线人机原型。所有开始、暂停和停止均由用户显式触发，应用不会在启动后自行运行 AI。")
    RuleSection("开始与先手", listOf("点击“开始对局 · 随机先手”后，系统随机分配你执红或执黑。", "红方遵从中国象棋规则先行；若你执黑，AI 会在点击开始后走红方第一步。", "未点击开始时，棋盘仅用于展示；不会加载引擎、不会搜索，也不能落子。"))
    RuleSection("暂停与停止", listOf("点击暂停会立即取消当前 AI 搜索并冻结棋盘；点击继续才会恢复。", "点击停止会取消搜索并结束本局，保留最后棋盘供查看；点击开始后会开启一局新的随机先手对局。"))
    RuleSection("AI 强度", listOf("设置中的 AI 强度改变时间预算、搜索深度、线程与哈希表建议值。", "AI 思考时改变强度会先暂停，避免旧强度结果在新设置下落子。", "分析档仅适合用户主动拆棋；普通对局采用单主变化策略。"))
    RuleSection("当前规则引擎边界", listOf("当前版本已实现棋子的基础移动、吃子、随机先后手与可取消 AI 调度。", "将军、应将、将死、困毙、长将长捉及重复局面的赛事级自动裁决尚未完成；对局中请以规则说明为准并手动停止有争议局面。"))
}

@Composable private fun SettingIntro(title: String, body: String) = Column(Modifier.padding(bottom = 18.dp)) { Text(title, color = Ink, fontFamily = FontFamily.Serif, fontSize = 28.sp, fontWeight = FontWeight.Black); Text(body, color = Jade, fontSize = 12.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 7.dp)) }
@Composable private fun RuleSection(title: String, items: List<String>) = Surface(color = Color(0xFFFCF8F0), shape = RoundedCornerShape(3.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) { Column(Modifier.padding(15.dp)) { Text(title, color = Vermilion, fontFamily = FontFamily.Serif, fontSize = 18.sp, fontWeight = FontWeight.Bold); items.forEach { Text("• $it", color = Ink, fontSize = 12.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp)) } } }
