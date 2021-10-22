package com.petja.securitycamera.remotecamera;

import static org.webrtc.SessionDescription.Type.ANSWER;
import static org.webrtc.SessionDescription.Type.OFFER;

import android.os.Build;
import android.util.Log;

import com.petja.securitycamera.SimpleSdpObserver;

import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.io.DataInputStream;
import java.net.Socket;
import java.net.SocketException;

public class SignalingServerListenerThread extends Thread {

    private static final String TAG = "petja";

    Socket socket;
    RemoteSignalingServer signalingServer;
    RemoteCameraActivity remoteCamera;

    public SignalingServerListenerThread(Socket socket, RemoteSignalingServer signalingServer, RemoteCameraActivity remoteCamera) {
        this.socket = socket;
        this.signalingServer = signalingServer;
        this.remoteCamera = remoteCamera;
    }

    @Override
    public void run() {
        try {

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

            while (socket.isConnected()) {
                Log.d(TAG, "Remote listening");
                String dataMessage = dataInputStream.readUTF();
                JSONObject jsonObject = new JSONObject(dataMessage);
                Log.d(TAG, "message " + jsonObject.toString());
                String action = jsonObject.getString("action");
                if (action.equals("message")) {
                        remoteCamera.monitorId = jsonObject.getInt("from");
                        JSONObject message = jsonObject;
                        Log.d(TAG, "connectToSignallingServer: got message " + message);
                        if (message.getString("type").equals("offer")) {
                            Log.d(TAG, "connectToSignallingServer: received an offer ");
                            Log.d(TAG, "before setRemoteDescription");
                            Log.d(TAG, remoteCamera.peerConnection.connectionState().toString());
                            remoteCamera.peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(OFFER, message.getString("sdp")));
                            Log.d(TAG, remoteCamera.peerConnection.connectionState().toString());
                            remoteCamera.doAnswer(jsonObject.getInt("from"));
                        } else if (message.getString("type").equals("answer")) {
                            remoteCamera.peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(ANSWER, message.getString("sdp")));
                        } else if (message.getString("type").equals("candidate")) {
                            Log.d(TAG, "connectToSignallingServer: receiving candidates");
                            IceCandidate candidate = new IceCandidate(message.getString("id"), message.getInt("label"), message.getString("candidate"));
                            remoteCamera.peerConnection.addIceCandidate(candidate);
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
