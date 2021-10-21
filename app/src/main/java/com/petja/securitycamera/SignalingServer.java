package com.petja.securitycamera;

import static com.petja.securitycamera.SignalingServerConfig.PORT;
import static com.petja.securitycamera.SignalingServerConfig.URL;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SignalingServer {

    private Socket socket;
    private boolean running;

    public SignalingServer() {
        this.running = false;
    }

    public void connectToServer() {
        running = true;
        try {
            socket = new Socket(URL, PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageAction(Object message) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("action", "message");
            jsonObject.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendMessage(jsonObject.toString());
    }

    public void sendMessage(String message) {
        new Thread(() -> {
            try {
                Log.d("petja", "before send " + message);
                if (socket != null && socket.isConnected() && !socket.isClosed()) {
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

    public void sendMessageSync(String message) {
        try {
            Log.d("petja", "before send " + message);
            if (socket != null && socket.isConnected() && !socket.isClosed()) {
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
    }

    private void sendBye() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("action", "bye");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendMessageSync(jsonObject.toString());
    }

    public Socket getSocket() {
        return socket;
    }

    public void stopServer() {
        new Thread(() -> {
            try {
                sendBye();
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
