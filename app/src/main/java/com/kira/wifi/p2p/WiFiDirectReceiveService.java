package com.kira.wifi.p2p;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static com.kira.wifi.p2p.MainActivity.EXTRA_IP_ADDR;
import static com.kira.wifi.p2p.MainActivity.TCP_CLIENT_ACTION;

public class WiFiDirectReceiveService extends IntentService {
    private static final String TAG = "WiFiDirectReceiveService";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public WiFiDirectReceiveService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e(TAG, "onHandleIntent: start");
        try (ServerSocket service = new ServerSocket(8988)) {
            while (true) {
                try (Socket socket = service.accept()) {
                    Log.e(TAG, "onHandleIntent: " + socket.getInetAddress().getHostAddress());
                    Intent mainIntent = new Intent(TCP_CLIENT_ACTION);
                    mainIntent.putExtra(EXTRA_IP_ADDR, socket.getInetAddress().getHostAddress());
                    sendBroadcast(mainIntent);

                    InputStream inputstream = socket.getInputStream();

                    File dirs = new File(Environment.getExternalStorageDirectory(), "wifip2p");
                    if (!dirs.exists())
                        dirs.mkdirs();
                    String fileName = String.valueOf(System.currentTimeMillis());
                    File f = new File(dirs, fileName);
                    //f.createNewFile();
                    Log.e(TAG, "server: copying files " + f.toString());
                    MainActivity.copyFile(inputstream, new FileOutputStream(f));
                    File newfile = new File(dirs, fileName + "." + FileType.getFileType(new FileInputStream(f)));
                    boolean ret = f.renameTo(newfile);
                    if (ret) {
                        Log.e(TAG, "onHandleIntent: rename ok");
                    }
                    Log.e(TAG, "server: final saving files " + newfile.toString());

                    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri uri = Uri.fromFile(newfile);
                    scanIntent.setData(uri);
                    sendBroadcast(scanIntent);

                } catch (Exception e) {
                    Log.e(TAG, "onHandleIntent: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onHandleIntent: " + e.getMessage());
        }
    }

    public WiFiDirectReceiveService() {
        super("WiFiDirectReceiveService");
    }
}