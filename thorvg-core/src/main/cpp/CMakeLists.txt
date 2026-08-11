
cmake_minimum_required(VERSION 3.4.1)

# configure import libs
set(THORVG_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../thorvg)

if (${ANDROID_ABI} STREQUAL "arm64-v8a" OR ${ANDROID_ABI} STREQUAL "x86_64")
    add_library(lib_thorvg STATIC IMPORTED)
    set_target_properties(lib_thorvg PROPERTIES IMPORTED_LOCATION
            ${THORVG_DIR}/lib/${ANDROID_ABI}/libthorvg-1.a)

    # build application's shared lib
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++14")

    add_library(lottie-libs SHARED
            lottie-libs.cpp
            LottieData.cpp
            svg-libs.cpp
            SvgData.cpp
            video-libs.cpp
            VideoData.cpp)

    target_link_options(lottie-libs PRIVATE
            "-Wl,-z,max-page-size=16384"
            "-Wl,-z,common-page-size=16384")

    target_include_directories(lottie-libs PRIVATE
            ${THORVG_DIR}/inc
            ${THORVG_DIR}/src/loaders/media)

    target_link_libraries(lottie-libs
            android
            lib_thorvg
            -ljnigraphics
            mediandk
            OpenSLES
            EGL
            GLESv2
            dl
            -fopenmp
            -static-openmp
            log)
endif()
