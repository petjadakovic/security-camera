package com.petja.securitycamera.remotecamera;

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

public class SignalingServerListenerThread extends Thread {

    private static final String TAG = "petja";

    Socket socket;
    SignalingServer signalingServer;
    RemoteCameraActivity remoteCamera;

    public SignalingServerListenerThread(Socket socket, SignalingServer signalingServer, RemoteCameraActivity remoteCamera) {
        this.socket = socket;
        this.signalingServer = signalingServer;
        this.remoteCamera = remoteCamera;
    }

    @Override
    public void run() {
        try {
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            JSONObject createOrJoinJSON = new JSONObject();
            createOrJoinJSON.put("action", "create or join");
            createOrJoinJSON.put("room", "foo");
            signalingServer.sendJSONMessage(createOrJoinJSON.toString());
            //startStreamingVideo();
            while(socket.isConnected()) {
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
                } else if(action.equals("joined")) {
                    Log.d(TAG, "connectToSignallingServer: joined");
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
                           // maybeStart();
                        }
                    } else {
                        JSONObject message = (JSONObject) arg;
                        Log.d(TAG, "connectToSignallingServer: got message " + message);
                        if (message.getString("type").equals("offer")) {
                            Log.d(TAG, "connectToSignallingServer: received an offer ");
//                            if (!isInitiator && !isStarted) {
//                                maybeStart();
//                            }
                            Log.d(TAG, "before setRemoteDescription");
                            remoteCamera.peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(OFFER, message.getString("sdp")));
                            remoteCamera.doAnswer();
                        } else if (message.getString("type").equals("answer")) {
                            remoteCamera.peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(ANSWER, message.getString("sdp")));
                        } else if (message.getString("type").equals("candidate")) {
                            Log.d(TAG, "connectToSignallingServer: receiving candidates");
                            IceCandidate candidate = new IceCandidate(message.getString("id"), message.getInt("label"), message.getString("candidate"));
                            remoteCamera.peerConnection.addIceCandidate(candidate);
                        }
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}
