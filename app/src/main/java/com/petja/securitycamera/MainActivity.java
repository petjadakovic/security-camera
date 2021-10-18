package com.petja.securitycamera;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.petja.securitycamera.monitorcamera.MonitorCameraActivity;
import com.petja.securitycamera.remotecamera.RemoteCameraActivity;

public class MainActivity extends AppCompatActivity {
    private Button remote, monitor, logout;

    private static final String TAG = "petja";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        remote = findViewById(R.id.remote_camera_button);
        monitor = findViewById(R.id.monitor_camera_button);
        logout = findViewById(R.id.logout);

        FirebaseAuth auth = FirebaseAuth.getInstance();


        remote.setOnClickListener(e -> {
            startActivity(new Intent(this, RemoteCameraActivity.class));
        });
        monitor.setOnClickListener(e -> {
            startActivity(new Intent(this, MonitorCameraActivity.class));
        });
        logout.setOnClickListener(view -> {
            auth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

}