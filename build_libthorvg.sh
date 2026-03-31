#!/bin/bash

set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
ABI_ARG="${1:-all}"

build_one() {
    abi_dir="$1"
    cross_file="$ROOT_DIR/lottie/build/tmp/android_cross_${abi_dir}.txt"
    build_dir="build-${abi_dir}"

    if [ ! -f "$cross_file" ]; then
        echo "Cross build file not found: $cross_file"
        echo "Run the setupCrossBuild Gradle task first for your host shell."
        exit 1
    fi

    cd "$ROOT_DIR/thorvg"
    rm -rf "$build_dir"
    meson setup "$build_dir" -Dloaders="lottie, webp, jpg, png" --cross-file "$cross_file" -Ddefault_library=static
    ninja -C "$build_dir"
}

case "$ABI_ARG" in
    arm64-v8a)
        build_one "arm64-v8a"
        ;;
    x86_64)
        build_one "x86_64"
        ;;
    all)
        build_one "arm64-v8a"
        build_one "x86_64"
        ;;
    *)
        echo "Unsupported ABI argument: $ABI_ARG"
        echo "Use arm64-v8a, x86_64, or all."
        exit 1
        ;;
esac
