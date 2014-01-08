package com.egergo.ncskeleton;

import java.net.DatagramPacket;
import java.net.InetAddress;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.libvpx.LibVpxDec;
import com.google.libvpx.LibVpxException;

public class ReceiverActivity extends Activity implements UdpManager.OnPacketListener {

    private static final String TAG = "ReceiverActivity";
    
    private TextView statusText;
    
    private SurfaceView surface;
    private boolean surfaceReady;
    
    private UdpManager udpManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_receiver);
        
        statusText = (TextView) findViewById(R.id.statusText);
        
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
        
        statusText.setText("Waiting for a sender...");
        statusText.setBackgroundColor(Color.RED);

        try {
            decoder = new LibVpxDec(false, true);
        } catch (LibVpxException ex) {
            throw new RuntimeException(ex);
        }

        try {
            udpManager = new UdpManager(NetworkState.getInstance().getIpAddress(), 9998, this);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }          
        
        decodedViewer = new DecodedViewer(surface.getHolder());
        decodedViewer.start();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        decoder.close();
        decoder = null;
        
        decodedViewer.close();
        decodedViewer = null;
        
        udpManager.close();
        udpManager = null;
    }
    
    
    private void onSurfaceChanged() {
        
    }

    private InetAddress sender;

    private RtpReceiver rtpReceiver = new RtpReceiver() {
        @Override
        protected void gotFrame(byte[] buffer, int offset, int length) {
            try {
                decodeFrame(buffer, 0, length);
            } catch (LibVpxException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onPacket(DatagramPacket packet) {
        if (sender == null) {
            sender = packet.getAddress();
            Log.w(TAG, "Receiving from: " + sender);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusText.setText(sender.getHostAddress());
                    statusText.setBackgroundColor(Color.TRANSPARENT);
                }
            });
        }

        if (!packet.getAddress().equals(sender)) {
            return;
        }

        rtpReceiver.receive(packet);
    }
    
    LibVpxDec decoder;
    DecodedViewer decodedViewer;
    
    
    private void onRtpPacket(Rtp.RtpPacket rtp, int offset, int length) {
        
    }
    
    
    private void decodeFrame(byte[] buffer, int offset, int length) throws LibVpxException {
        byte[] decoded = null;
//        Timer.Context ctx2 = decodingTime.time();
//        try {
            decoded = decoder.decodeFrameToBuffer(buffer, offset, length);
//        } finally {
//            ctx2.stop();
//        }

        if (decoded == null) {
            throw new LibVpxException("decoded is null");
        }

        if (decodedViewer != null) {
            decodedViewer.addPicture(decoded);
        }
    }    
    
}
