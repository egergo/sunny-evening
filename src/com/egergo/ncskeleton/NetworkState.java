package com.egergo.ncskeleton;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public final class NetworkState {

    private static final String TAG = "NetworkState";

    private static volatile NetworkState instance;

    private final Context context;
    private volatile InetAddress ipAddress;
    private volatile InetAddress broadcastAddress;

    private NetworkState(Context context) {
        this.context = context;
        
        // final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        refreshInternal();
    }
    
    public synchronized void refresh() {
        refreshInternal();
    }
    
    private void refreshInternal() {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if (dhcp == null) {
            throw new RuntimeException("no DHCP");
        }
        // Log.w(TAG, "DHCP: " + dhcp);
        // handle null somehow

        byte[] ipArr = new byte[4];
        for (int k = 0; k < 4; k++)
            ipArr[k] = (byte) ((dhcp.ipAddress >> k * 8) & 0xFF);

        try {
            ipAddress = InetAddress.getByAddress(ipArr);
        } catch (UnknownHostException ex) {
            // ignore
        }

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        
        try {
            broadcastAddress = InetAddress.getByAddress(quads);
        } catch (UnknownHostException ex) {
            // ignore
        }        
        
        Log.i(TAG, "ip=" + ipAddress + " broadcast=" + broadcastAddress);
    }
    
    public InetAddress getIpAddress() {
        return ipAddress;
    }
    
    public InetAddress getBroadcastAddress() {
        return broadcastAddress;
    }

    public static NetworkState getInstance() {
        if (instance == null) {
            Log.e(TAG, "NetworkState not yet initialized");
        }
        return instance;
    }

    public synchronized static void init(Context context) {
        if (instance != null) {
            Log.w(TAG, "NetworkState already initialized");
        }
        instance = new NetworkState(context);
    }


    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive " + intent.getAction());

            NetworkState.getInstance().refresh();
        }
    }

}
