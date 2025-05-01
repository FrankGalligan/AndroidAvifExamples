package com.example.avifjnirenderdirectory;

import java.nio.ByteBuffer;

public class NativeAvifDecoder {
    static {
        // This loads libavifdecoderjni.so, which links to libavif.so.
        // The OS dynamic linker will resolve libavif.so from the app's lib directory.
        System.loadLibrary("avifdecoderjni");
    }

    public ByteBuffer rgbBuffer;
    public int width;
    public int height;
    public long decodeMilli;
    public long yuvConversionMilli;

    // used from jni.
    public void initRgbBuffer(int width, int height) {
        int size = width * height * 4;
        rgbBuffer = ByteBuffer.allocateDirect(size);
        this.width = width;
        this.height = height;
    }

    /**
     * Decodes AVIF image data using the native libavif library.
     * @param avifData The raw byte array of the AVIF file.
     * @param dataLength The length of the avifData array.
     * @return a status code. -1 error, 0 good.
     * @throws RuntimeException if decoding fails.
     */
    public native long decodeAvifNative(byte[] avifData, int dataLength, int renderMode, int threadCount);
}
