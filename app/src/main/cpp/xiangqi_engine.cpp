#include <jni.h>
#include <atomic>
#include <chrono>
#include <cstdint>
#include <limits>

namespace {
std::atomic<bool> cancelled{false};

// 原型树：真实版本将把这一层替换为合法着生成、置换表与 NNUE 增量评估。
// 这里仍保留迭代加深、Alpha-Beta 窗口、截止时间和取消点，验证 JNI/调度边界。
uint64_t mix(uint64_t value) {
    value ^= value >> 30;
    value *= 0xbf58476d1ce4e5b9ULL;
    value ^= value >> 27;
    value *= 0x94d049bb133111ebULL;
    return value ^ (value >> 31);
}

int static_eval(uint64_t position) {
    return static_cast<int>((mix(position) >> 33) % 801) - 400;
}

int alpha_beta(uint64_t position, int depth, int alpha, int beta,
               std::chrono::steady_clock::time_point deadline) {
    if (cancelled.load(std::memory_order_relaxed) || std::chrono::steady_clock::now() >= deadline) {
        return static_eval(position);
    }
    if (depth <= 0) return static_eval(position);

    constexpr int branch_factor = 4;
    int best = std::numeric_limits<int>::min() + 1;
    // 简单 hash move 顺序，让相同局面在相同条件下保持可复现。
    const int start = static_cast<int>(mix(position) % branch_factor);
    for (int offset = 0; offset < branch_factor; ++offset) {
        const int move = (start + offset) % branch_factor;
        const uint64_t child = mix(position ^ (0x9e3779b97f4a7c15ULL + static_cast<uint64_t>(move + 1) * 0x100000001b3ULL));
        const int score = -alpha_beta(child, depth - 1, -beta, -alpha, deadline);
        if (cancelled.load(std::memory_order_relaxed) || std::chrono::steady_clock::now() >= deadline) return score;
        if (score > best) best = score;
        if (best > alpha) alpha = best;
        if (alpha >= beta) break;
    }
    return best;
}
}

extern "C" JNIEXPORT jint JNICALL
Java_com_xiangyan_nativeapp_engine_NativeEngine_findBestCandidate(
        JNIEnv*, jobject, jlong fingerprint, jint candidate_count, jint move_time_ms, jint depth_cap) {
    cancelled.store(false, std::memory_order_relaxed);
    if (candidate_count <= 0) return -1;

    const auto deadline = std::chrono::steady_clock::now() + std::chrono::milliseconds(move_time_ms > 0 ? move_time_ms : 1);
    const int safe_depth = depth_cap < 1 ? 1 : (depth_cap > 18 ? 18 : depth_cap);
    int chosen = 0;
    int best_score = std::numeric_limits<int>::min() + 1;

    // 迭代加深：每层都产生完整候选，从而让停止/超时之后仍有可返回的走法。
    for (int depth = 1; depth <= safe_depth; ++depth) {
        int depth_best = chosen;
        int depth_score = std::numeric_limits<int>::min() + 1;
        for (int candidate = 0; candidate < candidate_count; ++candidate) {
            if (cancelled.load(std::memory_order_relaxed) || std::chrono::steady_clock::now() >= deadline) return chosen;
            const uint64_t child = mix(static_cast<uint64_t>(fingerprint) ^ (static_cast<uint64_t>(candidate + 1) * 0x517cc1b727220a95ULL));
            const int score = -alpha_beta(child, depth - 1, -5000, 5000, deadline);
            if (score > depth_score) { depth_score = score; depth_best = candidate; }
        }
        if (!cancelled.load(std::memory_order_relaxed) && std::chrono::steady_clock::now() < deadline) {
            chosen = depth_best;
            best_score = depth_score;
        } else {
            break;
        }
    }
    (void)best_score;
    return chosen;
}

extern "C" JNIEXPORT void JNICALL
Java_com_xiangyan_nativeapp_engine_NativeEngine_cancelSearch(JNIEnv*, jobject) {
    cancelled.store(true, std::memory_order_relaxed);
}
