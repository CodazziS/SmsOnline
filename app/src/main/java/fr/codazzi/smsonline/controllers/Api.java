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
    private String android_id;
    private String api_url = "";
    private Context context;
    private SharedPreferences settings = null;

    /* Settings */
    private boolean reset_api;
    private boolean working;
    private int error;
    private int state;
    private long last_sync;
    private long last_sms;
    private long last_mms;
    private long last_work;
    private String token;
    private String user;
    private String key;
    private String unread_sms = "";


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

        //Log.d("api STATE", "State: " + this.state);
        switch (this.state) {
            case 0:
                this.getVersion();
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
                this.reset_api = true;
                saveSettings();
                //Log.e("STATE ERROR", "Unknown state action ("+state+")");
        }
    }

    private boolean getSettings() {
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

        return true;
    }

    private void saveSettings() {
        if (this.error != 0) {
            this.token = null;
            this.user = null;
            this.key = null;
            this.reset_api = true;
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
                    if (res.has("messages_to_send")) {
                        messages_to_send = res.getJSONArray("messages_to_send");
                        for (int i = 0; i < messages_to_send.length(); ++i) {
                            message_to_send = messages_to_send.getJSONObject(i);
                            String url = api_url + "Messages/ConfirmSent";
                            String smsdata =
                                    "user=" + URLEncoder.encode(user, "utf-8") +
                                    "&token=" + URLEncoder.encode(token, "utf-8") +
                                    "&android_id=" + URLEncoder.encode(android_id, "utf-8") +
                                    "&key=" + URLEncoder.encode(key, "utf-8") +
                                    "&message_id=" + URLEncoder.encode(message_to_send.getString("id"), "utf-8");

                            Ajax.post(url, smsdata, "sendMessage", this);
                            /*
                            message_to_send = messages_to_send.getJSONObject(i);
                            Messages.sendMessage(
                                    message_to_send.getString("address"),
                                    message_to_send.getString("body"),
                                    "sms");
                            this.confirmSent(message_to_send.getString("id"));
                            */
                        }
                    }
                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            this.error = -1;
        }
        this.saveSettings();
    }

    public void sendMessage(String data) {
        JSONObject res;
        try {

            res = new JSONObject(data);
            Log.e("API-SEND", "SEND: " + res.getString("address") + "-" + res.getString("body"));
            Messages.sendMessage(
                    res.getString("address"),
                    res.getString("body"),
                    "sms");
        } catch (JSONException e1) {
            e1.printStackTrace();
            this.error = -1;
        }
    }
    /*
    private void confirmSent(String message_id) {

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
    */

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
        }
        this.saveSettings();
    }

    private void getVersion() {
        try {
            String url = this.api_url + "Index/GetVersion";
            String email = URLEncoder.encode(this.settings.getString("email", ""), "utf-8");
            String password = URLEncoder.encode(this.settings.getString("password", ""), "utf-8");
            if (email.length() <= 4 || password.length() <= 4) {
                this.error = 1;
                this.saveSettings();
                return;
            }
            Ajax.get(url, "", "getVersionRes", this);
        } catch (Exception e) {
            e.printStackTrace();
            this.error = 1;
            this.saveSettings();
        }
    }
    public void getVersionRes(String data) {
        JSONObject res;
        this.error = -1;
        int api_version;

        try {
            if (data != null && !data.equals("")) {
                res = new JSONObject(data);
                this.error = res.getInt("error");
                api_version = res.getInt("api_version");
                if (this.error == 0 && api_version == this.context.getResources().getInteger(R.integer.api_version)) {
                    this.getToken();
                }
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }
}
