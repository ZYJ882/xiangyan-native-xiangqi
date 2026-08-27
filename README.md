# 象研局 Android Native

这是一个可由 Android Studio 打开的**原生安卓工程**。它使用 Kotlin、Jetpack Compose、CMake 与 C++/JNI，当前版本为 **0.3.0（versionCode 3）**，产物是仅供测试安装的 `app-debug.apk`。

## 已实现功能

应用提供可触摸的 9×10 中国象棋棋盘、简约扁平化自适应图标、右上角设置、难度档位、每步时间预算与显式对局控制。最近一步的**起点**以玉绿色空心圈高亮，**落点**以朱砂色填充与描边高亮，方便快速确认刚移动的棋子。启动后不会自动进入对局：用户点击“开始对局”才随机分配执红或执黑，红方按棋规先行。暂停与停止都会立即取消搜索；停止后需点击开始才能开启新局。界面线程不直接运行深度搜索；每次 AI 请求均在单独的 native worker 中按 token 管理，新的走子、暂停、停止或切换档位都会取消旧搜索并丢弃陈旧结果。冷启动路径只加载 Compose UI，原生库在第一次 AI 走子时惰性加载。

| 项目 | 当前状态 |
| --- | --- |
| Android API | `minSdk 26`、`targetSdk 35`、`compileSdk 35` |
| 界面 | Kotlin + Jetpack Compose |
| 原生层 | C++20 + CMake + JNI |
| 构建 ABI | `arm64-v8a`、`x86_64` |
| AI 原型 | 可取消的迭代加深 Alpha-Beta 搜索骨架，设置内选择强度 |
| APK 验证 | 已成功执行 `:app:assembleDebug` |

## 在 Android Studio 中运行

使用 Android Studio 的 **Open** 操作选择本目录。随后安装 Android API 35、NDK `27.3.13750724` 和 CMake `3.22.1`，等待 Gradle 同步完成后，选择物理安卓设备或模拟器执行 `app` 配置。

在命令行构建时，先设置 `ANDROID_SDK_ROOT`，然后执行：

```bash
./gradlew :app:assembleDebug
```

生成的 APK 路径为 `app/build/outputs/apk/debug/app-debug.apk`。这是标准调试签名包；如需对外分发，应配置自己的 release signing key，再执行 `:app:assembleRelease`。

## 重要边界

当前 C++ 搜索代码是为验证应用架构、JNI 粗粒度调用和取消语义而编写的原型，并非强引擎。下一阶段可以将 `xiangqi_engine.cpp` 换成经过规则测试的走法生成、置换表、静态搜索与 NNUE 评估实现；接入任何外部引擎或 NNUE 权重前，须分别核验其代码与权重许可。
