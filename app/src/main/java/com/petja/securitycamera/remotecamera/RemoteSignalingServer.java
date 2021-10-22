package com.petja.securitycamera.remotecamera;

import android.os.Build;
import android.util.Log;

import com.petja.securitycamera.FirebaseManager;
import com.petja.securitycamera.SignalingServer;

import org.json.JSONException;
import org.json.JSONObject;

public class RemoteSignalingServer extends SignalingServer {

    RemoteCameraActivity remoteCamera;
    public static long lastMotionDetected = 0;

    public RemoteSignalingServer(RemoteCameraActivity remoteCamera) {
        this.remoteCamera = remoteCamera;
    }

    public void connectToServer() {
        super.connectToServer();
        SignalingServerListenerThread signalingServerListenerThread = new SignalingServerListenerThread(getSocket(), this, remoteCamera);
        signalingServerListenerThread.start();
        registerRemote();
    }

    private void registerRemote() {
        new Thread(() -> {
            try {
                JSONObject tokenJSON = new JSONObject();
                tokenJSON.put("token", FirebaseManager.getInstance().getToken());
                sendMessageSync(tokenJSON.toString());
                JSONObject registerJSON = new JSONObject();
                registerJSON.put("action", "register_remote");
                registerJSON.put("name", Build.BRAND + ": " + Build.MODEL);
                sendMessageSync(registerJSON.toString());
                Log.d("petja", "remote registered");
            } catch (JSONException ignored) { }
        }).start();
    }

    public void sendMotionDetected() {
        if(System.currentTimeMillis() - lastMotionDetected > 10 * 60 * 1000) {
            lastMotionDetected = System.currentTimeMillis();
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("action", "motion_notification");
                jsonObject.put("notification_key", FirebaseManager.getInstance().notificationToken);
            }catch (Exception ignored) { }
            sendMessageSync(jsonObject.toString());
        }
    }
}
