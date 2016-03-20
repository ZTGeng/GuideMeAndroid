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
import android.support.v7.widget.Toolbar;
import android.view.View;
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

public class VIHomeActivity extends AppCompatActivity {

    SharedPreferences pref;
    String token,grav,usernameStr,oldpasstxt,newpasstxt;
    AppCompatButton chgpass,chgpassfr,cancel,logout,newVideoCall;
    Dialog dlg;
    AppCompatEditText oldpass,newpass;
    List<NameValuePair> params;
    AppCompatTextView username;
    AppCompatSpinner topicSpinner;
    AppCompatSpinner friendSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vi_home);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        username = (AppCompatTextView) findViewById(R.id.username_text);
        chgpass = (AppCompatButton)findViewById(R.id.change_btn);
        logout = (AppCompatButton)findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
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
        username.setText(usernameStr);

        chgpass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dlg = new Dialog(VIHomeActivity.this);
                dlg.setContentView(R.layout.change_password_frag);
                dlg.setTitle("Change Password");
                chgpassfr = (AppCompatButton) dlg.findViewById(R.id.change_btn);

                chgpassfr.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        oldpass = (AppCompatEditText) dlg.findViewById(R.id.oldpass);
                        newpass = (AppCompatEditText) dlg.findViewById(R.id.newpass);
                        oldpasstxt = oldpass.getText().toString();
                        newpasstxt = newpass.getText().toString();
                        params = new ArrayList<NameValuePair>();
                        params.add(new BasicNameValuePair("oldpass", oldpasstxt));
                        params.add(new BasicNameValuePair("newpass", newpasstxt));
                        params.add(new BasicNameValuePair("id", token));
                        ServerRequest sr = new ServerRequest();
                        JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/api/chgpass", params);
                        if (json != null) {
                            try {
                                String jsonstr = json.getString("response");
                                if (json.getBoolean("res")) {

                                    dlg.dismiss();
                                    Toast.makeText(getApplication(), jsonstr, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getApplication(), jsonstr, Toast.LENGTH_SHORT).show();

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                });
                cancel = (AppCompatButton) dlg.findViewById(R.id.cancelbtn);
                cancel.setOnClickListener(new View.OnClickListener() {
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


        newVideoCall = (AppCompatButton) findViewById(R.id.new_video_call);
        newVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent viVideoActivity = new Intent(VIHomeActivity.this, VIVideoActivity.class);
                startActivity(viVideoActivity);
                finish();
            }
        });
    }

}
