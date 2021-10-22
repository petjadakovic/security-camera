package com.petja.securitycamera.remotecamera;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import org.webrtc.NV21Buffer;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

public class CustomSurfaceViewRenderer extends SurfaceViewRenderer {

    public ImageView imageView;
    public TextView textView;
    public RemoteCameraActivity remoteCameraActivity;

    int[] lastImage;

    public CustomSurfaceViewRenderer(Context context) {
        super(context);
    }

    public CustomSurfaceViewRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    int i = 0;
    long lastFrameTime;
    long time;

    boolean notifyOnMotion;

    int fps;
    long fpsTime;

    String res;

    @Override
    public void onFrame(VideoFrame frame) {

        if(remoteCameraActivity.isPeerConnected()) {
            super.onFrame(frame);
            return;
        }


        time = System.currentTimeMillis();
        frame.retain();

        VideoFrame.I420Buffer i420Buffer = frame.getBuffer().toI420();
        byte[] data = createMyNV21Data(i420Buffer);
        boolean motionDetected = checkMotion(i420Buffer, data);
        if(motionDetected && notifyOnMotion) {
            remoteCameraActivity.signalingServer.sendMotionDetected();
        }


        NV21Buffer buffer = new NV21Buffer(data, frame.getBuffer().getWidth(), frame.getBuffer().getHeight(), null);
        VideoFrame videoFrame = new VideoFrame(buffer, frame.getRotation(), frame.getTimestampNs());
        i420Buffer.release();
        frame.release();

        if (i < 10) {
            Log.d("petja", "calc time: " + (System.currentTimeMillis() - time) + "ms");
            i++;
        }

        fps++;
        if (System.currentTimeMillis() - fpsTime > 1000) {
            String text = fps + "fps " + res;
            remoteCameraActivity.runOnUiThread(() -> textView.setText(text));
            fps = 0;
            fpsTime = System.currentTimeMillis() - System.currentTimeMillis() % 1000;
        }

        super.onFrame(videoFrame);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean checkMotion(VideoFrame.I420Buffer i420Buffer, byte[] buffer) {
        int width = i420Buffer.getWidth();
        int height = i420Buffer.getHeight();
        boolean checkDiff = lastImage != null && lastImage.length == width * height;
        if (!checkDiff) {
            byte[] lastImage = new byte[width * height];
            i420Buffer.getDataY().get(lastImage);
            this.lastImage = new int[lastImage.length];
            for(int i = 0; i < lastImage.length;i++){
                this.lastImage[i] = Byte.toUnsignedInt(lastImage[i]);
            }
            return false;
        } else {
            boolean motionDetected = false;
            //ByteBuffer byteBuffer = i420Buffer.getDataY();
            byte[] yData = new byte[width * height];
            // i420Buffer.getDataY().get(yData);
            System.arraycopy(buffer, 0, yData, 0, yData.length);

            int blockSize = Math.min(width, height) / 50 / 2 * 2;

            int strideY = i420Buffer.getStrideY();
            int blockLumen = 0;
            long time3 = System.currentTimeMillis();

            // System.arraycopy(yData, 0, buffer, 0, width * height);
//            if(checkDiff) {
//                time = System.currentTimeMillis();
//            }


            for (int y = 0; y < height / blockSize; y++) {
                for (int x = 0; x < width / blockSize; x++) {
                    long blockDiff = 0;
                        int yIndex = y * strideY * blockSize + x * blockSize;
                        for (int blockI = 0; blockI < blockSize; blockI++) {
                            for (int blockJ = 0; blockJ < blockSize; blockJ++) {
                                int yValue = Byte.toUnsignedInt(yData[yIndex]);
                                blockDiff += yValue - lastImage[yIndex];
                                lastImage[yIndex] = yValue;
                                yIndex++;
                            }
                            yIndex += strideY - blockSize;
                        }

                    blockDiff = Math.abs(blockDiff);
                    final int ySize = width * height;
                    //      if(x == 15000000 && y == 15) {
                    //           if(x % 2 == 0 && y % 2 == 0 || x % 2 == 1 && y % 2 == 1) {
                    if (blockDiff > blockSize * blockSize * 10) {
                        motionDetected = true;
                        blockLumen = (int) blockDiff;
                        int myY = y * blockSize;
                        if (myY > 0) {
                            myY /= 2;
                        }
                        int myX = x * blockSize;
                        if (myX > 0) {
                            myX /= 2;
                        }
                        for (int blockI = 0; blockI < blockSize; blockI += 1) {
                            for (int blockJ = 0; blockJ < blockSize; blockJ += 1) {
                                //buffer[y * strideY * blockSize + x * blockSize + strideY * blockI + blockJ] = -110;
                                int uvBlockI = blockI;
                                if (uvBlockI > 0) {
                                    uvBlockI /= 2;
                                }
                                int uvBlockJ = blockJ;
                                if (uvBlockJ > 0) {
                                    uvBlockJ /= 2;
                                }
                                int vIndex = ySize + myY * width + myX * 2 + uvBlockI * width + uvBlockJ * 2;
                                int uIndex = vIndex + 1;
                                if (uIndex < buffer.length) {
                                    buffer[vIndex] = -70;
                                    buffer[uIndex] = 127;
                                }
                            }
                        }
                    }
                }
            }
            int finalBlockLumen = blockLumen;
            //Log.d("petja", "time3: " + (System.currentTimeMillis() - time3) + "ms");
            //activity.runOnUiThread(() -> textView.setText(finalBlockLumen + ""));
            return motionDetected;
        }
    }

    private void testI420toNV21Conversion(ImageView iv, VideoFrame.I420Buffer i420Buffer) {
        // create a test I420 frame. Instead of this you can use the actual frame

        final int width = i420Buffer.getWidth();
        final int height = i420Buffer.getHeight();
        //convert to nv21, this is the same as byte[] from onPreviewCallback
        byte[] nv21Data = createNV21Data(i420Buffer);

        //let's test the conversion by converting the NV21 data to jpg and showing it in a bitmap.
        YuvImage yuvImage = new YuvImage(nv21Data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        byte[] imageBytes = out.toByteArray();
        Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        remoteCameraActivity.runOnUiThread(() -> iv.setImageBitmap(image));
    }

    public static byte[] createNV21Data(VideoFrame.I420Buffer i420Buffer) {
        final int width = i420Buffer.getWidth();
        final int height = i420Buffer.getHeight();
        final int chromaStride = width;
        final int chromaWidth = (width + 1) / 2;
        final int chromaHeight = (height + 1) / 2;
        final int ySize = width * height;
        final ByteBuffer nv21Buffer = ByteBuffer.allocateDirect(ySize + chromaStride * chromaHeight);
        // We don't care what the array offset is since we only want an array that is direct.
        @SuppressWarnings("ByteBufferBackingArray") final byte[] nv21Data = nv21Buffer.array();
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                final byte yValue = i420Buffer.getDataY().get(y * i420Buffer.getStrideY() + x);
                nv21Data[y * width + x] = yValue;
            }
        }
        for (int y = 0; y < chromaHeight; ++y) {
            for (int x = 0; x < chromaWidth; ++x) {
                final byte uValue = i420Buffer.getDataU().get(y * i420Buffer.getStrideU() + x);
                final byte vValue = i420Buffer.getDataV().get(y * i420Buffer.getStrideV() + x);
                nv21Data[ySize + y * chromaStride + 2 * x + 0] = vValue;
                nv21Data[ySize + y * chromaStride + 2 * x + 1] = uValue;
            }
        }
        return nv21Data;
    }

    byte[] nv21Data;

    public byte[] createMyNV21Data(VideoFrame.I420Buffer i420Buffer) {

        final int width = i420Buffer.getWidth();
        final int height = i420Buffer.getHeight();
        final int strideU = i420Buffer.getStrideU();
        final int strideV = i420Buffer.getStrideV();
        final int chromaStride = width;
        final int chromaWidth = (width + 1) / 2;
        final int chromaHeight = (height + 1) / 2;
        final int ySize = width * height;
        res = width + "x" + height;
        //final ByteBuffer nv21Buffer = ByteBuffer.allocateDirect(ySize + chromaStride * chromaHeight);
        // We don't care what the array offset is since we only want an array that is direct.
        // final ByteBuffer nv21Buffer = ByteBuffer.allocateDirect(ySize + chromaStride * chromaHeight);
        // We don't care what the array offset is since we only want an array that is direct.
        byte[] nv21Data = new byte[ySize + chromaStride * chromaHeight];

        i420Buffer.getDataY().get(nv21Data, 0, ySize);

        byte[] uData = new byte[(chromaHeight - 1) * strideU + chromaWidth];
        byte[] vData = new byte[(chromaHeight - 1) * strideV + chromaWidth];
        i420Buffer.getDataU().get(uData);
        i420Buffer.getDataV().get(vData);

        for (int y = 0; y < chromaHeight; ++y) {
            for (int x = 0; x < chromaWidth; ++x) {
                nv21Data[ySize + y * chromaStride + 2 * x + 0] = vData[y * strideV + x];
                nv21Data[ySize + y * chromaStride + 2 * x + 1] = uData[y * strideU + x];
            }
        }
        // Log.d("petja", "time2: " + (System.currentTimeMillis() - time2) + "ms");

        return nv21Data;
    }
}
