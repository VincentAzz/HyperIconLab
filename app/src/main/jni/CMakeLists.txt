cmake_minimum_required(VERSION 3.22.1)
project("sticker_jni")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/opencv-mobile-4.13.0-android/sdk/native/jni)
find_package(OpenCV REQUIRED core imgproc)

add_library(sticker_jni SHARED
        sticker_jni.cpp
        omp_stubs.cpp
)

target_include_directories(sticker_jni PRIVATE
        ${OpenCV_INCLUDE_DIRS}
)

target_link_libraries(sticker_jni
        ${OpenCV_LIBS}
        android
        log
        jnigraphics
)