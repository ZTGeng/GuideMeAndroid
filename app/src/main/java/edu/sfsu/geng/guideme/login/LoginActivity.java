package edu.sfsu.geng.guideme.login;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
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
import edu.sfsu.geng.guideme.helper.HelperHomeActivity;
import edu.sfsu.geng.guideme.visualimpairer.VIHomeActivity;

public class LoginActivity extends AppCompatActivity {

    AppCompatEditText email, password, res_email, code, newpass;
    AppCompatButton login, cont, cont_code, cancel ,cancel1, register, forpass;
    String emailText, passwordText,email_res_txt,code_txt,npass_txt;
    List<NameValuePair> params;
    SharedPreferences pref;
    Dialog reset;
    ServerRequest sr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sr = new ServerRequest();

        email = (AppCompatEditText)findViewById(R.id.email);
        password = (AppCompatEditText)findViewById(R.id.password);
        login = (AppCompatButton)findViewById(R.id.loginbtn);
        register = (AppCompatButton)findViewById(R.id.register);
        forpass = (AppCompatButton)findViewById(R.id.forgotpass);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent regactivity = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(regactivity);
                finish();
            }
        });


        login.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View view) {
                emailText = email.getText().toString();
                passwordText = password.getText().toString();
                params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("email", emailText));
                params.add(new BasicNameValuePair("password", passwordText));
                ServerRequest sr = new ServerRequest();

                String loginServer = Config.LOGIN_SERVER_ADDRESS + ":" + Config.LOGIN_SERVER_PORT;
                JSONObject json = sr.getJSON(loginServer + "/login",params);
                if(json != null){
                    try{
                        String jsonstr = json.getString("response");
                        if(json.getBoolean("res")){
                            String token = json.getString("token");
                            String grav = json.getString("grav");
                            String username = json.getString("username");
                            String role = json.getString("role");
                            SharedPreferences.Editor edit = pref.edit();
                            //Storing Data using SharedPreferences
                            edit.putString("token", token);
                            edit.putString("grav", grav);
                            edit.putString("username", username);
                            edit.commit();
                            Intent homeActivity;
                            if (role.equals("vi")) {
                                homeActivity = new Intent(LoginActivity.this, VIHomeActivity.class);
                            } else {// if (role.equals("helper")) {
                                homeActivity = new Intent(LoginActivity.this, HelperHomeActivity.class);
                            }
                            startActivity(homeActivity);
                            finish();
                        }

                        Toast.makeText(getApplication(), jsonstr, Toast.LENGTH_LONG).show();

                    }catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        forpass.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                reset = new Dialog(LoginActivity.this);
                reset.setTitle("Reset Password");
                reset.setContentView(R.layout.reset_pass_init);
                cont = (AppCompatButton)reset.findViewById(R.id.resbtn);
                cancel = (AppCompatButton)reset.findViewById(R.id.cancelbtn);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        reset.dismiss();
                    }
                });
                res_email = (AppCompatEditText)reset.findViewById(R.id.email);

                cont.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        email_res_txt = res_email.getText().toString();

                        params = new ArrayList<NameValuePair>();
                        params.add(new BasicNameValuePair("email", email_res_txt));

                        final String loginServer = Config.LOGIN_SERVER_ADDRESS + ":" + Config.LOGIN_SERVER_PORT;
                        JSONObject json = sr.getJSON(loginServer + "/api/resetpass", params);

                        if (json != null) {
                            try {
                                String jsonStr = json.getString("response");
                                if(json.getBoolean("res")){
                                    Log.e("JSON", jsonStr);
                                    Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_LONG).show();
                                    reset.setContentView(R.layout.reset_pass_code);
                                    cont_code = (AppCompatButton)reset.findViewById(R.id.conbtn);
                                    code = (AppCompatEditText)reset.findViewById(R.id.code);
                                    newpass = (AppCompatEditText)reset.findViewById(R.id.npass);
                                    cancel1 = (AppCompatButton)reset.findViewById(R.id.cancel);
                                    cancel1.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            reset.dismiss();
                                        }
                                    });
                                    cont_code.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            code_txt = code.getText().toString();
                                            npass_txt = newpass.getText().toString();
                                            Log.e("Code", code_txt);
                                            Log.e("New pass",npass_txt);
                                            params = new ArrayList<NameValuePair>();
                                            params.add(new BasicNameValuePair("email", email_res_txt));
                                            params.add(new BasicNameValuePair("code", code_txt));
                                            params.add(new BasicNameValuePair("newpass", npass_txt));

                                            JSONObject json = sr.getJSON(loginServer + "/api/resetpass/chg", params);

                                            if (json != null) {
                                                try {

                                                    String jsonstr = json.getString("response");
                                                    if(json.getBoolean("res")){
                                                        reset.dismiss();
                                                        Toast.makeText(getApplication(),jsonstr,Toast.LENGTH_LONG).show();

                                                    }else{
                                                        Toast.makeText(getApplication(),jsonstr,Toast.LENGTH_LONG).show();

                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                        }
                                    });
                                }else{

                                    Toast.makeText(getApplication(),jsonStr,Toast.LENGTH_LONG).show();

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                });


                reset.show();
            }
        });
    }

}
