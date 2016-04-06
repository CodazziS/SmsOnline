package fr.codazzi.smsonline.controllers;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import fr.codazzi.smsonline.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Date;

public class Api {
    /* Vars */
    String android_id;
    String api_url = "";
    Context context;
    SharedPreferences settings = null;

    /* Settings */
    boolean reset_api;
    boolean working;
    int error;
    int state;
    long last_sync;
    long last_sms;
    long last_mms;
    long last_work;
    String token;
    String user;
    String key;
    String unread_sms = "";


    public Api(Context _context, SharedPreferences _settings) {
        this.context = _context;
        this.settings = _settings;

        if (BuildConfig.DEBUG) {
            this.api_url = context.getString(R.string.api_url_debug);
        } else {
            this.api_url = context.getString(R.string.api_url);
        }
        this.android_id = Tools.getDeviceID(this.context);

    }

    public void Run() {
        if (!this.getSettings()) {
            return;
        }
        if (this.working) {
            if (this.last_work == 0 || this.last_work > ((new Date()).getTime() - 300000)) {
                this.reset_api = true;
                this.saveSettings();
            }
            return;
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("working", true);
        editor.putLong("last_working", (new Date()).getTime());
        editor.apply();

        Log.d("api STATE", "State: " + this.state);
        switch (this.state) {
            case 0:
                this.getToken();
                break;
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

    private boolean getSettings() {
        Log.d("API", "GetSettings");
        this.reset_api = this.settings.getBoolean("reset_api", false);
        if (this.reset_api) {
            this.reset_api = false;
            this.working = false;
            this.error = 0;
            this.state = 0;
            this.last_sync = 0;
            this.last_sms = 0;
            this.last_mms = 0;
            this.last_work = 0;
            this.token = null;
            this.user = null;
            this.key = null;
            this.unread_sms = null;
            this.saveSettings();
            Log.d("API", "Error -> API reseted");
            return false;
        } else {
            this.working = this.settings.getBoolean("working", false);
            this.error = this.settings.getInt("error", 0);
            this.state = this.settings.getInt("api_state", 0);
            this.last_sync = this.settings.getLong("last_sync", 0);
            this.last_sms = this.settings.getLong("last_sms", 0);
            this.last_mms = this.settings.getLong("last_mms", 0);
            this.last_work = this.settings.getLong("last_work", 0);
            this.token = this.settings.getString("api_token", null);
            this.user = this.settings.getString("api_user", null);
            this.key = this.settings.getString("api_key", null);
            this.unread_sms = this.settings.getString("api_unread_sms", null);
        }
        if (this.user != null) {
            Log.d("API_USER", this.user);
        } else {
            Log.d("API_USER", "User is null");
        }
        return true;
    }

    private void saveSettings() {
        if (this.error != 0) {
            this.token = null;
            this.user = null;
            this.key = null;
            this.reset_api = true;
            Log.d("API", "Error -> Need to reset the API");
        }
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putBoolean("reset_api", this.reset_api);
        editor.putBoolean("working", false);
        editor.putInt("error", this.error);
        editor.putInt("api_state", this.state);
        editor.putLong("last_sync", this.last_sync);
        editor.putLong("last_sms", this.last_sms);
        editor.putLong("last_mms", this.last_mms);
        editor.putLong("last_work", this.last_work);
        editor.putString("api_token", this.token);
        editor.putString("api_user", this.user);
        editor.putString("api_key", this.key);
        editor.putString("api_unread_sms", this.unread_sms);
        editor.apply();
    }

    private void syncMessages(boolean getAllMessages) {
        try {
            final Messages messages = new Messages();
            JSONArray messages_arr;
            String url;
            JSONObject result_obj;

            if (Tools.checkPermission(context, Manifest.permission.READ_SMS)) {
                if (getAllMessages) {
                    messages_arr = messages.getAllMessages(context);
                    url = this.api_url + "Messages/Resync";
                } else {
                    messages_arr = messages.getLastsMessages(context, this.last_sms, this.last_sms, this.unread_sms);
                    url = this.api_url + "Messages/Sync";
                }
                String data =
                        "user=" + URLEncoder.encode(this.user, "utf-8") +
                                "&token=" + URLEncoder.encode(this.token, "utf-8") +
                                "&android_id=" + URLEncoder.encode(this.android_id, "utf-8") +
                                "&key=" + URLEncoder.encode(this.key, "utf-8") +
                                "&messages=" + URLEncoder.encode(messages_arr.toString(), "utf-8");
                Ajax.post(url, data, "syncMessagesRes", this);

                result_obj = messages.confirmDates();
                this.last_sms = result_obj.getLong("lastDateSms");
                this.last_mms = result_obj.getLong("lastDateMms");
                this.unread_sms = result_obj.getString("unreadSmsList");
                this.last_sync = new Date().getTime();
            }
        } catch(Exception e){
            e.printStackTrace();
            this.error = 1;
        } finally {
            this.saveSettings();
        }
    }

    public void syncMessagesRes(String data) {
        /*
        SharedPreferences settings = (SharedPreferences) args[0];
        SharedPreferences.Editor editor = settings.edit();
        */
        JSONObject res;
        this.error = -1;
        JSONObject message_to_send;
        JSONArray messages_to_send;

        try {
            if (data != null && !data.equals("")) {
                res = new JSONObject(data);
                error = res.getInt("error");
                if (error == 0) {
                    this.state = 4;
                    //editor.putInt("api_state", 4);
                    if (res.has("messages_to_send")) {
                        messages_to_send = res.getJSONArray("messages_to_send");
                        for (int i = 0; i < messages_to_send.length(); ++i) {
                            message_to_send = messages_to_send.getJSONObject(i);
                            Messages.sendMessage(
                                    message_to_send.getString("address"),
                                    message_to_send.getString("body"),
                                    "sms");
                            this.confirmSent(message_to_send.getString("id"));
                            Log.d("API SWB", messages_to_send.get(i).toString());
                        }
                    }
                }
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
            Log.d("JSON", data);
            this.error = -1;
        }
        this.saveSettings();
        /*
        if (error != 0) {
            editor.putBoolean("reset_api", true);
        }
        editor.putBoolean("working", false);
        editor.apply();
        */
    }

    private void confirmSent(String message_id) {
        /*
        String api_url = (String) args[1];
        String user = (String) args[2];
        String token = (String) args[3];
        String android_id = (String) args[4];
        String key = (String) args[5];
        */

        try {
            String url = api_url + "Messages/ConfirmSent";
            String data =
                    "user=" + URLEncoder.encode(user, "utf-8") +
                            "&token=" + URLEncoder.encode(token, "utf-8") +
                            "&android_id=" + URLEncoder.encode(android_id, "utf-8") +
                            "&key=" + URLEncoder.encode(key, "utf-8") +
                            "&message_id=" + URLEncoder.encode(message_id, "utf-8");

            Ajax.post(url, data, null, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void syncContacts() {
        try {
            JSONArray contacts = Contacts.getAllContacts(context);
            String url = this.api_url + "Contacts/Add";
            String data =
                    "user=" + URLEncoder.encode(this.user, "utf-8") +
                            "&token=" + URLEncoder.encode(this.token, "utf-8") +
                            "&android_id=" + URLEncoder.encode(this.android_id, "utf-8") +
                            "&key=" + URLEncoder.encode(this.key, "utf-8") +
                            "&contacts=" + URLEncoder.encode(contacts.toString(), "utf-8");

            Ajax.post(url, data, "syncContactsRes", this);
        } catch (Exception e) {
            e.printStackTrace();
            this.error = 1;
            this.saveSettings();
        }
    }

    public void syncContactsRes(String data) {
        /*SharedPreferences settings = (SharedPreferences) args[0];
        SharedPreferences.Editor editor = settings.edit();*/
        JSONObject res;
        this.error = -1;
        try {
            if (data != null && !data.equals("")) {
                res = new JSONObject(data);
                this.error = res.getInt("error");
                this.state = 3;
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
            Log.d("JSON", data);
            error = -1;
        }
        this.saveSettings();
    }

    private void addDevice() {
        try {
            String url = this.api_url + "Devices/Add";
            String data =
                    "user=" + URLEncoder.encode(this.user, "utf-8") +
                    "&token=" + URLEncoder.encode(this.token, "utf-8") +
                    "&android_id=" + URLEncoder.encode(this.android_id, "utf-8") +
                    "&model=" + URLEncoder.encode(android.os.Build.MODEL, "utf-8");

            Ajax.post(url, data, "addDeviceRes", this);
        } catch (Exception e) {
            e.printStackTrace();
            this.error = 1;
            this.saveSettings();
        }
    }

    public void addDeviceRes(String data) {
        //SharedPreferences settings = (SharedPreferences) args[0];
        //SharedPreferences.Editor editor = settings.edit();
        JSONObject res;
        this.error = -1;

        try {
            if (data != null && !data.equals("")) {
                res = new JSONObject(data);
                this.error = res.getInt("error");
                this.state = 2;
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
            Log.d("JSON", data);
            this.error = -1;
        }
        this.saveSettings();
    }

    private void getToken() {
        try {
            String url = this.api_url + "Users/GetToken";
            String email = URLEncoder.encode(this.settings.getString("email", ""), "utf-8");
            String password = URLEncoder.encode(this.settings.getString("password", ""), "utf-8");
            if (email.length() <= 4 || password.length() <= 4) {
                this.error = 1;
                this.saveSettings();
                return;
            }
            String data = "email=" + email + "&password=" + password + "&type=" + URLEncoder.encode(this.android_id, "utf-8");
            Ajax.get(url, data, "getTokenRes", this);
        } catch (Exception e) {
            e.printStackTrace();
            this.error = 1;
            this.saveSettings();
        }
    }

    public void getTokenRes(String data) {
        JSONObject res;
        this.error = -1;

        try {
            if (data != null && !data.equals("")) {
                res = new JSONObject(data);
                this.error = res.getInt("error");
                if (this.error == 0) {
                    this.token = res.getString("token");
                    this.key = res.getString("key");
                    this.user = res.getString("user");
                    this.state = 1;
                }
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
            Log.d("JSON", data);
        }
        this.saveSettings();
    }

    /*
    static public void getTokenRes(String data, Object... args) {
        SharedPreferences settings = (SharedPreferences) args[0];
        SharedPreferences.Editor editor = settings.edit();
        JSONObject res;
        int error = -1;

        try {
            if (data != null && !data.equals("")) {
                res = new JSONObject(data);

                error = res.getInt("error");
                if (error == 0) {
                    editor.putString("api_token", res.getString("token"));
                    editor.putString("api_key", res.getString("key"));
                    editor.putString("api_user", res.getString("user"));
                    editor.putInt("api_state", 1);
                }
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
            Log.d("JSON", data);
            error = -1;
        }
        if (error != 0) {
            editor.putBoolean("reset_api", true);
        }
        editor.putBoolean("working", false);
        editor.apply();
    }*/
}
