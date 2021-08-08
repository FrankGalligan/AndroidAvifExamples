# Basic libavif on Android
This tutorial shows how to build libavif so that you can call into libavif
from your Android Studio project.

## Prequesties

- [Android Sutdio](https://developer.android.com/studio)
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
git clone -b v3.1.2 --depth 1 https://aomedia.googlesource.com/aom
cd aom && mkdir build.libavif && cd build.libavif
cmake -G Ninja -DCMAKE_BUILD_TYPE=Release -DENABLE_DOCS=0 -DENABLE_EXAMPLES=0 -DENABLE_TESTDATA=0 -DENABLE_TESTS=0 -DENABLE_TOOLS=0 -DBUILD_SHARED_LIBS=0 -DCMAKE_TOOLCHAIN_FILE=../build/cmake/toolchains/arm64-android-clang.cmake -DAOM_ANDROID_NDK_PATH=$ANDROID_NDK ..
ninja
cd ../../../
```

*Note: **v3.1.2** was pulled from
[aom.cmd](https://github.com/AOMediaCodec/libavif/blob/master/ext/aom.cmd). You
should chack that file to see if libavif is using a more recent version of
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
git clone -b 0.9.1 --depth 1 https://code.videolan.org/videolan/dav1d.git
cd dav1d && mkdir build  && cd build
meson setup --cross-file '../package/crossfiles/aarch64-android.meson' --default-library=static --buildtype release ..
ninja
cd ../../../
```

*Note: **v0.9.1** was pulled from
[dav1d.cmd](https://github.com/AOMediaCodec/libavif/blob/master/ext/dav1d.cmd). You
should chack that file to see if libavif is using a more recent version of
dav1d.*

### Build libyuv

```
cd ext
git clone --single-branch https://chromium.googlesource.com/libyuv/libyuv
cd libyuv
git checkout 2f0cbb9
mkdir build && cd build
cmake -DBUILD_SHARED_LIBS=0 -DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake -DANDROID_ARM_MODE=ON -DANDROID_NDK=$ANDROID_NDK -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=23  -DANDROID_ARM_NEON=TRUE ../
make
cd ../../../
```

*Note: **2f0cbb9** was pulled from
[libyuv.cmd](https://github.com/AOMediaCodec/libavif/blob/master/ext/libyuv.cmd). You
should chack that file to see if libavif is using a more recent version of
libyuv.*

### Build libavif Android Shared Library

```
mkdir build_android_arm64-v8a_so && cd build_android_arm64-v8a_so
cmake -DCMAKE_BUILD_TYPE=Release -DAVIF_LOCAL_AOM=1 -DAVIF_CODEC_AOM=1 -DAVIF_LOCAL_DAV1D=1 -DAVIF_CODEC_DAV1D=1 -DAVIF_LOCAL_LIBYUV=1 -DBUILD_SHARED_LIBS=1 -DAVIF_BUILD_APPS=0 -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake -DANDROID_ARM_MODE=ON -DANDROID_NDK=$ANDROID_NDK -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=23 ../
make
cd ../
```

At this point you should have a file named libavif.so in your
build_android_arm64-v8a_so directory. This is the shared library that will be
called from the Android Studio project.

## Android App that Calls libavif

In Android Studio create a new **Native C++** project. Pick the **c++11**
toolchain. Finish the proejct.


1. In file MainActivity.java.

Change
```
tv.setText(stringFromJNI());
```
to
```
tv.setText(avifVersionString());
```

Change
```
public native String stringFromJNI();
```
to
```
public native String avifVersionString();
```


2. In file native-lib.cpp

Add code to include avif.h
```
#include "avif/avif.h"
```

Add the function below.
*Note: You may need to change the function name to match the names of your
project.*

```
extern "C" JNIEXPORT jstring JNICALL
Java_com_example_avifexamples_basiclibavifandroid_MainActivity_avifVersionString(
        JNIEnv* env,
        jobject /* this */) {
    const char* const str_version = avifVersion();
    char codec_versions[256];
    avifCodecVersions(codec_versions);
    const uint32_t libyuv_version = avifLibYUVVersion();

    char temp[512];
    sprintf(temp, "AVIF: %s\nCodces: %s\nlibyuv: %d",
            str_version,codec_versions, libyuv_version);
    return env->NewStringUTF(temp);
}
```

3. In CMakeLists.txt

Add the code below. Right after make_minimum_required() should be fine.
*Note: You will need to replace **GITHUB_DIRECTORY** with the path to where you
cloned libavif.*

```
add_library( avif SHARED IMPORTED )
set_target_properties(avif PROPERTIES IMPORTED_LOCATION $GITHUB_DIRECTORY/libavif/build_android_arm64-v8a_so/libavif.so )
include_directories($GITHUB_DIRECTORY/libavif/include)
```

Add **avif** to the target_link_libraries. Like below:
```
target_link_libraries( # Specifies the target library.
        basiclibavifandroid

        avif

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
```

4. In build.grade (:app)
Modify build.grade (:app) to only build armv8 as aom currently only supports armv8. Add the code below after the buildFeatures block.
```
splits {

   // Configures multiple APKs based on ABI.
   abi {

       // Enables building multiple APKs per ABI.
       enable true

       // By default all ABIs are included, so use reset() and include to specify that we only
       // want APKs for x86 and x86_64.

       // Resets the list of ABIs that Gradle should create APKs for to none.
       reset()

       // Specifies a list of ABIs that Gradle should create APKs for.
       //include "x86", "x86_64"
       include "arm64-v8a"

       // Specifies that we do not want to also generate a universal APK that includes all ABIs.
       universalApk false
   }
}
```

