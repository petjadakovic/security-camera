package com.petja.securitycamera.monitorcamera;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import static com.petja.securitycamera.SignalingServerConfig.URL;
import static com.petja.securitycamera.SignalingServerConfig.PORT;

import com.petja.securitycamera.FirebaseManager;
import com.petja.securitycamera.SignalingServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class MonitorSignalingServer extends SignalingServer {

    MonitorCameraActivity monitorCamera;

    public MonitorSignalingServer(MonitorCameraActivity monitorCamera) {
        this.monitorCamera = monitorCamera;
    }

    public void connectToServer() {
        super.connectToServer();
        SignalingServerListenerThread signalingServerListenerThread = new SignalingServerListenerThread(getSocket(), this, monitorCamera);
        signalingServerListenerThread.start();
    }

    public static JSONObject getActiveCameras() {
        SignalingServer signalingServer = new SignalingServer();
        try {
            signalingServer.connectToServer();
            signalingServer.sendMessageSync(new JSONObject().put("token", FirebaseManager.getInstance().getToken()).toString());
            signalingServer.sendMessageSync(new JSONObject().put("action", "get_remote_devices").toString());
            DataInputStream dataInputStream = new DataInputStream(signalingServer.getSocket().getInputStream());
            String data = dataInputStream.readUTF();
            signalingServer.stopServer();
            return new JSONObject(data);
        } catch (Exception ignored) {
        }
        return null;
    }

    public static void sendNotificationKey(String key) {
        SignalingServer signalingServer = new SignalingServer();
        try {
            signalingServer.connectToServer();
            signalingServer.sendMessageSync(new JSONObject().put("token", FirebaseManager.getInstance().getToken()).toString());
            signalingServer.sendMessageSync(new JSONObject().put("action", "add_notification_key").put("notification_key", key).toString());
            signalingServer.stopServer();
        } catch (Exception ignored) {
        }
    }
}
