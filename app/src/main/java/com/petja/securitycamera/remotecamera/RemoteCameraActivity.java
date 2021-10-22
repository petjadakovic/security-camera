package com.petja.securitycamera.remotecamera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.petja.securitycamera.R;
import com.petja.securitycamera.SimpleSdpObserver;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class RemoteCameraActivity extends Activity {

    private static final String TAG = "petja";
    private static final int RC_CALL = 111;
    public static final String VIDEO_TRACK_ID = "PETJAv0";
//    public static final int VIDEO_RESOLUTION_WIDTH = 1280;
//    public static final int VIDEO_RESOLUTION_HEIGHT = 720;
    public static final int VIDEO_RESOLUTION_WIDTH = 1920;
    public static final int VIDEO_RESOLUTION_HEIGHT = 1080;
    public static final int FPS = 20;

    public int monitorId;


    private CustomSurfaceViewRenderer localVideo;
    private PeerConnectionFactory factory;
    public PeerConnection peerConnection;
    private EglBase rootEglBase;
    private VideoTrack videoTrackFromCamera;
    private CheckBox motionCheckbox;

    MediaConstraints audioConstraints;
    AudioSource audioSource;
    AudioTrack localAudioTrack;

    MediaStream mediaStream;

    public RemoteSignalingServer signalingServer;

    Button switchCameraButton;

    CameraType cameraType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote_camera);

        localVideo = findViewById(R.id.surface_view_local);
        localVideo.remoteCameraActivity = this;
        localVideo.imageView = findViewById(R.id.imageView);
        localVideo.textView = findViewById(R.id.text_view);
        switchCameraButton = findViewById(R.id.switch_camera);
        motionCheckbox = findViewById(R.id.motionCheckbox);

        if(RemoteActivitySavedState.getInstance().cameraType == null) {
            cameraType = CameraType.FRONT;
            RemoteActivitySavedState.getInstance().cameraType = cameraType;
        } else {
            cameraType = RemoteActivitySavedState.getInstance().cameraType;
        }

        Log.d("petja", "remote oncreate");

        switchCameraButton.setOnClickListener(view -> switchCamera());

        motionCheckbox.setOnCheckedChangeListener((compoundButton, b) -> {
            if(motionCheckbox.isChecked()) {
                Toast.makeText(getApplicationContext(), "Starting motion detection in 5s", Toast.LENGTH_SHORT).show();
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(motionCheckbox.isChecked()) {
                        localVideo.notifyOnMotion = true;
                    }
                }).start();
            } else {
                localVideo.notifyOnMotion = false;
                Toast.makeText(getApplicationContext(), "Motion detection disabled", Toast.LENGTH_SHORT).show();
            }
        });

//        if(RemoteActivitySavedState.getInstance().signalingServer == null) {
//            signalingServer = new SignalingServer(this);
//            RemoteActivitySavedState.getInstance().signalingServer = signalingServer;
//        } else {
//            signalingServer = RemoteActivitySavedState.getInstance().signalingServer;
//        }
        signalingServer = new RemoteSignalingServer(this);
        Log.d("petja", "servet contructrot");
        startSecurityCamera();

    }

    private void startSecurityCamera() {
        if(checkPermissions()) {
            long time = System.currentTimeMillis();
            RemoteActivitySavedState remoteActivitySavedState = RemoteActivitySavedState.getInstance();
            initializeSurfaceViews();
//            if(remoteActivitySavedState.factory == null) {
//                initializePeerConnectionFactory();
//                remoteActivitySavedState.factory = factory;
//            } else {
//                factory = remoteActivitySavedState.factory;
//                Log.d("petja", "setSAved factory");
//            }
            initializePeerConnectionFactory();
            createVideoTrackFromCameraAndShowIt();
//            if(remoteActivitySavedState.peerConnection == null) {
//                initializePeerConnections();
//                remoteActivitySavedState.peerConnection = peerConnection;
//            } else {
//                peerConnection = remoteActivitySavedState.peerConnection;
//                Log.d("petja", "setSAved peerConnection");
//            }
            initializePeerConnections();
            Log.d("petja", "init Time " + (System.currentTimeMillis() - time));
            new Thread(() -> {
                signalingServer.connectToServer();
                startStreamingVideo();
            }).start();
        }
    }

    private PeerConnection createPeerConnection(PeerConnectionFactory factory) {
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        String URL = "stun:stun.l.google.com:19302";
        iceServers.add(PeerConnection.IceServer.builder(URL).createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:3.10.164.245:3478").setUsername("camera").setPassword("camera123").createIceServer());
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        Log.d("petja", rtcConfig  + "");
        MediaConstraints pcConstraints = new MediaConstraints();

        PeerConnection.Observer pcObserver = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "onSignalingChange: " + signalingState.toString());
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
                    signalingServer.sendMessageAction(message, monitorId);
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
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(TAG, "onRemoveStream: ");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(TAG, "onDataChannel: " + dataChannel.bufferedAmount());
                dataChannel.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onBufferedAmountChange(long l) {

                    }

                    @Override
                    public void onStateChange() {

                    }

                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        String message = StandardCharsets.UTF_8.decode(buffer.data).toString();
                        if(message.equals("switch_camera")) {
                            Log.d("petja", "switch camera");
                            runOnUiThread(() -> switchCamera());
                        }
                    }
                });
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded: ");
                doAnswer(monitorId);
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                Log.d(TAG, "onAddTrack: ");
            }
        };

        return factory.createPeerConnection(rtcConfig, pcConstraints, pcObserver);
    }

    private void initializePeerConnections() {
        peerConnection = createPeerConnection(factory);
        Log.d("petja", "" + peerConnection);
    }

    private void startStreamingVideo() {
        mediaStream = factory.createLocalMediaStream("PETJA");
        mediaStream.addTrack(videoTrackFromCamera);
        mediaStream.addTrack(localAudioTrack);
        peerConnection.addStream(mediaStream);
        Log.d("petja", "user media");
    }

    private boolean checkPermissions() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), perms[0]) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                getApplicationContext(), perms[1]) ==
                PackageManager.PERMISSION_GRANTED) {
            return true;
        }  else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            ActivityCompat.requestPermissions(this, perms, RC_CALL);
            return false;
        }
    }

    private void initializeSurfaceViews() {
        rootEglBase = EglBase.create();
        localVideo.init(rootEglBase.getEglBaseContext(), null);
        localVideo.setEnableHardwareScaler(true);
    }

    private void initializePeerConnectionFactory() {

        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .createInitializationOptions());

        DefaultVideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());

        PeerConnectionFactory.Builder builder = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory);
        builder.setOptions(null);

        factory = builder.createPeerConnectionFactory();
    }

    public void doAnswer(int callerId) {
        Log.d(TAG, "before do answer");
        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "onCreateSuccess: answer " + sessionDescription.toString());
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                Log.d(TAG, "onCreateSuccess: answer set " + sessionDescription.toString());
                JSONObject message = new JSONObject();
                try {
                    message.put("type", "answer");
                    message.put("sdp", sessionDescription.description);
                    signalingServer.sendMessageAction(message, callerId);
                    Log.d(TAG, "onCreateSuccess: answer sent");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCreateFailure(String s) {
                super.onCreateFailure(s);
                Log.d(TAG, "onCreateFailure: answer " + s);
            }
        }, new MediaConstraints());
    }

    private void createVideoTrackFromCameraAndShowIt() {
        audioConstraints = new MediaConstraints();
        VideoCapturer videoCapturer = this.createVideoCapturer(cameraType);
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
        VideoSource videoSource = factory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
        videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);
        localVideo.setMirror(cameraType == CameraType.FRONT);

        videoTrackFromCamera = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        videoTrackFromCamera.setEnabled(true);
        videoTrackFromCamera.addSink(localVideo);

        //create an AudioSource instance
        audioSource = factory.createAudioSource(audioConstraints);
        localAudioTrack = factory.createAudioTrack("101", audioSource);

    }

    private void switchCamera() {
        cameraType = cameraType == CameraType.BACK ? CameraType.FRONT : CameraType.BACK;
        RemoteActivitySavedState.getInstance().cameraType = cameraType;
        Log.d(TAG, "setCamera " + cameraType);
        VideoCapturer videoCapturer = this.createVideoCapturer(cameraType);
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
        VideoSource videoSource = factory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
        videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);
        localVideo.setMirror(cameraType == CameraType.FRONT);

        if(videoTrackFromCamera != null) {
            videoTrackFromCamera.dispose();
        }
        videoTrackFromCamera = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        videoTrackFromCamera.setEnabled(true);
        videoTrackFromCamera.addSink(localVideo);

        if(peerConnection != null && mediaStream != null) {
            peerConnection.removeStream(mediaStream);
            mediaStream = factory.createLocalMediaStream("PETJA");
            mediaStream.addTrack(videoTrackFromCamera);
            mediaStream.addTrack(localAudioTrack);
            peerConnection.addStream(mediaStream);
        }
    }

    private VideoCapturer createVideoCapturer(CameraType cameraType) {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this), cameraType);
        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(true), cameraType);
        }
        return videoCapturer;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator, CameraType cameraType) {
        final String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName) && cameraType == CameraType.FRONT) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            } else if(enumerator.isBackFacing(deviceName) && cameraType == CameraType.BACK) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName) && cameraType == CameraType.FRONT) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            } else if (!enumerator.isBackFacing(deviceName) && cameraType == CameraType.BACK) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("petja","stop 1");
        this.signalingServer.stopServer();
        Log.d("petja","stop2");
        if(rootEglBase != null) {
            Log.d("petja","stop4");
            rootEglBase.release();
        }
        Log.d("petja","stop5");
        if(videoTrackFromCamera != null) {
            videoTrackFromCamera.dispose();
        }
        Log.d("petja","stop6");
        if(localAudioTrack != null){
            localAudioTrack.dispose();
        }
        if(peerConnection != null) {
            peerConnection.close();
            Log.d("petja","stop7");
        }
        Log.d("petja","stop8");
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this);
    }

    enum CameraType {
        FRONT,
        BACK
    }

    public boolean isPeerConnected() {
        if(peerConnection == null) {
            return false;
        } else {
            switch (peerConnection.connectionState()){
                case CONNECTING:
                case CONNECTED:
                    return true;
                case NEW:
                case DISCONNECTED:
                case FAILED:
                case CLOSED:
                    return false;
                default:
                    return false;
            }
        }
    }
}
