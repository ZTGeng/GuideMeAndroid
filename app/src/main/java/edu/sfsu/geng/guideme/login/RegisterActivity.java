package edu.sfsu.geng.guideme.login;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
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

    AppCompatEditText email,username,password;
    AppCompatButton login,register;
    RadioButton viRadioButton;
    String emailText, usernameText, passwordText, roleText;
    List<NameValuePair> params;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        email = (AppCompatEditText)findViewById(R.id.email);
        username = (AppCompatEditText)findViewById(R.id.username);
        password = (AppCompatEditText)findViewById(R.id.password);
        register = (AppCompatButton)findViewById(R.id.registerbtn);
        login = (AppCompatButton)findViewById(R.id.login);
        viRadioButton = (RadioButton)findViewById(R.id.radio_button_vi);
        roleText = "helper";

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent regactivity = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(regactivity);
                finish();
            }
        });


        register.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                emailText = email.getText().toString();
                usernameText = username.getText().toString();
                passwordText = password.getText().toString();
                roleText = viRadioButton.isChecked() ? "vi" : "helper";
                params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("email", emailText));
                params.add(new BasicNameValuePair("username", usernameText));
                params.add(new BasicNameValuePair("password", passwordText));
                params.add(new BasicNameValuePair("role", roleText));
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
    }

}
