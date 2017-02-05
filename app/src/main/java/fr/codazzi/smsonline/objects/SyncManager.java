package fr.codazzi.smsonline.objects;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import fr.codazzi.smsonline.Tools;

public class SyncManager {

    private Context context;
    private SharedPreferences settings;
    private String token;
    private String api_url;
    private int last_remote_revision;

    public SyncManager(Context _context, SharedPreferences _settings) {
        this.context = _context;
        this.settings = _settings;

        this.token = this.settings.getString("SyncManagerToken", null);
        this.api_url = "https://dev.smsonline.fr/api/";
    }

    private void startWork() {
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putBoolean("SyncManagerWorking", true);
        editor.apply();
    }

    private void stopWork() {
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putBoolean("SyncManagerWorking", false);
        editor.apply();
    }

    public void startSynchronization() {
        if (this.settings.getBoolean("SyncManagerWorking", false)) {
            Tools.storeLog(this.context, "SyncManager Is already working ...");
        }
        this.startWork();
        if (this.token == null) {
            this.getToken();
        } else {
            this.getLastRevision();
        }
    }

    private void getToken() {
        String result_str = null;
        JSONObject result;
        int error;

        try {
            /*String url = this.api_url + "Users/GetToken";
            String email = URLEncoder.encode(this.settings.getString("email", ""), "utf-8");
            String password = URLEncoder.encode(this.settings.getString("password", ""), "utf-8");
            if (email.length() <= 4 || password.length() <= 4) {
                this.check_error(1);
                this.saveSettings();
                return;
            }
            String data = "email=" + email + "&password=" + password + "&type=" + URLEncoder.encode(this.device_id, "utf-8");*/

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
                Log.d("SyncManOK", "GetToken result = " + result_str);
                this.sendRevision();
            } else {
                Log.d("SyncManERROR", "GetToken result = " + error);
                this.stopWork();
            }
        } catch (Exception e) {
            if (result_str != null) {
                Log.i("SyncMan", result_str);
            }
            e.printStackTrace();
            this.stopWork();
        }
    }

    private void getLastRevision() {
        this.stopWork();
    }

    private void sendRevision() {

    }

    private void getActionsQueue() {

        this.stopWork();
    }
}
