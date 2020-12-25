package com.petja.securitycamera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ImageCapture imageCapture;

    private File outputDirectory;
    private ExecutorService cameraExecutor;
    private Button captureButton;
    private PreviewView viewFinder;
    private TextView textView;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        captureButton = findViewById(R.id.camera_capture_button);
        viewFinder = findViewById(R.id.viewFinder);
        textView = findViewById(R.id.text_view);
        imageView = findViewById(R.id.image_view);

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{ Manifest.permission.CAMERA }, 1000);
        }




        // Set up the listener for take photo button
        captureButton.setOnClickListener(e -> {
            takePhoto();
        });

        outputDirectory = getOutputDirectory();

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        if(imageCapture == null) {
            return;
        }

        // Create time-stamped output file to hold the image
        File photoFile = new File(
                outputDirectory,
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US
                ).format(System.currentTimeMillis()) + ".jpg");

        // Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        String msg = "Photo capture succeeded: " + savedUri;
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d("petja", msg);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("petja", "Photo capture failed: ${exc.message}", exception);
                    }
                });
    }

    long time;
    private void startCamera() {
        ListenableFuture cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = (ProcessCameraProvider) cameraProviderFuture.get();
                    Preview preview = new Preview.Builder().build();
                    Log.d("petja", preview.toString());
                    Log.d("petja", preview.toString());
                    Log.d("petja", preview.toString());
                    Log.d("petja", preview.toString());
                    Log.d("petja", preview.toString());
                    preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                    imageCapture = new ImageCapture.Builder().build();
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                    cameraProvider.unbindAll();

                    ImageAnalysis imageAnalysis =
                            new ImageAnalysis.Builder()
                                    .setTargetResolution(new Size(1280, 720))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build();
                    time = System.currentTimeMillis();
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {

                        if(System.currentTimeMillis() - time > 500) {
                            time = System.currentTimeMillis();
                            ImageProxy.PlaneProxy planeProxy = image.getPlanes()[0];
                            int height = image.getHeight();
                            int width = image.getWidth();
                            ByteBuffer byteBuffer = planeProxy.getBuffer();
                            byte[] bytes = new byte[width * height];
                            byteBuffer.get(bytes);

//                            try {
//                                byte p = bytes[width*height-1];
//                                setText((p & 255) + "lum");
//                            } catch (Exception e){
//                                Log.d("petja", "error");
//                            }
                            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                            for(int i = 0;i < height; i++){
                                for (int j = 0;j < width;j++){
                                    int a = width / 255;
                                    int color = Math.min((j) / a, 255);
                                    if(color < 0) {
                                        color = 0;
                                    }
                                    bitmap.setPixel(j, i, Color.rgb(color, color, color));
                                }
                            }
                            imageView.setImageBitmap(bitmap);
                            setText((System.currentTimeMillis() - time) + "ms");
                            Log.d("petja size " , width + "," + height);

                        }
                        image.close();
                    });


                    cameraProvider.bindToLifecycle(((LifecycleOwner)this), cameraSelector, preview, imageCapture, imageAnalysis);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }, ContextCompat.getMainExecutor(this));
    }

    public void setText(String text){
        runOnUiThread(() -> {
            textView.setText(text);
        });
    }

    private File getOutputDirectory() {
        return getExternalCacheDir();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}