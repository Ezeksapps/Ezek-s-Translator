cmake_minimum_required(VERSION 3.10)
project(lang_detect)

include_directories(
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

add_library(lang_detect_jni SHARED
        src/lang_detect.cpp
)

target_link_libraries(lang_detect_jni
        ${CMAKE_CURRENT_SOURCE_DIR}/../lib/libcld2.a
        android
        log
)

target_compile_options(lang_detect_jni PRIVATE -flto)
target_link_options(lang_detect_jni PRIVATE -flto)

set_target_properties(lang_detect_jni PROPERTIES
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON
)