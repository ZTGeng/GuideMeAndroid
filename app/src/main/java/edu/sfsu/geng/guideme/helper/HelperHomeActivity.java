package edu.sfsu.geng.guideme.helper;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.ListViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.RatingBar;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import edu.sfsu.geng.guideme.Config;
import edu.sfsu.geng.guideme.R;
import edu.sfsu.geng.guideme.login.LoginActivity;
import edu.sfsu.geng.guideme.login.ServerRequest;

public class HelperHomeActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener {

    private static final String TAG = "HelperHome";

    SharedPreferences pref;
    String token, grav, usernameStr, oldpassStr, newpassStr, topicStr;
    int rateInt;
    AppCompatButton chgPasswordBtn, chgpassfrBtn, cancelBtn, logoutBtn, getRoomListBtn;
    Dialog dlg;
    AppCompatEditText oldpassEditText, newpassEditText;
    AppCompatSpinner topicSpinner;
    AppCompatTextView usernameText;

    List<NameValuePair> params;
    ListViewCompat roomList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_home);

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
                Intent loginactivity = new Intent(HelperHomeActivity.this, LoginActivity.class);

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
                dlg = new Dialog(HelperHomeActivity.this);
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
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.topics_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        topicSpinner.setAdapter(adapter);

        topicStr = "General";
        topicSpinner.setOnItemSelectedListener(this);

        roomList = (ListViewCompat) findViewById(R.id.room_list);
        final RoomListAdapter roomListAdapter = new RoomListAdapter(this, -1, new ArrayList<JSONObject>());
        roomList.setAdapter(roomListAdapter);

        roomList.setOnItemClickListener(this);

        getRoomListBtn = (AppCompatButton) findViewById(R.id.get_room_list_btn);
        getRoomListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roomListAdapter.clear();

                params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("topic", topicStr));
                ServerRequest sr = new ServerRequest();
                JSONObject json = sr.getJSON(Config.LOGIN_SERVER_ADDRESS + "/api/getroomlist", params);
                if (json != null) {
                    try {
                        String jsonstr = json.getString("response");
//                        Toast.makeText(getApplication(), jsonstr, Toast.LENGTH_SHORT).show();

                        int size = json.getInt("size");
                        if (size == 0) {
                            Toast.makeText(getApplication(), R.string.empty_list, Toast.LENGTH_LONG).show();
                            return;
                        }
                        JSONArray listJson = new JSONArray(json.getString("list"));
                        for (int i = 0; i < size; i++) {
                            roomListAdapter.add(listJson.getJSONObject(i));
                        }
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

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p/>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.v(TAG, "onItemClick");
        final JSONObject room = ((RoomListAdapter) parent.getAdapter()).getItem(position);
        try {
            final String roomId = room.getString("roomId");
            final String username = room.getString("username");
            final String topic = room.getString("topic");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(String.format(getResources().getString(R.string.room_confirm_message), username, topic));
            builder.setPositiveButton(R.string.room_enter_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent helperVideoActivity = new Intent(HelperHomeActivity.this, HelperVideoActivity.class);
                    helperVideoActivity.putExtra("sessionId", roomId);
//                    helperVideoActivity.putExtra("userId", usernameStr);
                    helperVideoActivity.putExtra("isNavigation", topic.equals("Navigation"));
                    startActivity(helperVideoActivity);
                    finish();
                }
            });
            builder.setNegativeButton(R.string.room_cancel_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class RoomListAdapter extends ArrayAdapter<JSONObject> {

        private final Context context;
        private final List<JSONObject> rooms;

        /**
         * Constructor
         *
         * @param context  The current context.
         * @param resource The resource ID for a layout file containing a TextView to use when
         *                 instantiating views.
         * @param objects  The objects to represent in the ListView.
         */
        public RoomListAdapter(Context context, int resource, List<JSONObject> objects) {
            super(context, resource, objects);
            this.context = context;
            this.rooms = objects;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.room_list_item, parent, false);
            AppCompatTextView usernameText = (AppCompatTextView) rowView.findViewById(R.id.room_item_username);
            RatingBar ratingBar = (RatingBar) rowView.findViewById(R.id.room_item_ratingBar);
            AppCompatTextView topicText = (AppCompatTextView) rowView.findViewById(R.id.room_item_topic);

            try {
                JSONObject room = rooms.get(position);
                usernameText.setText(room.getString("username"));
                topicText.setText(room.getString("topic"));
                int rateInt = Integer.parseInt(room.getString("rate"));
                ratingBar.setRating(rateInt);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return rowView;
        }
    }

}
