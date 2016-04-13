package edu.sfsu.geng.guideme.visualimpairer;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import edu.sfsu.geng.guideme.Config;
import edu.sfsu.geng.guideme.R;
import edu.sfsu.geng.guideme.login.LoginActivity;
import edu.sfsu.geng.guideme.login.ServerRequest;

public class VIHomeActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    SharedPreferences pref;
    String token, grav, usernameStr, oldpassStr, newpassStr, topicStr;
    int rateInt;
    AppCompatButton chgPasswordBtn, chgpassfrBtn, cancelBtn, logoutBtn, newVideoCallBtn;
    Dialog dlg;
    AppCompatEditText oldpassEditText, newpassEditText;
    List<NameValuePair> params;
    AppCompatTextView usernameText;
    AppCompatSpinner topicSpinner;
    AppCompatSpinner friendSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vi_home);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        usernameText = (AppCompatTextView) findViewById(R.id.username_text);
        chgPasswordBtn = (AppCompatButton)findViewById(R.id.change_btn);
        logoutBtn = (AppCompatButton)findViewById(R.id.logout);
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor edit = pref.edit();
                //Storing Data using SharedPreferences
                edit.putString("token", "");
                edit.commit();
                Intent loginactivity = new Intent(VIHomeActivity.this, LoginActivity.class);

                startActivity(loginactivity);
                finish();
            }
        });

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        token = pref.getString("token", "");
        grav = pref.getString("grav", "");
        usernameStr = pref.getString("username", "");
        usernameText.setText(usernameStr);
        rateInt = pref.getInt("rate", 5);

        chgPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });

        topicSpinner = (AppCompatSpinner) findViewById(R.id.topic_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.topics_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        topicSpinner.setAdapter(adapter);

        friendSpinner = (AppCompatSpinner) findViewById(R.id.friend_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> friends = ArrayAdapter.createFromResource(this,
                R.array.friends_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        friendSpinner.setAdapter(friends);

        topicStr = "General";
        topicSpinner.setOnItemSelectedListener(this);


        newVideoCallBtn = (AppCompatButton) findViewById(R.id.new_video_call);
        newVideoCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("username", usernameStr));
                params.add(new BasicNameValuePair("topic", topicStr));
                params.add(new BasicNameValuePair("rate", String.valueOf(rateInt)));

                ServerRequest sr = new ServerRequest();
                JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/api/createroom", params);
                if (json != null) {
                    try {
                        String jsonStr = json.getString("response");
                        Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_SHORT).show();
                        String roomId = json.getString("roomId");
                        Intent viVideoActivity = new Intent(VIHomeActivity.this, VIVideoActivity.class);
                        viVideoActivity.putExtra("sessionId", roomId);
//                        viVideoActivity.putExtra("userId", usernameStr);
                        viVideoActivity.putExtra("isNavigation", topicStr.equals("Navigation"));
                        startActivity(viVideoActivity);
                        finish();
//                        if (json.getBoolean("res")) {
//
//                            dlg.dismiss();
//                            Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_SHORT).show();
//                        } else {
//
//                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * <p>Callback method to be invoked when an item in this view has been
     * selected. This callback is invoked only when the newly selected
     * position is different from the previously selected position or if
     * there was no selected item.</p>
     * <p/>
     * Impelmenters can call getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param parent   The AdapterView where the selection happened
     * @param view     The view within the AdapterView that was clicked
     * @param position The position of the view in the adapter
     * @param id       The row id of the item that is selected
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        topicStr = parent.getAdapter().getItem(position).toString();
//        Toast.makeText(this, topicStr, Toast.LENGTH_LONG).show();
    }

    /**
     * Callback method to be invoked when the selection disappears from this
     * view. The selection can disappear for instance when touch is activated
     * or when the adapter becomes empty.
     *
     * @param parent The AdapterView that now contains no selected item.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
