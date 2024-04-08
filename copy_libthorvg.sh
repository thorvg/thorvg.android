#!/bin/bash

abi=$1

# Check if lib directory exists in thorvg directory
if [ ! -d "thorvg/lib" ]; then
    echo "lib directory doesn't exist in thorvg directory. Creating lib directory..."
    mkdir thorvg/lib
    echo "lib directory created successfully."
fi

# Check ABI and copy libthorvg.a file accordingly
if [ "$abi" -eq 1 ]; then
    abi_dir="arm64-v8a"
elif [ "$abi" -eq 2 ]; then
    abi_dir="x86_64"
else
    echo "Unsupported ABI."
    exit 1
fi

# Check if ABI directory exists in thorvg/lib directory
if [ ! -d "thorvg/lib/$abi_dir" ]; then
    echo "$abi_dir directory doesn't exist in thorvg/lib directory. Creating $abi_dir directory..."
    mkdir "thorvg/lib/$abi_dir"
    echo "$abi_dir directory created successfully."
fi

# Copy libthorvg.a file to the appropriate directory
if [ "$abi" -eq 1 ]; then
    cp "thorvg/build/src/libthorvg.a" "thorvg/lib/arm64-v8a/"
    echo "libthorvg.a copied to thorvg/lib/arm64-v8a/"
elif [ "$abi" -eq 2 ]; then
    cp "thorvg/build/src/libthorvg.a" "thorvg/lib/x86_64/"
    echo "libthorvg.a copied to thorvg/lib/x86_64/"
fi