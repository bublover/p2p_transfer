package com.kira.wifi.p2p;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class WiFiDirectSendService extends IntentService {
    private static final String TAG = "WiFiDirectSendService";
    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_ADDRESS = "go_host";
    public static final String EXTRAS_PORT = "go_port";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public WiFiDirectSendService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            Log.e(TAG, "onHandleIntent: intent null");
            return;
        }
        Context context = getApplicationContext();
        String action = intent.getAction();
        Log.e(TAG, "onHandleIntent: action " + action);
        if (ACTION_SEND_FILE.equals(intent.getAction())) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            Log.e(TAG, "onHandleIntent: uri " + fileUri);
            String host = intent.getExtras().getString(EXTRAS_ADDRESS);
            int port = intent.getExtras().getInt(EXTRAS_PORT);
            Log.e(TAG, "onHandleIntent: host " + host );
            Log.e(TAG, "onHandleIntent: port " + port );
            Socket socket = new Socket();

            try {
                Log.d(TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(TAG, "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    Log.d(TAG, e.toString());
                }
                MainActivity.copyFile(is, stream);
                Log.e(TAG, "Client: Data written " + is.available());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }

    public WiFiDirectSendService() {
        super("WiFiDirectSendService");
    }
}
