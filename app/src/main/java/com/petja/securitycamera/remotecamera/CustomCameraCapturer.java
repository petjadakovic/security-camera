package com.petja.securitycamera.remotecamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.opengl.GLES20;
import android.widget.ImageView;

import org.webrtc.Camera2Capturer;
import org.webrtc.JavaI420Buffer;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoFrame;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class CustomCameraCapturer extends Camera2Capturer {

    public CustomCameraCapturer(Context context, String cameraName, CameraEventsHandler eventsHandler) {
        super(context, cameraName, eventsHandler);
    }

    @Override
    public void startCapture(int width, int height, int framerate) {
        super.startCapture(width, height, framerate);
    }

    private void testI420toNV21Conversion(ImageView iv, VideoFrame.I420Buffer i420Buffer) {
        // create a test I420 frame. Instead of this you can use the actual frame

        final int width = i420Buffer.getWidth();
        final int height = i420Buffer.getHeight();
        //convert to nv21, this is the same as byte[] from onPreviewCallback
        byte[] nv21Data = createNV21Data(i420Buffer);

        //let's test the conversion by converting the NV21 data to jpg and showing it in a bitmap.
        YuvImage yuvImage = new YuvImage(nv21Data, ImageFormat.NV21,width,height,null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        byte[] imageBytes = out.toByteArray();
        Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        iv.setImageBitmap(image);
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

        private static void bitmapToI420(Bitmap src, JavaI420Buffer dest) {
        int width = src.getWidth();
        int height = src.getHeight();

        if (width != dest.getWidth() || height != dest.getHeight())
            return;

        int strideY = dest.getStrideY();
        int strideU = dest.getStrideU();
        int strideV = dest.getStrideV();
        ByteBuffer dataY = dest.getDataY();
        ByteBuffer dataU = dest.getDataU();
        ByteBuffer dataV = dest.getDataV();

        for (int line = 0; line < height; line++) {
            if (line % 2 == 0) {
                for (int x = 0; x < width; x += 2) {
                    int px = src.getPixel(x, line);
                    byte r = (byte) ((px >> 16) & 0xff);
                    byte g = (byte) ((px >> 8) & 0xff);
                    byte b = (byte) (px & 0xff);

                    dataY.put(line * strideY + x, (byte) (((66 * r + 129 * g + 25 * b) >> 8) + 16));
                    dataU.put(line / 2 * strideU + x / 2, (byte) (((-38 * r + -74 * g + 112 * b) >> 8) + 128));
                    dataV.put(line / 2 * strideV + x / 2, (byte) (((112 * r + -94 * g + -18 * b) >> 8) + 128));

                    px = src.getPixel(x + 1, line);
                    r = (byte) ((px >> 16) & 0xff);
                    g = (byte) ((px >> 8) & 0xff);
                    b = (byte) (px & 0xff);

                    dataY.put(line * strideY + x, (byte) (((66 * r + 129 * g + 25 * b) >> 8) + 16));
                }
            } else {
                for (int x = 0; x < width; x += 1) {
                    int px = src.getPixel(x, line);
                    byte r = (byte) ((px >> 16) & 0xff);
                    byte g = (byte) ((px >> 8) & 0xff);
                    byte b = (byte) (px & 0xff);

                    dataY.put(line * strideY + x, (byte) (((66 * r + 129 * g + 25 * b) >> 8) + 16));
                }
            }
        }
    }
}
