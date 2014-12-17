package com.apptitive.bluetootha2dp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
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

import com.apptitive.bluetootha2dp.connection.ClientThread;
import com.apptitive.bluetootha2dp.connection.ServerThread;

import java.util.Set;


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
     * A placeholder fragment containing a simple view.
     */
    public static class BluetoothFragment extends Fragment implements View.OnClickListener {

        static final int REQUEST_ENABLE_BT = 100;
        private BluetoothAdapter bluetoothAdapter;

        private Spinner spinnerPairedDevices;
        private ArrayAdapter<BluetoothDevice> arrayAdapterPairedDevices;

        public BluetoothFragment() {
        }

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
            bluetoothAdapter = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
                    ? BluetoothAdapter.getDefaultAdapter() : (BluetoothAdapter) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
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
            view.findViewById(R.id.btn_start_bluetooth).setOnClickListener(this);
            spinnerPairedDevices = (Spinner) view.findViewById(R.id.spinner_paired_devices);
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                listPairedDevices();
                if (bluetoothAdapter.startDiscovery()) {
                    IntentFilter deviceFoundIntentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    getActivity().registerReceiver(deviceFoundReceiver, deviceFoundIntentFilter);
                }
            }
            view.findViewById(R.id.btn_server).setOnClickListener(this);
            view.findViewById(R.id.btn_client).setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_start_bluetooth:
                    if (bluetoothAdapter == null) {
                        Toast.makeText(getActivity(), "Bluetooth not supported", Toast.LENGTH_SHORT).show();
                    } else if (!bluetoothAdapter.isEnabled()) {
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent, REQUEST_ENABLE_BT);
                    }
                    break;
                case R.id.btn_server:
                    ServerThread serverThread = new ServerThread(getActivity());
                    serverThread.start();
                    break;
                case R.id.btn_client:
                    ClientThread clientThread = new ClientThread(getActivity(), arrayAdapterPairedDevices.getItem(spinnerPairedDevices.getSelectedItemPosition()));
                    clientThread.start();
                    break;
            }
        }

        @Override
        public void onDestroy() {
            getActivity().unregisterReceiver(deviceFoundReceiver);
            super.onDestroy();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_ENABLE_BT) {
                switch (resultCode) {
                    case RESULT_OK:
                        Toast.makeText(getActivity(), "Thank you for enabling bluetooth", Toast.LENGTH_SHORT).show();
                        listPairedDevices();
                        if (bluetoothAdapter.startDiscovery()) {
                            IntentFilter deviceFoundIntentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                            getActivity().registerReceiver(deviceFoundReceiver, deviceFoundIntentFilter);
                        }
                        break;
                    case RESULT_CANCELED:
                        Toast.makeText(getActivity(), "Oops :(", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }
}
