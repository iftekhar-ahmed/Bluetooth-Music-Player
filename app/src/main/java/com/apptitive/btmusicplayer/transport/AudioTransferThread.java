package com.apptitive.btmusicplayer.transport;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Handler;

/**
 * Created by Iftekhar on 12/21/2014.
 */
public class AudioTransferThread extends Thread {

    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Handler mHandler;

    public AudioTransferThread(BluetoothSocket bluetoothSocket, Handler dataHandler) {
        this.bluetoothSocket = bluetoothSocket;
        mHandler = dataHandler;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {

    }
}
