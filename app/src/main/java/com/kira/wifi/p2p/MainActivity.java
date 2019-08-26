package com.kira.wifi.p2p;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static android.content.Intent.CATEGORY_OPENABLE;

public class MainActivity extends AppCompatActivity {

    public final static String TCP_CLIENT_ACTION = "tcp.client.action";
    public final static String EXTRA_IP_ADDR = "extra.ip.addr";
    private static final String TAG = "MainActivity";
    protected static final int CHOOSE_FILE_RESULT_CODE = 20;

    private WifiP2pManager mP2pManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver;
    private ListView mPeerListView;
    private WiFiPeerListAdapter mWiFiPeerListAdapter;
    private List<WifiP2pDevice> mPeers;
    private WifiP2pManager.PeerListListener myPeerListListener;
    private WifiP2pManager.ConnectionInfoListener mConnectionInfoListener;
    private WifiP2pManager.GroupInfoListener mGroupInfoListener;
    private FloatingActionButton mScanFab;
    private TextView mRoleText;
    private TextView mGoAddrText;
    private TextView mDevNameText;
    private TextView mNetworkNameText;
    private RotateAnimation mAnimation;
    private boolean mScanState = false;
    private Button mGoSwitchBtn;
    private Button mBrowseBtn;
    private List<String> mClientList;
    private Spinner mClientListSpin;
    private ArrayAdapter<String> mClientListAdapter;
    private WifiP2pInfo mP2pInfo;
    private BroadcastReceiver mMainReceiver;
    private boolean mGroupOwner;
    private String mClientAddr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        /* Open wifi first */
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean ok = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!ok) {
            Toast.makeText(this, "Please turn on location service.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        mScanFab = findViewById(R.id.scan);
        mRoleText = findViewById(R.id.role);
        mGoAddrText = findViewById(R.id.go_addr);
        mGoSwitchBtn = findViewById(R.id.go_switch);
        mBrowseBtn = findViewById(R.id.browse);
        mPeerListView = findViewById(R.id.peer_list);
        mDevNameText = findViewById(R.id.dev_name);
        mNetworkNameText = findViewById(R.id.net_name);
        mClientListSpin = findViewById(R.id.client_list);
        mPeers = new ArrayList<>();
        mWiFiPeerListAdapter = new WiFiPeerListAdapter(getApplicationContext(), mPeers);
        mClientList = new ArrayList<>();
        mClientListAdapter = new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_list_item_single_choice, mClientList);
        mClientListSpin.setAdapter(mClientListAdapter);

        mP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mP2pManager.initialize(getApplicationContext(), getMainLooper(), null);

        resetDisplayUI();

        mGoSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGoSwitchBtn.getText().equals(getString(R.string.go_create))) {
                    createGroup();
                } else {
                    removeGroup();
                }
            }
        });

        mBrowseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
            }
        });

        mPeerListView.setAdapter(mWiFiPeerListAdapter);

        myPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {
                Collection<WifiP2pDevice> refreshedPeers = peerList.getDeviceList();
                if (!refreshedPeers.equals(mPeers)) {
                    mPeers.clear();
                    mPeers.addAll(refreshedPeers);
                    updateP2pListView();
                }

                if (mPeers.size() == 0) {
                    Log.d(TAG, "No devices found");
                }
            }
        };

        mConnectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                mP2pInfo = info;
                if (info.groupFormed && info.isGroupOwner) {
                    setDisplayUI("GO");
                    mGroupOwner = true;
                } else if (info.groupFormed && !info.isGroupOwner){
                    mGroupOwner = false;
                    setDisplayUI("GC");
                }

                if (info.groupFormed) {
                    startService(new Intent(MainActivity.this,
                            WiFiDirectReceiveService.class));
                    mGoAddrText.setText(getString(R.string.go_addr)
                            + info.groupOwnerAddress.getHostAddress());
                } else {
                    resetDisplayUI();
                }
            }
        };

        mGroupInfoListener = new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
         //   mClientList.clear();
         //   for (WifiP2pDevice p2pDevice : group.getClientList()) {
         //       String connDevice = p2pDevice.deviceName + "\n" + p2pDevice.deviceAddress;
         //       Log.e(TAG, "onGroupInfoAvailable: " + connDevice);
         //       mClientList.add(connDevice);
         //   }
         //   updateClientListSpin();
                mNetworkNameText.setText(getString(R.string.net_name) + group.getNetworkName());
            }
        };

        mClientListSpin.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mClientAddr = mClientList.get(position);
                Log.e(TAG, "onItemSelected: address " + mClientAddr);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mAnimation =
                new RotateAnimation(0, 359, RotateAnimation.RELATIVE_TO_SELF,
                        0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mAnimation.setDuration(100);
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setRepeatMode(Animation.RESTART);

        mScanFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mScanState) {
                    startDiscovery();
                } else {
                    stopDiscovery();
                }
            }
        });

        mPeerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WifiP2pDevice p2pDevice = mPeers.get(position);
                Log.d(TAG, "Click Device: " + p2pDevice.deviceName + " " + p2pDevice.deviceAddress);
                if (p2pDevice.status == WifiP2pDevice.AVAILABLE) {
                    connect(p2pDevice);
                } else if (p2pDevice.status == WifiP2pDevice.INVITED) {
                    cancelConnect(p2pDevice);
                } else if (p2pDevice.status == WifiP2pDevice.CONNECTED) {
                    if (!mGroupOwner) {
                        removeGroup();
                    }
                }
            }
        });

        mMainReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (TCP_CLIENT_ACTION.equals(action)) {
                    String address = intent.getStringExtra(EXTRA_IP_ADDR);
                    if (!mClientList.contains(address)) {
                        mClientList.add(address);
                        updateClientListSpin();
                    }
                }
            }
        };

        IntentFilter p2pFilter = new IntentFilter();
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mReceiver = new WiFiDirectBroadcastReceiver(mP2pManager, mChannel, this,
                myPeerListListener, mConnectionInfoListener, mGroupInfoListener);
        registerReceiver(mReceiver, p2pFilter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(TCP_CLIENT_ACTION);
        registerReceiver(mMainReceiver, filter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case CHOOSE_FILE_RESULT_CODE:
                    if (mP2pInfo == null || data == null) {
                        Log.e(TAG, "onActivityResult: mP2pInfo or data null");
                        return;
                    }
                    Uri uri = data.getData();
                    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                            && (null == uri)) {
                        ClipData clipdata = data.getClipData();
                        for (int i = 0; i < clipdata.getItemCount(); i++) {
                            try {
                                String uriStr, addrStr = "";
                                if (!mGroupOwner) {
                                    if (mP2pInfo.groupOwnerAddress != null) {
                                        addrStr = mP2pInfo.groupOwnerAddress.getHostAddress();
                                    }
                                } else {
                                    addrStr = mClientAddr;
                                }

                                if (addrStr == null || addrStr.isEmpty()) {
                                    Log.e(TAG, "onActivityResult: addr empty");
                                    return;
                                }

                                Log.e(TAG, "onActivityResult: address " + addrStr);

                                uriStr = clipdata.getItemAt(i).getUri().toString();

                                Intent serviceIntent = new Intent(this, WiFiDirectSendService.class);
                                serviceIntent.setAction(WiFiDirectSendService.ACTION_SEND_FILE);
                                serviceIntent.putExtra(WiFiDirectSendService.EXTRAS_FILE_PATH, uriStr);
                                serviceIntent.putExtra(WiFiDirectSendService.EXTRAS_ADDRESS, addrStr);
                                serviceIntent.putExtra(WiFiDirectSendService.EXTRAS_PORT, 8988);
                                startService(serviceIntent);
                                //DO something
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    } else if (uri != null) {
                        String uriStr, addrStr = "";
                        if (!mGroupOwner) {
                            if (mP2pInfo.groupOwnerAddress != null) {
                                addrStr = mP2pInfo.groupOwnerAddress.getHostAddress();
                            }
                        } else {
                            addrStr = mClientAddr;
                        }

                        if (addrStr == null || addrStr.isEmpty()) {
                            Log.e(TAG, "onActivityResult: addr empty");
                            return;
                        }

                        Log.e(TAG, "onActivityResult: address " + addrStr);

                        uriStr = uri.toString();
                        Log.d(TAG, "Intent----------- " + uriStr);

                        Intent serviceIntent = new Intent(this, WiFiDirectSendService.class);
                        serviceIntent.setAction(WiFiDirectSendService.ACTION_SEND_FILE);
                        serviceIntent.putExtra(WiFiDirectSendService.EXTRAS_FILE_PATH, uriStr);
                        serviceIntent.putExtra(WiFiDirectSendService.EXTRAS_ADDRESS, addrStr);
                        serviceIntent.putExtra(WiFiDirectSendService.EXTRAS_PORT, 8988);
                        startService(serviceIntent);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void checkPermission() {
        ArrayList<String> permissionList = new ArrayList<String>();
        permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionList.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissionList.add(Manifest.permission.CHANGE_WIFI_STATE);
        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        Iterator<String> it = permissionList.iterator();
        while (it.hasNext()) {
            String permission = it.next();
            Log.e(TAG, "checkPermission: " + permission);
            int hasPermission = ContextCompat.checkSelfPermission(this, permission);
            if (hasPermission == PackageManager.PERMISSION_GRANTED) {
                it.remove();
            }
        }
        if (permissionList.size() == 0) {
            return;
        }
        String[] permissions = permissionList.toArray(new String[0]);
        ActivityCompat.requestPermissions(this, permissions, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unregisterReceiver(mMainReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private class WiFiPeerListAdapter extends BaseAdapter {
        private List<WifiP2pDevice> mPeerDevices;
        private LayoutInflater mInflater;

        public WiFiPeerListAdapter(Context context,
                                   List<WifiP2pDevice> peerDevices) {
            mInflater = LayoutInflater.from(context);
            mPeerDevices = peerDevices;
        }

        public int getCount() {
            return mPeerDevices.size();
        }

        public Object getItem(int position) {
            return mPeerDevices.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) mInflater
                        .inflate(R.layout.p2p_devices, null);
            }

            if (mPeerDevices.size() == 0)
                return vg;

            WifiP2pDevice device = mPeerDevices.get(position);

            ((TextView) vg.findViewById(R.id.device_name)).setText(device.deviceName);
            ((TextView) vg.findViewById(R.id.device_address)).setText(device.deviceAddress);

            String statusStr = "UNAVAILABLE";
            switch (device.status) {
                case WifiP2pDevice.UNAVAILABLE:
                    statusStr = "UNAVAILABLE";
                    break;
                case WifiP2pDevice.AVAILABLE:
                    statusStr = "AVAILABLE";
                    break;
                case WifiP2pDevice.FAILED:
                    statusStr = "FAILED";
                    break;
                case WifiP2pDevice.INVITED:
                    statusStr = "INVITED";
                    break;
                case WifiP2pDevice.CONNECTED:
                    statusStr = "CONNECTED";
                    break;
                default:
                    break;
            }
            ((TextView) vg.findViewById(R.id.device_status)).setText(statusStr);
            return vg;
        }
    }

    private void createGroup() {
        mP2pManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "createGroup onSuccess: ");
                setDisplayUI("GO");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "createGroup onFailure: reason " + reason);
            }
        });
    }

    private void removeGroup() {
        mP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "removeGroup onSuccess: ");
                resetDisplayUI();
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "removeGroup onFailure: reason " + reason);
            }
        });
    }

    private void connect(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        stopDiscovery();

        mP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(), "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "connect onFailure: reason " + reason);
            }
        });
    }

    private void cancelConnect(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        mP2pManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(), "Cancel Connect failed, reason: " +
                                reason,
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "cancelConnect onFailure: reason " + reason);
            }
        });
    }

    public void setP2pEnabled(boolean enabled) {

    }

    @SuppressLint("RestrictedApi")
    private void resetDisplayUI() {
        mGoSwitchBtn.setText(getString(R.string.go_create));
        mRoleText.setText(getString(R.string.role));
        mGoAddrText.setText(getString(R.string.go_addr));
        mNetworkNameText.setText(getString(R.string.net_name));
        mPeers.clear();
        mClientList.clear();
        updateClientListSpin();
        updateP2pListView();
        stopDiscovery();
        mScanFab.clearAnimation();
    }

    @SuppressLint("RestrictedApi")
    private void setDisplayUI(String role) {
        if (role.equalsIgnoreCase("GO")) {
        mGoSwitchBtn.setText(getString(R.string.go_remove));
        mRoleText.setText(getString(R.string.role) + getString(R.string.go));
        } else if (role.equalsIgnoreCase("GC")) {
            mGoSwitchBtn.setText(getString(R.string.go_create));
            mRoleText.setText(getString(R.string.role) + getString(R.string.gc));
        }
    }

    private void updateP2pListView() {
        if (mWiFiPeerListAdapter != null) {
            mWiFiPeerListAdapter.notifyDataSetChanged();
        }
    }

    private void updateClientListSpin() {
        if (mClientListAdapter != null) {
            mClientListAdapter.notifyDataSetChanged();
        }
    }

    private void stopDiscovery() {
        mP2pManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                mScanState = false;
                mScanFab.clearAnimation();
                Log.d(TAG, "stopPeerDiscovery onSuccess: ");
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(), "discoverPeers fail "
                        + reason, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startDiscovery() {
        mP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                mScanState = true;
                mScanFab.setAnimation(mAnimation);
                mScanFab.startAnimation(mAnimation);
                Log.d(TAG, "discoverPeers onSuccess: ");
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(), "discoverPeers fail "
                        + reason, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void setDeviceName(String name) {
        mDevNameText.setText(getString(R.string.dev_name) + name);
    }

    public List<String> getClientList() {
        return mClientList;
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return false;
        }
        return true;
    }
}
