package com.sphinx.heartbeatapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceNameAdapter extends ArrayAdapter<DeviceName> {
         public DeviceNameAdapter(Context context, ArrayList<DeviceName> deviceNames) {
                  super(context, 0, deviceNames);
         }

         @Override
         public View getView(int position, View convertView, ViewGroup parent) {

                  final  DeviceName deviceName = getItem(position);

                  if(convertView == null)
                           convertView = LayoutInflater.from(getContext()).inflate(R.layout.device_name_item_list, parent, false);

                  TextView deviceNameTextView = convertView.findViewById(R.id.device_name_text_view);
                  TextView deviceAddressTextView = convertView.findViewById(R.id.device_address_text_view);

                  deviceNameTextView.setText(deviceName.getDeviceName());
                  deviceAddressTextView.setText(deviceName.getDeviceAddress());

                  return  convertView;
         }
}
