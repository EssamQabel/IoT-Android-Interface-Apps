package com.sphinx.babytracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class NavigationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        final String address = getIntent().getStringExtra(ConnectActivity.EXTRA_DEVICE_ADDRESS);

        findViewById(R.id.camera_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NavigationActivity.this, CameraActivity.class);
                intent.putExtra(ConnectActivity.EXTRA_DEVICE_ADDRESS, address);
                startActivity(intent);
            }
        });

        findViewById(R.id.sensor_embedded_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NavigationActivity.this, SensorActivity.class);
                intent.putExtra(ConnectActivity.EXTRA_DEVICE_ADDRESS, address);
                startActivity(intent);
            }
        });

        findViewById(R.id.sensor_phone_location_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NavigationActivity.this, SensorPhoneActivity.class);
                intent.putExtra(ConnectActivity.EXTRA_DEVICE_ADDRESS, address);
                startActivity(intent);
            }
        });
    }
}
