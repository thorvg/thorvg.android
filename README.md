# thorvg.android

[ThorVG](https://github.com/thorvg) for Android System

thorvg-android enhances Lottie animations on Android by bridging the capabilities of ThorVG's graphics engine with Lottie's dynamic animations.
and it simplifies integration with a script that builds ThorVG and includes libthorvg.a for arm64-v8a and x86_64 ABIs.
<br />

## Prepare

Ensure that your development environment is configured to build thorvg. Refer to [ThorVG](https://github.com/thorvg) GitHub repository for detailed instructions on setting up the environment.
<br />

## Cross-building (libthorvg.a)

Follow these steps to cross-build libthorvg.a library.

Prepare for cross-building by executing the lottie:setupCrossBuild task.
To build for arm64, use 1 as the value for 'abi'. For x86_64, use 2.
```
gradle lottie:setupCrossBuild -Pabi=1
```

Execute build_libthorvg.sh script to perform cross-building.
```
./build_libthorvg.sh
```

Copy the generated libthorvg.a to the thorvg/lib directory using the copy_libthorvg.sh script.
If the first argument is 1, the library will be copied to the thorvg/lib/arm64-v8a/ directory. If it is 2, it will be copied to the thorvg/lib/x86_64/ directory.
```
./copy_libthorvg.sh 1
```

## Build thorvg-android project

Build thorvg-android project.
