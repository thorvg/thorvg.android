# ThorVG for Android

ThorVG Android enhances Lottie animations on Android by bridging the capabilities of ThorVG's graphics engine with Lottie animations.
It simplifies integration with a script that builds ThorVG for Android system(arm64-v8a, x86_64) to includes its binary(libthorvg.a) in your package.
<br />

## Preparation

Please ensure that you have installed the [Android SDK](https://developer.android.com/studio), also your development environment is configured to build ThorVG Android
```
$git clone https://github.com/thorvg/thorvg.android.git
$cd thorvg.android
$git submodule init --resursive
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
