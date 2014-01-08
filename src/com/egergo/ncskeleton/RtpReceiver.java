package com.egergo.ncskeleton;

import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Map;

public abstract class RtpReceiver {

    private static final String TAG = "RtpReceiver";

    private final Map<Integer, byte[]> packetCache = new HashMap<Integer, byte[]>();

    private boolean firstReceived;
    private long currentTimestamp;
    private int lastSeq;
    private int firstSeq;
    private int markerSeq;
    private boolean markerReceived;
    private boolean frameSent;
    private byte[] frameBuf = new byte[200000];

    public void receive(DatagramPacket packet) {
        Rtp.RtpPacket rtp = new Rtp.RtpPacket();
        int skip = rtp.deserialize(packet.getData(), packet.getOffset(), packet.getLength());

        if (packetCache.containsKey(rtp.getSequenceNumber())) {
            // packet already seen
            return;
        }

        // TODO: reordering might be an issue
        if (firstReceived || currentTimestamp != rtp.getTimestamp()) {
            // this is a new frame
            markerReceived = false;
            frameSent = false;
            firstSeq = rtp.getSequenceNumber();
            currentTimestamp = rtp.getTimestamp();
            packetCache.clear();
        }

        if (frameSent) {
            return;
        }

        byte[] buffer = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), buffer, 0, packet.getLength());
        packetCache.put(rtp.getSequenceNumber(), buffer);

        if (!markerReceived) {
            markerReceived = rtp.isMarker();
            markerSeq = rtp.getSequenceNumber();
        }

        if (markerReceived) {

            for (int x = firstSeq; x < markerSeq; x++) {
                if (!packetCache.containsKey(x)) {
                    return;
                }
            }

            // frame intact
            int wrote = 0;
            for (int x = firstSeq; x <= markerSeq; x++) {
                byte[] packet2 = packetCache.get(x);
                System.arraycopy(packet2, 12, frameBuf, wrote, packet2.length - 12);
                wrote += packet2.length - 12;
            }

            frameSent = true;
            gotFrame(frameBuf, 0, wrote);

        }
    }

    protected abstract void gotFrame(byte[] buffer, int offset, int length);
}
