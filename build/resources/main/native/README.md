# Native Library

Place the compiled ocgcore binary here before building the JAR.

| OS      | File                  |
|---------|-----------------------|
| Windows | ocgcore.dll           |
| Linux   | libocgcore.so         |
| macOS   | libocgcore.dylib      |

## Building ocgcore

```bash
git clone https://github.com/edo9300/ygopro-core
cd ygopro-core
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build .
```

Copy the output to this directory, then run:

```bash
./gradlew bootJar
```
