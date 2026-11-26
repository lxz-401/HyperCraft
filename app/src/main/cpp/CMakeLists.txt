cmake_minimum_required(VERSION 3.22.1)
project(HyperCraftNative LANGUAGES CXX)

# Layout: nativehook/ holds the target-agnostic hook infrastructure; targets/
# holds the per-feature entry points and resolvers that bind it to one host.
# The root include dir keeps `nativehook/...` includes valid from any target.
# The library name is the LSPosed native-hook contract: it must match the
# `libHyperCraftNative.so` entry of META-INF/xposed/native_init.list and the
# `System.loadLibrary` name in NativeHomeHooksOS4.
add_library(HyperCraftNative SHARED
    nativehook/native_log.cpp
    nativehook/safe_mode.cpp
    targets/home/hypercraft_home.cpp)

option(HYPERCRAFT_HYOS_BACKEND "Build the separate Zygisk Next native-only companion" OFF)
if(HYPERCRAFT_HYOS_BACKEND)
    if(NOT HYPERCRAFT_ZYGISK_NEXT_API_DIR)
        set(HYPERCRAFT_ZYGISK_NEXT_API_DIR "${CMAKE_CURRENT_SOURCE_DIR}/nativehook")
    endif()
    set_target_properties(HyperCraftNative PROPERTIES OUTPUT_NAME HyperCraftHyos)
    if(NOT EXISTS "${HYPERCRAFT_ZYGISK_NEXT_API_DIR}/zygisk_next_api.h")
        message(FATAL_ERROR "Supply the pinned Zygisk Next API 4 SDK header directory")
    endif()
    target_sources(HyperCraftNative PRIVATE targets/hyos/hyos_entry.cpp targets/hyos/hyos_control.cpp
        targets/hyos/hyos_app_entry_arm64.S
        targets/hyos/hyos_report_runtime.cpp
        targets/weather/weather_runtime.cpp
        targets/weather/weather_material_runtime.cpp
        targets/weather/weather_card_runtime.cpp
        targets/weather/weather_card_lifetime.cpp
        targets/weather/weather_card_arm64.S
        targets/home/home_startup.cpp targets/home/home_startup_arm64.S
        targets/home/recents_runtime.cpp targets/home/recents_gateway_arm64.S
        targets/home/home_report_hooks.cpp targets/home/home_report_dart_arm64.S)
    target_include_directories(HyperCraftNative PRIVATE ${HYPERCRAFT_ZYGISK_NEXT_API_DIR})
    target_compile_definitions(HyperCraftNative PRIVATE HYPERCRAFT_HYOS_BACKEND=1)
endif()

# The Dart-side trampolines are hand-written: they follow the Dart AOT register
# rules (x15 stack, x16/x17 scratch) rather than AAPCS, so the layout feature needs
# its own assembly sources independent of the dock option below.
enable_language(ASM)

target_sources(HyperCraftNative PRIVATE targets/home/home_layout_targets.cpp)
target_sources(HyperCraftNative PRIVATE targets/home/home_layout_config.cpp
    targets/home/home_layout_hooks.cpp
    targets/home/home_gestures_os4.cpp
    targets/home/home_recents_haptic_runtime.cpp
    targets/home/home_layout_dart_arm64.S
    targets/home/home_back_gesture_arm64.S)

# Launcher tweaks: the sign- and symbol-table-driven plan/apply path for the grid,
# icon-size and recents-clear features. Its files are third-party (Apache-2.0) and keep
# their own headers; the resolver locates every site from the image's own symbol table or
# from a signature that must match exactly once, so nothing here carries a fixed address.
enable_language(C)
set(HYPERCRAFT_TWEAKS_DIR ${CMAKE_CURRENT_SOURCE_DIR}/targets/home/tweaks)
target_sources(HyperCraftNative PRIVATE
    ${HYPERCRAFT_TWEAKS_DIR}/entry.cpp
    ${HYPERCRAFT_TWEAKS_DIR}/blob.cpp
    ${HYPERCRAFT_TWEAKS_DIR}/image.cpp
    ${HYPERCRAFT_TWEAKS_DIR}/scanner.cpp
    ${HYPERCRAFT_TWEAKS_DIR}/symtab.cpp
    ${HYPERCRAFT_TWEAKS_DIR}/ht_plan.cpp
    ${HYPERCRAFT_TWEAKS_DIR}/htcache.cpp
    ${HYPERCRAFT_TWEAKS_DIR}/xz/xz_crc32.c
    ${HYPERCRAFT_TWEAKS_DIR}/xz/xz_crc64.c
    ${HYPERCRAFT_TWEAKS_DIR}/xz/xz_dec_lzma2.c
    ${HYPERCRAFT_TWEAKS_DIR}/xz/xz_dec_stream.c)
# Third-party sources (Apache-2.0) - use targeted warning suppression instead of -Wno-everything
# -Wnon-pod-varargs: catches std::string passed to %s (UB) - CRITICAL, must keep enabled
# -Wformat / -Werror=format-security: format string safety
# Other style warnings suppressed per-file as needed
file(GLOB HYPERCRAFT_TWEAKS_SOURCES
    ${HYPERCRAFT_TWEAKS_DIR}/*.cpp ${HYPERCRAFT_TWEAKS_DIR}/xz/*.c)
set_source_files_properties(${HYPERCRAFT_TWEAKS_SOURCES} PROPERTIES
    COMPILE_OPTIONS
    "-Wno-unused-parameter;-Wno-sign-compare;-Wno-unused-variable;-Wno-unused-function"
    "-Wnon-pod-varargs;-Wformat;-Werror=format-security")

# clang-tidy integration for main codebase (not third-party tweaks)
find_program(CLANG_TIDY clang-tidy)
if(CLANG_TIDY)
    set(CMAKE_CXX_CLANG_TIDY "${CLANG_TIDY}"
        -checks="modernize-*,performance-*,readability-*,bugprone-*,clang-analyzer-*"
        -header-filter="${CMAKE_CURRENT_SOURCE_DIR}/nativehook/*;${CMAKE_CURRENT_SOURCE_DIR}/targets/home/*.h;${CMAKE_CURRENT_SOURCE_DIR}/targets/home/*.hpp"
        -format-style=file
    )
    message(STATUS "clang-tidy enabled for native code")
else()
    message(WARNING "clang-tidy not found, static analysis disabled")
endif()

option(HYPERCRAFT_DOCK_NATIVE_MOTION "Dynamically resolve OS4 native recents animation" ON)
if(HYPERCRAFT_DOCK_NATIVE_MOTION)
    target_sources(HyperCraftNative PRIVATE
        targets/home/dock_native_hooks.cpp
        targets/home/home_recent_statusbar.cpp
        targets/home/dock_native_motion.cpp
        targets/home/dock_native_motion_arm64.S)
    target_compile_definitions(HyperCraftNative PRIVATE HYPERCRAFT_DOCK_NATIVE_MOTION=1)
endif()

target_include_directories(HyperCraftNative PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

target_compile_features(HyperCraftNative PRIVATE cxx_std_20)
target_compile_options(HyperCraftNative PRIVATE
    -Wall
    -Wextra
    -Werror
    -fvisibility=hidden
)

target_link_libraries(HyperCraftNative PRIVATE android binder_ndk log)

# SystemUI's isolated palette backend has no Home/Hyos initialization contract.
# It is loaded on demand by AmbientNativeColorOS4 and is absent from the companion.
if(NOT HYPERCRAFT_HYOS_BACKEND)
    add_library(HyperCraftAmbientOS4 SHARED targets/systemui/ambient_color_os4.cpp)
    target_compile_features(HyperCraftAmbientOS4 PRIVATE cxx_std_20)
    target_compile_options(HyperCraftAmbientOS4 PRIVATE -Wall -Wextra -Werror -fvisibility=hidden)
    target_link_libraries(HyperCraftAmbientOS4 PRIVATE dl log)
endif()
