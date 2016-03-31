package edu.sfsu.geng.guideme.helper;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
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

import org.apache.http.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import edu.sfsu.geng.guideme.Config;
import edu.sfsu.geng.guideme.R;
import edu.sfsu.geng.guideme.video.SignalingChannel;

public class HelperVideoActivity extends AppCompatActivity implements
        SignalingChannel.JoinListener,
        SignalingChannel.DisconnectListener,
        SignalingChannel.SessionFullListener,
        SignalingChannel.MessageListener,
        SignalingChannel.PeerDisconnectListener,
        RtcSession.OnLocalCandidateListener,
        RtcSession.OnLocalDescriptionListener//,
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
    FloatingActionButton fab;

    private SignalingChannel mSignalingChannel;
//    private InputMethodManager mInputMethodManager;
//    private WindowManager mWindowManager;
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

//    private GoogleMap mMap;
//    private GoogleApiClient mGoogleApiClient;
//    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_helper_video);
        sessionId = getIntent().getStringExtra("sessionId");

        SharedPreferences pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        usernameStr = pref.getString("username", "");

        fab = (FloatingActionButton) findViewById(R.id.fab);

        connectText = (AppCompatTextView) findViewById(R.id.connect_text);
        if (connectText != null) {
            connectText.setText(R.string.connect_hint_helper);
        } else {
            Log.d(TAG, "onCreate: connectText is null!");
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

//        mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
//        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
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

//        createLocationRequest();
//        buildGoogleApiClient(this);

        join();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_vi_video);

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


//    public void onAcceptClicked(final View view) {
//        Log.d(TAG, "onAcceptClicked");
//        if (mRtcSession != null) {
//            if (mStreamSet != null) {
//                updateVideoView(true);
//                mRtcSession.start(mStreamSet);
//
//                params = new ArrayList<NameValuePair>();
//                params.add(new BasicNameValuePair("username", usernameStr));
//
//                ServerRequest sr = new ServerRequest();
//                JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/api/deleteroom", params);
//                if (json != null) {
//                    try {
//                        String jsonStr = json.getString("response");
//                        Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_SHORT).show();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
//            } else {
//                Log.d(TAG, "onAcceptClicked: mStreamSet is null!");
//            }
//        } else {
//            Log.d(TAG, "onAcceptClicked: mRtcSession is null!");
//        }
//    }
//
//    public void onDeclineClicked(final View view) {
//        Log.d(TAG, "onRefuseClicked");
//        // TODO: tell peer to quit
////        if (mRtcSession != null) {
////            mRtcSession.stop();
////        } else {
////            Log.d(TAG, "onAcceptClicked: mRtcSession is null!");
////        }
//        mPeerChannel = null;
//    }

    public void onFabClicked(final View view) {
        Log.d(TAG, "Floating button onClicked");
        onDisconnect();
        Intent homeActivity = new Intent(HelperVideoActivity.this, HelperHomeActivity.class);
        startActivity(homeActivity);
        finish();
    }

//    public void onSelfViewClicked(final View view) {
//        Log.d(TAG, "onSelfViewClicked");
//        if (mStreamSet != null) {
//            if (mRemoteView != null) {
//                mRemoteView.setRotation((mRemoteView.getRotation() + 1) % 4);
//            }
//        }
//    }

    public void onRemoteViewClicked(final View view) {
        if (mStreamSet != null) {
            if (mRemoteView != null) {
                mRemoteView.setRotation((mRemoteView.getRotation() + 1) % 4);
            }
        }
    }

//    private String getRandomSessionId() {
//        Random random = new Random();
//        return String.valueOf(random.nextInt(10000000));
//    }

    private void join() {
        Log.d(TAG, "onJoin");

//        String sessionId = getRandomSessionId();
//        connectText.setText(connectText.getText() + sessionId); // <- for test only

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
                    Log.e(TAG, "onMessage: sessionDescription is null!");
                }
            } catch (InvalidDescriptionException e) {
                e.printStackTrace();
            }
        }
//        if (json.has("orientation")) {
//                handleOrientation(json.getInt("orientation"));
//        }
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
    }

    @Override
    public void onSessionFull() {
        Toast.makeText(this, "Session is full", Toast.LENGTH_LONG).show();
    }

//    private void showSettings() {
//        mUrlSetting.requestFocus();
//        mInputMethodManager.showSoftInput(mUrlSetting, InputMethodManager.SHOW_IMPLICIT);
//        mSettingsHeader.setVisibility(View.VISIBLE);
//        mSettingsHeader.setRotationX(SETTINGS_ANIMATION_ANGLE);
//        mSettingsHeader.animate().rotationX(0).setDuration(SETTINGS_ANIMATION_DURATION).start();
//        mHeader.setVisibility(View.VISIBLE);
//        mHeader.animate()
//                .rotationX(-SETTINGS_ANIMATION_ANGLE)
//                .setDuration(SETTINGS_ANIMATION_DURATION)
//                .withEndAction(new Runnable() {
//                    @Override
//                    public void run() {
//                        mHeader.setVisibility(View.INVISIBLE);
//                    }
//                }).start();
//    }
//
//    private void hideSettings() {
//        mInputMethodManager.hideSoftInputFromWindow(mUrlSetting.getWindowToken(), 0);
//        mHeader.setVisibility(View.VISIBLE);
//        mHeader.setRotationX(SETTINGS_ANIMATION_ANGLE);
//        mHeader.animate().rotationX(0).setDuration(SETTINGS_ANIMATION_DURATION).start();
//        mSettingsHeader.setVisibility(View.VISIBLE);
//        mSettingsHeader.animate()
//                .rotationX(-SETTINGS_ANIMATION_ANGLE)
//                .setDuration(SETTINGS_ANIMATION_DURATION)
//                .withEndAction(new Runnable() {
//                    @Override
//                    public void run() {
//                        mSettingsHeader.setVisibility(View.INVISIBLE);
//                    }
//                }).start();
//    }

//    private void saveUrl(final String url) {
//        PreferenceManager.getDefaultSharedPreferences(this).edit()
//                .putString(PREFERENCE_KEY_SERVER_URL, url).commit();
//    }
//
//    private String getUrl() {
//        return PreferenceManager.getDefaultSharedPreferences(this)
//                .getString(PREFERENCE_KEY_SERVER_URL, Config.VIDEO_SERVER_ADDRESS);
//    }

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
        if (mRtcSession != null) {
            mRtcSession.stop();
        }
        mPeerChannel = null;
        finish();
//        System.exit(0);
    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        startLocationUpdates();
//    }
//

//    /**
//     * Build Google API Client for use to get current location
//     * @param context
//     */
//    private synchronized void buildGoogleApiClient(Context context) {
//        mGoogleApiClient = new GoogleApiClient.Builder(context)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .addApi(LocationServices.API)
//                .build();
//
//    }
//
//    /**
//     * Set location request parameters
//     */
//    private void createLocationRequest() {
//        mLocationRequest = new LocationRequest();
//        mLocationRequest.setInterval(UPDATE_INTERVAL);
//        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//    }
//
//    /**
//     * Requests location updates from the FusedLocationApi.
//     */
//    protected void startLocationUpdates() {
//        if (mGoogleApiClient == null) {
//            buildGoogleApiClient(this);
//        }
//        else if (!mGoogleApiClient.isConnected()) {
//            mGoogleApiClient.connect();
//        }
//        //only when current fragment is being viewed and location permission is granted
//        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
//        }
//    }
//
//    /**
//     * Removes location updates from the FusedLocationApi.
//     */
//    protected void stopLocationUpdates() {
//        if(mGoogleApiClient!=null && mGoogleApiClient.isConnected())
//            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
//    }
//
//    /**
//     * Start location update when Google API Client is connected
//     * @param bundle
//     */
//    @Override
//    public void onConnected(Bundle bundle) {
//        startLocationUpdates();
//    }
//
//    /**
//     * Reconnects Google API Client when connection is suspended
//     * @param i
//     */
//    @Override
//    public void onConnectionSuspended(int i) {
////        Log.d(TAG, "GoogleApiClient connection suspended");
//        mGoogleApiClient.connect();
//    }
//
//    @Override
//    public void onConnectionFailed(ConnectionResult connectionResult) {
////        Log.d(TAG, "GoogleApiClient connection failed");
//    }
//
//    /**
//     * Call back from Goolge API Client whenever the location is changed
//     * @param location Current location
//     */
//    @Override
//    public void onLocationChanged(Location location) {
////        Log.d(TAG, "onLocationChanged() called");
//        String latitude = String.valueOf(location.getLatitude());
//        String longitude = String.valueOf(location.getLongitude());
////        coordinateTextView.append(latitude + ", " + longitude + "   ");
//        sendLocation(latitude, longitude);
//    }
//
//    private void sendLocation(String latitude, String longitude) {
//        if (mPeerChannel != null) {
//            try {
//                JSONObject json = new JSONObject();
//                json.putOpt("lat", latitude);
//                json.putOpt("lng", longitude);
//                mPeerChannel.send(json);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
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

}
