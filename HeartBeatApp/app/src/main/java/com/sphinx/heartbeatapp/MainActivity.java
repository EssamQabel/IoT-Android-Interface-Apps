package com.sphinx.heartbeatapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


import me.tankery.lib.circularseekbar.CircularSeekBar;
import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity {

         // SPP UUID service - this should work for most devices
         private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

         // String for MAC address
         private static String address;

         //used to identify handler message
         final int handlerState = 0;

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
                                    Toast.makeText(context, "Device disconnected", Toast.LENGTH_SHORT).show();
                           }
                  }
         };
         private StringBuilder recDataString = new StringBuilder();
         private ConnectedThread mConnectedThread;
         private String valueInString = "0000";
         private int valueInInt = 0;

         // UI elements
         private TextView heartBeatsTextView;
         private TextView noInputValueTextView;
         private CircularSeekBar circularSeekBar;
         private LinearLayout blackLinearLayout;
         private GifImageView gifImage;

         @SuppressLint("HandlerLeak")
         @Override
         protected void onCreate(Bundle savedInstanceState) {
                  super.onCreate(savedInstanceState);
                  setContentView(R.layout.activity_main);

                  // get Bluetooth adapter
                  btAdapter = BluetoothAdapter.getDefaultAdapter();

                  // Get the address of the device we want to connect to
                  Intent intent = getIntent();
                  address = intent.getStringExtra(ConnectActivity.EXTRA_DEVICE_ADDRESS);

                  // Reference to the animated gif image
                  gifImage = findViewById(R.id.gifImage);

                  // Reference to the text view that is shown when there are no input value (input value <= 0)
                  noInputValueTextView = findViewById(R.id.no_input_value_text_view);

                  // Reference to the text view that display heart beats
                  heartBeatsTextView = findViewById(R.id.heart_beats_text_view);

                  // Reference to the circular seek bar, to show the heart beats range
                  circularSeekBar = findViewById(R.id.circularSeekBar);

                  // Reference to the black linear layout
                  blackLinearLayout = findViewById(R.id.black_linear_layout);

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
                                             // make sure there data before '~'
                                             if (endOfLineIndex > 0) {
                                                      //if it starts with # we know it is what we are looking for
                                                      if (recDataString.charAt(0) == '#')
                                                               // Get the generated value (0000-1023)
                                                               valueInString = recDataString.substring(1, 5);
                                                      //clear all string data
                                                      recDataString.delete(0, recDataString.length());
                                             }
                                    }
                                    // Get the integer representation of the received input, process and display the results
                                    valueInInt = Integer.valueOf(valueInString);
                                    // If we received any thing
                                    if (valueInInt > 1)
                                             updateUIWithNewValue(valueInInt);
                           }
                  };

                  // Look for changes in bluetooth connectivity
                  IntentFilter filter = new IntentFilter();
                  filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
                  filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
                  filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                  this.registerReceiver(mReceiver, filter);
         }

         // Update the UI using the received input
         private void updateUIWithNewValue(int valueInInt) {
                  // If the black screen is visible, then make it invisible
                  if (blackLinearLayout.getVisibility() == View.VISIBLE)
                           blackLinearLayout.setVisibility(View.INVISIBLE);

                  // If no input received, then hide the animated gif, and show the no input message, and vice versa
                  if (valueInInt <= 0) {
                           noInputValueTextView.setVisibility(View.VISIBLE);
                           gifImage.setVisibility(View.INVISIBLE);
                  } else {
                           noInputValueTextView.setVisibility(View.INVISIBLE);
                           gifImage.setVisibility(View.VISIBLE);
                  }

                  // Calculate the heart beats value
                  Double percentage = ((double) valueInInt / 1024);
                  Log.i("Transmission", "percentage = " + percentage);
                  int heartBeats = (int) ((percentage * 60) + 40);
                  Log.i("Transmission", "heartBeats = " + heartBeats);

                  // Display the input
                  heartBeatsTextView.setText(String.valueOf(heartBeats));
                  circularSeekBar.setProgress(Float.valueOf(String.valueOf(heartBeats - 39)));
         }

         // Creates secure outgoing connection with BT device using UUID
         private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
                  return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
         }

         @Override
         protected void onResume() {
                  super.onResume();

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
                                    Toast.makeText(this, "A disconnect exception happened", Toast.LENGTH_SHORT).show();
                                    btSocket.close();
                           } catch (IOException e2) {
                           }
                  }

                  // Start the receiving process
                  mConnectedThread = new ConnectedThread(btSocket);
                  mConnectedThread.start();
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
