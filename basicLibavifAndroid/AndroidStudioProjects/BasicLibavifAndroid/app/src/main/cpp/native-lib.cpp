#include <jni.h>
#include <string>

#include "avif/avif.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_avifexamples_basiclibavifandroid_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

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
