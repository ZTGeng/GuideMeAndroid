package edu.sfsu.geng.guideme.visualimpairer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.sfsu.geng.guideme.Config;
import edu.sfsu.geng.guideme.R;
import edu.sfsu.geng.guideme.ServerDialogFragment;
import edu.sfsu.geng.guideme.login.LoginActivity;
import edu.sfsu.geng.guideme.ServerRequest;

public class VIHomeActivity extends AppCompatActivity
        implements StartCallDialogFragment.StartCallDialogListener,
        SelectFriendDialogFragment.SelectFriendDialogListener,
        ServerDialogFragment.ServerDialogListener
{
    private static final String TAG = "VIHome";

    SharedPreferences pref;
    String token, grav, usernameStr, oldpassStr, newpassStr, rateStr;
    AppCompatButton callBtn, callNaviBtn;
    boolean isNavigation;
    String desString; // description or destination
    String[] friends;
    AppCompatButton chgpassfrBtn, cancelBtn;
    Dialog dlg;
    AppCompatEditText oldpassEditText, newpassEditText;
    List<NameValuePair> params;
    AppCompatTextView usernameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vi_home);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        token = pref.getString("token", "");
        grav = pref.getString("grav", "");
        usernameStr = pref.getString("username", "");
        rateStr = pref.getString("rate", "5.0");

        usernameText = (AppCompatTextView) findViewById(R.id.username_text);
        usernameText.setText(usernameStr);

        callBtn = (AppCompatButton) findViewById(R.id.call_btn);
        callNaviBtn = (AppCompatButton) findViewById(R.id.call_navi_btn);

        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isNavigation = false;
                startCall();
            }
        });

        callNaviBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isNavigation = true;
                startCall();
            }
        });

        // Asyc call to get friends list
        getFriendsList();
    }

    // friends array should not be null after this method
    private void getFriendsList() {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("username", usernameStr));

        ServerRequest sr = new ServerRequest();
        JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/api/getfriendlist", params);
        if (json != null) {
            try {
                String jsonStr = json.getString("response");
                Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_SHORT).show();
                if (json.getBoolean("res")) {
                    JSONArray friendsJson =  json.getJSONArray("friends");
                    if (friendsJson == null) {
                        System.out.println("Friend list is Null!!!");
                    } else {
                        try {
                            friends = new String[friendsJson.length()];
                            for (int i = 0; i < friendsJson.length(); i++) {
                                friends[i] = friendsJson.getString(i);
                            }
                            Log.v(TAG, "Friend list: " + Arrays.toString(friends));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /* Methods for the StartCallDialog */
    private void startCall() {
        StartCallDialogFragment dialogFragment = new StartCallDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), "StartCallDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String desStr) {
        desString = desStr; // des could be ""
        // if no friends, call strangers.
        if (friends == null || friends.length == 0) {
            callStrangers();
        } else {
            selectFriend();
        }
    }

    @Override
    public boolean getNavigation() {
        return isNavigation;
    }

    /* Methods for the SelectFriendDialog */
    private void selectFriend() {
        SelectFriendDialogFragment friendDialogFragment = new SelectFriendDialogFragment();
        friendDialogFragment.show(getSupportFragmentManager(), "SelectFriendDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, ArrayList<Integer> selectedFriends) {
        // friends is not null, otherwise the selectFriendDialog will not start
        if (selectedFriends.isEmpty()) {
            callStrangers();
        } else {
            String friendsStr = "";
            for (int i : selectedFriends) {
                if (i < friends.length) {
                    friendsStr += ",";
                    friendsStr += friends[i];
                }
            }
            callFriends(friendsStr.substring(1));
        }
    }

    /* Methods for the ServerDialog */
    @Override
    public void onServerChanged(String serverUrl) {
        pref.edit().putString("serverBaseUrl", serverUrl).apply();
    }

    @Override
    public void onUseDefaultServer() {
        pref.edit().putString("serverBaseUrl", Config.VIDEO_SERVER_ADDRESS).apply();
    }

    @Override
    // friends should not be null. Just in case.
    public String[] getFriends() {
        return friends != null ? friends : new String[0];
    }


    /* Methods for the Calling */
    private void callStrangers() {
        params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("username", usernameStr)); // should use token?
        params.add(new BasicNameValuePair("des", desString));
        params.add(new BasicNameValuePair("isNavigation", String.valueOf(isNavigation)));
        params.add(new BasicNameValuePair("rate", rateStr));

        ServerRequest sr = new ServerRequest();
        JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/api/createroom", params);
        if (json != null) {
            try {
                String jsonStr = json.getString("response");
                Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_SHORT).show();
                String roomId = json.getString("roomId");
                Intent viVideoActivity = new Intent(VIHomeActivity.this, VIVideoActivity.class);
                viVideoActivity.putExtra("sessionId", roomId);
                viVideoActivity.putExtra("isNavigation", isNavigation);
                startActivity(viVideoActivity);
                finish();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void callFriends(String friendsStr) {
        String roomId = "b" + String.valueOf((int) (Math.random() * 10000000));
        params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("username", usernameStr)); // should use token?
        params.add(new BasicNameValuePair("friends", friendsStr));
        params.add(new BasicNameValuePair("roomId", roomId));
        params.add(new BasicNameValuePair("des", desString));
        params.add(new BasicNameValuePair("isNavigation", String.valueOf(isNavigation)));
        params.add(new BasicNameValuePair("rate", rateStr));

        ServerRequest sr = new ServerRequest();
        JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/api/callfriendsbyname", params);
        if (json != null) {
            try {
                if (json.getBoolean("res")) {
                    Intent viVideoActivity = new Intent(VIHomeActivity.this, VIVideoActivity.class);
                    viVideoActivity.putExtra("sessionId", roomId);
                    viVideoActivity.putExtra("isNavigation", isNavigation);
                    startActivity(viVideoActivity);
                    finish();
                } else {
                    Log.e(TAG, "Fail to call friends!");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /* Methods for the menu button */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.change_server_menu:
                ServerDialogFragment dialogFragment = new ServerDialogFragment();
                dialogFragment.show(getSupportFragmentManager(), "ServerDialogFragment");
                return true;
            case R.id.change_password_menu:
                dlg = new Dialog(VIHomeActivity.this);
                dlg.setContentView(R.layout.change_password_frag);
                dlg.setTitle("Change Password");
                chgpassfrBtn = (AppCompatButton) dlg.findViewById(R.id.change_btn);

                chgpassfrBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        oldpassEditText = (AppCompatEditText) dlg.findViewById(R.id.oldpass);
                        newpassEditText = (AppCompatEditText) dlg.findViewById(R.id.newpass);
                        oldpassStr = oldpassEditText.getText().toString();
                        newpassStr = newpassEditText.getText().toString();
                        params = new ArrayList<NameValuePair>();
                        params.add(new BasicNameValuePair("oldpass", oldpassStr));
                        params.add(new BasicNameValuePair("newpass", newpassStr));
                        params.add(new BasicNameValuePair("id", token));
                        ServerRequest sr = new ServerRequest();
                        JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/api/chgpass", params);
                        if (json != null) {
                            try {
                                String jsonStr = json.getString("response");
                                if (json.getBoolean("res")) {

                                    dlg.dismiss();
                                    Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_SHORT).show();

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                });
                cancelBtn = (AppCompatButton) dlg.findViewById(R.id.cancelbtn);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dlg.dismiss();
                    }
                });
                dlg.show();
                return true;
            case R.id.logout_menu:
                SharedPreferences.Editor edit = pref.edit();
                //Storing Data using SharedPreferences
                edit.putString("token", "");
                edit.commit();
                Intent loginactivity = new Intent(VIHomeActivity.this, LoginActivity.class);

                startActivity(loginactivity);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}


