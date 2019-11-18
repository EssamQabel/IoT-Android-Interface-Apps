package com.sphinx.wallet;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

//This is for the normal ip accessing
public class CameraActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        final boolean[] isClicked = {false};
        final String[] link = new String[1];

        final EditText linkEditText = findViewById(R.id.link_edit_text);
        Button goButton = findViewById(R.id.go_button);


        final WebView mWebView;
        mWebView = findViewById(R.id.activity_main_webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);


        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl(link[0]);
                handler.postDelayed(this, 1000);
                Log.i("networking", "refreshed");
            }
        };

        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                link[0] = linkEditText.getText().toString();
                mWebView.loadUrl(link[0]);
                isClicked[0] = true;
            }
        });

        handler.postDelayed(runnable, 1000);
    }

}
