# 象研局 Android Native

这是一个可由 Android Studio 打开的**原生安卓工程**。它使用 Kotlin、Jetpack Compose 与 Pikafish UCI 原生引擎，当前版本为 **0.4.3（versionCode 7）**，产物是仅供测试安装的 `app-debug.apk`。

## 已实现功能

应用提供可触摸的 9×10 中国象棋棋盘、简约扁平化自适应图标、右上角设置、难度档位、每步时间预算与显式对局控制。无论随机分配执红或执黑，用户始终位于**棋盘下方**；棋盘上方固定为对手，并用红黑标签标识双方。最近一步的**起点**以玉绿色空心圈高亮，**落点**以朱砂色填充与描边高亮，方便快速确认刚移动的棋子。应用层会检测将军、将帅照面与无合法应对；出现将军时必须应将，受将军的将帅还会显示朱砂色外环。设置的 AI 强度会保存在本地，重启后恢复上一次选择。启动后不会自动进入对局：用户点击“开始对局”才随机分配执红或执黑，红方按棋规先行。暂停与停止都会立即取消搜索；停止后需点击开始才能开启新局。界面线程不直接运行深度搜索；每次 AI 请求均在单独的 UCI worker 中按 token 管理，新的走子、暂停、停止或切换档位都会取消旧搜索并丢弃陈旧结果。冷启动路径只加载 Compose UI；Pikafish 二进制和 NNUE 权重只会在首次 AI 走子后加载。

| 项目 | 当前状态 |
| --- | --- |
| Android API | `minSdk 26`、`targetSdk 35`、`compileSdk 35` |
| 界面 | Kotlin + Jetpack Compose |
| 原生层 | Pikafish UCI 子进程；`arm64-v8a`、`x86_64` 原生二进制 |
| 构建 ABI | `arm64-v8a`、`x86_64` |
| AI | Pikafish 迭代加深搜索 + NNUE；设置实际传递 Threads、Hash、MultiPV=1、时间/深度上限 |
| APK 验证 | 已成功执行 `:app:assembleDebug` |

## 在 Android Studio 中运行

使用 Android Studio 的 **Open** 操作选择本目录。随后安装 Android API 35 与 NDK `27.3.13750724`，等待 Gradle 同步完成后，选择物理安卓设备或模拟器执行 `app` 配置。仓库已包含 `arm64-v8a`、`x86_64` 的 Pikafish 预构建 UCI 二进制；需要从固定上游源码重新生成时，可先设置 `ANDROID_SDK_ROOT`，再执行 `tools/build_pikafish_android.sh`。

在命令行构建时，先设置 `ANDROID_SDK_ROOT`，然后执行：

```bash
./gradlew :app:assembleDebug
```

生成的 APK 路径为 `app/build/outputs/apk/debug/app-debug.apk`。这是标准调试签名包；如需对外分发，应配置自己的 release signing key，再执行 `:app:assembleRelease`。

## 重要边界

本工程将 Pikafish 代码与应用一起按 GPL-3.0 开源，并于 `third_party/Pikafish/` 提供固定上游源码。APK 中的官方 `pikafish.nnue` 权重仅限合法、**非商业**用途；不得用于在线作弊。权重来源、SHA-256、上游版本与再分发限制详见 `NOTICE.md`。当前 Kotlin 交互层仍处于原型阶段；玩家移动采用基础走法校验，完整的将军、将死、困毙及循环棋规裁决应作为下一阶段补齐项目。
