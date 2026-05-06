#!/bin/bash

set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
THORVG_DIR="$ROOT_DIR/thorvg"

abi="${1:-all}"

copy_one() {
    abi_dir="$1"
    lib_source="$THORVG_DIR/build-${abi_dir}/src/libthorvg-1.a"

    if [ ! -d "$THORVG_DIR/lib" ]; then
        echo "lib directory doesn't exist in thorvg directory. Creating lib directory..."
        mkdir -p "$THORVG_DIR/lib"
        echo "lib directory created successfully."
    fi

    if [ ! -d "$THORVG_DIR/lib/$abi_dir" ]; then
        echo "$abi_dir directory doesn't exist in thorvg/lib directory. Creating $abi_dir directory..."
        mkdir -p "$THORVG_DIR/lib/$abi_dir"
        echo "$abi_dir directory created successfully."
    fi

    if [ ! -f "$lib_source" ]; then
        echo "libthorvg-1.a not found: $lib_source"
        echo "Run ./build_libthorvg.sh ${abi_dir} first."
        exit 1
    fi

    cp "$lib_source" "$THORVG_DIR/lib/$abi_dir/"
    echo "libthorvg-1.a copied to thorvg/lib/$abi_dir/"
}

case "$abi" in
    arm64-v8a)
        copy_one "arm64-v8a"
        ;;
    x86_64)
        copy_one "x86_64"
        ;;
    all)
        copy_one "arm64-v8a"
        copy_one "x86_64"
        ;;
    *)
        echo "Unsupported ABI argument: $abi"
        echo "Use arm64-v8a, x86_64, or all."
        exit 1
        ;;
esac
