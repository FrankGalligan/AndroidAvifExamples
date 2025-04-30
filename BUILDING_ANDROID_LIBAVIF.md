# Building libavif for Android
This tutorial shows how to build
[libavif](https://github.com/AOMediaCodec/libavif) as a shared library for Android.

This tutorial will build and add all of the decoders (as well as one encoder) to the
libavif shared library. In your app you will most likely only add one decoder or one
decoder and one encoder to libavif.

## Prequesties

- [git](https://git-scm.com/)
- [CMake](https://cmake.org/)
- [Ninja](https://ninja-build.org/)
- [Meson](https://mesonbuild.com/)

## libavif

### Get libavif
Download libavif.
```
git clone https://github.com/AOMediaCodec/libavif.git
cd libavif
```

### Build libaom
Building [libaom](https://aomedia.googlesource.com/aom/) is very close to the
[process](https://github.com/AOMediaCodec/libavif/blob/master/ext/aom.cmd) for building libaom for the desktop.

We are only building for the arm64 target because armv7 is not supported by
libaom.

*Note: **ANDROID_NDK** must be set to the path of the Android NDK on your local
machine.*

```
cd ext
git clone -b v3.12.1 --depth 1 https://aomedia.googlesource.com/aom
cd aom && mkdir build.libavif && cd build.libavif
cmake -G Ninja -DCMAKE_BUILD_TYPE=Release -DENABLE_DOCS=0 -DENABLE_EXAMPLES=0 -DENABLE_TESTDATA=0 -DENABLE_TESTS=0 -DENABLE_TOOLS=0 -DBUILD_SHARED_LIBS=0 -DCMAKE_TOOLCHAIN_FILE=../build/cmake/toolchains/android.cmake -DAOM_ANDROID_NDK_PATH=$ANDROID_NDK ..
ninja
cd ../../../
```

*Note: **v3.12.1** was pulled from
[aom.cmd](https://github.com/AOMediaCodec/libavif/blob/master/ext/aom.cmd). You
should check that file to see if libavif is using a more recent version of
libaom.*

### Build dav1d

Add the Android toolchain to your path. E.g.
```
export PATH=~/Android/Sdk/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH
```

Building [dav1d](https://code.videolan.org/videolan/dav1d) is very close to the
[process](https://github.com/AOMediaCodec/libavif/blob/master/ext/dav1d.cmd) for
building dav1d for the desktop.

```
cd ext
git clone -b 1.5.1 --depth 1 https://code.videolan.org/videolan/dav1d.git
cd dav1d && mkdir build  && cd build
meson setup --cross-file '../package/crossfiles/aarch64-android.meson'
--default-library=static --buildtype release -Denable_tools=false
-Denable_tests=false ..
meson compile -C ./
cd ../../../
```

*Note: **v1.5.1** was pulled from
[dav1d.cmd](https://github.com/AOMediaCodec/libavif/blob/master/ext/dav1d.cmd). You
should check that file to see if libavif is using a more recent version of
dav1d.*

### Build libgav1

Building [libgav1](https://chromium.googlesource.com/codecs/libgav1/) is very close to the
[process](https://github.com/AOMediaCodec/libavif/blob/master/ext/libgav1.cmd) for
building ligav1 for the desktop.

```
cd ext
git clone -b v0.16.3 --depth 1 https://chromium.googlesource.com/codecs/libgav1
cd libgav1
git clone -b lts_2021_03_24 --depth 1 https://github.com/abseil/abseil-cpp.git third_party/abseil-cpp
mkdir build && cd build
cmake -G Ninja -DCMAKE_BUILD_TYPE=Release -DLIBGAV1_THREADPOOL_USE_STD_MUTEX=1 -DCMAKE_TOOLCHAIN_FILE=../cmake/toolchains/android.cmake -DLIBGAV1_ANDROID_NDK_PATH=$ANDROID_NDK ..
ninja
cd ../../..
```
*Note: **v0.16.3** was pulled from
[libgav1.cmd](https://github.com/AOMediaCodec/libavif/blob/master/ext/libgav1.cmd). You
should check that file to see if libavif is using a more recent version of
libgav1.*

*Note: **lts_2021_03_24** was pulled from
[libgav1.cmd](https://github.com/AOMediaCodec/libavif/blob/master/ext/libgav1.cmd). You
should check that file to see if libavif is using a more recent version of
abseil.*

### Build libyuv

```
cd ext
git clone --single-branch https://chromium.googlesource.com/libyuv/libyuv
cd libyuv
git checkout ce488afb7
mkdir build && cd build
cmake -DBUILD_SHARED_LIBS=0 -DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake -DANDROID_ARM_MODE=ON -DANDROID_NDK=$ANDROID_NDK -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=23  -DANDROID_ARM_NEON=TRUE ../
make
cd ../../../
```

*Note: **ce488afb7** was pulled from
[libyuv.cmd](https://github.com/AOMediaCodec/libavif/blob/master/ext/libyuv.cmd). You
should check that file to see if libavif is using a more recent version of
libyuv.*

### Build libavif Android Shared Library

```
mkdir build_android_arm64-v8a_so && cd build_android_arm64-v8a_so
cmake -DCMAKE_BUILD_TYPE=Release -DAVIF_CODEC_AOM=LOCAL -DAVIF_CODEC_DAV1D=LOCAL -DAVIF_LIBYUV=LOCAL -DBUILD_SHARED_LIBS=ON -DAVIF_BUILD_APPS=OFF -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake -DANDROID_ARM_MODE=ON -DANDROID_NDK=$ANDROID_NDK -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=23 ../
make
cd ../
```

At this point you should have a file named libavif.so in your
build_android_arm64-v8a_so directory. This is the shared library that can be
called from Android.

