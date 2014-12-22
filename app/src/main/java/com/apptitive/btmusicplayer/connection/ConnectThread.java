package com.apptitive.btmusicplayer.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.apptitive.btmusicplayer.utils.Constants;

import java.io.IOException;

/**
 * Created by Iftekhar on 11/8/2014.
 */
public class ConnectThread extends Thread {

    private BluetoothSocket mBluetoothSocket;
    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private Handler mHandler;

    public ConnectThread(Context context, BluetoothDevice bluetoothDevice, Handler handler) {
        this.context = context;
        mHandler = handler;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.i("bluetooth device", bluetoothDevice.getName());
        BluetoothSocket tempSocket = null;
        try {
            tempSocket = bluetoothDevice.createRfcommSocketToServiceRecord(Constants.SERVICE_UUID);
            Log.i("UUID", Constants.SERVICE_UUID.toString());
        } catch (IOException e) {
        }
        mBluetoothSocket = tempSocket;
    }

    @Override
    public void run() {
        bluetoothAdapter.cancelDiscovery();
        try {
            mBluetoothSocket.connect();
            Log.i("From Client", "server connected");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            try {
                mBluetoothSocket.close();
                mHandler.obtainMessage(Constants.CONNECTION_FAILED).sendToTarget();
            } catch (IOException e) {
            }
            return;
        }
        mHandler.obtainMessage(Constants.STATE_CONNECTED, mBluetoothSocket).sendToTarget();
    }

    public void cancel() {
        try {
            mBluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
