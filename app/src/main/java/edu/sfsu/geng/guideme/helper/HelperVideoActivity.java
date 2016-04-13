package edu.sfsu.geng.guideme.helper;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.ericsson.research.owr.Owr;
import com.ericsson.research.owr.sdk.InvalidDescriptionException;
import com.ericsson.research.owr.sdk.RtcCandidate;
import com.ericsson.research.owr.sdk.RtcCandidates;
import com.ericsson.research.owr.sdk.RtcConfig;
import com.ericsson.research.owr.sdk.RtcConfigs;
import com.ericsson.research.owr.sdk.RtcSession;
import com.ericsson.research.owr.sdk.RtcSessions;
import com.ericsson.research.owr.sdk.SessionDescription;
import com.ericsson.research.owr.sdk.SessionDescriptions;
import com.ericsson.research.owr.sdk.SimpleStreamSet;
import com.ericsson.research.owr.sdk.VideoView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import edu.sfsu.geng.guideme.Config;
import edu.sfsu.geng.guideme.R;
import edu.sfsu.geng.guideme.login.ServerRequest;
import edu.sfsu.geng.guideme.video.SignalingChannel;

public class HelperVideoActivity extends AppCompatActivity implements
        SignalingChannel.JoinListener,
        SignalingChannel.DisconnectListener,
        SignalingChannel.SessionFullListener,
        SignalingChannel.MessageListener,
        SignalingChannel.PeerDisconnectListener,
        RtcSession.OnLocalCandidateListener,
        RtcSession.OnLocalDescriptionListener,
        OnMapReadyCallback
//        LocationListener,
//        GoogleApiClient.ConnectionCallbacks,
//        GoogleApiClient.OnConnectionFailedListener
{

    private static final String TAG = "HelperVideo";

    /**
     * Initialize OpenWebRTC at startup
     */
    static {
        Log.d(TAG, "Initializing OpenWebRTC");
        Owr.init();
        Owr.runInBackground();
    }

    private AppCompatTextView connectText;
//    FloatingActionButton fab;
    private FloatingActionButton fabAddFriend;

    private SignalingChannel mSignalingChannel;
    private SignalingChannel.PeerChannel mPeerChannel;
    private RtcSession mRtcSession;
    private SimpleStreamSet mStreamSet;
    private VideoView mRemoteView;
    private RtcConfig mRtcConfig;
    private String sessionId;
    private String usernameStr;

    private List<NameValuePair> params;

    private final static boolean wantAudio = true;
    private final static boolean wantVideo = true;
    public static final int LOCATION_PERMISSION = 15;

    private boolean isNavigation;
    private GoogleMap mMap;
    private Marker curLocationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        sessionId = getIntent().getStringExtra("sessionId");
        isNavigation = getIntent().getBooleanExtra("isNavigation", false);

        setContentView(R.layout.activity_helper_video);

        SharedPreferences pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        usernameStr = pref.getString("username", "");

//        fab = (FloatingActionButton) findViewById(R.id.fab);
        fabAddFriend = (FloatingActionButton) findViewById(R.id.fab_add_friend);
        if (fabAddFriend != null) {
            fabAddFriend.setEnabled(false);
        }

        connectText = (AppCompatTextView) findViewById(R.id.connect_text);
        if (connectText != null) {
            connectText.setText(R.string.connect_hint_helper);
        } else {
            Log.d(TAG, "onCreate: connectText is null!");
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mRtcConfig = RtcConfigs.defaultConfig(Config.STUN_SERVER);

        //obtain necessary permissions for API level 23 and over (Marshmallow)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)   != PackageManager.PERMISSION_GRANTED
         || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
         || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)                 != PackageManager.PERMISSION_GRANTED
         || ContextCompat.checkSelfPermission(this, Manifest.permission.CAPTURE_AUDIO_OUTPUT)   != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                                                 Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                                 Manifest.permission.CAMERA,
                                                                 Manifest.permission.CAPTURE_AUDIO_OUTPUT}, LOCATION_PERMISSION);
        }

//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (isNavigation) {
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = new SupportMapFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.helper_video_linear_layout, mapFragment).commit();
//            ViewGroup.LayoutParams params = mapFragment.getView().getLayoutParams();
//            params.height = 500;
//            mapFragment.getView().setLayoutParams(params);
            mapFragment.getMapAsync(this);
        } else {
            LinearLayoutCompat linearLayoutCompat = (LinearLayoutCompat) findViewById(R.id.helper_video_linear_layout);
            if (linearLayoutCompat != null) {
                linearLayoutCompat.getLayoutParams().height = 0;
            }
//            if (mapFragment.getView() != null) {
//                mapFragment.getView().setVisibility(View.INVISIBLE);
//            }
//            getSupportFragmentManager().beginTransaction().remove(mapFragment).commit();
        }

        join();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_helper_video);

        connectText = (AppCompatTextView) findViewById(R.id.connect_text);
        if (connectText != null) {
            connectText.setText(R.string.connect_hint_helper);
        } else {
            Log.d(TAG, "onConfigurationChanged: connectText is null!");
        }


        updateVideoView(true);
    }

    private void updateVideoView(boolean running) {
        if (mStreamSet != null) {
            TextureView remoteView = (TextureView) findViewById(R.id.remote_view);
            if (remoteView != null) {
                remoteView.setVisibility(running ? View.VISIBLE : View.INVISIBLE);
            } else {
                Log.d(TAG, "updateVideoView: remoteView is null!");
            }
            if (running) {
                Log.d(TAG, "setting remoteView: " + remoteView);
                mRemoteView.setView(remoteView);
            } else {
                Log.d(TAG, "stopping remoteView");
                mRemoteView.stop();
            }
        } else {
            Log.d(TAG, "updateVideoView: mStreamSet is null!");
        }
    }

    public void onQuitClicked(final View view) {
        Log.d(TAG, "Floating button onClicked");
        if (mSignalingChannel != null) {
            mSignalingChannel.kickoff(Config.VIDEO_SERVER_ADDRESS, sessionId, usernameStr);
        } else {
            Log.d(TAG, "onFabClicked: mSignalingChannel is null!");
        }
//        onDisconnect();
        // Move to onDisconnect
//        Intent homeActivity = new Intent(HelperVideoActivity.this, HelperHomeActivity.class);
//        startActivity(homeActivity);
//        finish();
    }

    public void onAddFriend(final View view) {
        if (mPeerChannel != null) {
            // TODO check already friends
            try {
                JSONObject json = new JSONObject();
                json.putOpt("add", usernameStr);
                mPeerChannel.send(json);
                Toast.makeText(getApplication(), R.string.add_friend_send_message, Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "onAddFriend: mPeerChannel is null!");
        }

    }

    public void onRemoteViewClicked(final View view) {
        if (mStreamSet != null) {
            if (mRemoteView != null) {
                mRemoteView.setRotation((mRemoteView.getRotation() + 1) % 4);
            }
        }
    }

    private void join() {
        Log.d(TAG, "onJoin");

        mSignalingChannel = new SignalingChannel(Config.VIDEO_SERVER_ADDRESS, sessionId, usernameStr);
        mSignalingChannel.setJoinListener(this);
        mSignalingChannel.setDisconnectListener(this);
        mSignalingChannel.setSessionFullListener(this);

        mStreamSet = SimpleStreamSet.defaultConfig(wantAudio, wantVideo);
        mRemoteView = mStreamSet.createRemoteView();
        mRemoteView.setRotation((mRemoteView.getRotation() + 1) % 4);
        updateVideoView(true);
    }

    @Override
    public void onPeerJoin(final SignalingChannel.PeerChannel peerChannel) {
        Log.v(TAG, "onPeerJoin => " + peerChannel.getPeerId());
        mPeerChannel = peerChannel;
        mPeerChannel.setDisconnectListener(this);
        mPeerChannel.setMessageListener(this);

        mRtcSession = RtcSessions.create(mRtcConfig);
        mRtcSession.setOnLocalCandidateListener(this);
        mRtcSession.setOnLocalDescriptionListener(this);
    }

    @Override
    public void onPeerDisconnect(final SignalingChannel.PeerChannel peerChannel) {
        Log.d(TAG, "onPeerDisconnect => " + peerChannel.getPeerId());
        if (mRtcSession != null) {
            mRtcSession.stop();
        } else {
            Log.d(TAG, "onPeerDisconnect: mRtcSession is null!");
        }
        mPeerChannel = null;
        updateVideoView(false);
    }

    @Override
    public synchronized void onMessage(final JSONObject json) {
        if (json.has("candidate")) {
            JSONObject candidate = json.optJSONObject("candidate");
            Log.v(TAG, "candidate: " + candidate);
            RtcCandidate rtcCandidate = RtcCandidates.fromJsep(candidate);
            if (rtcCandidate != null) {
                mRtcSession.addRemoteCandidate(rtcCandidate);
            } else {
                Log.w(TAG, "invalid candidate: " + candidate);
            }
        }
        if (json.has("sdp")) {
            JSONObject sdp = json.optJSONObject("sdp");
            Log.v(TAG, "sdp: " + sdp);
            try {
                SessionDescription sessionDescription = SessionDescriptions.fromJsep(sdp);
                if (sessionDescription != null) {
                    if (sessionDescription.getType() == SessionDescription.Type.OFFER) {
                        onInboundCall(sessionDescription);
                    } else {
                        onAnswer(sessionDescription);
                    }
                } else {
                    Log.d(TAG, "onMessage: sessionDescription is null!");
                }
            } catch (InvalidDescriptionException e) {
                e.printStackTrace();
            }
        }
//        if (json.has("quit")) {
//            Log.v(TAG, "Being asked to quit!");
//            onDisconnect();
//            Intent homeActivity = new Intent(HelperVideoActivity.this, HelperHomeActivity.class);
//            startActivity(homeActivity);
//            finish();
//        }
        if (json.has("lat") && isNavigation) {
            Log.v(TAG, "Move the marker!");
            try {
                double latitude = Double.parseDouble(json.getString("lat"));
                double longitude = Double.parseDouble(json.getString("lng"));
                LatLng newLatLng = new LatLng(latitude, longitude);
                if (curLocationMarker != null) {
                    curLocationMarker.setPosition(newLatLng);
                } else {
                    Log.d(TAG, "onMessage: curLocationMarker is null!");
                }
                if (mMap != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(newLatLng));
                } else {
                    Log.d(TAG, "onMessage: mMap is null!");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (json.has("add")) {
            Log.v(TAG, "Add Friend Request!");
            try {
                final String friendName = json.getString("add");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(String.format(getResources().getString(R.string.add_friend_confirm_message), friendName));
                builder.setPositiveButton(R.string.add_friend_ok_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        params = new ArrayList<NameValuePair>();
                        params.add(new BasicNameValuePair("m_username", usernameStr));
                        params.add(new BasicNameValuePair("f_username", friendName));

                        ServerRequest sr = new ServerRequest();
                        JSONObject jsonRes = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/api/addfriend", params);

                        if (jsonRes != null) {
                            try {
                                String jsonStr = jsonRes.getString("response");
                                Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_SHORT).show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        try {
                            JSONObject jsonBack = new JSONObject();
                            jsonBack.putOpt("added", true);
                            jsonBack.putOpt("name", usernameStr);
                            mPeerChannel.send(jsonBack);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                builder.setNegativeButton(R.string.add_friend_cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            JSONObject jsonBack = new JSONObject();
                            jsonBack.putOpt("added", false);
                            mPeerChannel.send(jsonBack);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (json.has("added")) {
            try {
                boolean success = json.getBoolean("added");
                String name = json.getString("name");
                if (success) {
                    String message = String.format(getResources().getString(R.string.add_friend_accept), name);
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLocalCandidate(final RtcCandidate candidate) {
        if (mPeerChannel != null) {
            try {
                JSONObject json = new JSONObject();
                json.putOpt("candidate", RtcCandidates.toJsep(candidate));
                json.getJSONObject("candidate").put("sdpMid", "video");
                Log.d(TAG, "sending candidate: " + json);
                mPeerChannel.send(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "onLocalCandidate: mPeerChannel is null!");
        }
    }

    // This is the method using
    private void onInboundCall(final SessionDescription sessionDescription) {
        Log.d(TAG, "onInboundCall");
        connectText.setText("");
        fabAddFriend.setEnabled(true);
        if (mRtcSession != null) {
            try {
                mRtcSession.setRemoteDescription(sessionDescription);
                mRtcSession.start(mStreamSet);
            } catch (InvalidDescriptionException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "onInboundCall: mRtcSession is null!");
        }
    }

    private void onAnswer(final SessionDescription sessionDescription) {
        Log.d(TAG, "onAnswer");
        if (mRtcSession != null) {
            try {
                mRtcSession.setRemoteDescription(sessionDescription);
            } catch (InvalidDescriptionException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "onAnswer: mRtcSession is null!");
        }
    }

    @Override
    public void onLocalDescription(final SessionDescription localDescription) {
        if (mPeerChannel != null) {
            try {
                JSONObject json = new JSONObject();
                json.putOpt("sdp", SessionDescriptions.toJsep(localDescription));
                Log.d(TAG, "sending sdp: " + json);
                mPeerChannel.send(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisconnect() {
        Toast.makeText(this, "Disconnected from server", Toast.LENGTH_LONG).show();
        updateVideoView(false);
        mStreamSet = null;
        if (mRtcSession != null) {
            mRtcSession.stop();
        } else {
            Log.d(TAG, "onDisconnect: mRtcSession is null!");
        }
        mRtcSession = null;
        mSignalingChannel = null;
        Intent homeActivity = new Intent(HelperVideoActivity.this, HelperHomeActivity.class);
        startActivity(homeActivity);
//        finish();
    }

    @Override
    public void onSessionFull() {
        Toast.makeText(this, "Session is full", Toast.LENGTH_LONG).show();
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        stopLocationUpdates();
//    }
//
    /**
     * Shutdown the process as a workaround until cleanup has been fully implemented.
     */
    @Override
    protected void onStop() {
        super.onStop();
//        if (mRtcSession != null) {
//            mRtcSession.stop();
//        }
//        mPeerChannel = null;
//        finish();
//        System.exit(0);
//        if (mSignalingChannel != null) {
//            mSignalingChannel.kickoff(Config.VIDEO_SERVER_ADDRESS, sessionId, usernameStr);
//        } else {
//            Log.d(TAG, "onStop: mSignalingChannel is null!");
//        }
    }

    @Override
    public void onBackPressed() {
        onQuitClicked(findViewById(R.id.fab_quit));
    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        startLocationUpdates();
//    }
//

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in SF and move the camera
        LatLng sf = new LatLng(37.722, -122.48);
        curLocationMarker = mMap.addMarker(new MarkerOptions().position(sf).title("User Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sf));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15f));
    }

}
