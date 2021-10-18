package com.petja.securitycamera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_camera);

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
    long difference = 0;
    int[] lastImg;
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
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();
                time = System.currentTimeMillis();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
                   //  Log.d("petja", "size " +image.getWidth() + "," + image.getHeight());
                    if(System.currentTimeMillis() - time >= 100) {
                    time = System.currentTimeMillis();
                    ImageProxy.PlaneProxy planeProxy = image.getPlanes()[0];
                    //Log.d("petja", image.getImage().getClass().getName());
                    int height = image.getHeight();
                    int width = image.getWidth();
                    ByteBuffer byteBuffer = planeProxy.getBuffer();
                    long passedTime = System.currentTimeMillis() - time;
                    //setText(passedTime + "ms");
                    int size = 1;
                    int imgHeight = width / size;
                    int imgWidth = height / size;
                    Bitmap bitmap = Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.RGB_565);
                    int counter = 0;
                    int[] colors = new int[imgHeight * imgWidth];
                    int[] colorsCopy = new int[imgHeight * imgWidth];
                    difference = 0;
                    for(int y = 0;y < imgHeight; y++){
                        for (int x = 0;x < imgWidth;x++){
                            byte b = byteBuffer.get(((imgWidth - x - 1) * width) * size + y * size);
                            counter++;
                            int c = (b & 255);
                            // int c = 150;
                            colors[y * imgWidth + x] = Color.rgb(c,c,c);
                            colorsCopy[y * imgWidth + x] = Color.rgb(c,c,c);
//                                    if(lastImg != null){
//                                        difference += Math.abs(lastImg[y * imgWidth + x] - Color.rgb(c,c,c));
//                                    }
                            // bitmap.setPixel(x, y, Color.alpha(c));
                        }
                    }
                    int blockSize = 4;
                    for(int y = 0;y < imgHeight / blockSize; y++){
                        for (int x = 0;x < imgWidth / blockSize;x++){
                            if(lastImg != null){
                                int colorBlockA = 0;
                                int colorBlockB = 0;
                                for(int blockI = 0;blockI < blockSize; blockI++) {
                                    for(int blockJ = 0;blockJ < blockSize; blockJ++) {
                                        colorBlockA += colors[y * imgWidth * blockSize + x * blockSize + imgWidth * blockI + blockJ];
                                        colorBlockB += lastImg[y * imgWidth * blockSize + x * blockSize + imgWidth * blockI + blockJ];
                                    }
                                }
//                                int colorBlockA = colors[y * imgWidth * 2 + x * 2] + colors[y * imgWidth * 2 + x * 2 + 1] + colors[y * imgWidth * 2 + imgWidth + x * 2] + colors[y * imgWidth * 2 + imgWidth + x * 2+ 1];
//                                colorBlockA = colorBlockA / 4;
//                                int colorBlockB = lastImg[y * imgWidth * 2 + x * 2] + lastImg[y * imgWidth * 2 + x * 2 + 1] + lastImg[y * imgWidth * 2 + imgWidth + x * 2] + lastImg[y * imgWidth * 2 + imgWidth + x * 2+ 1];
//                                colorBlockB = colorBlockB / 4;
                                long colorDiff = Math.abs(colorBlockA - colorBlockB);
                                if(colorDiff > 5000000) {
                                   // colors[y * imgWidth * 2 + x * 2] = Color.RED;
                                    difference += colorDiff;
                                    for(int blockI = 0;blockI < blockSize; blockI+=2) {
                                        for(int blockJ = 0;blockJ < blockSize; blockJ += 2) {
                                            colors[y * imgWidth * blockSize + x * blockSize + imgWidth * blockI + blockJ] = Color.RED;
                                        }
                                    }
                                }
                            }
                            // bitmap.setPixel(x, y, Color.alpha(c));
                        }
                    }
                    bitmap.setPixels(colors, 0, imgWidth, 0, 0, imgWidth, imgHeight);
                    passedTime = System.currentTimeMillis() - time;
                    //Log.d("petja", "Total passed time " + passedTime);
                    imageView.setImageBitmap(bitmap);
                    lastImg = colorsCopy;
                    if(difference > 1000000){
                        difference /= 1000000;
                    }
                    setText(difference + " diff");
                    //Log.d("petja size " , width + "," + height);

                        }
                    image.close();
                });


                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
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
