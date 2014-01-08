package com.egergo.ncskeleton;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.SurfaceHolder;

public class DecodedViewer extends Thread {

    private static final String TAG = "DecodedViewer";

    private final BlockingQueue<byte[]> queue = new SynchronousQueue<byte[]>();
    private final int[] colorBuffer = new int[Constants.VIDEO_WIDTH * Constants.VIDEO_HEIGHT];
    private final SurfaceHolder surfaceHolder;
    
    public DecodedViewer(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }
    
    @Override
    public void run() {
        try {
            while (!interrupted()) {
                byte[] data = queue.take();
                
                ColorSpaces.yv12ToArgb(data, colorBuffer, Constants.VIDEO_WIDTH, Constants.VIDEO_HEIGHT);

                Canvas canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    try {
                        Bitmap orig = Bitmap.createBitmap(colorBuffer, Constants.VIDEO_WIDTH, Constants.VIDEO_HEIGHT, Bitmap.Config.ARGB_8888);
                        Matrix matrix = new Matrix();
                        matrix.postScale((float) canvas.getWidth() / orig.getWidth(), (float) canvas.getHeight() / orig.getHeight());
                        canvas.drawBitmap(orig, matrix, null);
                    } finally {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        } catch (InterruptedException ex) {
            // exit
        }
    }

    public void close() {
        interrupt();
    }
    
    public void addPicture(byte[] data) {
        // remove old unprocessed frame and add new
        queue.poll();
        queue.offer(data);
    }

}