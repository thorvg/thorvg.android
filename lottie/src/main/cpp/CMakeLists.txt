
cmake_minimum_required(VERSION 3.4.1)

# configure import libs
set(DISTRIBUTION_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../distribution)

#if (NOT ${ANDROID_ABI} STREQUAL "x86")
if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    add_library(lib_thorvg STATIC IMPORTED)
    set_target_properties(lib_thorvg PROPERTIES IMPORTED_LOCATION
            ${DISTRIBUTION_DIR}/thorvg/lib/${ANDROID_ABI}/libthorvg.a)

    # build application's shared lib
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++14")

    add_library(lottie-libs SHARED
            lottie-libs.cpp
            LottieData.cpp)

    target_include_directories(lottie-libs PRIVATE
            ${DISTRIBUTION_DIR}/thorvg/include)

    target_link_libraries(lottie-libs
            android
            lib_thorvg
            -ljnigraphics
            log)
endif()