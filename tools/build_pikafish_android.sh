#!/usr/bin/env bash
set -euo pipefail

# 从仓库内固定的 Pikafish 源码构建 Android UCI 二进制；不下载或覆盖 NNUE 权重。
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NDK="${ANDROID_NDK_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}/ndk/27.3.13750724}"
BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
SRC="$ROOT/third_party/Pikafish/src"
OUT="$ROOT/app/src/main/jniLibs"

test -x "$BIN/aarch64-linux-android29-clang++" || { echo "未找到 Android NDK 27.3.13750724：$NDK" >&2; exit 1; }
export PATH="$BIN:$PATH"

build() {
  local arch="$1" abi="$2"
  make -C "$SRC" clean >/dev/null
  make -C "$SRC" -j"$(getconf _NPROCESSORS_ONLN)" build ARCH="$arch" COMP=ndk
  mkdir -p "$OUT/$abi"
  cp "$SRC/pikafish" "$OUT/$abi/libpikafish.so"
}

build armv8 arm64-v8a
build x86-64 x86_64
echo "Pikafish Android UCI binaries updated in $OUT"
