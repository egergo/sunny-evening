package com.egergo.ncskeleton;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.egergo.ncskeleton.Rtp.RtpPacket;
import com.google.libvpx.LibVpxEnc;
import com.google.libvpx.LibVpxEncConfig;
import com.google.libvpx.LibVpxException;
import com.google.libvpx.VpxCodecCxPkt;

public class FrameProcessor extends Thread {

    private static final String TAG = "FrameProcessor";
    
    private final BlockingQueue<byte[]> queue = new SynchronousQueue<byte[]>();
    
    private final UdpManager udpManager;
    private final InetAddress broadcastAddress;
    
    private final Timer encodingTimer;
    private final Timer sendingTimer;

    public FrameProcessor(UdpManager udpManager, InetAddress broadcastAddress, MetricRegistry metricRegistry) throws LibVpxException {
        this.udpManager = udpManager;
        this.broadcastAddress = broadcastAddress;
        
        encodingTimer = metricRegistry.timer("encoding");
        sendingTimer = metricRegistry.timer("sending");
    }

    @Override
    public void run() {        
        LibVpxEnc encoder;        
        try {
            LibVpxEncConfig c = new LibVpxEncConfig(Constants.VIDEO_WIDTH, Constants.VIDEO_HEIGHT);
            c.setErrorResilient(LibVpxEncConfig.VPX_ERROR_RESILIENT_PARTITIONS);
            //c.setRCTargetBitrate(500 * 1024);
            encoder = new LibVpxEnc(c);
        } catch (LibVpxException ex) {
            Log.e(TAG, "Cannot initialize codec: " + ex);
            return;
        }
        
        
        try {
            while (!interrupted()) {
                //byte[] data = queue.take();
                byte[] data = queue.poll(1, TimeUnit.MILLISECONDS);
                if (data != null) {                
                    try {
                        Timer.Context ctx = encodingTimer.time();
                        try {
                            encodedPackets = encoder.encodeFrame(data, LibVpxEnc.VPX_IMG_FMT_YV12, System.nanoTime(), 66, 0, 1);
                        } finally {
                            ctx.stop();
                        }
                        
                        origSeq = seq;
                        for (VpxCodecCxPkt packet : encodedPackets) {
                            sendFrame(packet);
                        }
                    } catch (LibVpxException ex) {
                        Log.w(TAG, "Cannot encode: " + ex);
                    } catch (IOException ex) {
                        Log.w(TAG, "Cannot send: " + ex);
                    }
                } else if (encodedPackets != null) {
                    seq = origSeq;
                    try {
                        for (VpxCodecCxPkt packet : encodedPackets) {
                            sendFrame(packet);
                        }
                    } catch (IOException ex) {
                        Log.w(TAG, "Cannot send: " + ex);
                    }
                }
            }
        } catch (InterruptedException ex) {
            // exit
        } finally {
            if (encoder != null) {
                encoder.close();
            }
        }
    }
    
    int origSeq = 0;
    List<VpxCodecCxPkt> encodedPackets = null;
    
    int seq = 0;
    RtpPacket rtpPacket = new RtpPacket();
    
    byte[] buf = new byte[1500];
    
    private void sendFrame(VpxCodecCxPkt packet) throws IOException {
        if (udpManager != null) {
            rtpPacket.setPayloadType((byte) 34);
            rtpPacket.setSsrc(0x12);
            rtpPacket.setTimestamp(packet.pts & 0xFFFFFFFFl);

            int sent = 0;
            while (sent < packet.sz) {
                seq++;

                rtpPacket.setSequenceNumber(seq);

                int toSend = Math.min(1400, (int) packet.sz - sent);
                rtpPacket.setMarker(sent + toSend >= packet.sz);

                //byte[] buf = new byte[toSend + 12];
                rtpPacket.serialize(buf, packet.buffer, sent, toSend);
                // Log.i(TAG, "[" + seq + "] SEND " + buf.length + " (" + (sent + toSend) + " / " + packet.sz + ") (" + rtpPacket.getTimestamp() + ")" + rtpPacket.isMarker());
                DatagramPacket dgp = new DatagramPacket(buf, toSend + 12, broadcastAddress, 9998);
                
                Timer.Context ctx = sendingTimer.time();
                try {
                    udpManager.sendSync(dgp);
                } finally {
                    ctx.stop();
                }

                sent += toSend;
            }
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
