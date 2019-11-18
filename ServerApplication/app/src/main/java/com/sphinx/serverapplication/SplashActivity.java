package com.sphinx.serverapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Get the MAC address from the ConnectActivity to be resent
        final String address = getIntent().getStringExtra("device_address");

        findViewById(R.id.next_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Make an intent to start next activity while taking an extra which is the MAC address.
                Intent i = new Intent(SplashActivity.this, MainActivity.class);
                i.putExtra("device_address", address);
                startActivity(i);
            }
        });


    }
}
