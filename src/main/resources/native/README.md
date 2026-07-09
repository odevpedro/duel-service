# Native Library

Place the compiled ocgcore JNI bridge binary here before building the JAR.

| OS      | File                  |
|---------|-----------------------|
| Windows | ocgcore.dll           |
| Linux   | libocgcore.so         |
| macOS   | libocgcore.dylib      |

## Building the JNI bridge (automatic)

```bash
./gradlew fullBuildNative
```

This configures CMake, fetches ygopro-core, compiles the bridge and copies
the output to this directory.

## Building the JNI bridge (manual)

```bash
cd native/ocgcore-bridge
cmake -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build
cp build/libocgcore.so ../../src/main/resources/native/
```

## Building the JAR with native support

Once the .so/.dylib/.dll is in this directory:

```bash
./gradlew bootJar
```

Or run the full pipeline:

```bash
./gradlew fullBuildNative bootJar
```
