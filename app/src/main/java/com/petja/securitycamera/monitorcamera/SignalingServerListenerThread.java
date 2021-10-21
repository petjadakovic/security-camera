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
                if(action.equals("ipaddr")) {
                    Log.d(TAG, "connectToSignallingServer: ipaddr");
                } else if(action.equals("created")) {
                    Log.d(TAG, "connectToSignallingServer: created");
                } else if(action.equals("full")) {
                    Log.d(TAG, "connectToSignallingServer: full");
                } else if(action.equals("join")) {
                    Log.d(TAG, "connectToSignallingServer: join");
                    Log.d(TAG, "connectToSignallingServer: Another peer made a request to join room");
                    Log.d(TAG, "connectToSignallingServer: This peer is the initiator of room");
                    monitorCamera.doCall();
                } else if(action.equals("joined")) {
                    Log.d(TAG, "connectToSignallingServer: joined");
                    monitorCamera.doCall();
                } else if(action.equals("log")) {
                    JSONArray args = jsonObject.getJSONArray("args");
                    for (int i = 0;i < args.length(); i++) {
                        Log.d(TAG, "connectToSignallingServer: " + String.valueOf(args.get(i)));
                    }
                } else if(action.equals("message")) {
                    Log.d(TAG, "connectToSignallingServer: got a message");
                    Object arg = jsonObject.get("message");
                    if(arg instanceof String) {
                        String message = (String) arg;
                        if (message.equals("got user media")) {
                            Log.d(TAG, "before maybe start");
                        }
                    } else {
                        JSONObject message = (JSONObject) arg;
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
