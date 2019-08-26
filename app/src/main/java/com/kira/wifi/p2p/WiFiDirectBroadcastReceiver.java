package com.kira.wifi.p2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.Collection;
import java.util.List;


public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "WiFiDirectBroadcastReceiver";
    private MainActivity mActiviy;
    private WifiP2pManager mP2pManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager.PeerListListener myPeerListListener;
    private WifiP2pManager.ConnectionInfoListener mConnectionInfoListener;
    private WifiP2pManager.GroupInfoListener mGroupInfoListener;

    WiFiDirectBroadcastReceiver(WifiP2pManager p2pManager, WifiP2pManager.Channel channel,
                                MainActivity activity,
                                WifiP2pManager.PeerListListener peerListListener,
                                WifiP2pManager.ConnectionInfoListener connectionInfoListener,
                                WifiP2pManager.GroupInfoListener groupInfoListener) {
        mP2pManager = p2pManager;
        mChannel = channel;
        mActiviy = activity;
        myPeerListListener = peerListListener;
        mConnectionInfoListener = connectionInfoListener;
        mGroupInfoListener = groupInfoListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                mActiviy.setP2pEnabled(true);
            } else {
                mActiviy.setP2pEnabled(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (mP2pManager != null) {
                mP2pManager.requestPeers(mChannel, myPeerListListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                mP2pManager.requestConnectionInfo(mChannel, mConnectionInfoListener);
                mP2pManager.requestGroupInfo(mChannel, mGroupInfoListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.e(TAG, "onReceive: " + device.deviceName + " " + device.status);
            mActiviy.setDeviceName(device.deviceName);
            mP2pManager.requestConnectionInfo(mChannel, mConnectionInfoListener);
        }
    }
}
