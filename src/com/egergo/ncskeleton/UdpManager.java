package com.egergo.ncskeleton;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UdpManager {

    private static final String TAG = "UdpManager";

    private final DatagramSocket socket;
    private final UdpReceiver receiver;
    
    public UdpManager(InetAddress address, int port, OnPacketListener listener) throws SocketException {
        socket = new DatagramSocket(port);
        socket.setBroadcast(true);
        socket.setSendBufferSize(1);
        socket.setReceiveBufferSize(256 * 1024);

        receiver = new UdpReceiver(socket, listener);
        receiver.start();
    }
    
    public void sendSync(DatagramPacket packet) throws IOException {
        socket.send(packet);
    }

    public void close() {
        socket.close();
        receiver.shutdown();
    }


    public static class UdpReceiver extends Thread {

        private final DatagramSocket socket;
        private final OnPacketListener listener;

        public UdpReceiver(DatagramSocket socket, OnPacketListener listener) {
            this.socket = socket;
            this.listener = listener;
        }

        @Override
        public void run() {
            DatagramPacket packet = new DatagramPacket(new byte[1500], 1500);
            try {
                while (!interrupted()) {
                    socket.receive(packet);
                    listener.onPacket(packet);
                }
            } catch (IOException ex) {
                Log.w(TAG, "Receive exception: " + ex);
            }
        }

        public void shutdown() {
            interrupt();
        }
    }

    public interface OnPacketListener {
        void onPacket(DatagramPacket packet);
    }
}
