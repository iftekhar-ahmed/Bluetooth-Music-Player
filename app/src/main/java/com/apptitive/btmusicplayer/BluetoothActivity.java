package com.apptitive.btmusicplayer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.apptitive.btmusicplayer.connection.AcceptThread;
import com.apptitive.btmusicplayer.connection.ConnectThread;

import java.util.Set;

import static com.apptitive.btmusicplayer.utils.Constants.CONNECTION_FAILED;
import static com.apptitive.btmusicplayer.utils.Constants.STATE_CONNECTED;


public class BluetoothActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new BluetoothFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Fragment containing views for bluetooth connections.
     */
    public static class BluetoothFragment extends Fragment implements View.OnClickListener {

        private int mState;

        private Spinner spinnerPairedDevices;
        private BluetoothAdapter bluetoothAdapter;
        private AcceptThread mAcceptThread;
        private ConnectThread mConnectThread;
        private BluetoothSocket mConnectedSocket;
        private ArrayAdapter<BluetoothDevice> arrayAdapterPairedDevices;

        private static final int REQUEST_ENABLE_BT = 100;

        public BluetoothFragment() {
        }

        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mState = msg.what;
                switch (mState) {
                    case STATE_CONNECTED:
                        mConnectedSocket = mAcceptThread.getConnectedSocket();
                        mAcceptThread.cancel();
                        mAcceptThread = null;
                        mConnectThread.cancel();
                        mConnectThread = null;
                        Toast.makeText(getActivity(), "One connection established", Toast.LENGTH_SHORT).show();
                        break;
                    case CONNECTION_FAILED:
                        Toast.makeText(getActivity(), "Could not connect", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        private final BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice nearbyDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    arrayAdapterPairedDevices.add(nearbyDevice);
                }
            }
        };

        private void listPairedDevices() {
            Set<BluetoothDevice> paired_device = bluetoothAdapter.getBondedDevices();
            if (paired_device.size() > 0) {
                arrayAdapterPairedDevices = arrayAdapterPairedDevices == null
                        ? new ArrayAdapter<BluetoothDevice>(getActivity(), android.R.layout.simple_dropdown_item_1line) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView textView = (TextView) super.getView(position, convertView, parent);
                        textView.setText(getItem(position).getName());
                        return textView;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView textView = (TextView) super.getView(position, convertView, parent);
                        textView.setText(getItem(position).getName());
                        return textView;
                    }
                }
                        : arrayAdapterPairedDevices;
                for (BluetoothDevice bluetoothDevice : paired_device) {
                    arrayAdapterPairedDevices.add(bluetoothDevice);
                }
                spinnerPairedDevices.setAdapter(arrayAdapterPairedDevices);
                spinnerPairedDevices.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_bluetooth, container, false);
            return rootView;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            spinnerPairedDevices = (Spinner) view.findViewById(R.id.spinner_paired_devices);
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                listPairedDevices();
                if (bluetoothAdapter.startDiscovery()) {
                    IntentFilter deviceFoundIntentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    getActivity().registerReceiver(deviceFoundReceiver, deviceFoundIntentFilter);
                }
            }
            view.findViewById(R.id.btn_connect).setOnClickListener(this);
            if (bluetoothAdapter == null) {
                Toast.makeText(getActivity(), "Your device does not support Bluetooth", Toast.LENGTH_SHORT).show();
            } else {
                if (bluetoothAdapter.isEnabled()) {
                    mAcceptThread = new AcceptThread(getActivity(), mHandler);
                    mAcceptThread.start();
                } else {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);
                }
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_connect:
                    mConnectThread = new ConnectThread(getActivity(), arrayAdapterPairedDevices.getItem(spinnerPairedDevices.getSelectedItemPosition()), mHandler);
                    mConnectThread.start();
                    break;
            }
        }

        @Override
        public void onDestroy() {
            if (bluetoothAdapter.isEnabled()) {
                getActivity().unregisterReceiver(deviceFoundReceiver);
            }
            try {
                if (mAcceptThread != null) {
                    mAcceptThread.join();
                }
                if (mConnectThread != null) {
                    mConnectThread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            super.onDestroy();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_ENABLE_BT) {
                switch (resultCode) {
                    case RESULT_OK:
                        listPairedDevices();
                        if (bluetoothAdapter.startDiscovery()) {
                            IntentFilter deviceFoundIntentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                            getActivity().registerReceiver(deviceFoundReceiver, deviceFoundIntentFilter);
                        }
                        break;
                    case RESULT_CANCELED:
                        Toast.makeText(getActivity(), "Bluetooth not enabled. Exiting app", Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                }
            }
        }
    }
}
