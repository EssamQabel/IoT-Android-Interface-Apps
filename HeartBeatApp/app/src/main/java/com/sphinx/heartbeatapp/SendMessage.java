/*** NEGLECT THIS ACTIVITY IT  WAS USED IN TESTING ONLY**/
package com.sphinx.heartbeatapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SendMessage extends Activity {

         // SPP UUID service - this should work for most devices
         private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
         private BluetoothAdapter btAdapter = null;
         private BluetoothServerSocket btSocket = null;
         private ConnectedThread mConnectedThread;
         //The BroadcastReceiver that listens for bluetooth broadcasts
         private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
                  @Override
                  public void onReceive(Context context, Intent intent) {
                           String action = intent.getAction();
                           BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                           if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                                    Toast.makeText(context, "Device found", Toast.LENGTH_SHORT).show();
                           } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                                    Log.i("Transmission", "Device connected");
                                    Toast.makeText(context, "Device connected", Toast.LENGTH_SHORT).show();

                                    BluetoothSocket bluetoothSocket;

                                    try {
                                             btSocket = createBluetoothServerSocket(device);
                                    } catch (IOException e) {
                                    }
                                    try {
                                             bluetoothSocket = btSocket.accept();
                                             mConnectedThread = new ConnectedThread(bluetoothSocket);
                                             mConnectedThread.start();

                                             mConnectedThread.write("#0356~");
                                             Handler handler  = new Handler();
                                             Runnable runnable1 = new Runnable() {
                                                      @Override
                                                      public void run() {
                                                               mConnectedThread.write("#0350~");
                                                      }
                                             };
                                             Runnable runnable2 = new Runnable() {
                                                      @Override
                                                      public void run() {
                                                               mConnectedThread.write("#0380~");
                                                      }
                                             };
                                             Runnable runnable3 = new Runnable() {
                                                      @Override
                                                      public void run() {
                                                               mConnectedThread.write("#0375~");
                                                      }
                                             };
                                             Runnable runnable4 = new Runnable() {
                                                      @Override
                                                      public void run() {
                                                               mConnectedThread.write("#0400~");
                                                      }
                                             };
                                             handler.postDelayed(runnable1, 500);
                                             handler.postDelayed(runnable2, 500);
                                             handler.postDelayed(runnable3, 500);
                                             handler.postDelayed(runnable4, 500);
                                    } catch (IOException e) {
                                             e.printStackTrace();
                                    }


                           } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                                    Toast.makeText(context, "Device discovery finished", Toast.LENGTH_SHORT).show();
                           } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                                    Toast.makeText(context, "Device disconnect requested", Toast.LENGTH_SHORT).show();
                           } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                                    Log.i("Transmission", "Device disconnected");

                                    // Establish the Bluetooth socket connection.
                                    try {
                                             btSocket.close();
                                    } catch (IOException e) {

                                    }
                                    Toast.makeText(context, "Device disconnected", Toast.LENGTH_SHORT).show();
                           }
                  }
         };

         @Override
         public void onCreate(Bundle savedInstanceState) {
                  super.onCreate(savedInstanceState);
                  setContentView(R.layout.activity_send_message);
                  // get Bluetooth adapter
                  btAdapter = BluetoothAdapter.getDefaultAdapter();
                  checkBTState();
                  // Look for changes in bluetooth connectivity
                  IntentFilter filter = new IntentFilter();
                  filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
                  filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
                  filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                  this.registerReceiver(mReceiver, filter);

         }

         private BluetoothServerSocket createBluetoothServerSocket(BluetoothDevice device) throws IOException {

                  return btAdapter.listenUsingRfcommWithServiceRecord("Sphinx", BTMODULEUUID);
                  //creates secure outgoing connecetion with BT device using UUID
         }

         @Override
         public void onResume() {
                  super.onResume();
         }

         @Override
         public void onPause() {
                  super.onPause();
                  try {
                           if (btSocket != null)
                                    //Don't leave Bluetooth sockets open when leaving activity
                                    btSocket.close();
                  } catch (IOException e2) {
                           //insert code to deal with this
                  }
         }

         @Override
         protected void onDestroy() {
                  super.onDestroy();
                  unregisterReceiver(mReceiver);
         }

         //Checks that the Android device Bluetooth is available and prompts to be turned on if off
         private void checkBTState() {

                  if (btAdapter == null) {
                           Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
                  } else {
                           if (btAdapter.isEnabled()) {
                           } else {
                                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                    startActivityForResult(enableBtIntent, 1);
                           }
                  }
         }

         //create new class for connect thread
         private class ConnectedThread extends Thread {
                  private final OutputStream mmOutStream;

                  //creation of the connect thread
                  public ConnectedThread(BluetoothSocket socket) {
                           OutputStream tmpOut = null;
                           try {
                                    //Create I/O streams for connection
                                    tmpOut = socket.getOutputStream();
                           } catch (IOException e) {
                           }
                           mmOutStream = tmpOut;
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