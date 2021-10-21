package com.petja.securitycamera.remotecamera;

import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;

public class RemoteActivitySavedState {

    private static RemoteActivitySavedState instance;

    public static RemoteActivitySavedState getInstance() {
        if(instance == null) {
            instance = new RemoteActivitySavedState();
        }
        return instance;
    }

    public PeerConnection peerConnection;
    public PeerConnectionFactory factory;
    public RemoteCameraActivity.CameraType cameraType;
    public RemoteSignalingServer signalingServer;
}
