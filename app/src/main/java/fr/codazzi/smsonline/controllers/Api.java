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

    private int step;
    private int status;

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

    private JSONArray sms;
    private int sms_sync;
    private JSONArray mms;
    private int mms_sync;

    /* Vars used for confirm good network request */
    private long save_last_sms;
    private String save_unread_sms;
    private long save_last_mms;
    private String save_unread_mms;
    private long save_last_sync;

    public Api(Context _context, SharedPreferences _settings) {
        this.context = _context;
        this.settings = _settings;
        this.device_id = Tools.getDeviceID(this.context);
    }

    private void startWork() {
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
            if (this.last_work == 0 || this.last_work < ((new Date()).getTime() - 1000000)) {
                if (this.step == 4) {
                    Tools.storeLog(context, "Timeout - Force continue L77 Step 4");
                    this.saveSettings();
                } else {
                    Tools.storeLog(context, "Timeout - Api.java L80 Step:" + this.step);
                    this.reset_api = true;
                    this.saveSettings();
                }
            }
            return;
        }

        this.startWork();
        switch (this.step) {
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
                this.prepareSmsLoop(true);
                break;
            case 4:
                this.prepareSmsLoop(false);
                break;
            default:
                Tools.storeLog(context, "Error ?! - Api.java L106 - Not normal !");
                this.reset_api = true;
                saveSettings();
        }
    }

    private boolean getSettings() {
        this.reset_api = this.settings.getBoolean("reset_api", false);
        if (this.reset_api) {
            this.reset_api = false;
            this.working = false;
            this.step = 0;
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
            this.step = this.settings.getInt("step", 0);
            this.status = this.settings.getInt("status", 20);
            this.last_sync = this.settings.getLong("last_sync", 0);
            this.last_sms = this.settings.getLong("last_sms", 0);
            this.last_mms = this.settings.getLong("last_mms", 0);
            this.last_work = this.settings.getLong("last_work", 0);
            this.token = this.settings.getString("api_token", null);
            this.user = this.settings.getString("api_user", null);
            this.key = this.settings.getString("api_key", null);
            this.api_url = this.settings.getString("server_uri2", null);
            this.unread_sms = this.settings.getString("api_unread_sms", null);
            this.unread_mms = this.settings.getString("api_unread_mms", null);
        }
        return true;
    }

    private void saveSettings() {
        this.saveSettings(false);
    }

    private boolean check_error(int error) {
        if (error != 0) {
            this.token = null;
            this.user = null;
            this.key = null;
            this.reset_api = true;
            Tools.storeLog(context, "Error : " + String.valueOf(error) + " Step : " + this.step + " - Api.java L157");
            return true;
        }
        return false;
    }

    private void saveSettings(boolean working) {

        this.working = working;
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putBoolean("reset_api", this.reset_api);
        editor.putBoolean("working", this.working);
        editor.putInt("status", this.status);
        editor.putInt("step", this.step);
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

    private void prepareSmsLoop(boolean _getAllMessages) {
        this.getAllMessages = _getAllMessages;
        final Messages messages = new Messages();
        JSONArray messages_arr;
        JSONArray messages_tmp;
        int item = 0;

        try {
            if (Tools.checkPermission(context, Manifest.permission.READ_SMS)) {
                this.sms = new JSONArray();
                if (this.getAllMessages) {
                    messages_arr = messages.getAllSms(context);
                } else {
                    messages_arr = messages.getLastsSms(context, this.last_sms, this.unread_sms);
                }

                messages_tmp = new JSONArray();
                for (int i = 0; i < messages_arr.length(); i++) {
                    if (item >= 300) {
                        this.sms.put(messages_tmp);
                        messages_tmp = new JSONArray();
                        item = 0;
                    }
                    messages_tmp.put(messages_arr.getJSONObject(i));
                    item++;
                }
                this.sms.put(messages_tmp);
                this.sms_sync = 0;
                this.save_last_sms = messages.lastDateSms;
                this.save_unread_sms = messages.unreadSmsList;
                this.save_last_sync = new Date().getTime();
                syncSms();
            }
        } catch (Exception e) {
            Tools.storeLog(context, "Sync SMS Error - L217");
            Tools.storeLog(context, e.getMessage());
            Tools.storeLog(context, "Unexpected Exception due at line" + e.getStackTrace()[0].getLineNumber());
            this.check_error(1);
            this.saveSettings();
        }
    }

    private void syncSms() {
        String url;
        this.startWork();

        if (this.getAllMessages && this.sms_sync == 0) {
            url = this.api_url + "Messages/Resync";
        } else {
            url = this.api_url + "Messages/Sync";
        }

        try {
            if (this.sms_sync < this.sms.length()) {
                Tools.logDebug((this.sms_sync + 1) + "/" + this.sms.length() + " sms");
                JSONArray sms_arr = this.sms.getJSONArray(this.sms_sync);
                String data =
                    "user=" + URLEncoder.encode(this.user, "utf-8") +
                            "&token=" + URLEncoder.encode(this.token, "utf-8") +
                            "&device_id=" + URLEncoder.encode(this.device_id, "utf-8") +
                            "&key=" + URLEncoder.encode(this.key, "utf-8") +
                            "&messages=" + URLEncoder.encode(sms_arr.toString(), "utf-8");
                Ajax.post(url, data, "syncSmsRes", this);
                this.sms_sync++;
            } else {
                this.last_sms = this.save_last_sms;
                this.unread_sms = this.save_unread_sms;

                this.status = 25;
                this.saveSettings(true);
                this.prepareMmsLoop();
            }
        } catch (Exception e) {
            Tools.storeLog(context, "Sync SMS Error - L256");
            Tools.storeLog(context, e.getMessage());
            Tools.storeLog(context, "Unexpected Exception due at line" + e.getStackTrace()[0].getLineNumber());
            this.check_error(1);
            this.saveSettings();
        }
    }

    public void syncSmsRes(String data) {
        JSONObject res;
        JSONObject message_to_send;
        JSONArray messages_to_send;
        int error;

        try {
            if (data != null && !data.equals("")) {
                res = new JSONObject(data);
                error = res.getInt("error");
                if (error == 0) {
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
                    this.syncSms();
                } else {
                    Tools.logDebug(4, data);
                    this.check_error(error);
                    this.saveSettings();
                }
            } else {
                Tools.storeLog(context, "Network timeout without reseting api - SMS");
                this.saveSettings();
            }
        } catch (Exception e) {
            Tools.storeLog(context, "Sync SMSRes Error - L301");
            Tools.storeLog(context, e.getMessage());
            Tools.storeLog(context, "Unexpected Exception due at line" + e.getStackTrace()[0].getLineNumber());
            this.check_error(-1);
            this.saveSettings();
        }
    }

    private void prepareMmsLoop() {
        final Messages messages = new Messages();

        if (Tools.checkPermission(context, Manifest.permission.READ_SMS)) {
            if (this.settings.getBoolean("sync_mms", true)) {
                if (getAllMessages) {
                    this.mms = messages.getAllMms(context);
                } else {
                    this.mms = messages.getLastsMms(context, this.last_mms, this.unread_mms);
                }
            } else {
                Tools.logDebug(3, "no sync MMS");
                this.mms = new JSONArray();
            }

            this.mms_sync = 0;
            this.save_last_mms = messages.lastDateMms;
            this.save_unread_mms = messages.unreadMmsList;
            syncMms();
        }
    }

    private void syncMms() {
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
                this.last_mms = this.save_last_mms;
                this.unread_mms = this.save_unread_mms;
                this.last_sync = this.save_last_sync;
                Tools.logDebug("Synchronization ended");
                this.step = 4;
                this.status = 26;
                this.saveSettings();
            }
        } catch (Exception e) {
            Tools.storeLog(context, "syncMms Error - L360");
            Tools.storeLog(context, e.getMessage());
            Tools.storeLog(context, "Unexpected Exception due at line" + e.getStackTrace()[0].getLineNumber());
            this.check_error(1);
            this.saveSettings();
        }
    }

    public void syncMmsRes(String data) {
        JSONObject res;
        int error;

        try {
            if (data != null && !data.equals("")) {
                res = new JSONObject(data);
                error = res.getInt("error");
                if (error == 0) {
                    this.syncMms();
                } else {
                    Tools.logDebug(4, data);
                    this.check_error(-1);
                    this.saveSettings();
                }
            } else {
                Tools.storeLog(context, "Network timeout without reseting api - MMS");
                this.saveSettings();
            }
        } catch (Exception e) {
            Tools.storeLog(context, "syncMmsRes - L388");
            Tools.storeLog(context, e.getMessage());
            Tools.storeLog(context, "Unexpected Exception due at line" + e.getStackTrace()[0].getLineNumber());
            this.check_error(-1);
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
            this.check_error(-1);
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
            if (this.contacts_sync - 1 < this.contacts.length()) {
                String url = this.api_url + "Contacts/Add";
                String delete = "false";
                this.startWork();
                JSONArray contacts;

                // The first call sync all contacts without image
                if (this.contacts_sync == 0) {
                    delete = "true";
                    contacts = this.contacts.getJSONArray(0);
                    this.contacts = this.contacts.getJSONArray(1);
                } else {
                    contacts = new JSONArray();
                    contacts.put(this.contacts.get(this.contacts_sync - 1));
                }
                Tools.logDebug((this.contacts_sync + 1) + "/" + (this.contacts.length() + 1) + " contacts");

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
                this.step = 3;
                this.status = 24;
                this.contacts = null;
                this.saveSettings();
            }
        } catch (Exception e) {
            Tools.storeLog(context, "syncContacts - L456");
            Tools.storeLog(context, e.getMessage());
            Tools.storeLog(context, "Unexpected Exception due at line" + e.getStackTrace()[0].getLineNumber());
            this.check_error(1);
            this.saveSettings();
        }
    }

    public void syncContactsRes() {
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
            Tools.storeLog(context, "addDevice - L479");
            Tools.storeLog(context, e.getMessage());
            Tools.storeLog(context, "Unexpected Exception due at line" + e.getStackTrace()[0].getLineNumber());
            this.check_error(1);
            this.saveSettings();
        }
    }

    public void addDeviceRes(String data) {
        JSONObject res;
        int error = -1;

        try {
            if (data != null && !data.equals("")) {
                res = new JSONObject(data);
                error = res.getInt("error");
                if (error == 0) {
                    this.step = 2;
                    this.status = 23;
                }
            } else {
                // Prevent timeout
                error = 0;
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
        this.check_error(error);
        this.saveSettings();
    }

    private void getToken() {
        try {
            String url = this.api_url + "Users/GetToken";
            String email = URLEncoder.encode(this.settings.getString("email", ""), "utf-8");
            String password = URLEncoder.encode(this.settings.getString("password", ""), "utf-8");
            if (email.length() <= 4 || password.length() <= 4) {
                this.check_error(1);
                this.saveSettings();
                return;
            }
            String data = "email=" + email + "&password=" + password + "&type=" + URLEncoder.encode(this.device_id, "utf-8");
            Ajax.get(url, data, "getTokenRes", this);
        } catch (Exception e) {
            Tools.storeLog(context, "getToken - L523");
            Tools.storeLog(context, e.getMessage());
            Tools.storeLog(context, "Unexpected Exception due at line" + e.getStackTrace()[0].getLineNumber());
            this.check_error(1);
            this.saveSettings();
        }
    }
    public void getTokenRes(String data) {
        JSONObject res;
        int error = -1;

        try {
            if (data != null && !data.equals("")) {
                res = new JSONObject(data);
                error = res.getInt("error");
                if (error == 0) {
                    this.token = res.getString("token");
                    this.key = res.getString("key");
                    this.user = res.getString("user");
                    this.step = 1;
                    this.status = 22;
                }
            }
        } catch (JSONException e) {
            Tools.storeLog(context, "GetTokenRes - L547");
            Tools.storeLog(context, e.getMessage());
            Tools.storeLog(context, "Unexpected Exception due at line" + e.getStackTrace()[0].getLineNumber());
        }
        this.check_error(error);
        this.saveSettings();
    }

    private void getVersion() {
        try {
            String url = this.api_url + "Index/GetVersion";
            String email = URLEncoder.encode(this.settings.getString("email", ""), "utf-8");
            String password = URLEncoder.encode(this.settings.getString("password", ""), "utf-8");
            if (email.length() <= 4 || password.length() <= 4) {
                this.check_error(1);
                this.saveSettings();
                return;
            }
            Ajax.get(url, "", "getVersionRes", this);
        } catch (Exception e) {
            Tools.storeLog(context, "L567: GetVersion");
            Tools.storeLog(context, e.getMessage());
            Tools.storeLog(context, "Unexpected Exception due at line" + e.getStackTrace()[0].getLineNumber());
            this.check_error(1);
            this.saveSettings();
        }
    }
    public void getVersionRes(String data) {
        JSONObject res;
        int api_version;
        int error;

        try {
            if (data != null && !data.equals("")) {
                res = new JSONObject(data);
                error = res.getInt("error");
                api_version = res.getInt("api_version");
                if (error == 0 && api_version == this.context.getResources().getInteger(R.integer.api_version)) {
                    this.status = 21;
                    this.saveSettings(true);
                    this.getToken();
                } else {
                    this.check_error(16);
                    this.saveSettings();
                }
            } else {
                this.saveSettings();
            }
        } catch (JSONException e) {
            Tools.storeLog(context, "GetVersionRes - L596");
            Tools.storeLog(context, e.getMessage());
            Tools.storeLog(context, "Unexpected Exception due at line" + e.getStackTrace()[0].getLineNumber());
        }
    }
}
