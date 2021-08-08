package com.example.avifexamples.avifrendertest;

import android.util.Log;

import java.nio.ByteBuffer;

public class LibAvif {

  static {
    try {
      System.loadLibrary("libavif_jni");
    } catch (UnsatisfiedLinkError exception) {
      Log.e("###", "unable to load avifJNI");
      exception.printStackTrace();
    }
  }

  public ByteBuffer rgbBuffer;
  public int width;
  public int height;

  // used from jni.
  public void initRgbBuffer(int width, int height) {
    int size = width * height * 4;
    rgbBuffer = ByteBuffer.allocateDirect(size);
    this.width = width;
    this.height = height;
  }

  public native long avifDecode(LibAvif jLibAvif, ByteBuffer encoded, int len, int renderMode,
                                int threadCount, int libyuvMode);
}
