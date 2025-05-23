
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("avifjnirenderdirectory")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
#add_library(${CMAKE_PROJECT_NAME} SHARED
#    # List C/C++ source files with relative paths to this CMakeLists.txt.
#        native-avif-decoder.cpp)

# Add the JNI wrapper library that we are building
add_library(avifdecoderjni SHARED native-avif-decoder.cpp) # "avifdecoderjni" is loaded by Java

# Specify the include directory for libavif headers.
# IMPORTANT: This is an absolute path from your request. For a portable project,
# you should copy the 'avif' header directory into your cpp folder (e.g., cpp/include/avif)
# and use: target_include_directories(avifdecoderjni PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/include")
target_include_directories(avifdecoderjni PRIVATE
        "$GITHUB_DIRECTORY/include"
)
target_include_directories(avifdecoderjni PRIVATE
        "$GITHUB_DIRECTORY/ext/libyuv/include"
)

# Define the imported target for the pre-built libavif.so
# It's expected to be in app/src/main/jniLibs/${ANDROID_ABI}/libavif.so
add_library(libavif SHARED IMPORTED)
#set_target_properties(libavif PROPERTIES IMPORTED_LOCATION
#    "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libavif.so")
set_target_properties(libavif PROPERTIES IMPORTED_LOCATION $GITHUB_DIRECTORY/build_android_arm64-v8a_so/libavif.so )

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(avifdecoderjni
        libavif     # Link against the imported libavif
    # List libraries link to the target library
    android
    log)
