[![Discord](https://img.shields.io/badge/Community-5865f2?style=flat&logo=discord&logoColor=white)](https://discord.gg/n25xj6J6HM)
[![ThorVGPT](https://img.shields.io/badge/ThorVGPT-76A99C?style=flat&logo=openai&logoColor=white)](https://chat.openai.com/g/g-Ht3dYIwLO-thorvgpt)
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
<br />

## ThorVG Cross-Build 

Follow these steps to cross-build ThorVG Android library.

Prepare for cross-building by executing the lottie:setupCrossBuild task.
To build for arm64, use 1 as the value for 'abi'. For x86_64, use 2.
```
gradle lottie:setupCrossBuild -Pabi=1
```

Execute build_libthorvg.sh script to perform cross-building.
```
./build_libthorvg.sh
```

Copy the generated **libthorvg.a** to the thorvg/lib directory using the copy_libthorvg.sh script.
If the first argument is 1, the library will be copied to the thorvg/lib/arm64-v8a/ directory. If it is 2, it will be copied to the thorvg/lib/x86_64/ directory.
```
./copy_libthorvg.sh 1
```

## ThorVG-Android Build

Build and package the thorvg-android project.
