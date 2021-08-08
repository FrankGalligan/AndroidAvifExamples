package com.example.avifexamples.basiclibavifandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.example.avifexamples.basiclibavifandroid.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

  // Used to load the 'basiclibavifandroid' library on application startup.
  static {
    System.loadLibrary("basiclibavifandroid");
    System.loadLibrary("avif");
  }

  private ActivityMainBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    // Example of a call to a native method
    TextView tv = binding.sampleText;
    tv.setText(avifVersionString());
  }

  /**
   * A native method that is implemented by the 'basiclibavifandroid' native library,
   * which is packaged with this application.
   */
  public native String avifVersionString();
}