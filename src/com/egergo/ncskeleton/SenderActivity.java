package com.egergo.ncskeleton;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.List;
import java.util.TimerTask;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class SenderActivity extends Activity implements Camera.PreviewCallback {

    private static final String TAG = "SenderActivity";

    private SurfaceView surface;
    private boolean surfaceReady;

    private Camera camera;

    private FrameProcessor frameProcessor;
    private UdpManager udpManager;
    
    private LinearLayout statusPanel;
    
    private MetricRegistry metricRegistry;
    
    private java.util.Timer updateTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_sender);

        statusPanel = (LinearLayout) findViewById(R.id.statusPanel);
        statusPanel.setOnClickListener(new OnClickListener() {            
            @Override
            public void onClick(View v) {
                if (metricRegistry != null) {
                    ConsoleReporter.forRegistry(metricRegistry).build().report();
                }
            }
        });
        
        surface = (SurfaceView) findViewById(R.id.surface);
        surface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.i(TAG, "surfaceCreated");
                surfaceReady = true;
                onSurfaceChanged();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.i(TAG, "surfaceDestroyed");
                surfaceReady = false;
                onSurfaceChanged();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.i(TAG, "surfaceChanged format=" + format + " size=" + width + "x" + height);
                onSurfaceChanged();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        metricRegistry = new MetricRegistry();

        initCamera();

        try {
            udpManager = new UdpManager(NetworkState.getInstance().getIpAddress(), 9998, new UdpManager.OnPacketListener() {
                @Override
                public void onPacket(DatagramPacket packet) {
                }
            });            
            frameProcessor = new FrameProcessor(udpManager, NetworkState.getInstance().getBroadcastAddress(), metricRegistry);
            frameProcessor.start();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }        
        
        updateTimer = new java.util.Timer("update-timer");
        updateTimer.scheduleAtFixedRate(new TimerTask() {            
            @Override
            public void run() {
                runOnUiThread(new Runnable() {                    
                    @Override
                    public void run() {
                        Timer encodingTimer = metricRegistry.getTimers().get("encoding");
                        if (encodingTimer != null) {
                            ((TextView) findViewById(R.id.encodingFps)).setText(String.format("Encoding: %.1f FPS", encodingTimer.getOneMinuteRate()));
                            ((TextView) findViewById(R.id.encodingMedianTime)).setText(String.format("Encoding time: %.1f ms", encodingTimer.getSnapshot().getMedian() / 1000000));
                        }
                        Timer sendingTimer = metricRegistry.getTimers().get("sending");
                        if (sendingTimer != null) {
                            ((TextView) findViewById(R.id.sendingPercSec)).setText(String.format("Sending: %.1f/s", sendingTimer.getOneMinuteRate()));
                            ((TextView) findViewById(R.id.sendingMedianTime)).setText(String.format("Sending time: %.1f ms", sendingTimer.getSnapshot().getMedian() / 1000000));                            
                        }
                    }
                });
            }
        }, 0, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        
        updateTimer.cancel();      

        frameProcessor.close();
        frameProcessor = null;
        
        udpManager.close();
        udpManager = null;

        closeCamera();
        
        metricRegistry = null;
    }

    private void initCamera() {
        if (camera != null) {
            Log.w(TAG, "camera already initialized");
            return;
        }

        camera = Camera.open(Constants.CAMERA_ID);

        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewFormat(ImageFormat.YV12);
        parameters.setPreviewSize(Constants.VIDEO_WIDTH, Constants.VIDEO_HEIGHT);
        parameters.setPreviewFpsRange(Constants.PREVIEW_FPS_MIN, Constants.PREVIEW_FPS_MAX);
        camera.setParameters(parameters);

        camera.setPreviewCallback(this);

        onConfigurationChanged();

        onSurfaceChanged();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged " + newConfig);
        onConfigurationChanged();
    }

    private void onConfigurationChanged() {
        setCameraDisplayOrientation(this, 0, camera);
        adjustPreviewSize();
    }

    private void adjustPreviewSize() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        boolean landscape = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;

        int previewWidth = landscape ? Constants.VIDEO_WIDTH : Constants.VIDEO_HEIGHT;
        int previewHeight = landscape ? Constants.VIDEO_HEIGHT : Constants.VIDEO_WIDTH;

        Rect rect = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);

        float ratio = (float) previewWidth / previewHeight;
        int possibleWidth = (int) (rect.height() * ratio);
        if (possibleWidth <= rect.width()) {
            surface.setLayoutParams(new FrameLayout.LayoutParams(possibleWidth, rect.height(), Gravity.CENTER));
        } else {
            surface.setLayoutParams(new FrameLayout.LayoutParams(rect.width(), (int) (rect.width() / ratio), Gravity.CENTER));
        }
    }

    private void closeCamera() {
        if (camera == null) {
            Log.w(TAG, "camera already closed");
            return;
        }

        camera.stopPreview();
        try {
            camera.setPreviewDisplay(null);
        } catch (IOException ex) {
            // ignore
        }
        camera.release();
        camera = null;
    }

    private void onSurfaceChanged() {
        if (camera == null) {
            return;
        }

        if (surfaceReady) {
            try {
                camera.setPreviewCallback(this);
                camera.setPreviewDisplay(surface.getHolder());
                camera.startPreview();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            try {
                camera.stopPreview();
                camera.setPreviewDisplay(null);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        frameProcessor.addPicture(data);
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation) {
        case Surface.ROTATION_0:
            degrees = 0;
            break;
        case Surface.ROTATION_90:
            degrees = 90;
            break;
        case Surface.ROTATION_180:
            degrees = 180;
            break;
        case Surface.ROTATION_270:
            degrees = 270;
            break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

}
