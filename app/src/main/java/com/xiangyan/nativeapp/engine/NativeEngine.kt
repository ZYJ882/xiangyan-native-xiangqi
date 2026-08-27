package com.xiangyan.nativeapp.engine

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/** JNI 边界维持粗粒度：只提交局面摘要、候选数和时间预算，不逐节点跨语言回调。 */
object NativeEngine {
    init { System.loadLibrary("xiangqi_engine") }
    external fun findBestCandidate(boardFingerprint: Long, candidateCount: Int, moveTimeMs: Int, depthCap: Int): Int
    external fun cancelSearch()
}

class EngineController {
    private val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "xiangqi-engine").apply { priority = Thread.NORM_PRIORITY - 1 } }
    private val token = AtomicLong(0L)

    fun request(boardFingerprint: Long, candidateCount: Int, profile: EngineProfile, onMove: (token: Long, index: Int) -> Unit): Long {
        val requestToken = token.incrementAndGet()
        NativeEngine.cancelSearch()
        executor.execute {
            val result = NativeEngine.findBestCandidate(boardFingerprint, candidateCount, profile.timeBudgetMs, profile.depthCap)
            if (token.get() == requestToken) onMove(requestToken, result)
        }
        return requestToken
    }

    fun cancel() { token.incrementAndGet(); NativeEngine.cancelSearch() }
    fun close() { cancel(); executor.shutdownNow() }
}
