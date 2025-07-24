# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

thorvg.android is an Android library that integrates the ThorVG (Thor Vector Graphics) engine to provide enhanced Lottie animation rendering on Android. It serves as a drop-in replacement for Lottie animations with potentially better performance through the ThorVG rendering engine.

## Build Commands

### Building the Library

The build process involves cross-compiling ThorVG for Android and then building the Android library:

1. **Setup cross-build environment** (for arm64-v8a):
   ```bash
   gradle lottie:setupCrossBuild -Pabi=1
   ```
   For x86_64, use `-Pabi=2`

2. **Build ThorVG native library**:
   ```bash
   ./build_libthorvg.sh
   ```

3. **Copy the built library** (for arm64-v8a):
   ```bash
   ./copy_libthorvg.sh 1
   ```
   For x86_64, use `./copy_libthorvg.sh 2`

4. **Build the Android library**:
   ```bash
   ./gradlew :lottie:assembleRelease
   ```

5. **Build and run the sample app**:
   ```bash
   ./gradlew :sample:installDebug
   ```

### Running Tests

```bash
./gradlew :lottie:testDebugUnitTest
```

### Linting

```bash
./gradlew :lottie:lint
```

## Architecture

The project follows a multi-layer architecture:

### Layer Structure
```
Android App Layer (Java/Kotlin)
    ↓
JNI Bridge Layer (C++)
    ↓
ThorVG Engine (C++)
```

### Key Components

1. **Android API Layer** (`/lottie/src/main/java/com/airbnb/lottie/`):
   - `LottieAnimationView`: Main view component for displaying animations
   - `LottieDrawable`: Drawable implementation for animations
   - `LottieComposition`: Represents loaded animation data

2. **JNI Layer** (`/lottie/src/main/cpp/`):
   - `lottie-libs.cpp`: Main JNI entry point, handles Java-to-native calls
   - `LottieData.cpp/h`: Manages animation data in native memory
   - Links with pre-built ThorVG static library

3. **ThorVG Integration**:
   - Pre-built static library located at `/lottie/src/main/cpp/libs/{architecture}/libthorvg.a`
   - Cross-compilation configuration in `/thorvg/cross/`
   - Built using Meson build system

### Build System Integration

The project uses multiple build systems that work together:
- **Gradle**: Main Android build system
- **CMake**: Builds the JNI wrapper and links ThorVG
- **Meson**: Cross-compiles ThorVG for Android (via shell scripts)

### Architecture-specific Builds

Supports two architectures:
- `arm64-v8a` (ABI=1): For ARM 64-bit devices
- `x86_64` (ABI=2): For x86 64-bit emulators/devices

When modifying native code or build scripts, ensure changes work for both architectures.

## Development Notes

- When modifying JNI code, the method signatures must match exactly between Java and C++
- The ThorVG library is pre-built and stored in the repository; rebuilding is only needed when upgrading ThorVG
- The minimum SDK version is 14, but be mindful of API compatibility when adding features
- The project uses AndroidX libraries