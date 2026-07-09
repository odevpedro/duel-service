cmake_minimum_required(VERSION 3.20)
project(ocgcore-bridge VERSION 1.0.0 LANGUAGES CXX C)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(JNI REQUIRED)
include(FetchContent)

FetchContent_Declare(
    lua54
    GIT_REPOSITORY https://github.com/lua/lua
    GIT_TAG        v5.4.7
    GIT_SHALLOW    TRUE
)

FetchContent_Declare(
    ygopro-core
    GIT_REPOSITORY https://github.com/edo9300/ygopro-core
    GIT_TAG        master
    GIT_SHALLOW    TRUE
)

FetchContent_Declare(
    nlohmann_json
    GIT_REPOSITORY https://github.com/nlohmann/json
    GIT_TAG        v3.11.3
    GIT_SHALLOW    TRUE
)

FetchContent_MakeAvailable(lua54 ygopro-core nlohmann_json)

# ── Lua 5.4 static library ──────────────────────────
file(GLOB LUA_SOURCES ${lua54_SOURCE_DIR}/*.c)
list(REMOVE_ITEM LUA_SOURCES
    ${lua54_SOURCE_DIR}/lua.c
    ${lua54_SOURCE_DIR}/luac.c
    ${lua54_SOURCE_DIR}/onelua.c
    ${lua54_SOURCE_DIR}/ltests.c
)

add_library(lua54 STATIC ${LUA_SOURCES})
target_include_directories(lua54 PUBLIC ${lua54_SOURCE_DIR})
set_target_properties(lua54 PROPERTIES
    POSITION_INDEPENDENT_CODE ON
)
# Compile Lua as C++ so symbols match C++ mangling from ygopro-core
set_source_files_properties(${LUA_SOURCES} PROPERTIES LANGUAGE CXX)

# ── ygopro-core static library ──────────────────────
set(YGOPRO_CORE_SOURCES
    ${ygopro-core_SOURCE_DIR}/card.cpp
    ${ygopro-core_SOURCE_DIR}/duel.cpp
    ${ygopro-core_SOURCE_DIR}/effect.cpp
    ${ygopro-core_SOURCE_DIR}/field.cpp
    ${ygopro-core_SOURCE_DIR}/interpreter.cpp
    ${ygopro-core_SOURCE_DIR}/libcard.cpp
    ${ygopro-core_SOURCE_DIR}/libdebug.cpp
    ${ygopro-core_SOURCE_DIR}/libduel.cpp
    ${ygopro-core_SOURCE_DIR}/libeffect.cpp
    ${ygopro-core_SOURCE_DIR}/libgroup.cpp
    ${ygopro-core_SOURCE_DIR}/ocgapi.cpp
    ${ygopro-core_SOURCE_DIR}/operations.cpp
    ${ygopro-core_SOURCE_DIR}/playerop.cpp
    ${ygopro-core_SOURCE_DIR}/processor.cpp
    ${ygopro-core_SOURCE_DIR}/processor_visit.cpp
    ${ygopro-core_SOURCE_DIR}/scriptlib.cpp
)

add_library(ocgcore-core STATIC ${YGOPRO_CORE_SOURCES})
target_include_directories(ocgcore-core
    PUBLIC
        ${ygopro-core_SOURCE_DIR}
        ${lua54_SOURCE_DIR}
)
target_link_libraries(ocgcore-core PUBLIC lua54)
set_target_properties(ocgcore-core PROPERTIES POSITION_INDEPENDENT_CODE ON)

# ── Bridge shared library ───────────────────────────
set(OCGCORE_BRIDGE_SOURCES src/ocgcore_bridge.cpp)

add_library(ocgcore SHARED ${OCGCORE_BRIDGE_SOURCES})

target_include_directories(ocgcore
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${JNI_INCLUDE_DIRS}
        ${JAVA_INCLUDE_PATH}
        ${JAVA_INCLUDE_PATH2}
        ${ygopro-core_SOURCE_DIR}
        ${lua54_SOURCE_DIR}
        ${nlohmann_json_SOURCE_DIR}/single_include
)

if(WIN32)
    target_include_directories(ocgcore PRIVATE ${JNI_INCLUDE_DIRS}/win32)
endif()

if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    target_link_libraries(ocgcore
        PRIVATE
            -Wl,--whole-archive
            lua54
            -Wl,--no-whole-archive
            ocgcore-core
            ${CMAKE_DL_LIBS}
    )
else()
    target_link_libraries(ocgcore
        PRIVATE
            ocgcore-core
            lua54
            ${CMAKE_DL_LIBS}
    )
endif()

set_target_properties(ocgcore
    PROPERTIES
        LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/../../src/main/resources/native"
)

if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    set_target_properties(ocgcore PROPERTIES PREFIX "lib" SUFFIX ".so")
elseif(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    set_target_properties(ocgcore PROPERTIES PREFIX "lib" SUFFIX ".dylib")
elseif(CMAKE_SYSTEM_NAME STREQUAL "Windows")
    set_target_properties(ocgcore PROPERTIES SUFFIX ".dll")
endif()

install(TARGETS ocgcore
    LIBRARY DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/../../src/main/resources/native
)
