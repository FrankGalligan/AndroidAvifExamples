package com.example.avifexamples.avifrendertest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity
    implements View.OnClickListener, AdapterView.OnItemClickListener {

    /**
     * Id to identify a read external storage permission request.
     */
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 3;

    ArrayList<String> listItems = new ArrayList<>();
    ArrayAdapter<String> adapter;

    private boolean isChecked(int id) {
        return ((RadioButton) findViewById(id)).isChecked();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() != R.id.button_start) return;
        TextView tv = findViewById(R.id.filename);
        Intent exoPlayerIntent = new Intent(this, AvifDecoder.class);
        exoPlayerIntent.putExtra(AvifDecoder.CONTENT_TYPE_FILENAME, tv.getText().toString());
        int renderMode = -1;
        if (isChecked(R.id.decoder_gav1)) {
            renderMode = AvifDecoder.RENDER_MODE_LIBAVIF_GAV1;
        } else if (isChecked(R.id.decoder_dav1d)) {
            renderMode = AvifDecoder.RENDER_MODE_LIBAVIF_DAV1D;
        } else if (isChecked(R.id.decoder_aom)) {
            renderMode = AvifDecoder.RENDER_MODE_LIBAVIF_AOM;
        } else if (isChecked(R.id.decoder_platform_avif)) {
            renderMode = AvifDecoder.RENDER_MODE_PLATFORM_AVIF;
        }
        exoPlayerIntent.putExtra(AvifDecoder.CONTENT_TYPE_RENDER_MODE, renderMode);

        int threadCount = 1;
        try {
            threadCount =
                Integer.parseInt(((EditText) findViewById(R.id.thread_count)).getText().toString());
        } catch (Exception e) {
            // ignore.
        }
        threadCount = Math.max(threadCount, 1);
        exoPlayerIntent.putExtra(AvifDecoder.CONTENT_TYPE_THREAD_COUNT, threadCount);

        int loopCount = 1;
        try {
            loopCount =
                Integer.parseInt(((EditText) findViewById(R.id.loop_count)).getText().toString());
        } catch (Exception e) {
            // ignore.
        }
        loopCount = Math.max(loopCount, 1);
        exoPlayerIntent.putExtra(AvifDecoder.CONTENT_TYPE_LOOP_COUNT, loopCount);

        int sleep = 1;
        try {
            sleep =Integer.parseInt(
                ((EditText) findViewById(R.id.sleep_seconds)).getText().toString());
        } catch (Exception e) {
            // ignore.
        }
        sleep = Math.max(sleep, 0);
        exoPlayerIntent.putExtra(AvifDecoder.CONTENT_TYPE_SLEEP, sleep);

        int libyuvMode = -1;
        if (isChecked(R.id.libyuv_yes)) {
            libyuvMode = AvifDecoder.LIBYUV_MODE_SINGLE_THREADED;
        } else if (isChecked(R.id.libyuv_no)) {
            libyuvMode = AvifDecoder.LIBYUV_MODE_NONE;
        } else if (isChecked(R.id.libyuv_threaded)) {
            libyuvMode = AvifDecoder.LIBYUV_MODE_MULTI_THREADED;
        }
        exoPlayerIntent.putExtra(AvifDecoder.CONTENT_TYPE_LIBYUV_MODE, libyuvMode);

        startActivity(exoPlayerIntent);
    }

    private boolean checkPermissionForReadExternalStorage() {
        int result = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermissionForReadExternalStorage() {
        ActivityCompat.requestPermissions(
            (Activity) this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
            REQUEST_READ_EXTERNAL_STORAGE);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Image files can now be read.
                initializeUi();
            } else {
                TextView tv = (TextView) findViewById(R.id.notice);
                tv.setText("Need to grant permission to read external storage to read images.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TextView tv = findViewById(R.id.filename);
        tv.setText(getExternalFilesDir("images").getAbsolutePath() + "/" +
            listItems.get(position));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!checkPermissionForReadExternalStorage()) {
            try {
                requestPermissionForReadExternalStorage();
            } catch (Exception e) {
                // meh.
            }
        } else {
            initializeUi();
        }
    }

    private void initializeUi() {
        TextView tv = (TextView) findViewById(R.id.notice);
        if (!checkPermissionForReadExternalStorage()) {
            tv.setText("Grant permissions for storage!");
            return;
        }
        findViewById(R.id.button_start).setOnClickListener(this);
        ListView list = findViewById(R.id.image_list);
        list.setOnItemClickListener(this);
        File directory = new File(getExternalFilesDir("images").getAbsolutePath());
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        list.setAdapter(adapter);
        File[] filesInDirectory = directory.listFiles();
        if (filesInDirectory != null) {
            for (File file : filesInDirectory) {
                if (!file.isDirectory()) {
                    continue;
                }
                listItems.add(file.getName());
            }
        }
        if (listItems.size() > 0) {
            Collections.sort(listItems);
            adapter.notifyDataSetChanged();
        } else {
            tv.setText(
                "Restart the app after pushing files in a subdirectory of: "
                    + getExternalFilesDir("images").getAbsolutePath());
        }
    }
}