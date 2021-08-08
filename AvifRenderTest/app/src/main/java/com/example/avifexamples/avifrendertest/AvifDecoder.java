package com.example.avifexamples.avifrendertest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class AvifDecoder extends AppCompatActivity {

  /*package*/ static final String CONTENT_TYPE_FILENAME = "filename";
  /*package*/ static final String CONTENT_TYPE_RENDER_MODE = "render_mode";
  /*package*/ static final String CONTENT_TYPE_THREAD_COUNT = "thread_count";
  /*package*/ static final String CONTENT_TYPE_LIBYUV_MODE = "libyuv_mode";
  /*package*/ static final String CONTENT_TYPE_LOOP_COUNT = "loop_count";
  /*package*/ static final String CONTENT_TYPE_SLEEP = "sleep";

  /*package*/ static final int RENDER_MODE_LIBAVIF_GAV1 = 0;
  /*package*/ static final int RENDER_MODE_LIBAVIF_DAV1D = 1;
  /*package*/ static final int RENDER_MODE_LIBAVIF_AOM = 2;
  /*package*/ static final int RENDER_MODE_PLATFORM_AVIF = 3;

  /*package*/ static final int LIBYUV_MODE_SINGLE_THREADED = 0;
  /*package*/ static final int LIBYUV_MODE_NONE = 1;
  /*package*/ static final int LIBYUV_MODE_MULTI_THREADED = 2;


  private ImageView imageView;
  private TextView timingView;
  //private String directoryName;
  private int renderMode;
  private long startTime;
  private int threadCount;
  private int libyuvMode;
  private int loopCount;
  private int sleepSeconds;

  class AvifRenderer extends AsyncTask<Void, Void, Void> {

    List<File> files;
    int fileIndex;
    boolean success;
    Bitmap bitmap;
    int limit;
    int currentLoopCount;

    public AvifRenderer(List<File> files, int fileIndex, int limit, int currentLoopCount) {
      this.files = files;
      this.fileIndex = fileIndex;
      this.success = false;
      this.limit = limit;
      this.currentLoopCount = currentLoopCount;
    }

    @Override
    protected Void doInBackground(Void... voids) {
      try {
        RandomAccessFile raFile = new RandomAccessFile(files.get(fileIndex), "r");
        FileChannel channel = raFile.getChannel();
        int length = (int) channel.size();

        ByteBuffer buffer;
        if (renderMode == RENDER_MODE_PLATFORM_AVIF) {
          buffer = ByteBuffer.allocate(length);
        } else {
          buffer = ByteBuffer.allocateDirect(length);
        }
        channel.read(buffer);
        buffer.flip();

        if (renderMode == RENDER_MODE_PLATFORM_AVIF) {
          bitmap = BitmapFactory.decodeByteArray(buffer.array(), 0, length);
        } else {
          LibAvif libavif = new LibAvif();
          long result =
                libavif.avifDecode(libavif, buffer, length, renderMode, threadCount, libyuvMode);
          if (result != 0) {
            Log.e("###", "Avif decoding failed");
            return null;
          }
          bitmap = Bitmap.createBitmap(libavif.width, libavif.height, Bitmap.Config.ARGB_8888);
          bitmap.copyPixelsFromBuffer(libavif.rgbBuffer);
        }
        success = true;
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void voids) {
      if (!success) return;
      if (bitmap == null && renderMode == RENDER_MODE_PLATFORM_AVIF) {
        timingView.setText("no platform avif decoder was found.");
        return;
      }
      imageView.setImageBitmap(bitmap);
      long paintingTime = System.currentTimeMillis() - startTime;
      String renderModeString = "none";
      if (renderMode == RENDER_MODE_LIBAVIF_GAV1) {
        renderModeString = "gav1";
      } else if (renderMode == RENDER_MODE_LIBAVIF_DAV1D) {
        renderModeString = "dav1d";
      } else if (renderMode == RENDER_MODE_LIBAVIF_AOM) {
        renderModeString = "libaom";
      } else if (renderMode == RENDER_MODE_PLATFORM_AVIF) {
        renderModeString = "platform avif";
      }
      Log.e("###", "painting time for " + renderModeString + ": " + paintingTime);
      timingView.setText(renderModeString + ": " + paintingTime + "ms");
      ++fileIndex;
      if (fileIndex >= limit) {
        ++currentLoopCount;
      }
      if (currentLoopCount >= loopCount) {
        return;
      }
      if (fileIndex >= limit) {
        fileIndex = 0;
        try {
          Thread.sleep(sleepSeconds * 1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        startTime = System.currentTimeMillis();
      }
      new AvifRenderer(files, fileIndex, limit, currentLoopCount).execute((Void[]) null);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    String directoryName = intent.getStringExtra(CONTENT_TYPE_FILENAME);
    renderMode = intent.getIntExtra(CONTENT_TYPE_RENDER_MODE, RENDER_MODE_LIBAVIF_GAV1);
    threadCount = intent.getIntExtra(CONTENT_TYPE_THREAD_COUNT, 1);
    libyuvMode = intent.getIntExtra(CONTENT_TYPE_LIBYUV_MODE, LIBYUV_MODE_SINGLE_THREADED);
    loopCount = intent.getIntExtra(CONTENT_TYPE_LOOP_COUNT, 1);
    sleepSeconds = intent.getIntExtra(CONTENT_TYPE_SLEEP, 0);

    Log.i(
        "###",
        "render mode: "
            + renderMode
            + " threadCount: "
            + threadCount
            + " libyuvMode: "
            + libyuvMode);

    setContentView(R.layout.show_image);
    imageView = (ImageView) findViewById(R.id.exoImageView);
    timingView = (TextView) findViewById(R.id.timingView);

    String extension = "avif";
    File directory = new File(directoryName);
    List<File> files = new ArrayList<>();
    File[] filesInDirectory = directory.listFiles();
    if (filesInDirectory == null) {
      timingView.setText("No files found");
      return;
    }
    for (File file : filesInDirectory) {
      if (!file.getName().endsWith(extension)) {
        continue;
      }
      files.add(file);
    }

    if (files.isEmpty()) {
      timingView.setText("No ." + extension + " files found");
      return;
    }

    startTime = System.currentTimeMillis();
    new AvifRenderer(files, 0, files.size(), 0).execute((Void[]) null);
  }
}
