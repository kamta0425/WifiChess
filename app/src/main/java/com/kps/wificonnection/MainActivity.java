package com.kps.wificonnection;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener, WifiP2pManager.ConnectionInfoListener,
        DeviceListFragment.DeviceActionListener {

    public static final String TAG = "kamta";
    public static boolean flag = true;
    boolean retryChannel = false;
    private boolean isWifiP2pEnabled = false;
    IntentFilter intentFilter;
    WifiP2pManager manager;
    static WifiP2pInfo deviceInfo;
    Channel channel;
    BroadcastReceiver receiver = null;
    DeviceDetailFragment deviceDetailFragment;

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiBroadcastReceiver(manager, channel, this);
    }

    public void ButtonClick(View v) {
        switch (v.getId()) {
            case R.id.bOpenWifi:
                if (manager != null && channel != null) {
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                break;
            case R.id.bSearch:
                try {
                    if (!isWifiP2pEnabled) {
                        Toast.makeText(MainActivity.this, R.string.p2p_off_warning, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                            .findFragmentById(R.id.frag_list);
                    if (fragment != null) fragment.onInitiateDiscovery();
                    else Toast.makeText(this, "Null fragment object", Toast.LENGTH_SHORT).show();
                    manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(MainActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(int reasonCode) {
                            Toast.makeText(MainActivity.this, "Discovery Failed : " + reasonCode,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.d(TAG, "Exception = " + e);
                }
                break;
        }
    }

    @Override
    public void onChannelDisconnected() {
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            Log.d(TAG, "MainActivity onChannelDisconnected :- Channel lost. Trying again");
            //resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this, "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, "MainActivity onChannelDisconnected :- Severe! Channel is probably lost premanently. " +
                    "Try Disable/Re-Enable P2P.");
        }
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        deviceDetailFragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        deviceDetailFragment.showDetails(device);
    }

    @Override
    public void connect(final WifiP2pConfig config, final ProgressDialog progressDialog) {
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                //Log.d(TAG, "Device Connected ");
                Toast.makeText(MainActivity.this, "Device Connected ", Toast.LENGTH_SHORT).show();
                //showOptionDialog();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, "Connect failed. Retry.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOptionDialog() {
        String[] list = {"Chess", "Chat", "Share"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Select Option").setItems(list, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    final AsyncTask c = new AsyncTask() {
                        @Override
                        protected void onPreExecute() {
                            Log.d(TAG, "onPreExecute called");
                            flag = false;
                        }

                        @Override
                        protected Void doInBackground(Object[] params) {
                            Log.d(TAG, "doInBackground Socket send");
                            try {
                                ServerSocket serverSocket = new ServerSocket(8888);
                                Socket client = serverSocket.accept();
                                OutputStream oStream = client.getOutputStream();
                                oStream.write(1);
                                Log.d(TAG, "Socket Accepted 1 is written in stream");
                            } catch (IOException e) {
                                Log.d(TAG, "IOException in thread MainActivity" + e);
                            } catch (Exception e) {
                                Log.d(TAG, "Exception in thread MainActivity" + e);
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Object o) {
                            startActivity(new Intent(MainActivity.this, ChessActivity.class));
                            Log.d(TAG, "onPostExecute called");
                        }
                    };
                    c.execute();

                } else if (which == 1) {
                    startActivity(new Intent(MainActivity.this, ChatActivity.class));
                } else {
                    startActivity(new Intent(MainActivity.this, ShareActivity.class));
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
            }

            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
            }

        });
    }

    @Override
    public void cancelDisconnect() {
        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Aborting connection", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(MainActivity.this, "Connect abort request failed. Reason Code: "
                                + reasonCode, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent = new Intent();
        switch (id) {
            case R.id.action_settings:
                Toast.makeText(this, "Setting", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.wifi:
                Toast.makeText(this, "WifiP2p", Toast.LENGTH_SHORT).show();
                break;
            case R.id.chess:
                intent.setClass(MainActivity.this, ChessActivity.class);
                startActivity(intent);
                break;
            case R.id.chat:
                intent.setClass(MainActivity.this, ChatActivity.class);
                startActivity(intent);
                break;
            case R.id.share:
                intent.setClass(MainActivity.this, ShareActivity.class);
                startActivity(intent);
                break;
            case R.id.search:
                Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {   // for connecting multiple devices
        //Log.d(MainActivity.TAG, "onConnectionInfoAvailable called");
        deviceInfo = info;

        if (info.groupFormed && info.isGroupOwner) {
            Log.d(TAG, "IsGroupOwner = " + info.isGroupOwner);
        } else if (info.groupFormed) {
            Log.d(TAG, "IsGroupOwner = " + info.isGroupOwner);
        }else{
            Log.d(TAG, "Group not formed");
        }
    }
}
