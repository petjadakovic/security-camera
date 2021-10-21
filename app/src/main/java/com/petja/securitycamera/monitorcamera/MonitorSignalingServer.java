package com.petja.securitycamera.monitorcamera;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import static com.petja.securitycamera.SignalingServerConfig.URL;
import static com.petja.securitycamera.SignalingServerConfig.PORT;

import com.petja.securitycamera.SignalingServer;

import java.io.DataOutputStream;
import java.io.IOException;
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
}
