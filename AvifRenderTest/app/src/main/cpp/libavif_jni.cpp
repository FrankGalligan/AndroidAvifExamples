//
//

#include <android/log.h>

#include <jni.h>

#include <cstdint>
#include <cstring>
#include <mutex>  // NOLINT
#include <new>
#include <thread>
#include <vector>

#include <chrono>

#include "avif/avif.h"
#include "libyuv.h"  // NOLINT


#define LOG_TAG "avif_jni"
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))

#define FUNC(RETURN_TYPE, NAME, ...) \
  extern "C" { \
  JNIEXPORT RETURN_TYPE \
    Java_com_example_avifexamples_avifrendertest_LibAvif_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__);\
  } \
  JNIEXPORT RETURN_TYPE \
    Java_com_example_avifexamples_avifrendertest_LibAvif_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__)\

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}

uint64_t currentTimeMillis() {
    using namespace std::chrono;
    return duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count();
}

FUNC(jlong, avifDecode, jobject jLibAvif, jobject encoded, jint len, jint renderMode,
     jint threadCount, jint libyuvMode) {
    const uint8_t* const buffer = reinterpret_cast<const uint8_t*>(
            env->GetDirectBufferAddress(encoded));
    avifImage* image = avifImageCreateEmpty();
    avifDecoder* decoder = avifDecoderCreate();
    decoder->maxThreads = threadCount;
    // renderMode:
    // 0 -> gav1
    // 1 -> dav1d
    // 2 -> aom
    if (renderMode == 0) {
        decoder->codecChoice = AVIF_CODEC_CHOICE_LIBGAV1;
    } else if (renderMode == 1) {
        decoder->codecChoice = AVIF_CODEC_CHOICE_DAV1D;
    } else if (renderMode == 2) {
        decoder->codecChoice = AVIF_CODEC_CHOICE_AOM;
    }
    auto decodeStart = currentTimeMillis();
    avifResult res = avifDecoderReadMemory(decoder, image, buffer, len);
    auto decodeEnd = currentTimeMillis();
    LOGE("### avifdecode time: %lu", decodeEnd - decodeStart);
    if (res == AVIF_RESULT_OK) {
        avifRGBImage rgb;
        memset(&rgb, 0, sizeof(avifRGBImage));
        avifRGBImageSetDefaults(&rgb, image);
        rgb.format = AVIF_RGB_FORMAT_RGBA;

        // allocate rgb buffer in java.
        jclass libAvifClass =
                env->FindClass("com/example/avifexamples/avifrendertest/LibAvif");
        jmethodID initRgbBuffer = env->GetMethodID(
                libAvifClass, "initRgbBuffer", "(II)V");
        env->CallVoidMethod(jLibAvif, initRgbBuffer, static_cast<int32_t>(image->width),
                            static_cast<int32_t>(image->height));

        // get the rgb buffer pointer.
        jfieldID rgbBufferField =
                env->GetFieldID(libAvifClass, "rgbBuffer", "Ljava/nio/ByteBuffer;");
        jobject rgbBufferObject = env->GetObjectField(jLibAvif, rgbBufferField);
        rgb.pixels =
                reinterpret_cast<uint8_t*>(env->GetDirectBufferAddress(rgbBufferObject));
        rgb.rowBytes = rgb.width * 4;

        auto start = currentTimeMillis();
        // libyuvMode:
        // 0 -> single threaded.
        // 1 -> none
        // 2 -> multi threaded.
        if (libyuvMode == 1) {
            res = avifImageYUVToRGB(image, &rgb);
        } else if (libyuvMode == 0 || threadCount == 1) {
            libyuv::I420ToABGR(image->yuvPlanes[0],
                               image->yuvRowBytes[0],
                               image->yuvPlanes[1],
                               image->yuvRowBytes[1],
                               image->yuvPlanes[2],
                               image->yuvRowBytes[2],
                               rgb.pixels, rgb.rowBytes,
                               image->width,
                               image->height);
        } else {
            // threaded.
            uint8_t* y_plane = image->yuvPlanes[0];
            uint8_t* u_plane = image->yuvPlanes[1];
            uint8_t* v_plane = image->yuvPlanes[2];
            uint8_t* rgb_pixels = rgb.pixels;
            std::vector<std::thread> threads;
            for (int i = 0; i < threadCount - 1; ++i) {
                const int height = static_cast<int32_t>(image->height) / threadCount;
                threads.push_back(std::thread(
                        libyuv::I420ToABGR, y_plane,
                        image->yuvRowBytes[0],
                        u_plane,
                        image->yuvRowBytes[1],
                        v_plane,
                        image->yuvRowBytes[2],
                        rgb_pixels, rgb.rowBytes,
                        image->width,
                        height));
                y_plane += image->yuvRowBytes[0] * height;
                u_plane += image->yuvRowBytes[1] * (height / 2);
                v_plane += image->yuvRowBytes[2] * (height / 2);
                rgb_pixels += rgb.rowBytes * height;
            }
            // work in current thread.
            const int height = static_cast<int>(image->height) / threadCount +
                    (static_cast<int>(image->height) % threadCount);
            libyuv::I420ToABGR(y_plane,
                               image->yuvRowBytes[0],
                               u_plane,
                               image->yuvRowBytes[1],
                               v_plane,
                               image->yuvRowBytes[2],
                               rgb_pixels, rgb.rowBytes,
                               image->width,
                               height);
            for (auto& thread : threads) {
                thread.join();
            }
        }
        auto end = currentTimeMillis();
        LOGE("### yuv2rgb time: %lu", end - start);
        if (res != AVIF_RESULT_OK) {
            LOGE("### rgb conversion failed: %d", res);
            return -1;
        }
    } else {
        LOGE("### avif decode failed: %d", res);
    }

    avifImageDestroy(image);
    auto destroyStart = currentTimeMillis();
    avifDecoderDestroy(decoder);
    auto destroyEnd = currentTimeMillis();
    LOGE("### avifdestroy time: %lu", destroyEnd - destroyStart);
    return (res == AVIF_RESULT_OK) ? 0 : -1;
}