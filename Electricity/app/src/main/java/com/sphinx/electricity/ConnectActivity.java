package com.sphinx.electricity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class ConnectActivity extends AppCompatActivity {

    // EXTRA string to send on to mainactivity
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // textview for connection status
    ListView pairedListView;

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayList<DeviceName> deviceNames;
    private DeviceNameAdapter deviceNameAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        // Set up the ArrayList<DeviceName> object
        deviceNames = new ArrayList<>();
        deviceNameAdapter = new DeviceNameAdapter(this,deviceNames);

        // Find and set up the ListView for paired devices
        pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(deviceNameAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        //It is best to check BT status at onResume in case something has changed while app was paused etc
        checkBTState();

        // Clears the array so items aren't duplicated when resuming from onPause
        deviceNameAdapter.clear();

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices and append to pairedDevices list
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // Add previously paired devices to the array
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                DeviceName deviceName = new DeviceName(device.getName(), device.getAddress());

                // Check if the discovered device already discovered, if so then don't add it
                Boolean newDeviceNotInAdapter = true;
                for (int i = 0; i < deviceNameAdapter.getCount(); i++) {
                    if (deviceName.getDeviceAddress().equals(deviceNameAdapter.getItem(i).getDeviceAddress()))
                        newDeviceNotInAdapter = false;
                }
                if( newDeviceNotInAdapter )
                    deviceNameAdapter.add(deviceName);
            }
        } else {
            deviceNameAdapter.add(new DeviceName("No paired devices found", ""));
        }
    }

    // Set up on-click listener for the list
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            TextView addressTextView = v.findViewById(R.id.device_address_text_view);
            String address = addressTextView.getText().toString();

            // Make an intent to start next activity while taking an extra which is the MAC address.
            Intent i = new Intent(ConnectActivity.this, MainActivity.class);
            i.putExtra(EXTRA_DEVICE_ADDRESS, address);
            startActivity(i);
        }
    };

    // Method to check if the device has Bluetooth and if it is on.
    //Prompts the user to turn it on if it is off
    private void checkBTState() {
        // Check device has Bluetooth and that it is turned on
        mBtAdapter = BluetoothAdapter.getDefaultAdapter(); // CHECK THIS OUT THAT IT WORKS!!!
        if (mBtAdapter == null) {
            Toast.makeText(getBaseContext(), "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (!mBtAdapter.isEnabled()) {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }
}
