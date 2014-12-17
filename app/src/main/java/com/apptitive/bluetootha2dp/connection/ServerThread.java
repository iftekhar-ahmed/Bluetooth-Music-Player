package com.apptitive.bluetootha2dp.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.apptitive.bluetootha2dp.utils.Constants;

import java.io.IOException;

/**
 * Created by Iftekhar on 11/8/2014.
 */
public class ServerThread extends Thread {

    private BluetoothServerSocket mServerSocket;
    private BluetoothAdapter mBluetoothAdapter;
    private Context context;

    public ServerThread(Context context) {
        this.context = context;
        mBluetoothAdapter = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
                ? BluetoothAdapter.getDefaultAdapter() : (BluetoothAdapter) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothServerSocket bluetoothServerSocket = null;
        try {
            bluetoothServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(Constants.SERVICE_NAME, Constants.SERVICE_UUID);
        } catch (Exception exception) {
        }
        mServerSocket = bluetoothServerSocket;
    }

    @Override
    public void run() {
        BluetoothSocket bluetoothSocket = null;
        while (true) {
            try {
                bluetoothSocket = mServerSocket.accept();
            } catch (IOException e) {
                Log.i(getClass().getSimpleName(), "Socket closed");
                break;
            }
            if (bluetoothSocket != null) {
                Handler handler = new Handler(context.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "One connection established", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            }
        }
    }

    public void cancel() {
        try {
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
