# ajlua
Java and Android porting of native Lua

## API reference
https://yappy.github.io/ajlua/

## Lua reference manual
Official: https://www.lua.org/manual/5.3/

Unofficial Japanese: http://milkpot.sakura.ne.jp/lua/lua53_manual_ja.html

## How to build

### Java project
(Native part)
1. Install CMake and C/C++ build tools. (make, gcc, g++, or clang, etc.)
1. Create cmake build directory and cd to it. `build/` will be ignored by git.
   e.g. `$ mkdir build; cd build`
1. CMake configure the `native/` dir in this repository.
   e.g. `$ cmake ../native`
1. `$ make install` (`$ make -j install` to speed up)

(Java part)
1. Install JDK 8 or later.
1. Build tool is `Gradle` but it will be automatically downloaded by
   `gradlew` command. Of course you can use your own Gradle installed.
1. cd to `/java` dir in this repository.
1. `$ ./gradlew test` to test the native and java library.
   If failed, check the native build and install results.
1. `$ ./gradlew app` to start a sample application. (GUI needed)

### Android project
1. Install Android Studio.
1. Open SDK Manager and install `CMake` and `NDK`.
1. `Open an existing Android Studio project` or `File` `Open`.
1. Select `/android` dir in this repository.
1. `Make` `Make project`.
1. `app` dir is a sample application project. `Run` `Run 'app'` to run.
