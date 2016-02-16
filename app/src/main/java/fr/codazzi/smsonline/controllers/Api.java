package fr.codazzi.smsonline.controllers;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import fr.codazzi.smsonline.*;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Api {
    String token = null;
    String user = null;

    int state = 0;
    int error = 0;

    Context context = null;
    Boolean reset_api = false;
    Boolean wifi_only = true;

    Messages messages;


    /* Debug */
    int nb_access = 0;

    public Api(Context _context) {
        this.context = _context;
    }

    public void Sync() {
        ConnectivityManager connManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // @TODO : Maybe change getNetworkInfo for the new version, and fetch all networks
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        Log.i("SYNCHRONIZATION", "Sync Thread: " + nb_access++ + " access." );

        if (this.wifi_only && !mWifi.isConnected() && !this.user.equals("test@example.com")) {
            Log.i("SYNCHRONIZATION", "Wifi only.");
            this.error = 2;
            this.saveState();
            return;
        }

        this.readState();
        if (this.reset_api) {
            this.resetApi();
        } else if (this.token == null || this.user == null) {
            Log.i("API", "GetToken");
            this.getToken();
        } else {
            switch (state) {
                case 1:
                    this.addDevice();
                    break;
                case 2:
                    this.syncContacts();
                    messages = new Messages();
                    break;
                case 3:
                    this.syncMessages();
                    break;
                default:
                    Log.e("STATE ERROR", "Unknow state action ("+state+")");
            }
        }
        this.saveState();
    }

    private void resetApi() {
        this.token = null;
        this.user = null;
        this.state = 0;
        this.error = 0;
        this.reset_api = false;
        Log.e("API", "Reset API");
    }

    public void readState() {
        SharedPreferences settings;

        settings = context.getSharedPreferences("swb_infos", 0);
        this.reset_api = settings.getBoolean("reset_api", false);
        this.wifi_only = settings.getBoolean("wifi_only", true);
    }

    private void saveState() {
        if (this.error != 0) {
            this.token = null;
            this.user = null;
        }
        SharedPreferences settings = this.context.getSharedPreferences("swb_infos", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("error", this.error);
        editor.putInt("state", this.state);
        editor.putBoolean("reset_api", this.reset_api);
        //@TODO add date
        //editor.putString("password", this.password);
        editor.apply();
    }

    private void syncContacts () {
        final Api self = this;
        try {
            if (Tools.checkPermission(context, Manifest.permission.READ_CONTACTS)) {
                JSONArray contacts = Contacts.getAllContacts(context);
                Ion.with(context)
                        .load(context.getString(R.string.api_url)  + "Contacts/Add")
                        .setBodyParameter("user", this.user)
                        .setBodyParameter("token", this.token)
                        .setBodyParameter("contacts", contacts.toString())
                        .asString()
                        .setCallback(new FutureCallback<String>() {
                            @Override
                            public void onCompleted(Exception e, String result) {
                                JSONObject res;
                                try {
                                    if (result != null) {
                                        Log.i("TEST CONTACTS", result);
                                        res = new JSONObject(result);
                                        self.error = res.getInt("error");
                                    } else {
                                        e.printStackTrace();
                                        self.error = 2;
                                    }
                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                    self.error = -1;
                                }
                                self.saveState();
                            }
                        });
            } else {
                Log.i("CONTACTS", "NO permission");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Same if contacts is not synchronized, we go in next step
        this.state++;
        this.saveState();
    }

    private void syncMessages () {
        final Api self = this;

        Log.i("MESSAGES", "Sync Messages");
        if (Tools.checkPermission(context, Manifest.permission.READ_SMS)) {
            JSONArray messageArray = this.messages.getAllMessages();
            Ion.with(context)
                    .load(context.getString(R.string.api_url) + "Messages/Resync")
                    .setBodyParameter("user", this.user)
                    .setBodyParameter("token", this.token)
                    .setBodyParameter("messages", messageArray.toString())
                    .asString()
                    .setCallback(new FutureCallback<String>() {
                        @Override
                        public void onCompleted(Exception e, String result) {
                            JSONObject res;
                            Log.i("TEST MESSAGES", result);
                            try {
                                if (result != null) {
                                    res = new JSONObject(result);
                                    self.error = res.getInt("error");
                                } else {
                                    e.printStackTrace();
                                    self.error = 2;
                                }
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                                self.error = -1;
                            }
                            self.saveState();
                        }
                    });
        } else {
            Log.i("MESSAGES", "No permissions");
        }
    }

    private void addDevice() {
        final Api self = this;
        String android_id = Tools.getDeviceID(this.context);

        Ion.with(context)
            .load(context.getString(R.string.api_url)  + "Devices/Add")
            .setBodyParameter("user", this.user)
            .setBodyParameter("token", this.token)
            .setBodyParameter("android_id", android_id)
            .asString()
            .setCallback(new FutureCallback<String>() {
                @Override
                public void onCompleted(Exception e, String result) {
                    JSONObject res;

                    try {
                        if (result != null) {
                            res = new JSONObject(result);
                            self.error = res.getInt("error");
                            self.state = 2;
                        } else {
                            e.printStackTrace();
                            self.error = 2;
                        }
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                        self.error = -1;
                    }
                    self.saveState();
                }
            });
    }

    private void getToken() {
        String email;
        String password;
        final Api self = this;

        try {
            /* Verify is we have all informations */
            SharedPreferences settings = this.context.getSharedPreferences("swb_infos", 0);
            email = URLEncoder.encode(settings.getString("email", ""), "utf-8");
            password = URLEncoder.encode(settings.getString("password", ""), "utf-8");
            if (email.length() > 4 && password.length() > 4) {
                Ion.with(context)
                    .load(context.getString(R.string.api_url) + "Users/GetToken?email=" + email + "&password=" + password)
                    .asString()
                    .setCallback(new FutureCallback<String>() {
                        @Override
                        public void onCompleted(Exception e, String result) {
                            JSONObject res;
                            int error;
                            try {
                                if (result != null) {
                                    res = new JSONObject(result);
                                    error = res.getInt("error");
                                    if (error == 0) {
                                        self.token = res.getString("token");
                                        self.user = res.getString("user");
                                        self.state = 1;
                                    }
                                    self.error = error;
                                } else {
                                    e.printStackTrace();
                                    self.error = 2;
                                }
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                                self.error = -1;
                            }
                            self.saveState();
                        }
                    });
            } else {
                self.error = 1;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

}
