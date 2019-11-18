package com.sphinx.wallet;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class SensorPhoneActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 9000;
    String longitude, latitude;
    // UI elements
    TextView latitudeTextView, longitudeTextView;
    private boolean mLocationPermissionGranted = false;
    private LocationManager lm;
    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitude = String.valueOf(location.getLongitude());
            latitude = String.valueOf(location.getLatitude());

            updateUI(latitude, longitude);

//            Toast.makeText(SensorPhoneActivity.this, "longitude: " + longitude + " latitude: " + latitude, Toast.LENGTH_SHORT).show();
            if (ActivityCompat.checkSelfPermission(SensorPhoneActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(SensorPhoneActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_phone);

        // Setting UI elements
        latitudeTextView = findViewById(R.id.latitude_text_view);
        longitudeTextView = findViewById(R.id.longitude_text_view);

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        getLocation();
    }

    private void updateUI(String receivedLatitude, String receivedLongitude) {
        latitudeTextView.setText(receivedLatitude);
        longitudeTextView.setText(receivedLongitude);
    }

    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getLocationPermission();
            return;
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
        }

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(SensorPhoneActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                   ActivityCompat.checkSelfPermission(SensorPhoneActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, locationListener);
                handler.postDelayed(this, 2000);
            }
        };
        handler.postDelayed(runnable, 2000);

        Toast.makeText(this, "first longitude: " + longitude + " latitude: " + latitude, Toast.LENGTH_LONG).show();
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
           android.Manifest.permission.ACCESS_FINE_LOCATION)
           == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            getLocation();
        } else {
            ActivityCompat.requestPermissions(this,
               new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
               PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }
}
