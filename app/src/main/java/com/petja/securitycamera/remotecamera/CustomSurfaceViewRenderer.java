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
    public Activity activity;
    public TextView textView;

    ByteBuffer lastImage;

    public CustomSurfaceViewRenderer(Context context) {
        super(context);
    }

    public CustomSurfaceViewRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    int i = 0;
    long lastFrameTime;

    @Override
    public void onFrame(VideoFrame frame) {
        if(System.currentTimeMillis() < 0) {
      //  if(System.currentTimeMillis() - lastFrameTime > 500) {
            frame.retain();

            long time = System.currentTimeMillis();
            VideoFrame.I420Buffer i420Buffer = frame.getBuffer().toI420();
            byte[] data = createMyNV21Data(i420Buffer);
            checkMotion(i420Buffer, data);
            i420Buffer.release();
            NV21Buffer buffer = new NV21Buffer(data, frame.getBuffer().getWidth(), frame.getBuffer().getHeight(), null);
            VideoFrame videoFrame = new VideoFrame(buffer, frame.getRotation(), frame.getTimestampNs());
            if (i < 10) {
                Log.d("petja", "calc time: " + (System.currentTimeMillis() - time) + "ms");
                i++;
            }
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            frame.release();
            super.onFrame(videoFrame);
            //lastFrameTime = System.currentTimeMillis();
        } else {
            super.onFrame(frame);
        }
    }

    int z = 0;

    @RequiresApi(api = Build.VERSION_CODES.O)
    private ByteBuffer checkMotion(VideoFrame.I420Buffer i420Buffer, byte[] buffer) {
        if (lastImage == null) {
            lastImage = i420Buffer.getDataY();
            return lastImage;
        } else {
            long change = 0;
            int width = i420Buffer.getWidth();
            int height = i420Buffer.getHeight();
            ByteBuffer byteBuffer = i420Buffer.getDataY();
           // ByteBuffer byteBufferCopy = ByteBuffer.wrap(Arrays.copyOf(byteBuffer.array(), byteBuffer.capacity()));
            ByteBuffer byteBufferCopy = ByteBuffer.allocateDirect(byteBuffer.capacity());
            //int blockSize = 8;
            int blockSize = Math.min(width, height) / 40;
            if (i < 10) {
                //Log.d("petja", "blocksize " + blockSize);
                //Log.d("petja", "capacity " + byteBuffer.capacity());
            }
            int strideY = i420Buffer.getStrideY();
            long difference = 0;
            int blockLumen = 0;
            for (int y = 0; y < height / blockSize; y++) {
                for (int x = 0; x < width / blockSize; x++) {

                    int blockA = 0;
                    int blockB = 0;
                    for (int blockI = 0; blockI < blockSize; blockI++) {
                        for (int blockJ = 0; blockJ < blockSize; blockJ++) {
                            int yIndex = y * strideY * blockSize + x * blockSize + strideY * blockI + blockJ;
                            byte yAValue = byteBuffer.get(yIndex);
                            blockA += Byte.toUnsignedInt(yAValue);
                            blockB += Byte.toUnsignedInt(lastImage.get(yIndex));
                            byteBufferCopy.put(yIndex, yAValue);
                            //final byte yValue = data.get(y * i420Buffer.getStrideY() + x);
                            buffer[y * strideY * blockSize + x * blockSize + strideY * blockI + blockJ] = yAValue;
                        }
                    }
//                    if (i == 2 && y == 10 && x < 10) {
//                        Log.d("petja", "blockA Y " + blockA);
//                        Log.d("petja", "blockB Y " + blockB);
//                        Log.d("petja", "----------------------");
//                    }
                    long blockDiff = Math.abs(blockA - blockB);
                    final int ySize = width * height;
                    //if(x % 2 == 0 && y % 2 == 0 || x % 2 == 1 && y % 2 == 1) {
                    if (blockDiff > blockSize * blockSize * 10) {
                        blockLumen = (int) blockDiff;
//                        if (z < 10) {
//                            Log.d("petja", "x " + x + " y " + y);
//                            Log.d("petja", "blockA Y " + blockA);
//                            Log.d("petja", "blockB Y " + blockB);
//                            Log.d("petja", "----------------------");
//                            z++;
//                        }
                        difference += blockDiff;
                        int chromaWidth = width / 2;
                        for (int blockI = 0; blockI < blockSize; blockI += 2) {
                            for (int blockJ = 0; blockJ < blockSize; blockJ += 2) {
                                buffer[y * strideY * blockSize + x * blockSize + strideY * blockI + blockJ] = -30;
                                if (ySize + y * chromaWidth * blockSize + x * blockSize + 1 + blockI * chromaWidth + blockJ < buffer.length) {
                                    buffer[ySize + y * chromaWidth * blockSize + x * blockSize + 0 + blockI * chromaWidth + blockJ] = -19;
                                    buffer[ySize + y * chromaWidth * blockSize + x * blockSize + 1 + blockI * chromaWidth + blockJ] = Byte.MAX_VALUE;
                                }
                            }
                        }
                    }
                }
            }
            lastImage = byteBufferCopy;
            long finalChange = difference;
            int finalBlockLumen = blockLumen;

            activity.runOnUiThread(() -> textView.setText(finalBlockLumen + ""));
            return byteBuffer;
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
        activity.runOnUiThread(() -> iv.setImageBitmap(image));
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
        final int chromaStride = width;
        final int chromaWidth = (width + 1) / 2;
        final int chromaHeight = (height + 1) / 2;
        final int ySize = width * height;
        //final ByteBuffer nv21Buffer = ByteBuffer.allocateDirect(ySize + chromaStride * chromaHeight);
        // We don't care what the array offset is since we only want an array that is direct.
        //byte[] nv21Data = new byte[ySize + chromaStride * chromaHeight];
        if (this.nv21Data == null || this.nv21Data.length != ySize + chromaStride * chromaHeight) {
            nv21Data = new byte[ySize + chromaStride * chromaHeight];
        }
        ByteBuffer uData = i420Buffer.getDataU();
        ByteBuffer vData = i420Buffer.getDataV();
        for (int y = 0; y < chromaHeight; ++y) {
            for (int x = 0; x < chromaWidth; ++x) {
                final byte uValue = uData.get(y * i420Buffer.getStrideU() + x);
                final byte vValue = vData.get(y * i420Buffer.getStrideV() + x);
//                nv21Data[ySize + y * chromaStride + 2 * x + 0] = -19;
//                nv21Data[ySize + y * chromaStride + 2 * x + 1] = Byte.MAX_VALUE;
                nv21Data[ySize + y * chromaStride + 2 * x + 0] = vValue;
                nv21Data[ySize + y * chromaStride + 2 * x + 1] = uValue;
            }
        }
//        for (int y = 0; y < height; ++y) {
//            for (int x = 0; x < width; ++x) {
//                final byte yValue = data.get(y * i420Buffer.getStrideY() + x);
//                nv21Data[y * width + x] = yValue;
//            }
//        }
        return nv21Data;
    }
}
