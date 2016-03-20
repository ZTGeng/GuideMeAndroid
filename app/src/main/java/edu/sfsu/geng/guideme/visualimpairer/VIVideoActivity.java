package edu.sfsu.geng.guideme.visualimpairer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
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
//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.location.LocationListener;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import edu.sfsu.geng.guideme.Config;
import edu.sfsu.geng.guideme.R;
import edu.sfsu.geng.guideme.video.SignalingChannel;

public class VIVideoActivity extends AppCompatActivity implements
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

    private static final String TAG = "VIVideo";

    /**
     * Initialize OpenWebRTC at startup
     */
    static {
        Log.d(TAG, "Initializing OpenWebRTC");
        Owr.init();
        Owr.runInBackground();
    }

    private AppCompatTextView connectText;
    private AppCompatButton acceptButton, declineButton;
    FloatingActionButton fab;

    private SignalingChannel mSignalingChannel;
//    private InputMethodManager mInputMethodManager;
//    private WindowManager mWindowManager;
    private SignalingChannel.PeerChannel mPeerChannel;
    private RtcSession mRtcSession;
    private SimpleStreamSet mStreamSet;
    private VideoView mSelfView;
    private RtcConfig mRtcConfig;


    private final static boolean wantAudio = true;
    private final static boolean wantVideo = true;
    public static final int LOCATION_PERMISSION = 15;
//    private GoogleApiClient mGoogleApiClient;
//    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_vi_video);


        fab = (FloatingActionButton) findViewById(R.id.fab);

        connectText = (AppCompatTextView) findViewById(R.id.connect_text);
        if (connectText != null) {
            connectText.setText(R.string.connect_hint);
        } else {
            Log.d(TAG, "onCreate: connectText is null!");
        }

        acceptButton = (AppCompatButton) findViewById(R.id.accept_btn);
        if (acceptButton != null) {
            acceptButton.setEnabled(false);
        } else {
            Log.d(TAG, "onCreate: acceptButton is null!");
        }

        declineButton = (AppCompatButton) findViewById(R.id.decline_btn);
        if (declineButton != null) {
            declineButton.setEnabled(false);
        } else {
            Log.d(TAG, "onCreate: declineButton is null!");
        }

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
            connectText.setText(R.string.connect_hint);
        } else {
            Log.d(TAG, "onConfigurationChanged: connectText is null!");
        }

        acceptButton = (AppCompatButton) findViewById(R.id.accept_btn);
        if (acceptButton != null) {
            acceptButton.setEnabled(false);
        } else {
            Log.d(TAG, "onConfigurationChanged: acceptButton is null!");
        }

        declineButton = (AppCompatButton) findViewById(R.id.decline_btn);
        if (declineButton != null) {
            declineButton.setEnabled(false);
        } else {
            Log.d(TAG, "onConfigurationChanged: declineButton is null!");
        }

        updateVideoView(true);
    }

    private void updateVideoView(boolean running) {
        if (mStreamSet != null) {
            TextureView selfView = (TextureView) findViewById(R.id.self_view);
            if (selfView != null) {
                selfView.setVisibility(running ? View.VISIBLE : View.INVISIBLE);
            } else {
                Log.d(TAG, "updateVideoView: selfView is null!");
            }
            if (running) {
                Log.d(TAG, "setting self-view: " + selfView);
                mSelfView.setView(selfView);
            } else {
                Log.d(TAG, "stopping self-view");
                mSelfView.stop();
            }
        } else {
            Log.d(TAG, "updateVideoView: mStreamSet is null!");
        }
    }


    public void onAcceptClicked(final View view) {
        Log.d(TAG, "onAcceptClicked");
        if (mRtcSession != null) {
            if (mStreamSet != null) {
                mRtcSession.start(mStreamSet);
            } else {
                Log.d(TAG, "onAcceptClicked: mStreamSet is null!");
            }
        } else {
            Log.d(TAG, "onAcceptClicked: mRtcSession is null!");
        }
        acceptButton.setEnabled(false);
        declineButton.setEnabled(false);
    }

    public void onDeclineClicked(final View view) {
        Log.d(TAG, "onRefuseClicked");
        // TODO: tell setver to remove peer
//        if (mRtcSession != null) {
//            mRtcSession.stop();
//        } else {
//            Log.d(TAG, "onAcceptClicked: mRtcSession is null!");
//        }
        mPeerChannel = null;
        acceptButton.setEnabled(false);
        declineButton.setEnabled(false);
    }

    public void onFabClicked(final View view) {
        Log.d(TAG, "Floating button onClicked");
        onDisconnect();
        Intent homeActivity = new Intent(VIVideoActivity.this, VIHomeActivity.class);
        startActivity(homeActivity);
        finish();
    }

//    public void onSelfViewClicked(final View view) {
//        Log.d(TAG, "onSelfViewClicked");
//        if (mStreamSet != null) {
//            if (mSelfView != null) {
//                mSelfView.setRotation((mSelfView.getRotation() + 1) % 4);
//            }
//        }
//    }

    private String getRandomSessionId() {
        Random random = new Random();
        return String.valueOf(random.nextInt(10000000));
    }

    private void join() {
        Log.d(TAG, "onJoin");

        String sessionId = getRandomSessionId();
        connectText.setText(connectText.getText() + sessionId); // <- for test only

        mSignalingChannel = new SignalingChannel(Config.VIDEO_SERVER_ADDRESS, sessionId);
        mSignalingChannel.setJoinListener(this);
        mSignalingChannel.setDisconnectListener(this);
        mSignalingChannel.setSessionFullListener(this);

        mStreamSet = SimpleStreamSet.defaultConfig(wantAudio, wantVideo);
        //select back camera
        CameraSource.getInstance().selectSource(1);

        mSelfView = CameraSource.getInstance().createVideoView();
        mSelfView.setRotation((mSelfView.getRotation() + 3) % 4);
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

        String message = peerChannel.getPeerId() + " joined.";
        connectText.setText(message);
        acceptButton.setEnabled(true);
        declineButton.setEnabled(true);
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
