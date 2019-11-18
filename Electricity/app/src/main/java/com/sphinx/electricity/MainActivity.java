package com.sphinx.electricity;

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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    //used to identify handler message
    final int handlerState = 0;

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

    private String fridgeData, washerData, cookerData, warningData;
    private boolean isFanOn = true;

    // UI elements
    TextView fridgeTextView;
    TextView washerTextView;
    TextView cookerTextView;
    TextView warningTextView;
    TextView fanTextView;
    Button fanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the UI elements
        fridgeTextView = findViewById(R.id.fridge_text_view);
        washerTextView = findViewById(R.id.washer_text_view);
        cookerTextView = findViewById(R.id.cooker_text_view);
        warningTextView = findViewById(R.id.warning_text_view);
        fanTextView = findViewById(R.id.fan_text_view);
        fanButton = findViewById(R.id.fan_button);


        // Refer to this activity
        thisActivity = this;

        // get Bluetooth adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get the address of the device we want to connect to
        Intent intent = getIntent();
        address = intent.getStringExtra(ConnectActivity.EXTRA_DEVICE_ADDRESS);

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

                    int endOfFridgeData = recDataString.indexOf("~");
                    int endOfWasherData = recDataString.indexOf("!");
                    int endOfCookerData = recDataString.indexOf("@");
                    int endOfWarningData = recDataString.indexOf("$");
//                    int endOfIsFanWorking = recDataString.indexOf("$");

                    try{
                        // make sure there data before '~'
                        if (endOfFridgeData > 0) {
                            //if it starts with # we know it is what we are looking for
                            if (recDataString.charAt(0) == '#') {
                                fridgeData = recDataString.substring(1, endOfFridgeData);
                                washerData = recDataString.substring(endOfFridgeData + 1, endOfWasherData);
                                cookerData = recDataString.substring(endOfWasherData + 1, endOfCookerData);
                                warningData = recDataString.substring(endOfCookerData + 1, endOfWarningData);
//                            isFanWorking = recDataString.substring(endOfCookerData + 1, endOfIsFanWorking);
                            }
                            //clear all string data
                            recDataString.delete(0, recDataString.length());
                        }
                        updateUI(fridgeData, washerData, cookerData, warningData);
                    }
                    catch (Exception e){

                    }
                }
            }
        };


        // Look for changes in bluetooth connectivity
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        thisActivity.registerReceiver(mReceiver, filter);

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

        fanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isFanOn){
                    fanTextView.setText("OFF");
                    isFanOn = false;
                    mConnectedThread.write("n");
                    fanButton.setText("ON");
                }
                else{
                    fanTextView.setText("ON");
                    isFanOn = true;
                    mConnectedThread.write("y");
                    fanButton.setText("OFF");
                }
            }
        });
    }

    // Creates secure outgoing connection with BT device using UUID
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    // Update the UI using the received data
    private void updateUI(String fridgeData, String washerData, String cookerData, String warningData){
        fridgeTextView.setText(fridgeData);
        washerTextView.setText(washerData);
        cookerTextView.setText(cookerData);
        warningTextView.setText(warningData);
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
        // Streams to get/send the received data
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        // Creation of the connect thread
        private ConnectedThread(BluetoothSocket socket) {
            // Get and initialize the InputStream
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
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

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }
}
