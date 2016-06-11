package edu.sfsu.geng.guideme.visualimpairer;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.widget.TextView;
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

public class VIRateActivity extends AppCompatActivity {

    SharedPreferences pref;

    private AppCompatButton submitBtn;
    private List<NameValuePair> params;
    private AppCompatButton decreaseBtn, increaseBtn;
    private TextView rateNumberText;
    private float rateFloat;
    private String helperName, myName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vi_rate);
        helperName = getIntent().getStringExtra("helperName");

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        myName = pref.getString("username", "");

        rateFloat = 5.0f;
        rateNumberText = (TextView) findViewById(R.id.vi_rate_number);

        decreaseBtn = (AppCompatButton) findViewById(R.id.vi_rate_decrease_btn);
        decreaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rateFloat > 0) {
                    rateFloat -= 0.5f;
                    rateNumberText.setText(String.valueOf(rateFloat));
                }
            }
        });

        increaseBtn = (AppCompatButton) findViewById(R.id.vi_rate_increase_btn);
        increaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rateFloat < 5) {
                    rateFloat += 0.5f;
                    rateNumberText.setText(String.valueOf(rateFloat));
                }
            }
        });

        submitBtn = (AppCompatButton) findViewById(R.id.vi_rate_btn);
        assert submitBtn != null;
//        submitBtn.setOnClickListener(this);
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmit(v);
            }
        });

    }

    public void onSubmit(View v) {
//        Toast.makeText(this, String.valueOf(rateFloat), Toast.LENGTH_SHORT).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(String.format(getResources().getString(R.string.vi_rate_message), helperName, String.valueOf(rateFloat)));
        builder.setPositiveButton(R.string.vi_rate_ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("username", helperName));
                params.add(new BasicNameValuePair("rater", myName));
                params.add(new BasicNameValuePair("rate", String.valueOf(rateFloat)));
                ServerRequest sr = new ServerRequest();
                JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/api/rate", params);

                try {
                    Toast.makeText(getApplication(), json.getString("response"), Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Intent homeActivity = new Intent(VIRateActivity.this, VIHomeActivity.class);
                startActivity(homeActivity);
                finish();
            }
        });
        builder.setNegativeButton(R.string.vi_rate_change_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }
}
