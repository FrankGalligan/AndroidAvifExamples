# Basic libavif on Android
This tutorial shows how to build libavif so that you can call the into libavif
form your Android Studio project.

## Prequesties

- [Android Sutdio](https://developer.android.com/studio)
- [git](https://git-scm.com/)
- [CMake](https://cmake.org/)
- [Ninja](https://ninja-build.org/)
- [Meson](https://mesonbuild.com/)

## libavif

### Get libavif
Download libavif.
'''
git clone https://github.com/AOMediaCodec/libavif.git
cd libavif
'''

### Build libaom
Building [libaom](https://aomedia.googlesource.com/aom/) is very close to the
[process](https://github.com/AOMediaCodec/libavif/blob/master/ext/aom.cmd) for building libaom for the desktop.

We are only building for the arm64 target because armv7 is not supported by
libaom.

*Note: $ANDROID_NDK must be set to the path of the Android NDK on your local
machine.*

'''
cd ext
git clone -b v3.1.2 --depth 1 https://aomedia.googlesource.com/aom
$ cd aom && mkdir build.libavif && cd build.libavif
$ cmake -G Ninja -DCMAKE_BUILD_TYPE=Release -DENABLE_DOCS=0 -DENABLE_EXAMPLES=0 -DENABLE_TESTDATA=0 -DENABLE_TESTS=0 -DENABLE_TOOLS=0 -DBUILD_SHARED_LIBS=0 -DCMAKE_TOOLCHAIN_FILE=../build/cmake/toolchains/arm64-android-clang.cmake -DAOM_ANDROID_NDK_PATH=$ANDROID_NDK ..
$ ninja
$ cd ../../../
'''

*Note: v3.1.2 was pulled from
[aom.cmd](https://github.com/AOMediaCodec/libavif/blob/master/ext/aom.cmd). You
should chack that file to see if libavif is using a more recent version of
libaom.*

### Build dav1d

Add the Android toolchain to your path. E.g.
'''
export PATH=~/Android/Sdk/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH
'''

Building [dav1d](https://code.videolan.org/videolan/dav1d) is very close to the
[process](https://github.com/AOMediaCodec/libavif/blob/master/ext/dav1d.cmd) for
building dav1d for the desktop.

'''
cd ext
git clone -b 0.9.1 --depth 1 https://code.videolan.org/videolan/dav1d.git
cd dav1d && mkdir build  && cd build
meson setup --cross-file '../package/crossfiles/aarch64-android.meson' --default-library=static --buildtype release ..
ninja
cd ../../../
'''

*Note: v0.9.1 was pulled from
[dav1d.cmd](https://github.com/AOMediaCodec/libavif/blob/master/ext/dav1d.cmd). You
should chack that file to see if libavif is using a more recent version of
dav1d.*

### Build libyuv

'''
cd ext
git clone --single-branch https://chromium.googlesource.com/libyuv/libyuv
cd libyuv
git checkout 2f0cbb9
mkdir build && cd build
cmake -DBUILD_SHARED_LIBS=0 -DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake -DANDROID_ARM_MODE=ON -DANDROID_NDK=$ANDROID_NDK -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=23  -DANDROID_ARM_NEON=TRUE ../
make
cd ../../../

*Note: 2f0cbb9 was pulled from
[libyuv.cmd](https://github.com/AOMediaCodec/libavif/blob/master/ext/libyuv.cmd). You
should chack that file to see if libavif is using a more recent version of
libyuv.*


