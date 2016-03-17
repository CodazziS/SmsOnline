package fr.codazzi.smsonline.controllers;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import fr.codazzi.smsonline.*;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

public class Api {
    Context context;
    String android_id;

    /* settings */
    int error = 0;
    String token;
    String user;
    String key;
    int state;
    long last_sync;
    long last_sms;
    long last_mms;
    String unread_sms = "";
    boolean reset_api = false;
    SharedPreferences settings = null;

    public Api(Context _context) {
        this.context = _context;
        this.android_id = Tools.getDeviceID(this.context);
    }

    private void saveSettings() {
        if (this.error != 0) {
            this.token = null;
            this.user = null;
            this.key = null;
        }
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("error", this.error);
        editor.putInt("state", this.state);
        editor.putLong("last_sync", this.last_sync);
        editor.putLong("last_sms", this.last_sms);
        editor.putLong("last_mms", this.last_mms);
        editor.putString("api_token", this.token);
        editor.putString("api_key", this.key);
        editor.putString("api_user", this.user);
        editor.putString("api_unread_sms", this.unread_sms);
        editor.putBoolean("working", false);
        if (this.reset_api) {
            editor.putBoolean("reset_api", true);
        }
        editor.apply();
    }

    public void Run(SharedPreferences _settings) {
        this.settings = _settings;
        if (settings.getBoolean("working", false)) {
            return;
        }

        this.state = settings.getInt("state", 0);
        this.token = settings.getString("api_token", null);
        this.key = settings.getString("api_key", null);
        this.user = settings.getString("api_user", null);
        this.unread_sms = settings.getString("api_unread_sms", null);
        this.last_sync = settings.getLong("last_sync", 0);

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("working", true);
        editor.apply();

        Log.d("api STATE", "State: " + state);
        if (this.token == null || this.user == null) {
            this.getToken(settings.getString("email", ""), settings.getString("password", ""));
        } else {
            switch (settings.getInt("state", -1)) {
                case 1:
                    this.addDevice();
                    break;
                case 2:
                    this.syncContacts();
                    break;
                case 3:
                    this.syncMessages(true);
                    break;
                case 4:
                    this.syncMessages(false);
                    break;
                default:
                    Log.e("STATE ERROR", "Unknown state action ("+state+")");
            }
        }
        //this.saveSettings();
    }

    private void syncContacts () {
        final Api self = this;
        try {
            if (Tools.checkPermission(context, Manifest.permission.READ_CONTACTS)) {
                JSONArray contacts = Contacts.getAllContacts(context);
                Ion.with(context)
                        .load(context.getString(R.string.api_url) + "Contacts/Add")
                        .setBodyParameter("user", this.user)
                        .setBodyParameter("token", this.token)
                        .setBodyParameter("key", this.key)
                        .setBodyParameter("android_id", this.android_id)
                        .setBodyParameter("contacts", contacts.toString())
                        .asString()
                        .setCallback(new FutureCallback<String>() {
                            @Override
                            public void onCompleted(Exception e, String result) {
                                JSONObject res;
                                try {
                                    if (result != null) {
                                        res = new JSONObject(result);
                                        self.error = res.getInt("error");
                                    } else {
                                        self.error = 2;
                                    }
                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                    Log.d("JSON", result);
                                    self.error = -1;
                                }
                                self.saveSettings();
                            }
                        });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Same if contacts is not synchronized, we go in next step
        this.state++;
        this.saveSettings();
    }

    private void syncMessages (Boolean getAllMessages) {
        final Api self = this;
        final Messages messages = new Messages();
        JSONArray messages_arr;

        if (Tools.checkPermission(context, Manifest.permission.READ_SMS)) {
            String url;
            if (getAllMessages) {
                messages_arr = messages.getAllMessages(context);
                url = "Messages/Resync";
            } else {
                messages_arr = messages.getLastsMessages(context, this.last_sms, this.last_sms, this.unread_sms);
                url = "Messages/Sync";
            }
            Ion.with(context)
                    .load(context.getString(R.string.api_url) + url)
                    .setBodyParameter("user", this.user)
                    .setBodyParameter("token", this.token)
                    .setBodyParameter("android_id", this.android_id)
                    .setBodyParameter("key", this.key)
                    .setBodyParameter("messages", messages_arr.toString())
                    .asString()
                    .setCallback(new FutureCallback<String>() {
                        @Override
                        public void onCompleted(Exception e, String result) {
                            JSONObject res;
                            JSONObject result_obj;
                            self.state = 4;
                            try {
                                if (result != null) {
                                    res = new JSONObject(result);
                                    self.error = res.getInt("error");
                                    if (self.error == 0) {
                                        result_obj = messages.confirmDates();
                                        self.last_sms = result_obj.getLong("lastDateSms");
                                        self.last_mms = result_obj.getLong("lastDateMms");
                                        self.unread_sms = result_obj.getString("unreadSmsList");
                                        self.last_sync = new Date().getTime();
                                    } else {
                                        self.reset_api = true;
                                    }
                                } else {
                                    self.error = 2;
                                }
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                                Log.d("JSON", result);
                                self.error = -1;
                            }
                            self.saveSettings();
                        }
                    });
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
            .setBodyParameter("model", android.os.Build.MODEL)
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
                            self.error = 2;
                        }
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                        Log.d("JSON", result);
                        self.error = -1;
                    }
                    self.saveSettings();
                }
            });
    }

    private void getToken(String email, String password) {
        final Api self = this;

        try {
            email = URLEncoder.encode(email, "utf-8");
            password = URLEncoder.encode(password, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            self.error = 1;
            return;
        }

        if (email.length() <= 4 || password.length() <= 4) {
            self.error = 1;
            return;
        }

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
                                self.key = res.getString("key");
                                self.state = 1;
                            } else {
                                self.reset_api = true;
                            }
                            self.error = error;
                        } else {
                            e.printStackTrace();
                            self.error = 2;
                        }
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                        Log.d("JSON", result);
                        self.error = -1;
                    }
                    self.saveSettings();
                }
            });
    }

}
