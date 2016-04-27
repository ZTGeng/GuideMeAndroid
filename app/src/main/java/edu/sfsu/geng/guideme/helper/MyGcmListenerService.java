package edu.sfsu.geng.guideme.helper;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import java.util.Date;

import edu.sfsu.geng.guideme.R;

public class MyGcmListenerService extends GcmListenerService {
    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String senderName = data.getString("senderName");
        String roomId = data.getString("roomId");
        boolean isNavigation = "true".equals(data.getString("isNavigation"));
        long time = data.getLong("time");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Sender: " + senderName + " roomId: " + roomId + " isNavigation: " + isNavigation + " time: " + (new Date(time)).toString());

//        if (from.startsWith("/topics/")) {
//            // message received from some topic.
//        } else {
//            // normal downstream message.
//        }

        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        sendNotification(senderName, roomId, isNavigation);
        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param senderName GCM message received.
     * @param roomId GCM message received.
     */
    private void sendNotification(String senderName, String roomId, boolean isNavigation) {
        Intent helperVideoActivity = new Intent(this, HelperVideoActivity.class);
        helperVideoActivity.putExtra("sessionId", roomId);
        helperVideoActivity.putExtra("isNavigation", isNavigation);
        helperVideoActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, helperVideoActivity,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle("GuideMe Message")
                .setContentText(String.format(getString(R.string.notify_message), senderName))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
