package com.example.avifjnirenderdirectory;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AvifJniExample";

    /*package*/ static final int RENDER_MODE_LIBAVIF_GAV1 = 0;
    /*package*/ static final int RENDER_MODE_LIBAVIF_DAV1D = 1;
    /*package*/ static final int RENDER_MODE_LIBAVIF_AOM = 2;
    /*package*/ static final int RENDER_MODE_PLATFORM_AVIF = 3;

    private long startTime;

    private Button buttonStart;

    private ImageView imageView;
    private TextView timingView;

    private NativeAvifDecoder avifDecoder;
    private ExecutorService executorService;

    private long decodeResult;
    private Bitmap decodedBitmap;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String renderModeString;
    private int renderMode;

    private int threadCount = 1;
    private int loopCount = 1;
    private int sleep = 1;

    private int fileIndex = 0;

    private int currentLoopCount = 0;

    private File baseFileDir;

    private int totalRenderCount;
    private long totalPaintingMilli;
    private long totalDecodeMilli;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        try {
            avifDecoder = new NativeAvifDecoder();
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library. Check JNI setup and ABI filters.", e);
            Toast.makeText(this, "Error: Native library load failed. App may not function.", Toast.LENGTH_LONG).show();
            // Potentially disable further functionality or show a more prominent error
            return;
        }

        executorService = Executors.newSingleThreadExecutor(); // Or newCachedThreadPool()

        resetConfigScreen();
    }

    private void resetConfigScreen() {
        setContentView(R.layout.config_screen);
        buttonStart = findViewById(R.id.buttonStart);

        baseFileDir = getAppSpecificDirectory();

        TextView tv = findViewById(R.id.filename);
        tv.setText(baseFileDir.toString());

        avifDecoder.decodeMilli = 0;
        avifDecoder.yuvConversionMilli = 0;

        buttonStart.setOnClickListener(bv -> onClickStart(bv));
    }
    private File getAppSpecificDirectory() {
        // getExternalFilesDir() is for "external" storage that is private to the app.
        // It returns null if external storage is not currently available.
        File fileDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (fileDir != null && !fileDir.exists()) {
            if (!fileDir.mkdirs()) {
                Log.e(TAG, "Failed to create app-specific directory: " + fileDir.getAbsolutePath());
                // Fallback or error handling
            }
        }
        if (fileDir != null) {
            Log.d(TAG, "App-specific dir: " + fileDir.getAbsolutePath());
            //Toast.makeText(MainActivity.this, "App-specific dir: " + fileDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "App-specific external storage not available.");
            Toast.makeText(MainActivity.this, "Error: App-specific storage unavailable.", Toast.LENGTH_SHORT).show();
        }
        return fileDir;
    }

    private boolean isChecked(int id) {
        return ((RadioButton) findViewById(id)).isChecked();
    }

    private void onClickStart(View v) {
        if (v.getId() != R.id.buttonStart) return;
        //TextView tv = findViewById(R.id.filename);

        renderModeString = "none";
        if (isChecked(R.id.decoderGav1)) {
            renderMode = RENDER_MODE_LIBAVIF_GAV1;
            renderModeString = "gav1";
        } else if (isChecked(R.id.decoderDav1d)) {
            renderMode = RENDER_MODE_LIBAVIF_DAV1D;
            renderModeString = "dav1d";
        } else if (isChecked(R.id.decoderAom)) {
            renderMode = RENDER_MODE_LIBAVIF_AOM;
            renderModeString = "libaom";
        } else if (isChecked(R.id.decoderPlatformAvif)) {
            renderMode = RENDER_MODE_PLATFORM_AVIF;
            renderModeString = "platform avif";
        }
        Log.d(TAG, "Render mode picked:: " + renderModeString);

        try {
            threadCount =
                    Integer.parseInt(((EditText) findViewById(R.id.threadCount)).getText().toString());
        } catch (Exception e) {
            // ignore.
        }
        threadCount = Math.max(threadCount, 1);

        try {
            loopCount =
                    Integer.parseInt(((EditText) findViewById(R.id.loopCount)).getText().toString());
        } catch (Exception e) {
            // ignore.
        }
        loopCount = Math.max(loopCount, 1);

        try {
            sleep =Integer.parseInt(
                    ((EditText) findViewById(R.id.sleepSeconds)).getText().toString());
        } catch (Exception e) {
            // ignore.
        }
        sleep = Math.max(sleep, 0);

        final String imageDir = baseFileDir.getAbsolutePath();
        renderImages(imageDir);
    }

    private void renderImages(String directoryName) {
        setContentView(R.layout.show_image);
        imageView = (ImageView) findViewById(R.id.exoImageView);
        timingView = (TextView) findViewById(R.id.timingView);

        String extension = "avif";
        File directory = new File(directoryName);
        List<File> files = new ArrayList<>();
        File[] filesInDirectory = directory.listFiles();
        if (filesInDirectory == null) {
            timingView.setText("No files found");
            Toast.makeText(MainActivity.this, "No files found", Toast.LENGTH_LONG).show();
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
            Toast.makeText(MainActivity.this, "No ." + extension + " files found", Toast.LENGTH_LONG).show();
            return;
        }

        // Reset control variables.
        fileIndex = 0;
        currentLoopCount = 0;

        totalRenderCount = 0;
        totalPaintingMilli = 0;
        totalDecodeMilli = 0;

        renderNextImage(files);
    }

    private void makeBackButtonVisible() {
        Button backButton = findViewById(R.id.buttonBackToConfig);
        backButton.setOnClickListener(v -> {
            resetConfigScreen();
        });

        if (backButton != null) {
            backButton.setVisibility(View.VISIBLE);
        }
    }
    private void renderNextImage(List<File> files) {
        if (sleep > 0) {
            try {
                Thread.sleep(sleep * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        final int currentFileIndex = fileIndex;

        startTime = System.currentTimeMillis();

        executorService.submit(() -> {
            try {
                RandomAccessFile raFile = new RandomAccessFile(files.get(currentFileIndex), "r");
                final long fileLength = raFile.length();
                byte[] avifData = new byte[(int) fileLength];
                raFile.readFully(avifData);

                decodeResult = 0;
                if (renderMode == RENDER_MODE_PLATFORM_AVIF) {
                    decodedBitmap = BitmapFactory.decodeByteArray(avifData, 0, avifData.length);
                } else {
                    decodeResult = avifDecoder.decodeAvifNative(avifData, avifData.length, renderMode, threadCount);
                    decodedBitmap = Bitmap.createBitmap(avifDecoder.width, avifDecoder.height, Bitmap.Config.ARGB_8888);
                    decodedBitmap.copyPixelsFromBuffer(avifDecoder.rgbBuffer);
                }

                mainHandler.post(() -> {
                    if (decodeResult == 0) {
                        imageView.setImageBitmap(decodedBitmap);

                        final long paintingTime = System.currentTimeMillis() - startTime;

                        totalRenderCount++;
                        totalPaintingMilli += paintingTime;
                        totalDecodeMilli += avifDecoder.decodeMilli;
                        final long renderAvg = totalPaintingMilli / totalRenderCount;
                        final long decodeAvg = totalDecodeMilli / totalRenderCount;
                        String output = String.format("%s:Avg Render:%d ms Decode:%d ms", renderModeString, renderAvg, decodeAvg);

                        Log.e("###", "painting time for " + renderModeString + ": " + paintingTime);
                        Log.e("###", output);
                        //timingView.setText(renderModeString + ": " + paintingTime + "ms");
                        timingView.setText(output);


                        // Setup for next image.
                        ++fileIndex;
                        if (fileIndex >= files.size()) {
                            ++currentLoopCount;
                        }
                        if (currentLoopCount >= loopCount) {
                            makeBackButtonVisible();
                            return;
                        }
                        renderNextImage(files);
                    } else {
                        Log.e(TAG, "Failed to decode AVIF or result is invalid.");
                        Toast.makeText(MainActivity.this, "Failed to decode AVIF image.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "IOException reading file ", e);
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Error reading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } catch (UnsatisfiedLinkError ule) {
                Log.e(TAG, "UnsatisfiedLinkError (JNI setup issue): ", ule);
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "JNI Link Error: " + ule.getMessage(), Toast.LENGTH_LONG).show();
                });
            } catch (
                    Exception e) { // Catch other exceptions, including those from JNI (like RuntimeException)
                Log.e(TAG, "Exception during AVIF decoding process: ", e);
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "AVIF Decoding error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}