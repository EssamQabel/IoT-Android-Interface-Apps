package com.sphinx.wallet;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MoneyActivity extends AppCompatActivity {

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;
    //used to identify handler message
    final int handlerState = 0;
    // String for received input
    private String receivedInput;
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

    private int money = 0;
    private SharedPreferences sharedpreferences;

    // UI elements
    private EditText moneyEditText;
    private TextView moneyTextView;
    private Button withdraButton;
    private Button depositButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_money);

        moneyEditText = findViewById(R.id.money_edit_text);
        withdraButton = findViewById(R.id.withdra_button);
        depositButton = findViewById(R.id.deposit_button);
        moneyTextView = findViewById(R.id.money_label);


        String MyPREFERENCES = "com.sphinx.wallet.myPreferences";

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        money = sharedpreferences.getInt("money", 0);
        moneyTextView.setText(String.valueOf(money));

        withdraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                withdrawMoney(moneyEditText.getText().toString());
            }
        });
        depositButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                depositMoney(moneyEditText.getText().toString());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        thisActivity = this;

        // get Bluetooth adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get the address of the device we want to connect to
        address = getIntent().getStringExtra(ConnectActivity.EXTRA_DEVICE_ADDRESS);

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
                    int endOfLatitudeIndex = recDataString.indexOf("~");
                    try {
                        // make sure there data before '~'
                        if (endOfLatitudeIndex > 0) {
                            //if it starts with # we know it is what we are looking for
                            if (recDataString.charAt(0) == '#') {
                                receivedInput = recDataString.substring(1, endOfLatitudeIndex);
                            }
                            if (receivedInput.equals("000"))
                                finish(); // To finish the money activity and go back to the navigation activity
                            //clear all string data
                            recDataString.delete(0, recDataString.length());
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

    // Destroy the Broadcast Receiver upon exit
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void depositMoney(String amount) {
        if (isValidInput(amount)) {
            money += Integer.valueOf(amount);
            moneyTextView.setText(String.valueOf(money));
        }
    }

    private void withdrawMoney(String amount) {
        if (isValidInput(amount)) {
            if ((money - Integer.valueOf(amount)) >= 0)
                money -= Integer.valueOf(amount);
            moneyTextView.setText(String.valueOf(money));
        }
    }

    private boolean isValidInput(String rawData) {
        return !rawData.equals("");
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
        }


        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putInt("money", money);
        editor.apply();
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
