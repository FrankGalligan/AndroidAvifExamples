# Basic libavif on Android
This tutorial shows how to build a very basic Android Studio project that calls
into a native libavif shared library.

## Prequesties

- [Android Sutdio](https://developer.android.com/studio)

## libavif
You first need to complete the [Building libavif for
Android](../BUILDING_ANDROID_LIBAVIF.md) tutorial.

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

## Example Android Studio Project
An almost finished Android Studio project based on this tutorial is located
[here](AndroidStudioProjects/BasicLibavifAndroid/). See the
[README](AndroidStudioProjects/BasicLibavifAndroid/README.md) for more
information.

