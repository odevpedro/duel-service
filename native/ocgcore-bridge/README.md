# ocgcore-bridge — JNI glue library

This project builds `libocgcore.so` (or `.dylib` / `.dll`) — the JNI bridge that
connects the Java `OcgCoreBridge` native declarations to the real
[ygopro-core](https://github.com/edo9300/ygopro-core) C++ game engine.

## How it works

```
Java (OcgCoreBridge.native methods)
    |
    | JNI call
    v
libocgcore.so  ←── THIS PROJECT
    |
    | C API (ocgcore_bridge_api.h)
    v
ygopro-core (edo9300/ygopro-core)  ←── Linked at build time
```

## Build

### Prerequisites

- JDK 21+ (for `jni.h`)
- CMake 3.20+
- A C++20 compiler (GCC 11+, Clang 14+, MSVC 2022)

### Build steps

```bash
# From the project root
cd native/ocgcore-bridge

# Configure
cmake -B build -DCMAKE_BUILD_TYPE=Release

# Build
cmake --build build

# The output libocgcore.so is placed at:
#   src/main/resources/native/libocgcore.so
```

### FetchContent note

The CMake script uses `FetchContent` to download and build
[ygopro-core](https://github.com/edo9300/ygopro-core) automatically.
If you already have a local checkout, you can override the source
directory by passing `-D FETCHCONTENT_SOURCE_DIR_YGOPRO-CORE=/path/to/ygopro-core`.

## Output

| Platform | File                          | Destination                              |
|----------|-------------------------------|------------------------------------------|
| Linux    | `libocgcore.so`               | `src/main/resources/native/libocgcore.so` |
| macOS    | `libocgcore.dylib`            | `src/main/resources/native/libocgcore.dylib` |
| Windows  | `ocgcore.dll`                 | `src/main/resources/native/ocgcore.dll`  |

## Integration

The Java `OcgCoreLoader` extracts the library from the classpath at runtime
and loads it via `System.load()`. When the native library is absent the
system falls back to `OcgCoreStub` automatically.
