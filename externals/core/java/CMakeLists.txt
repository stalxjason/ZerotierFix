cmake_minimum_required(VERSION 3.2)

project(ZeroTierOneJNI)

find_package(Java COMPONENTS Development)
message("JAVA_HOME: $ENV{JAVA_HOME}")

if(WIN32)
set(Java_INCLUDE_DIRS $ENV{JAVA_HOME}/include)
endif()

if(APPLE)
set(Java_INCLUDE_DIRS "/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.10.sdk/System/Library/Frameworks/JavaVM.framework/Headers")
endif()

message("Java Include Dirs: ${Java_INCLUDE_DIRS}")

if(WIN32)
    add_definitions(-DNOMINMAX)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /EHsc /W3 /MP")
endif()

if(APPLE)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -arch i386 -arch x86_64 -Wall -O3 -flto -fPIE -fvectorize -fstack-protector -mmacosx-version-min=10.7 -Wno-unused-private-field")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${CMAKE_C_FLAGS} -fno-rtti")
endif()

# The upstream java/CMakeLists.txt still lists ext/lz4/lz4.c, ext/json-parser/json.c,
# node/Defaults.cpp and node/Dictionary.cpp, but those files were removed from the
# engine tree (their code is inlined into Packet.cpp / other node sources in 1.14+).
# So we compile the full node/ engine via GLOB instead of the stale explicit list.
#
# Metrics.cpp MUST be kept in the build: it unconditionally defines the
# ZeroTier::Metrics counter singletons (registry_ptr / packets / pkt_*_in / ...),
# and the engine references these counters unguarded from Bond.cpp, Network.cpp,
# Node.cpp, IncomingPacket.cpp, Switch.cpp, Peer.cpp, Membership.cpp, Multicaster.cpp.
# Only the *publishing* logic inside Metrics.cpp is disabled by the upstream default
# macro ZT_NO_PEER_METRICS; the counter definitions live OUTSIDE that guard, so
# excluding Metrics.cpp would cause undefined-symbol link errors.
# prometheus-cpp-lite is header-only (impls are inline in core/include/prometheus/*.h);
# its include dirs are enough -- no separate library needs to be linked.
add_definitions(-DZT_NO_PEER_METRICS)

file(GLOB NODE_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../node/*.cpp")

set(src_files
    ../ext/http-parser/http_parser.c
    ${NODE_SOURCES}
    ../osdep/Http.cpp
    ../osdep/OSUtils.cpp
    jni/com_zerotierone_sdk_Node.cpp
    jni/ZT_jniutils.cpp
    jni/ZT_jnicache.cpp
    )

set(include_dirs
    ${CMAKE_CURRENT_SOURCE_DIR}/../include/
    ${CMAKE_CURRENT_SOURCE_DIR}/../node/
    ${CMAKE_CURRENT_SOURCE_DIR}/../ext
    ${CMAKE_CURRENT_SOURCE_DIR}/../ext/prometheus-cpp-lite-1.0/core/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../ext/prometheus-cpp-lite-1.0/simpleapi/include
    ${Java_INCLUDE_DIRS})

if(WIN32)
    set(include_dirs
        ${include_dirs}
        ${Java_INCLUDE_DIRS}/win32)
endif()

include_directories(
    ${include_dirs}
    )

add_library(${PROJECT_NAME} SHARED ${src_files})

if(APPLE)
    set_target_properties(${PROJECT_NAME} PROPERTIES SUFFIX ".jnilib")
endif()

set(link_libs )

if(WIN32)
    set(link_libs
        wsock32
        ws2_32

        )
endif()

# On Android the engine and JNI glue call __android_log_print(), which lives
# in liblog. The upstream CMakeLists (used on desktop/macOS) never links it,
# so we must add it for the Android build or the final .so fails to link.
if(ANDROID)
    find_library(log-lib log)
    set(link_libs ${link_libs} ${log-lib})
endif()

target_link_libraries(${PROJECT_NAME} ${link_libs})
