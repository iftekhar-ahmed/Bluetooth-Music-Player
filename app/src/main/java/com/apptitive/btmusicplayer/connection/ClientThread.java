package com.apptitive.btmusicplayer.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import com.apptitive.btmusicplayer.utils.Constants;

import java.io.IOException;

/**
 * Created by Iftekhar on 11/8/2014.
 */
public class ClientThread extends Thread {

    private BluetoothSocket mBluetoothSocket;
    private BluetoothAdapter bluetoothAdapter;
    private Context context;

    public ClientThread(Context context, BluetoothDevice bluetoothDevice) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothSocket tempSocket = null;
        try {
            tempSocket = bluetoothDevice.createRfcommSocketToServiceRecord(Constants.SERVICE_UUID);
        } catch (IOException e) {
        }
        mBluetoothSocket = tempSocket;
    }

    @Override
    public void run() {
        bluetoothAdapter.cancelDiscovery();
        try {
            mBluetoothSocket.connect();
        } catch (IOException e) {
            try {
                mBluetoothSocket.close();
            } catch (IOException e1) {
            }
            return;
        }
        Handler handler = new Handler(context.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "One connection established", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void cancel() {
        try {
            mBluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
