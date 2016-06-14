package edu.sfsu.geng.guideme.video;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by geng on 3/15/16.
 */
public class SignalingChannel {
    public static final String TAG = "EventSource";

    private final HttpClient mHttpSendClient = new DefaultHttpClient();
    private final Handler mMainHandler;
    private final String mClientToServerUrl;
    private final String mServerToClientUrl;
    private Handler mSendHandler;
    private InputStream mEventStream;
    private Map<String, PeerChannel> mPeerChannels = new HashMap<>();
    private JoinListener mJoinListener;
    private DisconnectListener mDisconnectListener;
    private SessionFullListener mSessionFullListener;
    private RefreshListListener mRefreshListListener;

    public SignalingChannel(String baseUrl, String session, String userId, String roleOrRate) {
//        String userId = new BigInteger(40, new Random()).toString(32);
        mServerToClientUrl = baseUrl + "/stoc/" + session + "/" + userId + "/" + roleOrRate;
        mClientToServerUrl = baseUrl + "/ctos/" + session + "/" + userId;
        mMainHandler = new Handler(Looper.getMainLooper());
        Thread sendThread = new SendThread();
        sendThread.start();
        open();
    }

    private class SendThread extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            mSendHandler = new Handler();
            Looper.loop();
            Log.d(TAG, "SendThread: quit");
        }
    }

    private void open() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(mServerToClientUrl);

                try {
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    HttpEntity httpEntity = httpResponse.getEntity();

                    if (httpEntity != null) {
                        mEventStream = httpEntity.getContent();
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(mEventStream));
                        readEventStream(bufferedReader);
                    }
                } catch (IOException exception) {
                    Log.e(TAG, "SSE: " + exception);
                    exception.printStackTrace();
                } finally {
                    if (mEventStream != null) {
                        try {
                            mEventStream.close();
                        } catch (IOException ignored) {
                        }
                        mEventStream = null;
                    }
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            for (PeerChannel peerChannel : mPeerChannels.values()) {
                                peerChannel.onDisconnect();
                            }
                            if (mDisconnectListener != null) {
                                mDisconnectListener.onDisconnect();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    public void kickoff(String baseUrl, String session, String peerId) {
        final String mKickoffUrl = baseUrl + "/kickoff/" + session + "/" + peerId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(mKickoffUrl);

                try {
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    HttpEntity httpEntity = httpResponse.getEntity();
                } catch (IOException exception) {
                    Log.e(TAG, "Kickoff Error: " + exception);
                    exception.printStackTrace();
                }
            }
        }).start();
    }

    public void select(String baseUrl, String session, String peerId) {
        final String mSelectUrl = baseUrl + "/select/" + session + "/" + peerId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(mSelectUrl);

                try {
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    HttpEntity httpEntity = httpResponse.getEntity();
                } catch (IOException exception) {
                    Log.e(TAG, "Select Error: " + exception);
                    exception.printStackTrace();
                }
            }
        }).start();
    }

    private void readEventStream(final BufferedReader bufferedReader) throws IOException {
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.length() > 1) {
                String[] eventSplit = line.split(":", 2);

                if (eventSplit.length != 2 || !eventSplit[0].equals("event")) {
                    Log.w(TAG, "SSE: invalid event: " + line + " => " + Arrays.toString(eventSplit));
                    while (!(line = bufferedReader.readLine()).isEmpty()) {
                        Log.w(TAG, "SSE: skipped after malformed event: " + line);
                    }
                    break;
                }

                final String event = eventSplit[1];

                if ((line = bufferedReader.readLine()) != null) {
                    final String[] dataSplit = line.split(":", 2);

                    if (dataSplit.length != 2 || !dataSplit[0].equals("data")) {
                        Log.w(TAG, "SSE: invalid data: " + line + " => " + Arrays.toString(dataSplit));
                    }
                    final String data = dataSplit[1];

                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            handleEvent(event, data);
                        }
                    });
                }
            }
        }
    }

    private void handleEvent(final String event, final String data) {
        if (event.startsWith("user-")) {
            String peer = event.substring(5);
            PeerChannel peerChannel = mPeerChannels.get(peer);
            if (peerChannel != null) {
                peerChannel.onMessage(data);
            }
        } else if (event.equals("join")) {
            PeerChannel peerChannel = new PeerChannel(data);
            mPeerChannels.put(data, peerChannel);
            if (mJoinListener != null) {
                mJoinListener.onPeerJoin(peerChannel);
            }
        } else if (event.equals("leave")) {
            PeerChannel peerChannel = mPeerChannels.remove(data);
            if (peerChannel != null) {
                peerChannel.onDisconnect();
            }
        } else if (event.equals("kicked")) {
            if (mDisconnectListener != null) {
                mDisconnectListener.onDisconnect();
            }
        } else if (event.equals("sessionfull")) {
            if (mSessionFullListener != null) {
                mSessionFullListener.onSessionFull();
            }
        } else if (event.equals("refresh")) {
            if (mRefreshListListener != null) {
                mRefreshListListener.onRefresh(data);
            }
        } else {
            Log.w(TAG, "unhandled event: " + event);

        }
    }

    public void setJoinListener(JoinListener joinListener) {
        mJoinListener = joinListener;
    }

    public void setDisconnectListener(DisconnectListener onDisconnectListener) {
        mDisconnectListener = onDisconnectListener;
    }

    public void setSessionFullListener(final SessionFullListener sessionFullListener) {
        mSessionFullListener = sessionFullListener;

    }
    public void setRefreshListListener(final RefreshListListener refreshListListener) {
        mRefreshListListener = refreshListListener;
    }

    public interface MessageListener {
        public void onMessage(JSONObject data);
    }

    public interface JoinListener {
        public void onPeerJoin(final PeerChannel peerChannel);
    }

    public interface SessionFullListener {
        public void onSessionFull();
    }

    public interface DisconnectListener {
        public void onDisconnect();
    }

    public interface PeerDisconnectListener {
        public void onPeerDisconnect(final PeerChannel peerChannel);
    }

    public interface RefreshListListener {
        public void onRefresh(String helperListJson);
    }

    public class PeerChannel {
        private final String mPeerId;
        private MessageListener mMessageListener;
        private PeerDisconnectListener mDisconnectListener;
        private boolean mDisconnected = false;

        private PeerChannel(String peerId) {
            mPeerId = peerId;
        }

        public void send(final JSONObject message) {
            if (mDisconnected) {
                Log.w(TAG, "tried to send message to disconnected peer: " + mPeerId);
                return;
            }
            mSendHandler.post(new Runnable() {
                @Override
                public void run() {
                    HttpPost httpPost = new HttpPost(mClientToServerUrl + "/" + mPeerId);
                    try {
                        httpPost.setEntity(new ByteArrayEntity(message.toString().getBytes("UTF8")));
                        mHttpSendClient.execute(httpPost);
                    } catch (IOException exception) {
                        Log.e(TAG, "failed to send message to " + mPeerId + ": " + exception.toString());
                    }
                }
            });
        }

        private void onMessage(String message) {
            if (mDisconnected) {
                Log.w(TAG, "got message from disconnected peer: " + mPeerId);
                return;
            }
            if (mMessageListener != null) {
                try {
                    JSONObject json = new JSONObject(message);
                    mMessageListener.onMessage(json);
                } catch (JSONException exception) {
                    Log.w(TAG, "failed to decode message: " + exception);
                }
            }
        }

        private void onDisconnect() {
            mDisconnected = true;
            if (mDisconnectListener != null) {
                mDisconnectListener.onPeerDisconnect(this);
                mDisconnectListener = null;
                mMessageListener = null;
            }
        }

        public void setMessageListener(final MessageListener messageListener) {
            mMessageListener = messageListener;
        }

        public void setDisconnectListener(final PeerDisconnectListener onDisconnectListener) {
            mDisconnectListener = onDisconnectListener;
        }

        public String getPeerId() {
            return mPeerId;
        }

        @Override
        public String toString() {
            return "User[" + mPeerId + "]";
        }
    }
}
