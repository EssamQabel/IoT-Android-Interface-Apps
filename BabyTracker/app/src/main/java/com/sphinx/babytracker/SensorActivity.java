package com.sphinx.babytracker;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class SensorActivity extends AppCompatActivity {

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    //used to identify handler message
    final int handlerState = 0;
    // UI elements
    TextView latitudeTextView, longitudeTextView, tempTextView, warningTextView, carbonTextView;
    private Activity thisActivity;
    // Bluetooth connection variables
    private Handler bluetoothIn;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Toast.makeText(context, "Device found", Toast.LENGTH_SHORT).show();
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Toast.makeText(context, "Device connected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(context, "Device discovery finished", Toast.LENGTH_SHORT).show();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                Toast.makeText(context, "Device disconnect requested", Toast.LENGTH_SHORT).show();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                // Establish the Bluetooth socket connection.
                try {
                    btSocket.connect();
                } catch (IOException e) {
                }
//                Toast.makeText(context, "Device disconnected", Toast.LENGTH_SHORT).show();
            }
        }
    };
    private StringBuilder recDataString = new StringBuilder();
    private ConnectedThread mConnectedThread;
    private String receivedLatitude, receivedLongitude, receivedTemp, receivedWarning, receivedCarbon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        thisActivity = this;

        // get Bluetooth adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get the address of the device we want to connect to
        Intent intent = getIntent();
        address = intent.getStringExtra(ConnectActivity.EXTRA_DEVICE_ADDRESS);

        // Setting UI elements
        latitudeTextView = findViewById(R.id.latitude_text_view);
        longitudeTextView = findViewById(R.id.longitude_text_view);
        tempTextView = findViewById(R.id.temp_text_view);
        warningTextView = findViewById(R.id.warning_text_veiw);
        carbonTextView = findViewById(R.id.carbon_text_view);

        // Assign the new handler to handle the received message
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                //if message is what we want
                if (msg.what == handlerState) {
                    // msg.arg1 = bytes from connect thread
                    String readMessage = (String) msg.obj;
                    //keep appending to string until '~'
                    recDataString.append(readMessage);
                    // determine the end-of-line
                    int endOfLineIndex = recDataString.indexOf("~");

                    // The protocol:
                    // #[latitude][~][longitude][!][temperature][@][warning][$]

                    try {
                        if (recDataString.indexOf("$") == -1) {
                            int endOfCarbon = recDataString.indexOf("~");
                            int endOfTemperature = recDataString.indexOf("!");
                            int endOfWarning = recDataString.indexOf("@");

                            if (recDataString.charAt(0) == '#') {
                                receivedCarbon = recDataString.substring(1, endOfCarbon);
                                receivedTemp = recDataString.substring(endOfCarbon + 1, endOfTemperature);
                                receivedWarning = recDataString.substring(endOfTemperature + 1, endOfWarning);
                            }
                            // Get the generated value (0000-1023)
                            //clear all string data
                            recDataString.delete(0, recDataString.length());
                            // Update the ui with the received data
                            updateUI(receivedTemp, receivedWarning, receivedCarbon);

                        } else {
                            int endOfCarbon = recDataString.indexOf("~");
                            int endOfTemperature = recDataString.indexOf("!");
                            int endOfWarning = recDataString.indexOf("@");
                            int endOflatitude = recDataString.indexOf("$");
                            int endOfLongitude = recDataString.indexOf("%");

                            // make sure there data before '~'
                            if (endOfLineIndex > 0) {
                                //if it starts with # we know it is what we are looking for
                                if (recDataString.charAt(0) == '#') {
                                    receivedCarbon = recDataString.substring(1, endOfCarbon);
                                    receivedTemp = recDataString.substring(endOfCarbon + 1, endOfTemperature);
                                    receivedWarning = recDataString.substring(endOfTemperature + 1, endOfWarning);
                                    receivedLatitude = recDataString.substring(endOfWarning + 1, endOflatitude);
                                    receivedLongitude = recDataString.substring(endOflatitude + 1, endOfLongitude);
                                }
                                // Get the generated value (0000-1023)
                                //clear all string data
                                recDataString.delete(0, recDataString.length());
                                // Update the ui with the received data
                                updateUI(receivedLatitude, receivedLongitude, receivedTemp, receivedWarning, receivedCarbon);
                            }
                        }
                    } catch (Exception e) {

                    }
                }
            }
        };

        // Look for changes in bluetooth connectivity
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);

        // Create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Create the BluetoothSocket object, we want to connect to
        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                Toast.makeText(thisActivity, "", Toast.LENGTH_SHORT).show();
                btSocket.close();
            } catch (IOException e2) {
            }
        }

        // Start the receiving process
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    // Creates secure outgoing connection with BT device using UUID
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    private void updateUI(String receivedTemp, String receivedWarning, String receivedCarbon) {
        tempTextView.setText(receivedTemp);
        warningTextView.setText(receivedWarning);
        carbonTextView.setText(receivedCarbon);
    }

    private void updateUI(String receivedLatitude, String receivedLongitude, String receivedTemp, String receivedWarning, String receivedCarbon) {
        latitudeTextView.setText(receivedLatitude);
        longitudeTextView.setText(receivedLongitude);
        tempTextView.setText(receivedTemp);
        warningTextView.setText(receivedWarning);
        carbonTextView.setText(receivedCarbon);
    }

    // Destroy the Broadcast Receiver upon exit
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
        }
    }

    // Create new class for connected thread
    private class ConnectedThread extends Thread {
        // InputStream to get the received data
        private final InputStream mmInStream;

        // Creation of the connect thread
        private ConnectedThread(BluetoothSocket socket) {
            // Get and initialize the InputStream
            InputStream tmpIn = null;
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
        }

        public void run() {
            // Variables to hold the inputted dat
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    // Read bytes from input buffer
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
    }

}
