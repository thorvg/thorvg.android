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
./gradlew thorvg-core:buildThorvg
```

Windows PowerShell:
```
.\gradlew.bat thorvg-core:buildThorvg
```

If `meson` is not on PATH, you can pass it explicitly:
```
.\gradlew.bat thorvg-core:buildThorvg -PmesonPath="C:\path\to\meson.exe"
```

Single-ABI builds are also supported:
```
./gradlew thorvg-core:buildThorvg -Pabi=arm64-v8a
./gradlew thorvg-core:buildThorvg -Pabi=x86_64
```

This workflow:
1. runs `thorvg-core:setupCrossBuild`
2. generates cross files under `thorvg-core/build/tmp/`, for example:
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

```
./gradlew assembleDebug
```

To install and run the sample app:

```
./gradlew :sample:installDebug
```

If you change ThorVG native build options such as enabled loaders, rebuild the bundled ThorVG static libraries first:

```
./gradlew thorvg-core:buildThorvg
```

## Modules

- `thorvg-core`: Android bindings for ThorVG Lottie and SVG rendering.
- `thorvg-compose`: Jetpack Compose adapters for Lottie and SVG.
- `thorvg-view`: Android View adapters for Lottie and SVG.
- `sample`: Sample app that demonstrates both Compose and View integrations.

## Compose Usage

Lottie from a raw resource:

```kotlin
val state = rememberLottieState(
    isPlaying = true,
    repeatCount = LottieConstants.INFINITE
)

Lottie(
    resId = R.raw.swinging,
    state = state,
    modifier = Modifier.size(220.dp)
)
```

SVG from a raw resource, asset, or `Uri`:

```kotlin
Svg(resId = R.raw.tiger)
Svg(assetName = "thorvg_mono_black.svg")
Svg(uri = svgUri)
```

## View Usage

Lottie in XML:

```xml
<org.thorvg.view.lottie.LottieView
    android:layout_width="220dp"
    android:layout_height="220dp"
    android:autoStart="true"
    android:repeatCount="-1"
    app:rawRes="@raw/swinging" />
```

SVG in XML:

```xml
<org.thorvg.view.svg.SvgView
    android:layout_width="match_parent"
    android:layout_height="240dp"
    app:assetName="thorvg_mono_black.svg" />
```

Or configure a `SvgView` programmatically:

```kotlin
svgView.setSvgRawResource(R.raw.tiger)
svgView.setSvgAsset("thorvg_mono_black.svg")
svgView.setSvgUri(svgUri)
```

## Sample App

The sample app home screen exposes four entry points:

- `Open Compose Sample`: opens the Lottie Compose sample screen.
- `Open SVG Sample`: opens the SVG Compose sample screen.
- `Open View Sample`: opens the Lottie View sample activity.
- `Open SVG View Sample`: opens the SVG View sample activity.

Compose samples are switched inside `MainActivity`, while View samples are hosted by `ViewSampleActivity` and display either a Lottie or SVG fragment based on the requested `SampleType`.
