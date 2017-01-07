package com.kps.wificonnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;


public class WiFiBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = "kamta";
    private WifiP2pManager manager;
    private Channel channel;
    private MainActivity activity;

    public WiFiBroadcastReceiver(WifiP2pManager manager, Channel channel, MainActivity mainActivity) {
        this.manager = manager;
        this.channel = channel;
        this.activity = mainActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // UI update to indicate wifi p2p status.

            //Toast.makeText(context, "STATE_CHANGED", Toast.LENGTH_SHORT).show();
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                activity.setIsWifiP2pEnabled(true);
            } else {
                activity.setIsWifiP2pEnabled(false);
                // activity.resetData();
            }
            //Log.d(MainActivity.TAG, "P2P state changed - " + state);

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            //Toast.makeText(context, "PEERS_CHANGED", Toast.LENGTH_SHORT).show();
            if (manager != null) {
                manager.requestPeers(channel, (PeerListListener) activity.getFragmentManager().
                        findFragmentById(R.id.frag_list));
            } else Log.d(MainActivity.TAG, "Managetr null in Broadcast Peer_Changed_Action");
            // Call WifiP2pManager.requestPeers() to get a list of current peers

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            //Respond to new connection or disconnections
            //Log.d(MainActivity.TAG,"P2P_CONNECTION_CHANGED");
            //Toast.makeText(context, "CONNECTION_CHANGED", Toast.LENGTH_SHORT).show();
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (manager == null || networkInfo.isFailover()) return;

            if (networkInfo.isConnected()) {
                manager.requestConnectionInfo(channel, (WifiP2pManager.ConnectionInfoListener) activity);
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing

            // Log.d(MainActivity.TAG, "DEVICE_CHANGED");
            //Toast.makeText(context, "DEVICE_CHANGED", Toast.LENGTH_SHORT).show();
        }
    }
}
