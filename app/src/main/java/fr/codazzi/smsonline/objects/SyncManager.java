package fr.codazzi.smsonline.objects;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import fr.codazzi.smsonline.Tools;

public class SyncManager {

    private Context context;
    private SharedPreferences settings;
    private String api_token;
    private String api_key;
    private int api_user;
    private String api_url;
    private int api_revision;

    public SyncManager(Context _context, SharedPreferences _settings) {
        this.context = _context;
        this.settings = _settings;
        this.initManager();
    }

    private void initManager() {
        this.api_token = this.settings.getString("api_token", null);
        this.api_key = this.settings.getString("api_key", null);
        this.api_user = this.settings.getInt("api_user", 0);
        this.api_url = "https://dev.smsonline.fr/api/";
    }

    private void startWork() {
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putBoolean("SyncManagerWorking", true);
        editor.apply();
    }

    private void stopWork() { this.stopWork(false);}
    private void stopWork(boolean error) {
        Log.d("Sync", "Stop work");
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putBoolean("SyncManagerWorking", false);
        if (!error) {
            editor.putString("api_token", this.api_token);
            editor.putString("api_key", this.api_key);
            editor.putInt("api_user", this.api_user);
        } else {
            editor.putString("api_token", null);
            editor.putString("api_key", null);
            editor.putInt("api_user", 0);
        }
        editor.apply();
    }

    public void startSynchronization() {
        if (this.settings.getBoolean("SyncManagerWorking", false)) {
            Tools.storeLog(this.context, "SyncManager Is already working ...");
            // @TODO RETURN
        }
        this.startWork();
        if (this.api_token == null) {
            this.getToken();
        } else {
            this.getLastRevision();
        }
    }

    private void getToken() {
        String result_str;
        JSONObject result;
        int error;
        Log.d("SYNC", "getToken");

        try {
            ExecutorService executor = Executors.newFixedThreadPool(1);
            String url = this.api_url + "Users/GetToken2";
            String email = "test@smsonline.fr";
            String password = "azerty";
            String data = "email=" + URLEncoder.encode(email, "utf-8") +
                    "&password=" + URLEncoder.encode(password, "utf-8") +
                    "&type=android" +
                    "&device_model=" + URLEncoder.encode(android.os.Build.MODEL, "utf-8") +
                    "&device_id=" + URLEncoder.encode(Tools.getDeviceID(this.context), "utf-8");

            Callable<String> worker = new Api("GET", url, data);
            Future<String> future = executor.submit(worker);
            result_str = future.get();
            result = new JSONObject(result_str);
            error = result.getInt("error");
            if (error == 0) {
                this.api_token = result.getString("token");
                this.api_key = result.getString("key");
                this.api_user = result.getInt("user");
                this.api_revision = result.getInt("revision");
                this.sendRevision();
            } else {
                Tools.storeLog(this.context, "GetToken Error : E" + error);
                this.stopWork(true);
            }
        } catch (Exception e) {
            Tools.storeLog(this.context, e.getMessage());
            this.stopWork(true);
        }
    }

    private void getLastRevision() {
        String result_str;
        JSONObject result;
        int error;
        Log.d("SYNC", "getLastRevision");

        try {
            ExecutorService executor = Executors.newFixedThreadPool(1);
            String url = this.api_url + "Users/getRevisionId";
            String data = "user=" + this.api_user +
                    "&token=" + URLEncoder.encode(this.api_token, "utf-8") +
                    "&key=" + URLEncoder.encode(this.api_key, "utf-8") +
                    "&device_model=" + URLEncoder.encode(android.os.Build.MODEL, "utf-8") +
                    "&device_id=" + URLEncoder.encode(Tools.getDeviceID(this.context), "utf-8");

            Callable<String> worker = new Api("GET", url, data);
            Future<String> future = executor.submit(worker);
            result_str = future.get();
            result = new JSONObject(result_str);
            error = result.getInt("error");
            if (error == 0) {
                this.api_revision = result.getInt("revision");
                this.sendRevision();
            } else {
                Tools.storeLog(this.context, "GetToken Error : E" + error);
                this.stopWork(true);
            }
        } catch (Exception e) {
            Tools.storeLog(this.context, e.getMessage());
            this.stopWork(true);
        }
    }

    private void sendRevision() {
        RevisionsManager revman = new RevisionsManager(this.context, this.settings);
        int max_revision = revman.countRevisions();
        JSONObject revision;
        JSONArray sms_ids_array;
        JSONArray mms_ids_array;
        JSONArray contacts_ids_array;
        Log.d("Sync", "SendRevision");

        try {
            if (max_revision == this.api_revision) {
                this.getActionsQueue();
            } else if (max_revision < this.api_revision) {
                Tools.storeLog(this.context, "Remote revision is higher than device revision");
                stopWork(true);
            } else {
                revision = revman.getRevision(this.api_revision + 1);
                sms_ids_array = this.splitter(revision.getJSONArray("new_sms_ids"));
                mms_ids_array = revision.getJSONArray("new_mms_ids");
                contacts_ids_array = revision.getJSONArray("new_contacts_ids");

                /* CONTACTS */
                if (!this.syncContacts(contacts_ids_array)) {
                    stopWork(true);
                }

                /* SMS */
                if (!this.syncSms(sms_ids_array)) {
                    stopWork(true);
                }

                /* MMS */
                if (!this.syncMms(mms_ids_array)) {
                    stopWork(true);
                }

                this.validRevision(this.api_revision + 1);
                stopWork();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Tools.storeLog(this.context, e.getMessage());
            stopWork(true);
        }
    }

    private boolean syncContacts(JSONArray contacts_ids_array) throws Exception {
        JSONArray contacts = ContactsManager.getContactsValues(this.context, contacts_ids_array);
        for (int i = 0; i < contacts.length(); i++) {
            String result_str;
            JSONObject result;
            int error;
            Log.d("SYNC", "syncContacts");

            ExecutorService executor = Executors.newFixedThreadPool(1);
            String url = this.api_url + "Contacts/Add";
            String data = "user=" + this.api_user +
                    "&token=" + URLEncoder.encode(this.api_token, "utf-8") +
                    "&key=" + URLEncoder.encode(this.api_key, "utf-8") +
                    "&reset=" + false +
                    "&contacts=" + URLEncoder.encode(contacts.getJSONArray(i).toString(), "utf-8") +
                    "&device_id=" + URLEncoder.encode(Tools.getDeviceID(this.context), "utf-8");

            Callable<String> worker = new Api("POST", url, data);
            Future<String> future = executor.submit(worker);
            result_str = future.get();
            result = new JSONObject(result_str);
            error = result.getInt("error");
            if (error != 0) {
                Tools.storeLog(this.context, "syncContacts Error : E" + error);
                return false;
            }
        }
        return true;
    }

    private boolean syncSms(JSONArray sms_ids_array) throws Exception {
        for (int i = 0; i < sms_ids_array.length(); i++) {
            String result_str;
            JSONObject result;
            int error;
            Log.d("SYNC", "syncSms");

            JSONArray list = SmsManager.getSmsValues(this.context, sms_ids_array.getJSONArray(i));
            ExecutorService executor = Executors.newFixedThreadPool(1);
            String url = this.api_url + "Messages/addSmsList";
            String data = "user=" + this.api_user +
                    "&token=" + URLEncoder.encode(this.api_token, "utf-8") +
                    "&key=" + URLEncoder.encode(this.api_key, "utf-8") +
                    "&messages=" + URLEncoder.encode(list.toString(), "utf-8") +
                    "&device_id=" + URLEncoder.encode(Tools.getDeviceID(this.context), "utf-8");

            Callable<String> worker = new Api("POST", url, data);
            Future<String> future = executor.submit(worker);
            result_str = future.get();
            result = new JSONObject(result_str);
            error = result.getInt("error");
            if (error != 0) {
                Tools.storeLog(this.context, "syncSms Error : E" + error);
                return false;
            }
        }
        return true;
    }

    private boolean syncMms(JSONArray mms_ids_array) throws Exception {
        JSONArray list = MmsManager.getMmsValues(this.context, mms_ids_array);

        for (int i = 0; i < list.length(); i++) {
            String result_str;
            JSONObject result;
            int error;
            Log.d("SYNC", "syncSms");
            ExecutorService executor = Executors.newFixedThreadPool(1);
            String url = this.api_url + "Messages/addMmsList";
            String data = "user=" + this.api_user +
                    "&token=" + URLEncoder.encode(this.api_token, "utf-8") +
                    "&key=" + URLEncoder.encode(this.api_key, "utf-8") +
                    "&messages=" + URLEncoder.encode(list.toString(), "utf-8") +
                    "&device_id=" + URLEncoder.encode(Tools.getDeviceID(this.context), "utf-8");

            Callable<String> worker = new Api("POST", url, data);
            Future<String> future = executor.submit(worker);
            result_str = future.get();
            result = new JSONObject(result_str);
            error = result.getInt("error");
            if (error != 0) {
                Tools.storeLog(this.context, "syncMms Error : E" + error);
                return false;
            }
        }
        return true;
    }

    private void validRevision(int rev) {
        String result_str;
        JSONObject result;
        int error;
        Log.d("SYNC", "validRevision");

        try {
            ExecutorService executor = Executors.newFixedThreadPool(1);
            String url = this.api_url + "Users/validRevision";
            String data = "user=" + this.api_user +
                    "&token=" + URLEncoder.encode(this.api_token, "utf-8") +
                    "&key=" + URLEncoder.encode(this.api_key, "utf-8") +
                    "&device_model=" + URLEncoder.encode(android.os.Build.MODEL, "utf-8") +
                    "&revision=" + rev +
                    "&device_id=" + URLEncoder.encode(Tools.getDeviceID(this.context), "utf-8");

            Callable<String> worker = new Api("POST", url, data);
            Future<String> future = executor.submit(worker);
            result_str = future.get();
            result = new JSONObject(result_str);
            error = result.getInt("error");
            if (error != 0) {
                Tools.storeLog(this.context, "validRevision Error : E" + error);
                this.stopWork(true);
            }
        } catch (Exception e) {
            Tools.storeLog(this.context, e.getMessage());
            this.stopWork(true);
        }
    }

    private void getActionsQueue() {
        Log.d("Sync", "GetActionQueue");
        this.stopWork();
    }

    private JSONArray splitter(JSONArray array) throws JSONException {
        JSONArray final_array = new JSONArray();
        JSONArray current_array = new JSONArray();
        int count = array.length();
        for (int i = 0; i < count; i++) {
            current_array.put(array.getInt(i));
            if (i % 100 == 0) {
                final_array.put(current_array);
                current_array = new JSONArray();
            }
        }
        return final_array;
    }
}
