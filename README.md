[![CodeFactor](https://www.codefactor.io/repository/github/thorvg/thorvg.android/badge)](https://www.codefactor.io/repository/github/thorvg/thorvg.android)
[![Discord](https://img.shields.io/badge/Community-5865f2?style=flat&logo=discord&logoColor=white)](https://discord.gg/n25xj6J6HM)
[![OpenCollective](https://img.shields.io/badge/OpenCollective-84B5FC?style=flat&logo=opencollective&logoColor=white)](https://opencollective.com/thorvg)
[![License](https://img.shields.io/badge/licence-MIT-green.svg?style=flat)](LICENSE)

# ThorVG for Android
<p align="center">
  <img width="800" height="auto" src="https://github.com/thorvg/thorvg.site/blob/main/readme/logo/512/thorvg-banner.png">
</p>
ThorVG Android enhances Lottie animations on Android by bridging the capabilities of ThorVG's graphics engine with Lottie animations.
It simplifies integration with a script that builds ThorVG for Android system(arm64-v8a, x86_64) to includes its binary(libthorvg.a) in your package.
<br />

## Preparation

Please ensure that you have installed the [Android SDK](https://developer.android.com/studio), also your development environment is configured to build ThorVG Android
```
$git clone https://github.com/thorvg/thorvg.android.git
$cd thorvg.android
$git submodule update --init --recursive
```
Please refer to [ThorVG](https://github.com/thorvg/thorvg) for detailed information on setting up the ThorVG build environment.
The project requires Java 17 to run Gradle and the Android Gradle Plugin.
The automated ThorVG build also requires `meson`. `ninja` is resolved automatically from the Android SDK CMake installation when available, or can be provided from your system `PATH`.
<br />

## ThorVG Cross-Build 

Follow these steps to cross-build ThorVG Android library.

The simplest path is to let Gradle generate the cross files, build ThorVG, and copy the resulting static libraries in one step.

If `abi` is omitted, the default is `all`, which builds both `arm64-v8a` and `x86_64`.

Linux / macOS / WSL:
```
./gradlew lottie:buildThorvg
```

Windows PowerShell:
```
.\gradlew.bat lottie:buildThorvg
```

If `meson` is not on PATH, you can pass it explicitly:
```
.\gradlew.bat lottie:buildThorvg -PmesonPath="C:\path\to\meson.exe"
```

Single-ABI builds are also supported:
```
./gradlew lottie:buildThorvg -Pabi=arm64-v8a
./gradlew lottie:buildThorvg -Pabi=x86_64
```

This workflow:
1. runs `lottie:setupCrossBuild`
2. generates cross files under `lottie/build/tmp/`, for example:
- `android_cross_arm64-v8a.txt`
- `android_cross_x86_64.txt`
3. runs `meson` and `ninja`
4. copies the generated `libthorvg.a` files into:
   - `thorvg/lib/arm64-v8a/`
   - `thorvg/lib/x86_64/`

If you still want the manual bash-based flow, the helper scripts remain available:
- `./build_libthorvg.sh`
- `./copy_libthorvg.sh`

These scripts default to `all` when no ABI argument is provided, and can also be run with `arm64-v8a` or `x86_64`.

## ThorVG-Android Build

Build and package the thorvg-android project.
