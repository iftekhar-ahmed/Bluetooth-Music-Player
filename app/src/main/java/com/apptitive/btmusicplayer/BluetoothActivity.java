package com.apptitive.btmusicplayer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
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
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.apptitive.btmusicplayer.connection.AcceptThread;
import com.apptitive.btmusicplayer.connection.ConnectThread;
import com.apptitive.btmusicplayer.transport.AudioStreamThread;
import com.apptitive.btmusicplayer.utils.AudioDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static com.apptitive.btmusicplayer.utils.Constants.CONNECTION_FAILED;
import static com.apptitive.btmusicplayer.utils.Constants.CONNECTION_INTERRUPTED;
import static com.apptitive.btmusicplayer.utils.Constants.DATA_READ;
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
        private Button buttonConnectDevice;
        private Button buttonAudioStream;

        private BluetoothAdapter mBluetoothAdapter;
        private ArrayAdapter<BluetoothDevice> arrayAdapterPairedDevices;
        private BluetoothSocket mConnectedSocket;
        private AcceptThread mAcceptThread;
        private ConnectThread mConnectThread;
        private AudioStreamThread mAudioStreamThread;
        private InputStream audioFileInputStream;
        private AudioTrack audioTrack;

        private final int REQUEST_ENABLE_BT = 100;
        private final int SAMPLE_RATE_IN_HZ = 44100;
        public final int BUFFER_SIZE = AudioTrack.getMinBufferSize(
                SAMPLE_RATE_IN_HZ,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        );

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

        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mState = msg.what;
                switch (mState) {
                    case STATE_CONNECTED:
                        mConnectedSocket = (BluetoothSocket) msg.obj;
                        Toast.makeText(getActivity(), "One connection established", Toast.LENGTH_SHORT).show();
                        mAudioStreamThread = new AudioStreamThread(mConnectedSocket, this, BUFFER_SIZE);
                        mAudioStreamThread.start();
                        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_OUT_STEREO,
                                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE, AudioTrack.MODE_STREAM);
                        audioTrack.play();
                        buttonConnectDevice.setEnabled(false);
                        buttonAudioStream.setEnabled(true);
                        break;
                    case CONNECTION_FAILED:
                        Toast.makeText(getActivity(), "Could not connect", Toast.LENGTH_SHORT).show();
                        buttonConnectDevice.setEnabled(true);
                        buttonAudioStream.setEnabled(false);
                        audioTrack.release();
                        mAcceptThread = new AcceptThread(getActivity(), this);
                        mAcceptThread.start();
                        break;
                    case CONNECTION_INTERRUPTED:
                        Toast.makeText(getActivity(), "Connection was interrupted", Toast.LENGTH_SHORT).show();
                        buttonConnectDevice.setEnabled(true);
                        buttonAudioStream.setEnabled(false);
                        audioTrack.release();
                        mAcceptThread = new AcceptThread(getActivity(), this);
                        mAcceptThread.start();
                        break;
                    case DATA_READ:
                        byte[] audio_data = (byte[]) msg.obj;
                        int byte_count = msg.arg1;
                        if (byte_count != -1) {
                            audioTrack.write(audio_data, 0, byte_count);
                        }
                        break;
                }
            }
        };

        public BluetoothFragment() {
        }

        /*private void sendAudio(InputStream audioFileInputStream) {
            if (mAudioStreamThread == null) {
                return;
            }
            byte[] buffer = new byte[BUFFER_SIZE];

            try {
                while (audioFileInputStream.read(buffer) != -1) {
                    mAudioStreamThread.write(buffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/

        private void listPairedDevices() {
            Set<BluetoothDevice> paired_device = mBluetoothAdapter.getBondedDevices();
            if (paired_device.size() > 0) {
                arrayAdapterPairedDevices = new ArrayAdapter<BluetoothDevice>(getActivity(), android.R.layout.simple_dropdown_item_1line) {
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
                };
                for (BluetoothDevice bluetoothDevice : paired_device) {
                    arrayAdapterPairedDevices.add(bluetoothDevice);
                }
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Toast.makeText(getActivity(), "Your device does not support Bluetooth", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            if (mBluetoothAdapter.isEnabled()) {
                listPairedDevices();
                spinnerPairedDevices.setAdapter(arrayAdapterPairedDevices);
                /*if (mBluetoothAdapter.startDiscovery()) {
                    IntentFilter deviceFoundIntentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    getActivity().registerReceiver(deviceFoundReceiver, deviceFoundIntentFilter);
                }*/
            } else {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (mBluetoothAdapter.isEnabled() && mAcceptThread == null) {
                mAcceptThread = new AcceptThread(getActivity(), mHandler);
                mAcceptThread.start();
            }
        }

        @Override
        public void onDestroy() {
            /*if (mBluetoothAdapter.isEnabled()) {
                getActivity().unregisterReceiver(deviceFoundReceiver);
            }*/
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
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_bluetooth, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            spinnerPairedDevices = (Spinner) view.findViewById(R.id.spinner_paired_devices);
            spinnerPairedDevices.setVisibility(View.VISIBLE);
            buttonConnectDevice = (Button) view.findViewById(R.id.btn_connect);
            buttonConnectDevice.setOnClickListener(this);
            buttonAudioStream = (Button) view.findViewById(R.id.btn_stream_audio);
            buttonAudioStream.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_connect:
                    mConnectThread = new ConnectThread(getActivity(), arrayAdapterPairedDevices.getItem(spinnerPairedDevices.getSelectedItemPosition()), mHandler);
                    mConnectThread.start();
                    break;
                case R.id.btn_stream_audio:
                    /*audioFileInputStream = getResources().openRawResource(R.raw.blur);
                    sendAudio(audioFileInputStream);*/
                    new AudioDecoder(getResources().openRawResourceFd(R.raw.blur).getFileDescriptor(), mAudioStreamThread) {
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_ENABLE_BT) {
                switch (resultCode) {
                    case RESULT_OK:
                        listPairedDevices();
                        spinnerPairedDevices.setAdapter(arrayAdapterPairedDevices);
                        /*if (mBluetoothAdapter.startDiscovery()) {
                            IntentFilter deviceFoundIntentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                            getActivity().registerReceiver(deviceFoundReceiver, deviceFoundIntentFilter);
                        }*/
                        break;
                    case RESULT_CANCELED:
                        Toast.makeText(getActivity(), "Bluetooth not enabled. Exiting app", Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                }
            }
        }
    }
}
