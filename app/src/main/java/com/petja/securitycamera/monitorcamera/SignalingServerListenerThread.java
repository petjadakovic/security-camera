package com.petja.securitycamera.monitorcamera;

import static org.webrtc.SessionDescription.Type.ANSWER;
import static org.webrtc.SessionDescription.Type.OFFER;

import android.util.Log;

import com.petja.securitycamera.SimpleSdpObserver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class SignalingServerListenerThread extends Thread {

    private static final String TAG = "petja";

    Socket socket;
    MonitorSignalingServer signalingServer;
    MonitorCameraActivity monitorCamera;

    public SignalingServerListenerThread(Socket socket, MonitorSignalingServer signalingServer, MonitorCameraActivity monitorCamera) {
        this.socket = socket;
        this.signalingServer = signalingServer;
        this.monitorCamera = monitorCamera;
    }

    @Override
    public void run() {
        try {
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            JSONObject createOrJoinJSON = new JSONObject();
            createOrJoinJSON.put("action", "create or join");
            createOrJoinJSON.put("room", "foo");
            signalingServer.sendMessage(createOrJoinJSON.toString());
            //startStreamingVideo();
            while(socket.isConnected()) {
                Log.d(TAG, "Monitor listening");
                String dataMessage = dataInputStream.readUTF();
                JSONObject jsonObject = new JSONObject(dataMessage);
                Log.d(TAG, "message " + jsonObject.toString());
                String action = jsonObject.getString("action");
                if(action.equals("message")) {
                    Log.d(TAG, "connectToSignallingServer: got a message");

                        JSONObject message = jsonObject;
                        Log.d(TAG, "connectToSignallingServer: got message " + message);
                        if (message.getString("type").equals("offer")) {
                            Log.d(TAG, "connectToSignallingServer: received an offer ");
                            Log.d(TAG, "before setRemoteDescription");
                            Log.d(TAG, monitorCamera.peerConnection.connectionState().toString());
                            monitorCamera.peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(OFFER, message.getString("sdp")));
                            Log.d(TAG, monitorCamera.peerConnection.connectionState().toString());
                            monitorCamera.doAnswer();
                            Log.d(TAG, monitorCamera.peerConnection.connectionState().toString());
                        } else if (message.getString("type").equals("answer")) {
                            Log.d(TAG, "answer message");
                            monitorCamera.peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(ANSWER, message.getString("sdp")));
                            Log.d(TAG, monitorCamera.peerConnection.connectionState().toString());
                        } else if (message.getString("type").equals("candidate")) {
                            Log.d(TAG, "connectToSignallingServer: receiving candidates");
                            IceCandidate candidate = new IceCandidate(message.getString("id"), message.getInt("label"), message.getString("candidate"));
                            monitorCamera.peerConnection.addIceCandidate(candidate);
                        }

                }
            }
        } catch (Exception e) {
            if (e instanceof SocketException && socket.isClosed()) {
                Log.d("petja", "socket closed");
            } else {
                e.printStackTrace();
            }
        }
    }
}
