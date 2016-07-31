package fr.codazzi.smsonline.controllers;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;

import fr.codazzi.smsonline.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Date;

public class Api {
    /* Vars */
    private String device_id;
    private String api_url = "";
    private Context context;
    private SharedPreferences settings = null;
    private boolean getAllMessages = true;

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
    private String unread_mms = "";
    private JSONArray contacts;
    private int contacts_sync;

    public Api(Context _context, SharedPreferences _settings) {
        this.context = _context;
        this.settings = _settings;
        this.device_id = Tools.getDeviceID(this.context);
    }

    public void startWork() {
        this.last_work = (new Date()).getTime();

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("working", true);
        editor.putLong("last_working", this.last_work);
        editor.apply();
    }

    public void Run() {
        if (!this.getSettings()) {
            return;
        }
        if (this.working) {
            if (this.last_work == 0 || this.last_work < ((new Date()).getTime() - 300000)) {
                Tools.logDebug(2, "Reset work: timeout");
                this.reset_api = true;
                this.saveSettings();
            }
            return;
        }

        this.startWork();
        switch (this.state) {
            case 0:
                this.getVersion();
                break;
            case 1:
                this.addDevice();
                break;
            case 2:
                this.prepareContactsLoop();
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
            this.unread_mms = null;
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
            this.api_url = this.settings.getString("server_uri", null);
            this.unread_sms = this.settings.getString("api_unread_sms", null);
            this.unread_mms = this.settings.getString("api_unread_mms", null);
        }
        return true;
    }

    private void saveSettings() {
        this.saveSettings(false);
    }

    private void saveSettings(boolean working) {
        if (this.error != 0) {
            this.token = null;
            this.user = null;
            this.key = null;
            this.reset_api = true;
        }
        this.working = working;
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putBoolean("reset_api", this.reset_api);
        editor.putBoolean("working", this.working);
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
        editor.putString("api_unread_mms", this.unread_mms);
        editor.apply();
    }

    private void syncMessages(boolean _getAllMessages) {
        this.getAllMessages = _getAllMessages;
        try {
            final Messages messages = new Messages();
            JSONArray messages_arr;
            String url;

            if (Tools.checkPermission(context, Manifest.permission.READ_SMS)) {
                if (this.getAllMessages) {
                    messages_arr = messages.getAllSms(context);
                    url = this.api_url + "Messages/Resync";
                } else {
                    messages_arr = messages.getLastsSms(context, this.last_sms, this.unread_sms);
                    url = this.api_url + "Messages/Sync";
                }

                String data =
                        "user=" + URLEncoder.encode(this.user, "utf-8") +
                                "&token=" + URLEncoder.encode(this.token, "utf-8") +
                                "&device_id=" + URLEncoder.encode(this.device_id, "utf-8") +
                                "&key=" + URLEncoder.encode(this.key, "utf-8") +
                                "&messages=" + URLEncoder.encode(messages_arr.toString(), "utf-8");
                Ajax.post(url, data, "syncMessagesRes", this);

                this.last_sms = messages.lastDateSms;
                this.unread_sms = messages.unreadSmsList;
                this.last_sync = new Date().getTime();
            }
        } catch(Exception e){
            e.printStackTrace();
            this.error = 1;
        }
        this.saveSettings(true);
    }

    public void syncMessagesRes(String data) {
        JSONObject res;
        this.error = -1;
        JSONObject message_to_send;
        JSONArray messages_to_send;

        try {
            if (data != null && !data.equals("")) {
                res = new JSONObject(data);
                this.error = res.getInt("error");
                if (this.error == 0) {
                    //this.state = 4;
                    if (res.has("messages_to_send")) {
                        messages_to_send = res.getJSONArray("messages_to_send");
                        for (int i = 0; i < messages_to_send.length(); ++i) {
                            message_to_send = messages_to_send.getJSONObject(i);
                            String url = api_url + "Messages/ConfirmSent";
                            String smsdata =
                                    "user=" + URLEncoder.encode(user, "utf-8") +
                                    "&token=" + URLEncoder.encode(token, "utf-8") +
                                    "&device_id=" + URLEncoder.encode(device_id, "utf-8") +
                                    "&key=" + URLEncoder.encode(key, "utf-8") +
                                    "&message_id=" + URLEncoder.encode(message_to_send.getString("id"), "utf-8");

                            Ajax.post(url, smsdata, "sendMessage", this);
                        }
                    }
                }
            }
        } catch (Exception e1) {
            Tools.logDebug(4, data);
            e1.printStackTrace();
            this.error = -1;
        }
        this.saveSettings(true);
        this.prepareMmsLoop();
    }

    private JSONArray mms;
    private int mms_sync;

    public void prepareMmsLoop() {
        final Messages messages = new Messages();

        if (Tools.checkPermission(context, Manifest.permission.READ_SMS)) {
            if (getAllMessages) {
                this.mms = messages.getAllMms(context);
            } else {
                this.mms = messages.getLastsMms(context, this.last_mms, this.unread_mms);
            }

            this.mms_sync = 0;
            this.last_mms = messages.lastDateMms;
            this.unread_mms = messages.unreadMmsList;
            this.last_sync = new Date().getTime();
            syncMms();
        }
    }

    public void syncMms() {
        String url = this.api_url + "Messages/Sync";
        this.startWork();
        JSONArray mms_arr = new JSONArray();

        try {
            if (this.mms_sync < this.mms.length()) {
                Tools.logDebug((this.mms_sync + 1) + "/" + this.mms.length() + " mms");
                JSONObject mmsobj = this.mms.getJSONObject(this.mms_sync);
                mmsobj = Messages.getMmsInfos(this.context, mmsobj.getString("id"), mmsobj);
                mms_arr.put(mmsobj);
                String data =
                        "user=" + URLEncoder.encode(this.user, "utf-8") +
                                "&token=" + URLEncoder.encode(this.token, "utf-8") +
                                "&device_id=" + URLEncoder.encode(this.device_id, "utf-8") +
                                "&key=" + URLEncoder.encode(this.key, "utf-8") +
                                "&messages=" + URLEncoder.encode(mms_arr.toString(), "utf-8");
                Ajax.post(url, data, "syncMmsRes", this);
                this.mms_sync++;
            } else {
                Tools.logDebug("Synchronization ended");
                this.state = 4;
                this.error = 0;
                this.saveSettings();
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.error = 1;
            this.saveSettings();
        }
    }

    public void syncMmsRes(String data) {
        JSONObject res;
        this.error = -1;

        try {
            if (data != null && !data.equals("")) {
                res = new JSONObject(data);
                this.error = res.getInt("error");
                if (this.error == 0) {
                    this.syncMms();
                } else {
                    Tools.logDebug(4, data);
                    this.error = -1;
                    this.saveSettings();
                }
            }
        } catch (Exception e1) {
            Tools.logDebug(4, data);
            e1.printStackTrace();
            this.error = -1;
            this.saveSettings();
        }
    }

    public void sendMessage(String data) {
        JSONObject res;
        try {

            res = new JSONObject(data);
            Messages.sendMessage(
                    res.getString("address"),
                    res.getString("body"),
                    "sms");
        } catch (JSONException e1) {
            e1.printStackTrace();
            this.error = -1;
        }
    }

    private void prepareContactsLoop() {
        try {
            this.contacts = Contacts.getAllContacts(context);
            this.contacts_sync = 0;
            this.syncContacts();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void syncContacts() {
        try {
            if (this.contacts_sync < this.contacts.length()) {
                String url = this.api_url + "Contacts/Add";
                String delete = "false";
                this.startWork();
                JSONArray contacts;

                // The first call syn all contacts without image
                if (this.contacts_sync == 0) {
                    delete = "true";
                    contacts = this.contacts.getJSONArray(0);
                    this.contacts = this.contacts.getJSONArray(1);
                } else {
                    contacts = new JSONArray();
                    contacts.put(this.contacts.get(this.contacts_sync - 1));
                }
                Tools.logDebug((this.contacts_sync + 1) + "/" + this.contacts.length() + " contacts");

                String data =
                        "user=" + URLEncoder.encode(this.user, "utf-8") +
                                "&token=" + URLEncoder.encode(this.token, "utf-8") +
                                "&device_id=" + URLEncoder.encode(this.device_id, "utf-8") +
                                "&key=" + URLEncoder.encode(this.key, "utf-8") +
                                "&reset=" + delete +
                                "&contacts=" + URLEncoder.encode(contacts.toString(), "utf-8");
                Ajax.post(url, data, "syncContactsRes", this);
                this.contacts_sync++;
            } else {
                Tools.logDebug("Contacts synchronization ended");
                this.state = 3;
                this.error = 0;
                this.contacts = null;
                this.saveSettings();
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.error = 1;
            this.saveSettings();
        }
    }

    public void syncContactsRes(String data) {
        this.syncContacts();
    }

    private void addDevice() {
        try {
            String url = this.api_url + "Devices/Add";
            String data =
                    "user=" + URLEncoder.encode(this.user, "utf-8") +
                    "&token=" + URLEncoder.encode(this.token, "utf-8") +
                    "&device_id=" + URLEncoder.encode(this.device_id, "utf-8") +
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
            String data = "email=" + email + "&password=" + password + "&type=" + URLEncoder.encode(this.device_id, "utf-8");
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
                } else {
                    this.error = 16;
                    this.saveSettings();
                }
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }
}
