#include <jni.h>
#include <vector>
#include <string>
#include <android/log.h>
#include "avif/avif.h" // This needs to be found via the include path in CMakeLists.txt

#define TAG "NativeAvifDecoder"
// IMPORTANT: Update this to your app's actual package name structure for JNI functions
//#define JNI_PACKAGE_PATH "com/example/avifjnirender"
//#define JNI_CLASS_NAME "NativeAvifDecoder"
//#define JNI_METHOD_NAME "decodeAvifNative"

// Helper to construct the full JNI function name
#define JNI_FUNCTION_NAME_INTERNAL(package_path, class_name, method_name) \
    Java_ ## package_path ## _ ## class_name ## _ ## method_name
#define JNI_FUNCTION_NAME(package_path, class_name, method_name) \
    JNI_FUNCTION_NAME_INTERNAL(package_path, class_name, method_name)

uint64_t currentTimeMillis() {
    using namespace std::chrono;
    return duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count();
}

extern "C" JNIEXPORT jlong JNICALL
JNI_FUNCTION_NAME(com_example_avifjnirenderdirectory, NativeAvifDecoder, decodeAvifNative) (
        JNIEnv* env,
        jobject jLibAvif,
        jbyteArray avif_data_array,
        jint len,
        jint renderMode,
        jint threadCount) {
    jbyte* avif_jbytes = env->GetByteArrayElements(avif_data_array, nullptr);
    if (avif_jbytes == nullptr) {
        jclass oomClass = env->FindClass("java/lang/OutOfMemoryError");
        if (oomClass) env->ThrowNew(oomClass, "Cannot get AVIF byte array elements");
        return -1;
    }
    const uint8_t* buffer = reinterpret_cast<const uint8_t*>(avif_jbytes);
    avifImage* image = avifImageCreateEmpty();
    if (!image) {
        env->ReleaseByteArrayElements(avif_data_array, avif_jbytes, JNI_ABORT);
        jclass runtimeExClass = env->FindClass("java/lang/RuntimeException");
        if (runtimeExClass) env->ThrowNew(runtimeExClass, "Failed to create AVIFImage");
        return -1;
    }

    avifDecoder *decoder = avifDecoderCreate();
    if (!decoder) {
        env->ReleaseByteArrayElements(avif_data_array, avif_jbytes, JNI_ABORT);
        jclass runtimeExClass = env->FindClass("java/lang/RuntimeException");
        if (runtimeExClass) env->ThrowNew(runtimeExClass, "Failed to create AVIF decoder");
        return -1;
    }

    // renderMode:
    // 0 -> gav1
    // 1 -> dav1d
    // 2 -> aom
    if (renderMode == 0) {
        decoder->codecChoice = AVIF_CODEC_CHOICE_LIBGAV1;
    } else if (renderMode == 2) {
        decoder->codecChoice = AVIF_CODEC_CHOICE_AOM;
    } else {
        decoder->codecChoice = AVIF_CODEC_CHOICE_DAV1D;
    }

    decoder->maxThreads = threadCount;

    auto decodeStart = currentTimeMillis();
    avifResult res = avifDecoderReadMemory(decoder, image, buffer, len);
    auto decodeMilli = currentTimeMillis() - decodeStart;
    //LOGE("### avifdecode time: %lu", decodeEnd - decodeStart);
    __android_log_print(ANDROID_LOG_INFO, TAG, "### avifdecode time: %lu", decodeMilli);

    jclass libAvifClass =
            env->FindClass("com/example/avifjnirenderdirectory/NativeAvifDecoder");

    jfieldID decodeMilliId = env->GetFieldID(libAvifClass, "decodeMilli", "J");
    env->SetLongField(jLibAvif, decodeMilliId, static_cast<int64_t>(decodeMilli));

    avifRGBImage rgb;
    memset(&rgb, 0, sizeof(avifRGBImage));

    if (res == AVIF_RESULT_OK) {
        avifRGBImageSetDefaults(&rgb, image);
        rgb.format = AVIF_RGB_FORMAT_RGBA;

        // allocate rgb buffer in java.
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
        res = avifImageYUVToRGB(image, &rgb);
        auto colorConvertMilli = currentTimeMillis();
        //LOGE("### yuv2rgb time: %lu", end - start);
        __android_log_print(ANDROID_LOG_INFO, TAG, "### yuv2rgb time: %lu", colorConvertMilli);
        if (res != AVIF_RESULT_OK) {
            //LOGE("### rgb conversion failed: %d", res);
            __android_log_print(ANDROID_LOG_ERROR, TAG, "### rgb conversion failed: %d", res);

            return -1;
        }

        jfieldID yuvConversionMilli = env->GetFieldID(libAvifClass, "yuvConversionMilli", "J");
        env->SetLongField(jLibAvif, yuvConversionMilli, static_cast<int64_t>(colorConvertMilli));
    } else {
        //LOGE("### avif decode failed: %d", res);
        __android_log_print(ANDROID_LOG_ERROR, TAG, "### avif decode failed: %d", res);
    }

    env->ReleaseByteArrayElements(avif_data_array, avif_jbytes, JNI_ABORT);
    jclass runtimeExClass = env->FindClass("java/lang/RuntimeException");

    avifImageDestroy(image);
    auto destroyStart = currentTimeMillis();
    avifDecoderDestroy(decoder);
    auto destroyEnd = currentTimeMillis();
    //LOGE("### avifdestroy time: %lu", destroyEnd - destroyStart);
    __android_log_print(ANDROID_LOG_INFO, TAG, "### avifdestroy time: %lu", destroyEnd - destroyStart);

    return (res == AVIF_RESULT_OK) ? 0 : -1;
}
