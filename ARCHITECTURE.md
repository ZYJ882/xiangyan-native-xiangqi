# 象研局 Android Native：架构约定

## 模块边界

应用采用单一 `app` 模块，内部以包边界保持职责清晰。`ui` 包只负责 Compose 渲染、触控与无障碍语义；`game` 包保存对局状态、走法记录及回合机；`engine` 包负责配置、调度和 JNI；`native` 目录保存 C++ 棋局、走法生成与可中断搜索。首屏不会同步加载原生库或执行引擎搜索，AI 引擎仅在用户选择“人机对弈”后按需创建。

| 模块/包 | 主要对象 | 不负责的事情 |
| --- | --- | --- |
| `ui` | `XiangqiApp`、`BoardCanvas`、`GameViewModel` | 不直接调用 JNI，不解析引擎输出。 |
| `game` | `GameState`、`Piece`、`Move`、`BoardRules` | 不创建线程，不做深度搜索。 |
| `engine` | `NativeEngine`、`EngineController`、`EngineProfile` | 不保存 Compose 状态，不绘制棋盘。 |
| `cpp` | `Board`、`SearchEngine`、JNI bridge | 不操作 Android View 或 UI 回调。 |

## 领域模型

棋盘使用 9 列 × 10 行，行列均以零为基。`Piece` 包含阵营、类型与位置；`Move` 是不可变的起止点；`GameState` 保存棋子列表、当前回合、选中位置、历史记录与 AI 的瞬时状态。界面仅渲染状态快照；所有用户操作经 `GameViewModel` 进入规则校验，再决定是否让引擎思考。

| 状态 | 允许转换 | 防护规则 |
| --- | --- | --- |
| `HumanTurn` | `Thinking` | 只有合法落子会触发。 |
| `Thinking(token)` | `HumanTurn`、`Finished` | 只有 token 一致的引擎结果可回写。 |
| `Finished` | `HumanTurn` | 新局会创建新棋局与新 token。 |

## 引擎协议

`NativeEngine.findBestMove(fen, profile)` 在后台线程工作；C++ 搜索按 `movetimeMs` 截止，或观察取消原子标记。`EngineController` 为每次搜索生成 token。发生新局、悔棋、再落子、退出页面、进入后台或切换档位时，它先取消旧任务，再忽略任何旧 token 的结果。这个原型实现的是确定性、可中断的 Alpha-Beta 基础搜索，不包含第三方引擎或神经网络权重。

| 档位 | 每步预算 | 线程目标 | 哈希目标 | 设计目的 |
| --- | ---: | ---: | ---: | --- |
| `Starter` | 120ms | 1 | 16MB | 学习与即时反馈。 |
| `Casual` | 240ms | 1 | 32MB | 初级对手。 |
| `Standard` | 550ms | 2 | 64MB | 默认平衡档。 |
| `Advanced` | 1200ms | 2–4 | 128MB | 强棋力对弈。 |
| `Analysis` | 3500ms | 2–4 | 256MB | 用户主动触发的复盘。 |

> 本工程使用自有原型搜索代码，未打包 Pikafish 代码或 NNUE 权重。接入第三方引擎前，应独立完成其代码和模型权重的许可审查。

## 启动预算

`Application` 中不执行初始化；`MainActivity` 只建立主题、ViewModel 和棋盘首帧。`NativeEngine` 是惰性对象，且默认使用规则引擎演示模式，直到点击“AI 走一步”才加载原生库。正式发布时应将 `StartupMode.COLD` 的 Macrobenchmark、Baseline Profile 和启动 Profile 写入 CI 回归。
