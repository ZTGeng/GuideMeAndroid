package edu.sfsu.geng.guideme.helper;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
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

public class HelperHomeActivity extends AppCompatActivity {

    SharedPreferences pref;
    String token,grav,usernameStr,oldpasstxt,newpasstxt;
    AppCompatButton chgpass,chgpassfr,cancel,logout;
    Dialog dlg;
    AppCompatEditText oldpass,newpass;
    List<NameValuePair> params;
    AppCompatTextView username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_home);

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
                Intent loginactivity = new Intent(HelperHomeActivity.this, LoginActivity.class);

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
                dlg = new Dialog(HelperHomeActivity.this);
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
                        String loginServer = Config.LOGIN_SERVER_ADDRESS + ":" + Config.LOGIN_SERVER_PORT;
                        JSONObject json = sr.getJSON(loginServer + "/api/chgpass", params);
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

    }

}
