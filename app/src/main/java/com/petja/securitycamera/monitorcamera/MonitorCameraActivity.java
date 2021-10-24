package com.petja.securitycamera.monitorcamera;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.petja.securitycamera.FirebaseManager;
import com.petja.securitycamera.R;
import com.petja.securitycamera.SimpleSdpObserver;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioTrack;
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
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MonitorCameraActivity extends Activity {

    private static final String TAG = "petja";

    public static int remoteId;


    private SurfaceViewRenderer remoteVideo;
    private PeerConnectionFactory factory;
    public PeerConnection peerConnection;
    private EglBase rootEglBase;
    private CheckBox motionCheckbox;

    private AudioTrack remoteAudioTrack;
    private VideoTrack remoteVideoTrack;

    MonitorSignalingServer signalingServer;
    Button switchCameraButton;
    DataChannel dataChannel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitor_camera);
        remoteVideo = findViewById(R.id.surface_view_remote);
        switchCameraButton = findViewById(R.id.switch_camera);
        motionCheckbox = findViewById(R.id.motionCheckbox);

        switchCameraButton.setOnClickListener(view -> switchCamera());


        motionCheckbox.setOnCheckedChangeListener((compoundButton, b) -> {
            if (dataChannel != null && dataChannel.state() == DataChannel.State.OPEN) {
                String message = "enable_motion_detection";
                if (!motionCheckbox.isChecked()) {
                    message = "disable_motion_detection";
                }
                dataChannel.send(new DataChannel.Buffer(ByteBuffer.wrap(message.getBytes()), false));
            }
        });

        signalingServer = new MonitorSignalingServer(this);
        connectToSecurityCamera();
    }

    private void switchCamera() {
        if (dataChannel != null && dataChannel.state() == DataChannel.State.OPEN) {
            dataChannel.send(new DataChannel.Buffer(ByteBuffer.wrap("switch_camera".getBytes()), false));
            Log.d(TAG, "Sent data");
        } else {
            Log.d(TAG, "Failed to send: " + dataChannel.state());
        }
    }

    private void connectToSecurityCamera() {
        initializeSurfaceViews();
        initializePeerConnectionFactory();
        initializePeerConnections();

        new Thread(() -> {
            signalingServer.connectToServer();
            JSONObject tokenJSON = new JSONObject();
            try {
                tokenJSON.put("token", FirebaseManager.getInstance().getToken());
            } catch (JSONException ignored) {
            }
            signalingServer.sendMessageSync(tokenJSON.toString());
            doCall();
        }).start();
    }

    private PeerConnection createPeerConnection(PeerConnectionFactory factory) {
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        String URL = "stun:stun.l.google.com:19302";
        iceServers.add(PeerConnection.IceServer.builder(URL).createIceServer());
       // iceServers.add(PeerConnection.IceServer.builder("turn:numb.viagenie.ca:3478").setUsername("kengur1111@gmail.com").setPassword("kengur123").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:3.10.164.245:3478").setUsername("camera").setPassword("camera123").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
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
                    signalingServer.sendMessageAction(message, remoteId);
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
                if (remoteAudioTrack != null) {
                    remoteVideoTrack.dispose();
                }
                if (remoteVideoTrack != null) {
                    remoteVideoTrack.dispose();
                }
                remoteVideoTrack = mediaStream.videoTracks.get(0);
                remoteAudioTrack = mediaStream.audioTracks.get(0);
                remoteAudioTrack.setEnabled(true);
                remoteVideoTrack.setEnabled(true);
                remoteVideoTrack.addSink(remoteVideo);
                //remoteVideo.setMirror(remoteVideoTrack.id().endsWith("FRONT"));
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

    private void initializePeerConnections() {
        peerConnection = createPeerConnection(factory);
        dataChannel = peerConnection.createDataChannel(System.currentTimeMillis() + "", new DataChannel.Init());
        dataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {
                Log.d("petja", "channel buffer change");
            }

            @Override
            public void onStateChange() {
                Log.d("petja", "channel state " + dataChannel.state());
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                Log.d("petja", "channel message ");
                String message = StandardCharsets.UTF_8.decode(buffer.data).toString();
                Log.d("petja", "channel message " + message);
                if(message.equals("enable_motion_detection")) {
                    runOnUiThread(() -> motionCheckbox.setChecked(true));
                } else if(message.equals("disable_motion_detection")) {
                    runOnUiThread(() -> motionCheckbox.setChecked(false));
                }
            }
        });
    }

    private void initializeSurfaceViews() {
        rootEglBase = EglBase.create();
        remoteVideo.init(rootEglBase.getEglBaseContext(), null);
        remoteVideo.setEnableHardwareScaler(true);
        remoteVideo.setMirror(true);
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

    public void doAnswer() {
        Log.d(TAG, "before do answer");
        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "onCreateSuccess: answer " + sessionDescription.toString());
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                Log.d(TAG, "after setLocal desc");
                JSONObject message = new JSONObject();
                try {
                    message.put("type", "answer");
                    message.put("sdp", sessionDescription.description);
                    Log.d(TAG, "before send answer");
                    signalingServer.sendMessageAction(message, remoteId);
                    Log.d(TAG, "after done answer");
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

    public void setMotionCheckbox(boolean enabled) {
        runOnUiThread(() -> motionCheckbox.setChecked(enabled));
    }

    public void doCall() {
        Log.d(TAG, "callling");
        MediaConstraints sdpMediaConstraints = new MediaConstraints();

        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "onCreateSuccess:offer ");
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                JSONObject message = new JSONObject();
                try {
                    message.put("type", "offer");
                    message.put("sdp", sessionDescription.description);
                    signalingServer.sendMessageAction(message, remoteId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, sdpMediaConstraints);
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.signalingServer.stopServer();
        if (peerConnection != null) {
            peerConnection.close();
        }
        if (rootEglBase != null) {
            Log.d("petja", "stop");
            rootEglBase.release();
        }
        if (remoteAudioTrack != null) {
            try {
                remoteAudioTrack.dispose();
            } catch (Exception ignored) {
            }
        }
        if (remoteVideoTrack != null) {
            try {
                remoteVideoTrack.dispose();
            } catch (Exception ignored) {
            }
        }
    }
}
