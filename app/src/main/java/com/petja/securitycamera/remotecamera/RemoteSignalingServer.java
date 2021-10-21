package com.petja.securitycamera.remotecamera;

import static com.petja.securitycamera.SignalingServerConfig.URL;
import static com.petja.securitycamera.SignalingServerConfig.PORT;

import android.util.Log;

import com.petja.securitycamera.SignalingServer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class RemoteSignalingServer extends SignalingServer {

    RemoteCameraActivity remoteCamera;

    public RemoteSignalingServer(RemoteCameraActivity remoteCamera) {
        this.remoteCamera = remoteCamera;
    }

    public void connectToServer() {
        super.connectToServer();
        SignalingServerListenerThread signalingServerListenerThread = new SignalingServerListenerThread(getSocket(), this, remoteCamera);
        signalingServerListenerThread.start();
    }

}
