package edu.sfsu.geng.guideme.login;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import edu.sfsu.geng.guideme.Config;
import edu.sfsu.geng.guideme.R;
import edu.sfsu.geng.guideme.ServerRequest;

public class RegisterActivity extends AppCompatActivity {

    final static int PASSWORDLIMIT = 8;

    //    RadioButton viRadioButton;
    AppCompatEditText email,username,password;
    AppCompatButton login,registerV, registerH;
    String emailText, usernameText, passwordText;//, roleText;
    List<NameValuePair> params;
    boolean emailOK, usernameOK, passwordOK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        email = (AppCompatEditText)findViewById(R.id.email);
        username = (AppCompatEditText)findViewById(R.id.username);
        password = (AppCompatEditText)findViewById(R.id.password);
        registerV = (AppCompatButton)findViewById(R.id.registerbtnv);
        registerH = (AppCompatButton)findViewById(R.id.registerbtnh);
        login = (AppCompatButton)findViewById(R.id.login);
//        viRadioButton = (RadioButton)findViewById(R.id.radio_button_vi);
//        roleText = "helper";

        emailOK = false;
        usernameOK = false;
        passwordOK = false;

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent regactivity = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(regactivity);
                finish();
            }
        });


        registerV.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (!isInputValid()) return;

                emailText = email.getText().toString();
                usernameText = username.getText().toString();
                passwordText = password.getText().toString();
                params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("email", emailText));
                params.add(new BasicNameValuePair("username", usernameText));
                params.add(new BasicNameValuePair("password", passwordText));
                params.add(new BasicNameValuePair("role", "vi"));
                ServerRequest sr = new ServerRequest();
                JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/register", params);

                if(json != null){
                    try{
                        String jsonStr = json.getString("response");

                        Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_LONG).show();

                        Log.d("Hello", jsonStr);

                        Intent regactivity = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(regactivity);
                        finish();
                    }catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        registerH.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (!isInputValid()) return;

                emailText = email.getText().toString();
                usernameText = username.getText().toString();
                passwordText = password.getText().toString();
                params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("email", emailText));
                params.add(new BasicNameValuePair("username", usernameText));
                params.add(new BasicNameValuePair("password", passwordText));
                params.add(new BasicNameValuePair("role", "helper"));
                ServerRequest sr = new ServerRequest();
                JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/register", params);

                if(json != null){
                    try{
                        String jsonStr = json.getString("response");

                        Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_LONG).show();

                        Log.d("Hello", jsonStr);

                        Intent regactivity = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(regactivity);
                        finish();
                    }catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailOK = (s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordOK = (s.length() >= PASSWORDLIMIT);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                for (int i = 0; i < s.length(); i++) {
                    if (!Character.isLetterOrDigit(s.charAt(i))) {
                        username.setTextColor(Color.RED);
                        usernameOK = false;
                        return;
                    }
                }
                username.setTextColor(Color.BLACK);
                usernameOK = true;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private boolean isInputValid() {
        if (!emailOK) {
            Toast.makeText(getApplication(), R.string.email_not_ok_hint, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!usernameOK) {
            Toast.makeText(getApplication(), R.string.username_not_ok_hint, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!passwordOK) {
            Toast.makeText(getApplication(), String.format(getResources().getString(R.string.password_not_ok_hint), PASSWORDLIMIT), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

}
