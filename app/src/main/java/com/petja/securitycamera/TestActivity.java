package com.petja.securitycamera;

import static org.webrtc.SessionDescription.Type.ANSWER;
import static org.webrtc.SessionDescription.Type.OFFER;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class TestActivity extends AppCompatActivity {
    private Button captureButton;

    private static final String TAG = "petja";
    private static final int RC_CALL = 111;
    public static final String VIDEO_TRACK_ID = "PETJAv0";
    public static final int VIDEO_RESOLUTION_WIDTH = 1280;
    public static final int VIDEO_RESOLUTION_HEIGHT = 720;
    public static final int FPS = 30;

    //private ActivitySamplePeerConnectionBinding binding;
    private PeerConnection peerConnection;
    private EglBase rootEglBase;
    private PeerConnectionFactory factory;
    private VideoTrack videoTrackFromCamera;

    private SurfaceViewRenderer localVideo, remoteVideo;

    private Socket socket;
    private boolean isInitiator;
    private boolean isChannelReady;
    private boolean isStarted;


    MediaConstraints audioConstraints;
    AudioSource audioSource;
    AudioTrack localAudioTrack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        localVideo = findViewById(R.id.surface_view_local);
        remoteVideo = findViewById(R.id.surface_view_remote);

        captureButton = findViewById(R.id.camera_capture_button);

        // Set up the listener for take photo button
        captureButton.setOnClickListener(e -> {
            startActivity(new Intent(this, CameraActivity.class));
        });

        test();
    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            });

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i = 0;i < grantResults.length; i++) {
            if(grantResults[i] == PackageManager.PERMISSION_DENIED) {
                return;
            }
        }
        start();
    }

    private void test() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), perms[0]) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                getApplicationContext(), perms[1]) ==
                PackageManager.PERMISSION_GRANTED) {
            start();
        }  else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            ActivityCompat.requestPermissions(this, perms, RC_CALL);
        }

    }


    private void start() {
        initializeSurfaceViews();
        initializePeerConnectionFactory();
        createVideoTrackFromCameraAndShowIt();
        initializePeerConnections();

        new Thread(() -> {
            connectToSignallingServer();
        }).start();
    }

    private void connectToSignallingServer() {
        try {
            String URL = "192.168.0.17";
            Log.e(TAG, "REPLACE ME: IO Socket:" + URL);
            socket = new Socket(URL, 3000);
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            JSONObject createOrJoinJSON = new JSONObject();
            createOrJoinJSON.put("action", "create or join");
            createOrJoinJSON.put("room", "foo");
            sendJSONMessage(createOrJoinJSON.toString());
            startStreamingVideo();
            while(socket.isConnected()) {
                String dataMessage = dataInputStream.readUTF();
                JSONObject jsonObject = new JSONObject(dataMessage);
                Log.d(TAG, "message " + jsonObject.toString());
                String action = jsonObject.getString("action");
                if(action.equals("ipaddr")) {
                    Log.d(TAG, "connectToSignallingServer: ipaddr");
                } else if(action.equals("created")) {
                    Log.d(TAG, "connectToSignallingServer: created");
                    isInitiator = true;
                } else if(action.equals("full")) {
                    Log.d(TAG, "connectToSignallingServer: full");
                } else if(action.equals("join")) {
                    Log.d(TAG, "connectToSignallingServer: join");
                    Log.d(TAG, "connectToSignallingServer: Another peer made a request to join room");
                    Log.d(TAG, "connectToSignallingServer: This peer is the initiator of room");
                    isChannelReady = true;
                } else if(action.equals("joined")) {
                    Log.d(TAG, "connectToSignallingServer: joined");
                    isChannelReady = true;
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
                            maybeStart();
                        }
                    } else {
                        JSONObject message = (JSONObject) arg;
                        Log.d(TAG, "connectToSignallingServer: got message " + message);
                        if (message.getString("type").equals("offer")) {
                            Log.d(TAG, "connectToSignallingServer: received an offer " + isInitiator + " " + isStarted);
                            if (!isInitiator && !isStarted) {
                                maybeStart();
                            }
                            peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(OFFER, message.getString("sdp")));
                            doAnswer();
                        } else if (message.getString("type").equals("answer") && isStarted) {
                            peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(ANSWER, message.getString("sdp")));
                        } else if (message.getString("type").equals("candidate") && isStarted) {
                            Log.d(TAG, "connectToSignallingServer: receiving candidates");
                            IceCandidate candidate = new IceCandidate(message.getString("id"), message.getInt("label"), message.getString("candidate"));
                            peerConnection.addIceCandidate(candidate);
                        }
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void startStreamingVideo() {
        MediaStream mediaStream = factory.createLocalMediaStream("PETJA");
        mediaStream.addTrack(videoTrackFromCamera);
        mediaStream.addTrack(localAudioTrack);
        peerConnection.addStream(mediaStream);
        Log.d("petja", "user media");
        sendMessage("got user media");
    }

    private void doAnswer() {
        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                JSONObject message = new JSONObject();
                try {
                    message.put("type", "answer");
                    message.put("sdp", sessionDescription.description);
                    sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new MediaConstraints());
    }

    private void maybeStart() {
        Log.d(TAG, "maybeStart: " + isStarted + " " + isChannelReady);
        if (!isStarted && isChannelReady) {
            isStarted = true;
            if (isInitiator) {
                doCall();
            }
        }
    }

    private void doCall() {
        MediaConstraints sdpMediaConstraints = new MediaConstraints();

        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "onCreateSuccess: ");
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                JSONObject message = new JSONObject();
                try {
                    message.put("type", "offer");
                    message.put("sdp", sessionDescription.description);
                    sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, sdpMediaConstraints);
    }

    private void initializePeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        factory = PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory();
    }

    private void initializePeerConnections() {
        peerConnection = createPeerConnection(factory);
    }

    private PeerConnection createPeerConnection(PeerConnectionFactory factory) {
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        String URL = "stun:stun.l.google.com:19302";
        iceServers.add(PeerConnection.IceServer.builder(URL).createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        MediaConstraints pcConstraints = new MediaConstraints();

        PeerConnection.Observer pcObserver = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "onSignalingChange: ");
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: ");
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d(TAG, "onIceConnectionReceivingChange: ");
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "onIceGatheringChange: ");
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "onIceCandidate: ");
                JSONObject message = new JSONObject();

                try {
                    message.put("type", "candidate");
                    message.put("label", iceCandidate.sdpMLineIndex);
                    message.put("id", iceCandidate.sdpMid);
                    message.put("candidate", iceCandidate.sdp);

                    Log.d(TAG, "onIceCandidate: sending candidate " + message);
                    sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(TAG, "onIceCandidatesRemoved: ");
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "onAddStream: " + mediaStream.videoTracks.size());
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                AudioTrack remoteAudioTrack = mediaStream.audioTracks.get(0);
                remoteAudioTrack.setEnabled(true);
                remoteVideoTrack.setEnabled(true);
                remoteVideoTrack.addSink(remoteVideo);

            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(TAG, "onRemoveStream: ");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(TAG, "onDataChannel: ");
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded: ");
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                Log.d(TAG, "onAddTrack: ");
            }
        };

        return factory.createPeerConnection(rtcConfig, pcConstraints, pcObserver);
    }

    private void createVideoTrackFromCameraAndShowIt() {
        audioConstraints = new MediaConstraints();
        VideoCapturer videoCapturer = this.createVideoCapturer();
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
        VideoSource videoSource = factory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
        videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);

        videoTrackFromCamera = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        videoTrackFromCamera.setEnabled(true);
        videoTrackFromCamera.addSink(localVideo);

        //create an AudioSource instance
        audioSource = factory.createAudioSource(audioConstraints);
        localAudioTrack = factory.createAudioTrack("101", audioSource);

    }

    private void initializeSurfaceViews() {
        rootEglBase = EglBase.create();
        localVideo.init(rootEglBase.getEglBaseContext(), null);
        localVideo.setEnableHardwareScaler(true);
        localVideo.setMirror(true);

        remoteVideo.init(rootEglBase.getEglBaseContext(), null);
        remoteVideo.setEnableHardwareScaler(true);
        remoteVideo.setMirror(true);

    }

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
        }
        return videoCapturer;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this);
    }

    private void sendJSONMessage(String message) {
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
                    Log.d("petja", socket.isConnected() + " " + socket.isBound() + " " + socket.isClosed());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendMessageNoThread(String message) {
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
                Log.d("petja", socket.isConnected() + " " + socket.isBound() + " " + socket.isClosed());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Object message) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("action", "message");
            jsonObject.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendJSONMessage(jsonObject.toString());
    }
}