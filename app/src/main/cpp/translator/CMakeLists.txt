cmake_minimum_required(VERSION 3.10)
project(translator)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

add_library(translator_jni SHARED
        src/translator.cpp
        src/translator_jni.cpp
)

target_link_libraries(translator_jni
        ${CMAKE_CURRENT_SOURCE_DIR}/../lib/libctranslate2.a
        ${CMAKE_CURRENT_SOURCE_DIR}/../lib/libclog.a
        ${CMAKE_CURRENT_SOURCE_DIR}/../lib/libcpuinfo.a
        ${CMAKE_CURRENT_SOURCE_DIR}/../lib/libruy.a
        ${CMAKE_CURRENT_SOURCE_DIR}/../lib/libsentencepiece.a

        # System libraries
        android
        log
)

target_compile_options(translator_jni PRIVATE -flto)
target_link_options(translator_jni PRIVATE -flto)