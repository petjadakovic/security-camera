package com.petja.securitycamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.petja.securitycamera.monitorcamera.MonitorCameraActivity;
import com.petja.securitycamera.monitorcamera.MonitorSignalingServer;
import com.petja.securitycamera.remotecamera.RemoteCameraActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.PeerConnectionFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;

public class MainActivity extends Activity {
    private Button remote, monitor;
    TextView logout;
    TextView back;
    TextView empty;
    DrawerLayout drawerLayout;
    TextView email;
    TextView name;
    ImageView menuIcon;
    ImageView userImage;
    ProgressBar progressBar;
    NavigationView navigationView;
    MainActivity mainActivity;

    ListAdapter listAdapter;

    LinearLayout buttons, remotePhones;
    RecyclerView recyclerView;

    private static final String TAG = "petja";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        remote = findViewById(R.id.remote_camera_button);
        monitor = findViewById(R.id.monitor_camera_button);


        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        menuIcon = findViewById(R.id.menuIcon);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        buttons = findViewById(R.id.buttons);
        remotePhones = findViewById(R.id.remotePhones);
        recyclerView = findViewById(R.id.recyclerView);
        back = findViewById(R.id.back);
        empty = findViewById(R.id.empty);
        progressBar = findViewById(R.id.progressBar);

        View view = navigationView.getHeaderView(0);
        name = view.findViewById(R.id.name);
        email = view.findViewById(R.id.email);
        userImage = view.findViewById(R.id.imageView);
        logout = navigationView.findViewById(R.id.logout);

        if (user != null) {
            name.setText(user.getDisplayName());
            email.setText(user.getEmail());
            if (user.getPhotoUrl() != null) {
                downloadAndSetImage(user.getPhotoUrl().toString());
            }
            Log.d("petjalog", "img " + userImage);
        }

        mainActivity = this;

        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        remote.setOnClickListener(v -> {
            Log.d("petja", "before start activity");
            startActivity(new Intent(this, RemoteCameraActivity.class));
        });
        monitor.setOnClickListener(v -> {
            //startActivity(new Intent(this, MonitorCameraActivity.class));
            getPhones();
        });
        logout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
        back.setOnClickListener(v -> {
            buttons.setVisibility(View.VISIBLE);
            remotePhones.setVisibility(View.GONE);
        });

        sendNotificationToken();
    }

    private void downloadAndSetImage(String url) {
        new Thread(() -> {
            try {
                Bitmap bmp = BitmapFactory.decodeStream(new URL(url).openStream());
                Bitmap finalBmp = editImage(bmp);
                runOnUiThread(() -> userImage.setImageBitmap(finalBmp));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private Bitmap editImage(Bitmap bmp) {
        bmp = Bitmap.createScaledBitmap(bmp, 150, 150, false);
        int radius = Math.min(bmp.getHeight(), bmp.getWidth()) / 2;
        int centerX = bmp.getWidth() / 2;
        int centerY = bmp.getHeight() / 2;
        for (int i = 0; i < bmp.getHeight(); i++) {
            for (int j = 0; j < bmp.getWidth(); j++) {
                int lenSquare = (centerY - i) * (centerY - i) + (centerX - j) * (centerX - j);
                if (lenSquare > radius * radius) {
                    bmp.setPixel(j, i, Color.TRANSPARENT);
                }
            }
        }
        return bmp;
    }

    private void getPhones() {
        progressBar.setVisibility(View.VISIBLE);
        buttons.setVisibility(View.GONE);
        remotePhones.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        recyclerView.setAdapter(new ListAdapter(new RemoteDevice[0], mainActivity));
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        new Thread(() -> {
            RemoteDevice[] remoteDevices = null;
            try {
                JSONObject remotes = MonitorSignalingServer.getActiveCameras();
                if (remotes != null) {
                    Log.d("petjalog", "got " + remotes.toString());
                    JSONArray jsonArray = remotes.getJSONArray("devices");
                    remoteDevices = new RemoteDevice[jsonArray.length()];
                    for (int i = 0; i < remoteDevices.length; i++) {
                        remoteDevices[i] = new RemoteDevice(jsonArray.getJSONObject(i).getInt("id"), jsonArray.getJSONObject(i).getString("name"), jsonArray.getJSONObject(i).getString("ip"));
                    }

                }
            } catch (Exception ignore) {
            }
            RemoteDevice[] finalRemoteDevices = remoteDevices;
            runOnUiThread(() -> {
                if (finalRemoteDevices == null || finalRemoteDevices.length == 0) {
                    empty.setVisibility(View.VISIBLE);
                } else {
                    empty.setVisibility(View.GONE);
                    recyclerView.setAdapter(new ListAdapter(finalRemoteDevices, mainActivity));
                    recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                }
                progressBar.setVisibility(View.GONE);
            });
        }).start();
    }

    private void sendNotificationToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    // Get new FCM registration token
                    String token = task.getResult();

                    Log.d(TAG, "got token " + token);
                    FirebaseManager.getInstance().notificationToken = token;

                    new Thread(() -> {
                        int i = 0;
                        //Wait for firebase auth token
                        while (FirebaseManager.getInstance().token == null && i < 10) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            i++;
                        }
                        if (FirebaseManager.getInstance().token != null) {
                            MonitorSignalingServer.sendNotificationKey(token);
                        }
                    }).start();
                });
    }

    public void startMonitorActivity(int remoteDeviceId) {
        MonitorCameraActivity.remoteId = remoteDeviceId;
        startActivity(new Intent(this, MonitorCameraActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        buttons.setVisibility(View.VISIBLE);
        remotePhones.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
    }
}