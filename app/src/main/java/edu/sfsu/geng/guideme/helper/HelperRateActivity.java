package edu.sfsu.geng.guideme.helper;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.widget.RatingBar;
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

public class HelperRateActivity extends AppCompatActivity {

    SharedPreferences pref;

    private AppCompatButton submitBtn, cancelBtn;
    private List<NameValuePair> params;
    private RatingBar ratingBar;
    private float rateFloat;
    private String viName, myName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_rate);
        viName = getIntent().getStringExtra("viName");

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        myName = pref.getString("username", "");

        ratingBar = (RatingBar) findViewById(R.id.helper_ratingBar);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                rateFloat = rating;
            }
        });
        ratingBar.setRating(5);

        submitBtn = (AppCompatButton) findViewById(R.id.helper_rate_btn);
        assert submitBtn != null;
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmit(v);
            }
        });

        cancelBtn = (AppCompatButton) findViewById(R.id.helper_rate_cancel_btn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel(v);
            }
        });

    }

    public void onSubmit(View v) {
//        Toast.makeText(this, String.valueOf(rateFloat), Toast.LENGTH_SHORT).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(String.format(getResources().getString(R.string.helper_rate_message), viName, String.valueOf(rateFloat)));
        builder.setPositiveButton(R.string.vi_rate_ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("username", viName));
                params.add(new BasicNameValuePair("rater", myName));
                params.add(new BasicNameValuePair("rate", String.valueOf(rateFloat)));
                ServerRequest sr = new ServerRequest();
                JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/api/rate", params);

                try {
                    Toast.makeText(getApplication(), json.getString("response"), Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Intent homeActivity = new Intent(HelperRateActivity.this, HelperHomeActivity.class);
                startActivity(homeActivity);
                finish();
            }
        });
        builder.setNegativeButton(R.string.helper_rate_change_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    public void onCancel(View v) {
        Intent homeActivity = new Intent(HelperRateActivity.this, HelperHomeActivity.class);
        startActivity(homeActivity);
        finish();
    }

}
