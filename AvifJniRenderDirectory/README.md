# AvifJniRenderDirectory

This example application will decode and render, as fast as possible, all
AVIF images in a chosen directory. The application lets you choose different
decoders and settings to see the affect it has on decode performance.

To finish the project you need to:
1. Complete the [Building libavif for
Android](../../../BUILDING_ANDROID_LIBAVIF.md) tutorial.
2. In [CMakeLists.txt](app/src/main/cpp/CMakeLists.txt) change **$GITHUB_DIRECTORY** to point to your libavif path from step 1.

Once you have completed steps 1 and 2 you should have a working Android Studio
project that you can build and run on an Android device.

3. Run the application once to get the documents directory for the application.
E.g. "/storage/emulated/0/Android/data/com.example.avifjnirenderdirectory/files/Documents/"

4. Push the AVIF files you want to time and render to the directory in step 3
   with adb.
E.g. adb push ${INPUT_DIRECTORY}/*.avif ${STEP3_DIRECTORY}

