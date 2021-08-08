# BasicLibavifAndroid
This is an almost working basic example of calling a native libavif library in
an Android Studio project.

To finish the project you need to:
1. Complete the [Building libavif for
Android](../../../BUILDING_ANDROID_LIBAVIF.md) tutorial.
2. In [CMakeLists.txt](app/src/main/cpp/CMakeLists.txt) change **$GITHUB_DIRECTORY** to point to your libavif path from step 1.

Once you have completed steps 1 and 2 you should have a working Android Studio
project that you can build and run on an Android device.

