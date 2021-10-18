package com.petja.securitycamera.monitorcamera;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import static com.petja.securitycamera.SignalingServerConfig.URL;
import static com.petja.securitycamera.SignalingServerConfig.PORT;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SignalingServer {

    Socket socket;
    MonitorCameraActivity monitorCamera;
    public boolean running;

    public SignalingServer(MonitorCameraActivity monitorCamera) {
        this.monitorCamera = monitorCamera;
    }

    public void connectToServer() {
        running = true;
        try {
            socket = new Socket(URL, PORT);
            SignalingServerListenerThread signalingServerListenerThread = new SignalingServerListenerThread(socket, this, monitorCamera);
            signalingServerListenerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Object message) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("action", "message");
            jsonObject.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendJSONMessage(jsonObject.toString());
    }

    public void sendJSONMessage(String message) {
        new Thread(() -> {
            try {
                Log.d("petja", "before send " + message);
                if(socket != null && socket.isConnected() && !socket.isClosed()) {
                    Log.d("petja", "before send " + message);
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeUTF(message);
                    dataOutputStream.flush();
                    Log.d("petja", "sent " + message);
                    Log.d("petja", socket.isConnected() + " " + socket.isBound() + " " + socket.isClosed());
                } else {
                    Log.d("petja", "Faled to send message: Socket not connected");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void stopServer() {
        try {
            if(this.socket != null) {
                this.socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
