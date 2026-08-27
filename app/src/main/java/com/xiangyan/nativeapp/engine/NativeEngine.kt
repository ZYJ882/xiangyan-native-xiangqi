package com.xiangyan.nativeapp.engine

import android.content.Context
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * 单进程 UCI 会话。二进制与 50MB NNUE 权重均在用户开始人机对局后才首次访问，
 * 因而不处于冷启动路径。UI 线程绝不直接等待 UCI 输出。
 */
private class PikafishSession(private val context: Context) {
    private val lock = Any()
    private var process: Process? = null
    private var input: BufferedReader? = null
    private var output: BufferedWriter? = null

    fun bestMove(fen: String, profile: EngineProfile): String? {
        ensureStarted()
        configure(profile)
        send("position fen $fen")
        send("go movetime ${profile.timeBudgetMs} depth ${profile.depthCap}")
        while (true) {
            val line = input?.readLine() ?: return null
            if (line.startsWith("bestmove ")) return line.split(Regex("\\s+")).getOrNull(1)?.takeIf { it != "(none)" && it != "0000" }
        }
    }

    fun stop() { runCatching { send("stop") } }
    fun newGame() { runCatching { send("ucinewgame"); send("isready"); waitFor("readyok") } }

    fun close() {
        runCatching { send("quit") }
        process?.destroy()
        input?.close(); output?.close()
        process = null; input = null; output = null
    }

    private fun ensureStarted() {
        if (process?.isAlive == true) return
        val workDir = File(context.filesDir, "pikafish").apply { mkdirs() }
        val network = File(workDir, "pikafish.nnue")
        if (!network.exists() || network.length() == 0L) {
            context.assets.open("pikafish.nnue").use { source -> FileOutputStream(network).use { source.copyTo(it) } }
        }
        val executable = File(context.applicationInfo.nativeLibraryDir, "libpikafish.so")
        require(executable.exists()) { "Pikafish 引擎文件缺失" }
        val started = ProcessBuilder(executable.absolutePath).directory(workDir).redirectErrorStream(true).start()
        process = started
        input = BufferedReader(InputStreamReader(started.inputStream))
        output = BufferedWriter(OutputStreamWriter(started.outputStream))
        send("uci"); waitFor("uciok")
        send("setoption name EvalFile value ${network.absolutePath}")
        send("isready"); waitFor("readyok")
    }

    private fun configure(profile: EngineProfile) {
        send("setoption name Threads value ${profile.threads}")
        send("setoption name Hash value ${profile.hashMb}")
        send("setoption name MultiPV value 1")
        send("setoption name Move Overhead value 35")
        send("isready"); waitFor("readyok")
    }

    private fun send(command: String) = synchronized(lock) { output?.apply { write(command); newLine(); flush() } ?: error("Pikafish 尚未启动") }
    private fun waitFor(marker: String) { while (true) if ((input?.readLine() ?: error("Pikafish 异常退出")).trim() == marker) return }
}

class EngineController(context: Context) {
    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "pikafish-uci").apply { priority = Thread.NORM_PRIORITY - 1 } }
    private val token = AtomicLong(0L)
    private var session: PikafishSession? = null

    fun request(fen: String, profile: EngineProfile, onMove: (token: Long, uci: String?) -> Unit): Long {
        val requestToken = token.incrementAndGet()
        session?.stop()
        executor.execute {
            val result = runCatching { (session ?: PikafishSession(appContext).also { session = it }).bestMove(fen, profile) }.getOrNull()
            if (token.get() == requestToken) onMove(requestToken, result)
        }
        return requestToken
    }

    fun newGame() { token.incrementAndGet(); session?.stop(); executor.execute { session?.newGame() } }
    fun cancel() { token.incrementAndGet(); session?.stop() }
    fun close() { cancel(); executor.execute { session?.close() }; executor.shutdownNow() }
}
