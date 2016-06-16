package edu.sfsu.geng.guideme.visualimpairer;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.ListViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.ericsson.research.owr.Owr;
import com.ericsson.research.owr.sdk.CameraSource;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import edu.sfsu.geng.guideme.Config;
import edu.sfsu.geng.guideme.R;
import edu.sfsu.geng.guideme.ServerRequest;
import edu.sfsu.geng.guideme.video.SignalingChannel;

public class VIVideoActivity extends AppCompatActivity implements
        SignalingChannel.JoinListener,
        SignalingChannel.DisconnectListener,
        SignalingChannel.SessionFullListener,
        SignalingChannel.MessageListener,
        SignalingChannel.PeerDisconnectListener,
        SignalingChannel.RefreshListListener,
        RtcSession.OnLocalCandidateListener,
        RtcSession.OnLocalDescriptionListener,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        AdapterView.OnItemClickListener
{

    private static final String TAG = "VIVideo";

    private static final int UPDATE_INTERVAL = 2000;
    private static final int FASTEST_UPDATE_INTERVAL = 1000;

    /**
     * Initialize OpenWebRTC at startup
     */
    static {
        Log.d(TAG, "Initializing OpenWebRTC");
        Owr.init();
        Owr.runInBackground();
    }

//    private AppCompatTextView connectText;
//    private AppCompatButton acceptButton, declineButton;
//    FloatingActionButton fab;
//    private FloatingActionButton fabAddFriend;
    private AppCompatButton quitButton;
    private AppCompatButton toggleLocationButton;
    private AppCompatButton addFriendButton;
    private ListViewCompat waitingHelperList;
    private HelperListAdapter helperListAdapter;

    private boolean isSendingLocation;
    private boolean isVideoStart;
    private String helperName;

    private SignalingChannel mSignalingChannel;
//    private InputMethodManager mInputMethodManager;
//    private WindowManager mWindowManager;
    private SignalingChannel.PeerChannel mPeerChannel;
    private RtcSession mRtcSession;
    private SimpleStreamSet mStreamSet;
    private VideoView mSelfView;
    private RtcConfig mRtcConfig;
    private String sessionId;
    private String usernameStr;
    private boolean isNavigation;

    private List<NameValuePair> params;


    private final static boolean wantAudio = true;
    private final static boolean wantVideo = true;
    public static final int LOCATION_PERMISSION = 15;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_vi_video);
        sessionId = getIntent().getStringExtra("sessionId");
        isNavigation = getIntent().getBooleanExtra("isNavigation", false);

        SharedPreferences pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        usernameStr = pref.getString("username", "");

        //fab = (FloatingActionButton) findViewById(R.id.fab);
//        fabAddFriend = (FloatingActionButton) findViewById(R.id.fab_add_friend);
//        if (fabAddFriend != null) {
//            fabAddFriend.setEnabled(false);
//        }
        quitButton = (AppCompatButton) findViewById(R.id.quit_btn);
        if (quitButton != null) {
            quitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onQuitClicked(v);
                }
            });
        }
        toggleLocationButton = (AppCompatButton) findViewById(R.id.toggle_location_btn);
        if (toggleLocationButton != null) {
            toggleLocationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onToggleSendLocation(v);
                }
            });
        }
        addFriendButton = (AppCompatButton) findViewById(R.id.add_friend_btn);
        if (addFriendButton != null) {
            addFriendButton.setEnabled(false);
            addFriendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onAddFriend(v);
                }
            });
        }
        isSendingLocation = true;

        waitingHelperList = (ListViewCompat) findViewById(R.id.waiting_helper_list);
        helperListAdapter = new HelperListAdapter(this, -1, new ArrayList<JSONObject>());
        waitingHelperList.setAdapter(helperListAdapter);
        waitingHelperList.setOnItemClickListener(this);

//        connectText = (AppCompatTextView) findViewById(R.id.connect_text);
//        if (connectText != null) {
//            connectText.setText(R.string.connect_hint_vi);
//        } else {
//            Log.d(TAG, "onCreate: connectText is null!");
//        }
//
//        acceptButton = (AppCompatButton) findViewById(R.id.accept_btn);
//        if (acceptButton != null) {
//            acceptButton.setEnabled(false);
//            acceptButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    onAcceptClicked(v);
//                }
//            });
//        } else {
//            Log.d(TAG, "onCreate: acceptButton is null!");
//        }
//
//        declineButton = (AppCompatButton) findViewById(R.id.decline_btn);
//        if (declineButton != null) {
//            declineButton.setEnabled(false);
//            declineButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    onDeclineClicked(v);
//                }
//            });
//        } else {
//            Log.d(TAG, "onCreate: declineButton is null!");
//        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

//        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mRtcConfig = RtcConfigs.defaultConfig(Config.STUN_SERVER);

        //obtain necessary permissions for API level 23 and over (Marshmallow)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)   != PackageManager.PERMISSION_GRANTED
//         || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
         || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)                 != PackageManager.PERMISSION_GRANTED
         || ContextCompat.checkSelfPermission(this, Manifest.permission.CAPTURE_AUDIO_OUTPUT)   != PackageManager.PERMISSION_GRANTED) {
//         || ContextCompat.checkSelfPermission(this, Manifest.permission.)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
//                                                                 Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                                 Manifest.permission.CAMERA,
                                                                 Manifest.permission.CAPTURE_AUDIO_OUTPUT}, LOCATION_PERMISSION);
        }

        if (isNavigation) {
            createLocationRequest();
            buildGoogleApiClient(this);
        } else {
            isSendingLocation = false;
            if (toggleLocationButton != null) {
                toggleLocationButton.setVisibility(View.INVISIBLE);
            }
//            View fabGps = findViewById(R.id.fab_gps);
//            if (fabGps != null) fabGps.setVisibility(View.INVISIBLE);
        }

        join();
    }

//    @Override
//    public void onConfigurationChanged(final Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        setContentView(R.layout.activity_vi_video);
//
//        quitButton = (AppCompatButton) findViewById(R.id.quit_btn);
//        toggleLocationButton = (AppCompatButton) findViewById(R.id.toggle_location_btn);
//        addFriendButton = (AppCompatButton) findViewById(R.id.add_friend_btn);
//        if (addFriendButton != null) {
//            addFriendButton.setEnabled(false);
//        }
//
//        connectText = (AppCompatTextView) findViewById(R.id.connect_text);
//        if (connectText != null) {
//            connectText.setText(R.string.connect_hint_vi);
//        } else {
//            Log.d(TAG, "onConfigurationChanged: connectText is null!");
//        }
//
//        acceptButton = (AppCompatButton) findViewById(R.id.accept_btn);
//        if (acceptButton != null) {
//            acceptButton.setEnabled(false);
//        } else {
//            Log.d(TAG, "onConfigurationChanged: acceptButton is null!");
//        }
//
//        declineButton = (AppCompatButton) findViewById(R.id.decline_btn);
//        if (declineButton != null) {
//            declineButton.setEnabled(false);
//        } else {
//            Log.d(TAG, "onConfigurationChanged: declineButton is null!");
//        }
//
//        updateVideoView(true);
//    }

    private void updateVideoView(boolean running) {
//        if (mStreamSet != null) {
//            TextureView selfView = (TextureView) findViewById(R.id.self_view);
//            if (selfView != null) {
//                selfView.setVisibility(running ? View.VISIBLE : View.INVISIBLE);
//            } else {
//                Log.d(TAG, "updateVideoView: selfView is null!");
//            }
//            if (running) {
//                Log.d(TAG, "setting selfView: " + selfView);
//                mSelfView.setView(selfView);
//            } else {
//                Log.d(TAG, "stopping selfView");
//                mSelfView.stop();
//            }
//        } else {
//            Log.e(TAG, "updateVideoView: mStreamSet is null!");
//        }
    }


    public void onAcceptClicked(final View view) {
        Log.d(TAG, "onAcceptClicked");
        if (mRtcSession != null) {
            if (mStreamSet != null) {
                isVideoStart = true;
                helperName = mPeerChannel.getPeerId();

                updateVideoView(true);
                mRtcSession.start(mStreamSet);
//                connectText.setText(R.string.connect_start_vi);
//                acceptButton.setEnabled(false);
//                declineButton.setEnabled(false);

                params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("username", usernameStr));

                ServerRequest sr = new ServerRequest();
                JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/api/deleteroom", params);
                if (json != null) {
                    try {
                        String jsonStr = json.getString("response");
//                        Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Log.d(TAG, "onAcceptClicked: mStreamSet is null!");
            }
        } else {
            Log.d(TAG, "onAcceptClicked: mRtcSession is null!");
        }
    }

//    public void onDeclineClicked(final View view) {
//        Log.d(TAG, "onRefuseClicked");
//        if (mSignalingChannel != null) {
//            mSignalingChannel.kickoff(Config.VIDEO_SERVER_ADDRESS, sessionId, mPeerChannel.getPeerId());
//        } else {
//            Log.d(TAG, "onDeclineClicked: mSignalingChannel is null!");
//        }
//        mPeerChannel = null;
////        acceptButton.setEnabled(false);
////        declineButton.setEnabled(false);
////        fabAddFriend.setEnabled(false);
//        addFriendButton.setEnabled(false);
//    }

    public void onQuitClicked(final View view) {
        Log.d(TAG, "Quit button onClicked");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.quit_confirm_message);
        builder.setPositiveButton(R.string.quit_confirm_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                quit();
            }
        });
        builder.setNegativeButton(R.string.quit_cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void quit() {
        Log.d(TAG, "Quit");
        if (mSignalingChannel != null) {
//            if (mPeerChannel != null) {
//                mSignalingChannel.kickoff(Config.VIDEO_SERVER_ADDRESS, sessionId, mPeerChannel.getPeerId());
//            }
//            mSignalingChannel.kickoff(Config.VIDEO_SERVER_ADDRESS, sessionId, usernameStr);
            mSignalingChannel.quit();
        } else {
            Log.d(TAG, "onFabClicked: mSignalingChannel is null!");
        }
//        onDisconnect();
//        Intent homeActivity = new Intent(VIVideoActivity.this, VIHomeActivity.class);
//        startActivity(homeActivity);
//        finish();
    }

    public void onAddFriend(final View view) {
//        if (mPeerChannel != null) {
        if (isVideoStart) {
            // TODO check already friends
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setMessage(String.format(getResources().getString(R.string.send_request_confirm_message), mPeerChannel.getPeerId()));
            builder.setMessage(String.format(getResources().getString(R.string.send_request_confirm_message), helperName));
            builder.setPositiveButton(R.string.add_friend_ok_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    addFriendButton.setEnabled(false);
                    try {
                        JSONObject json = new JSONObject();
                        json.putOpt("add", usernameStr);
                        mPeerChannel.send(json);
                        Toast.makeText(getApplication(), R.string.add_friend_send_message, Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
            builder.setNegativeButton(R.string.add_friend_cancel_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
//            Log.d(TAG, "onAddFriend: mPeerChannel is null!");
            Log.d(TAG, "onAddFriend: video session has not started!");
        }
    }


    private void join() {
        Log.d(TAG, "onJoin");

        mSignalingChannel = new SignalingChannel(Config.VIDEO_SERVER_ADDRESS, sessionId, usernameStr, true);
        mSignalingChannel.setJoinListener(this);
        mSignalingChannel.setDisconnectListener(this);
//        mSignalingChannel.setSessionFullListener(this);
        mSignalingChannel.setRefreshListListener(this);
        mSignalingChannel.join("");

        mStreamSet = SimpleStreamSet.defaultConfig(wantAudio, wantVideo);
        //select back camera
        CameraSource.getInstance().selectSource(1);

//        mSelfView = CameraSource.getInstance().createVideoView();
//        mSelfView.setRotation((mSelfView.getRotation() + 3) % 4);
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

        String message = peerChannel.getPeerId() + " joined.";
//        connectText.setText(message);
//        acceptButton.setEnabled(true);
//        declineButton.setEnabled(true);
//        fabAddFriend.setEnabled(true);
        addFriendButton.setEnabled(true);

        onAcceptClicked(null);
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

//        connectText.setText(R.string.connect_hint_vi);
//        acceptButton.setEnabled(false);
//        declineButton.setEnabled(false);
        addFriendButton.setEnabled(false);
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
                    Log.d(TAG, "onMessage: seesionDescription is null!");
                }
            } catch (InvalidDescriptionException e) {
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
                if (success) {
                    String name = json.getString("name");
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

    private void onInboundCall(final SessionDescription sessionDescription) {
        Log.d(TAG, "onInboundCall");
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

    // Use this method
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
        Log.d(TAG, "onDisconnect");
        Toast.makeText(this, "Disconnected from server", Toast.LENGTH_SHORT).show();
        updateVideoView(false);
        mStreamSet = null;
        if (mRtcSession != null) {
            mRtcSession.stop();
        } else {
            Log.d(TAG, "onDisconnect: mRtcSession is null!");
        }
        mRtcSession = null;
        mSignalingChannel = null;

        params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("username", usernameStr));
        ServerRequest sr = new ServerRequest();
        JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/api/deleteroom", params);
//        if (json != null) {
//            try {
//                String jsonStr = json.getString("response");
////                Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_SHORT).show();
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }

        if (isVideoStart) {
            Intent rateActivity = new Intent(VIVideoActivity.this, VIRateActivity.class);
            rateActivity.putExtra("helperName", helperName);
            startActivity(rateActivity);
        } else {
//            Intent rateActivity = new Intent(VIVideoActivity.this, VIRateActivity.class);
//            rateActivity.putExtra("helperName", "FakeName");
//            startActivity(rateActivity);
            Intent homeActivity = new Intent(VIVideoActivity.this, VIHomeActivity.class);
            startActivity(homeActivity);
        }
        finish();
    }

    @Override
    public void onSessionFull() {
        Toast.makeText(this, "Session is full", Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (isNavigation && isSendingLocation)
            stopLocationUpdates();
    }

    /**
     * Shutdown the process as a workaround until cleanup has been fully implemented.
     */
    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        quit();
//        onQuitClicked(findViewById(R.id.quit_btn));
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(isNavigation && isSendingLocation)
            startLocationUpdates();
    }


    /**
     * Build Google API Client for use to get current location
     * @param context
     */
    private synchronized void buildGoogleApiClient(Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

    }

    /**
     * Set location request parameters
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        if (mGoogleApiClient == null) {
            buildGoogleApiClient(this);
        }
        else if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        //only when current fragment is being viewed and location permission is granted
        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            Log.d(TAG, "Location is updating...");
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * Start location update when Google API Client is connected
     * @param bundle
     */
    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    /**
     * Reconnects Google API Client when connection is suspended
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "GoogleApiClient connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient connection failed");
    }

    /**
     * Call back from Goolge API Client whenever the location is changed
     * @param location Current location
     */
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged() called");
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
        sendLocation(latitude, longitude);
    }

    private void sendLocation(String latitude, String longitude) {
        Log.d(TAG, "Send lat and lng!");
        if (mPeerChannel != null) {
            try {
                JSONObject json = new JSONObject();
                json.putOpt("lat", latitude);
                json.putOpt("lng", longitude);
                mPeerChannel.send(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void onToggleSendLocation(View view) {
        Log.d(TAG, "onToggleSendLocation");
        if (isSendingLocation) {
            stopLocationUpdates();
            toggleLocationButton.setText(R.string.toggle_location_button_off);
            isSendingLocation = false;
        } else {
            startLocationUpdates();
            toggleLocationButton.setText(R.string.toggle_location_button_on);
            isSendingLocation = true;
        }
    }

    /**
     * When a waiting helper on the list has been clicked.
     * <p/>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (isVideoStart) return;
        Log.d(TAG, "VI click on a helper on the list");
        final JSONObject helper = ((HelperListAdapter) parent.getAdapter()).getItem(position);
        try {
            final String username = helper.getString("username");
//            final float rate = (float) helper.getDouble("rate");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(String.format(getResources().getString(R.string.confirm_accept_helper), username));
            builder.setPositiveButton(R.string.accept_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (mSignalingChannel != null) {
                        mSignalingChannel.select(username);
                    } else {
                        Log.d(TAG, "onItemClick: mSignalingChannel is null!");
                    }
                }
            });
            builder.setNegativeButton(R.string.decline_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /* When a helper join or leave the waiting list, need to refresh it */
    @Override
    public void onRefresh(String helperListJson) {
//        System.out.println("===================");
//        System.out.println(helperListJson);
        helperListAdapter.clear();
        JSONArray helperArray = null;
        try {
            helperArray = new JSONArray(helperListJson);
            for (int i = 0; i < helperArray.length(); i++) {
                helperListAdapter.add(helperArray.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
//        boolean permissionsGranted = true; //if all permissions are granted
//
//        for (int grantResult : grantResults) {
//            if (grantResult == PackageManager.PERMISSION_DENIED) {
//                permissionsGranted = false;
//                break;
//            }
//        }
//
//        if (permissionsGranted) {
//            startLocationUpdates();
//        }
//    }

    private class HelperListAdapter extends ArrayAdapter<JSONObject> {

        private final Context context;
        private final List<JSONObject> helpers;

        /**
         * Constructor
         *
         * @param context  The current context.
         * @param resource The resource ID for a layout file containing a TextView to use when
         *                 instantiating views.
         * @param objects  The objects to represent in the ListView.
         */
        public HelperListAdapter(Context context, int resource, List<JSONObject> objects) {
            super(context, resource, objects);
            this.context = context;
            this.helpers = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.helper_list_item, parent, false);
            AppCompatTextView usernameText = (AppCompatTextView) rowView.findViewById(R.id.helper_item_username);
            AppCompatTextView ratingText = (AppCompatTextView) rowView.findViewById(R.id.helper_item_rating);

            try {
                JSONObject helper = helpers.get(position);
                usernameText.setText(helper.getString("username"));
                ratingText.setText(String.valueOf(helper.getDouble("rate")));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return rowView;
        }
    }

}
