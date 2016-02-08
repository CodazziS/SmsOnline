package fr.codazzi.smsonline.controllers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import fr.codazzi.smsonline.*;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

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
    int nb_access = 0;
    int nb_errors = 0;

    public Api(Context _context) {
        this.context = _context;
    }

    public void Sync() {
        Log.i("SYNCHRONIZATION", "Sync Thread: " + nb_errors + " errors on " + nb_access + " access." );
        if (this.token == null || this.user == null) {
            Log.i("API", "GetToken");
            this.getToken();
        } else {
            resetToken();
            Log.i("API", "ResetToken");
        }
        this.saveState();
    }

    private void callback() {
        if (this.error != 0) {
            this.token = null;
            this.user = null;
        }
        this.saveState();
    }

    private void resetToken() {
        this.token = null;
        this.user = null;
        this.state = 0;
        this.error = 0;
    }

    private void saveState() {
        SharedPreferences settings = this.context.getSharedPreferences("swb_infos", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("error", this.error);
        editor.putInt("state", this.state);
        //@TODO add date
        //editor.putString("password", this.password);
        editor.apply();
    }

    private void addDevice() {
        final Api self = this;
        String android_id = Tools.getDeviceID(this.context);

        Ion.with(context)
            .load(context.getString(R.string.api_url)  + "Devices/Add")
            .setBodyParameter("user", this.user)
            .setBodyParameter("token", this.token)
            .setBodyParameter("android_id", android_id)
            .asJsonObject()
            .setCallback(new FutureCallback<JsonObject>() {
                @Override
                public void onCompleted(Exception e, JsonObject result) {
                    JSONObject res;

                    try {
                        if (result != null) {
                            res = new JSONObject(result.toString());
                            self.error = res.getInt("error");
                        } else {
                            e.printStackTrace();
                            self.error = 2;
                        }
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                        self.error = -1;
                    }
                    self.callback();
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
                nb_access++;
                Ion.with(context)
                    .load(context.getString(R.string.api_url) + "Users/GetToken?email=" + email + "&password=" + password)
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {
                            JSONObject res;
                            int error;

                            try {
                                if (result != null) {
                                    res = new JSONObject(result.toString());
                                    error = res.getInt("error");
                                    if (error == 0) {
                                        self.token = res.getString("token");
                                        self.user = res.getString("user");
                                        self.state = 1;
                                        self.addDevice();
                                    }
                                    self.error = error;
                                } else {
                                    nb_errors++;
                                    e.printStackTrace();
                                    self.error = 2;
                                }
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                                nb_errors++;
                                self.error = -1;
                            }
                            self.callback();
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
