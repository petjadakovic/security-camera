package com.petja.securitycamera.remotecamera;

import static com.petja.securitycamera.SignalingServerConfig.URL;
import static com.petja.securitycamera.SignalingServerConfig.PORT;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SignalingServer {

    Socket socket;
    RemoteCameraActivity remoteCamera;
    public boolean running;

    public SignalingServer(RemoteCameraActivity remoteCamera) {
        this.remoteCamera = remoteCamera;
    }

    public void connectToServer() {
        running = true;
        try {
            socket = new Socket(URL, PORT);
            Log.d("petja", "connected to socket " + socket.isConnected() + socket.isBound());
            SignalingServerListenerThread signalingServerListenerThread = new SignalingServerListenerThread(socket, this, remoteCamera);
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
