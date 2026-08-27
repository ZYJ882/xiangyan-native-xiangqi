package com.xiangyan.nativeapp.engine

enum class EngineProfile(val label: String, val timeBudgetMs: Int, val depthCap: Int, val threads: Int, val hashMb: Int) {
    Starter("入门", 120, 7, 1, 16), Casual("初级", 240, 10, 1, 32), Standard("中级", 550, 14, 2, 64), Advanced("高级", 1200, 18, 4, 128), Analysis("分析", 3500, 22, 4, 256)
}
